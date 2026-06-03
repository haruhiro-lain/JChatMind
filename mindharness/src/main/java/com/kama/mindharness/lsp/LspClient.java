package com.kama.mindharness.lsp;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LSP（Language Server Protocol）客户端
 * <p>
 * 核心职责：
 * <ol>
 *   <li>管理 Language Server 进程生命周期（启动/停止）</li>
 *   <li>实现 JSON-RPC 2.0 over stdin/stdout 通信</li>
 *   <li>解析 LSP 协议帧（Content-Length header + JSON body）</li>
 *   <li>封装常用 LSP 方法：initialize, didOpen, diagnostic, hover, shutdown</li>
 * </ol>
 * <p>
 * 面试要点：
 * <ul>
 *   <li>LSP 基于 JSON-RPC 2.0，使用 Content-Length 头部进行消息分帧</li>
 *   <li>Language Server 作为独立进程运行，通过 stdin/stdout 通信</li>
 *   <li>必须先 initialize → initialized 握手，才能发送后续请求</li>
 *   <li>textDocument/didOpen 通知服务器打开文档</li>
 *   <li>诊断结果通过 textDocument/publishDiagnostics 通知推送</li>
 * </ul>
 */
@Slf4j
public class LspClient implements AutoCloseable {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * 等待 initialize 响应的超时时间
     */
    private static final long INIT_TIMEOUT_SECONDS = 15;

    /**
     * 请求 ID 自增
     */
    private final AtomicInteger requestId = new AtomicInteger(1);

    /**
     * 缓存的诊断结果（由 publishDiagnostics 通知推送）
     */
    private final Map<String, List<LspMessage.Diagnostic>> diagnosticCache = new ConcurrentHashMap<>();

    /**
     * 未完成的请求（id → CompletableFuture）
     */
    private final Map<Object, CompletableFuture<LspMessage.Response>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * Language Server 进程
     */
    private Process process;

    /**
     * 向 LS 写入的流
     */
    private BufferedWriter writer;

    /**
     * 后台读取 LS 响应的线程
     */
    private Thread readerThread;

    /**
     * 是否已初始化
     */
    private volatile boolean initialized = false;

    /**
     * 启动 Language Server 并完成初始化握手
     *
     * @param serverCommand 启动命令，如 ["typescript-language-server", "--stdio"]
     * @param rootUri       项目根路径 URI
     */
    public void start(List<String> serverCommand, String rootUri) throws IOException {
        log.info("[LSP] 启动 Language Server: {}", String.join(" ", serverCommand));

        ProcessBuilder pb = new ProcessBuilder(serverCommand);
        pb.redirectErrorStream(false);  // stderr 单独处理
        this.process = pb.start();

        // 启动 stderr 读取线程（用于日志）
        Thread stderrReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("[LSP-stderr] {}", line);
                }
            } catch (IOException ignored) {
            }
        }, "lsp-stderr-reader");
        stderrReader.setDaemon(true);
        stderrReader.start();

        this.writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

        // 启动响应读取线程
        this.readerThread = new Thread(this::readResponses, "lsp-reader");
        this.readerThread.setDaemon(true);
        this.readerThread.start();

        // 发送 initialize 请求
        Map<String, Object> capabilities = new LinkedHashMap<>();
        Map<String, Object> textDocument = new LinkedHashMap<>();
        textDocument.put("hover", Map.of("contentFormat", List.of("markdown", "plaintext")));
        capabilities.put("textDocument", textDocument);

        LspMessage.InitializeParams initParams = LspMessage.InitializeParams.builder()
                .processId((int) ProcessHandle.current().pid())
                .rootUri(rootUri)
                .capabilities(capabilities)
                .build();

        LspMessage.Response initResponse = sendRequest("initialize", initParams, INIT_TIMEOUT_SECONDS);
        LspMessage.InitializeResult initResult = OBJECT_MAPPER.convertValue(
                initResponse.getResult(), LspMessage.InitializeResult.class);

        log.info("[LSP] Initialize 成功, 服务器能力: {}", initResult.getCapabilities());

        // 发送 initialized 通知
        sendNotification("initialized", Map.of());

        this.initialized = true;
        log.info("[LSP] Language Server 初始化完成");
    }

    /**
     * 打开一个文档（通知 Language Server）
     */
    public void didOpen(String uri, String languageId, String text) {
        ensureInitialized();
        LspMessage.DidOpenTextDocumentParams params = LspMessage.DidOpenTextDocumentParams.builder()
                .textDocument(LspMessage.TextDocumentItem.builder()
                        .uri(uri)
                        .languageId(languageId)
                        .version(1)
                        .text(text)
                        .build())
                .build();
        sendNotification("textDocument/didOpen", params);
        log.debug("[LSP] didOpen: {} ({})", uri, languageId);
    }

    /**
     * 获取文档诊断结果（从 publishDiagnostics 缓存读取）
     */
    public List<LspMessage.Diagnostic> getDiagnostics(String uri) {
        return diagnosticCache.getOrDefault(uri, Collections.emptyList());
    }

    /**
     * 请求悬停信息（Hover）
     */
    public LspMessage.Hover hover(String uri, int line, int character) {
        ensureInitialized();
        try {
            LspMessage.HoverParams params = LspMessage.HoverParams.builder()
                    .textDocument(LspMessage.TextDocumentIdentifier.builder().uri(uri).build())
                    .position(LspMessage.Position.builder().line(line).character(character).build())
                    .build();
            LspMessage.Response response = sendRequest("textDocument/hover", params, 5);
            if (response.getResult() == null) {
                return null;
            }
            return OBJECT_MAPPER.convertValue(response.getResult(), LspMessage.Hover.class);
        } catch (Exception e) {
            log.warn("[LSP] Hover 请求失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== JSON-RPC 通信实现 ====================

    /**
     * 发送 JSON-RPC Request 并等待响应
     */
    private LspMessage.Response sendRequest(String method, Object params, long timeoutSeconds) {
        int id = requestId.getAndIncrement();
        LspMessage.Request request = LspMessage.Request.builder()
                .id(id)
                .method(method)
                .params(params)
                .build();

        CompletableFuture<LspMessage.Response> future = new CompletableFuture<>();
        pendingRequests.put(id, future);

        try {
            String json = OBJECT_MAPPER.writeValueAsString(request);
            writeMessage(json);

            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            pendingRequests.remove(id);
            throw new RuntimeException("LSP 请求超时: " + method, e);
        } catch (Exception e) {
            pendingRequests.remove(id);
            throw new RuntimeException("LSP 请求失败: " + method, e);
        }
    }

    /**
     * 发送 JSON-RPC Notification（无需回复）
     */
    private void sendNotification(String method, Object params) {
        LspMessage.Notification notification = LspMessage.Notification.builder()
                .method(method)
                .params(params)
                .build();
        try {
            String json = OBJECT_MAPPER.writeValueAsString(notification);
            writeMessage(json);
        } catch (Exception e) {
            log.error("[LSP] 发送 Notification 失败: {}", method, e);
        }
    }

    /**
     * 写入消息（使用 Content-Length header 分帧）
     */
    private synchronized void writeMessage(String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        String header = "Content-Length: " + bytes.length + "\r\n\r\n";
        writer.write(header);
        writer.write(json);
        writer.flush();
    }

    /**
     * 后台线程：持续读取 Language Server 的响应
     */
    private void readResponses() {
        try (InputStream input = process.getInputStream();
             BufferedInputStream bufferedInput = new BufferedInputStream(input)) {

            while (!Thread.currentThread().isInterrupted()) {
                // 读取 Content-Length header
                int contentLength = readContentLength(bufferedInput);
                if (contentLength < 0) {
                    break; // EOF
                }

                // 读取 JSON body
                byte[] bodyBytes = new byte[contentLength];
                int bytesRead = 0;
                while (bytesRead < contentLength) {
                    int n = bufferedInput.read(bodyBytes, bytesRead, contentLength - bytesRead);
                    if (n < 0) {
                        return; // EOF
                    }
                    bytesRead += n;
                }

                String json = new String(bodyBytes, StandardCharsets.UTF_8);
                handleResponse(json);
            }
        } catch (EOFException e) {
            log.info("[LSP] Language Server 连接关闭");
        } catch (IOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                log.error("[LSP] 读取响应异常: {}", e.getMessage());
            }
        }
    }

    /**
     * 从输入流中读取 Content-Length 头部值
     */
    private int readContentLength(InputStream input) throws IOException {
        StringBuilder header = new StringBuilder();
        int prev = -1, curr;
        int crlfcrlf = 0; // 检测 \r\n\r\n

        while ((curr = input.read()) != -1) {
            header.append((char) curr);
            if (prev == '\r' && curr == '\n') {
                crlfcrlf++;
                if (crlfcrlf >= 2 && header.toString().endsWith("\r\n\r\n")) {
                    break;
                }
            } else if (curr != '\r') {
                crlfcrlf = 0;
            }
            prev = curr;
        }

        if (header.isEmpty()) {
            return -1; // EOF
        }

        String headerStr = header.toString();
        for (String line : headerStr.split("\r\n")) {
            if (line.toLowerCase().startsWith("content-length:")) {
                return Integer.parseInt(line.substring("content-length:".length()).trim());
            }
        }
        throw new IOException("无法解析 Content-Length header: " + headerStr);
    }

    /**
     * 处理收到的 JSON-RPC 消息
     */
    private void handleResponse(String json) {
        try {
            // 尝试解析为 Response（有 id）
            @SuppressWarnings("unchecked")
            Map<String, Object> map = OBJECT_MAPPER.readValue(json, Map.class);
            Object id = map.get("id");
            String method = (String) map.get("method");

            if (id != null) {
                // 这是一个 Response
                LspMessage.Response response = OBJECT_MAPPER.readValue(json, LspMessage.Response.class);
                CompletableFuture<LspMessage.Response> future = pendingRequests.remove(id);
                if (future != null) {
                    future.complete(response);
                }
            } else if (method != null && "textDocument/publishDiagnostics".equals(method)) {
                // 这是一个 publishDiagnostics 通知
                handlePublishDiagnostics(map);
            }
            // 其他通知忽略

        } catch (Exception e) {
            log.error("[LSP] 解析响应失败: {}", e.getMessage());
            log.debug("[LSP] 原始 JSON: {}", json);
        }
    }

    /**
     * 处理 publishDiagnostics 通知，缓存诊断结果
     */
    @SuppressWarnings("unchecked")
    private void handlePublishDiagnostics(Map<String, Object> map) {
        try {
            Map<String, Object> params = (Map<String, Object>) map.get("params");
            if (params == null) return;

            String uri = (String) params.get("uri");
            List<Map<String, Object>> diagnosticsRaw = (List<Map<String, Object>>) params.get("diagnostics");

            if (uri != null && diagnosticsRaw != null) {
                List<LspMessage.Diagnostic> diagnostics = diagnosticsRaw.stream()
                        .map(d -> OBJECT_MAPPER.convertValue(d, LspMessage.Diagnostic.class))
                        .toList();
                diagnosticCache.put(uri, diagnostics);
                log.debug("[LSP] 收到诊断通知: uri={}, count={}", uri, diagnostics.size());
            }
        } catch (Exception e) {
            log.warn("[LSP] 解析 publishDiagnostics 失败: {}", e.getMessage());
        }
    }

    // ==================== 生命周期管理 ====================

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("LSP 客户端尚未初始化，请先调用 start()");
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 发送 shutdown + exit，关闭 Language Server
     */
    @Override
    public void close() {
        try {
            if (initialized) {
                sendRequest("shutdown", null, 5);
                sendNotification("exit", null);
            }
        } catch (Exception e) {
            log.warn("[LSP] Shutdown 异常: {}", e.getMessage());
        }

        if (readerThread != null) {
            readerThread.interrupt();
        }
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
        log.info("[LSP] Language Server 已关闭");
    }
}
