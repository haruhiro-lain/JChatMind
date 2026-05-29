package com.kama.jchatmind.lsp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * LSP 服务编排层
 * <p>
 * 职责：
 * <ol>
 *   <li>管理 {@link LspClient} 生命周期</li>
 *   <li>编码检测 / 回退到 {@link LspSimpleAnalyzer}</li>
 *   <li>提供统一的分析入口</li>
 * </ol>
 * <p>
 * 策略：优先尝试真实 Language Server；不可用时自动回退到内置正则分析器。
 */
@Slf4j
@Service
public class LspService {

    /**
     * 文件的 LSP URI 前缀（内存文件，非真实路径）
     */
    private static final String FILE_URI_PREFIX = "file:///memory/";

    /**
     * languageId 与 Language Server 命令的映射
     */
    private static final Map<String, List<String>> SERVER_COMMANDS = Map.of(
            "typescript", List.of("typescript-language-server", "--stdio"),
            "javascript", List.of("typescript-language-server", "--stdio"),
            "typescriptreact", List.of("typescript-language-server", "--stdio"),
            "javascriptreact", List.of("typescript-language-server", "--stdio")
    );

    private final Object lock = new Object();
    private final LspSimpleAnalyzer fallbackAnalyzer = new LspSimpleAnalyzer();

    private LspClient activeClient;
    private String activeLanguageId;

    /**
     * 分析代码并返回诊断结果
     *
     * @param code       源代码内容
     * @param languageId 语言 ID（typescript, javascript, java, python 等）
     * @return 诊断结果列表
     */
    public List<LspMessage.Diagnostic> analyzeDiagnostics(String code, String languageId) {
        // 1. 尝试使用真实 Language Server
        List<LspMessage.Diagnostic> realDiagnostics = analyzeWithRealServer(code, languageId);
        if (realDiagnostics != null) {
            log.info("[LspService] 使用真实 Language Server 分析, 诊断数: {}", realDiagnostics.size());
            return realDiagnostics;
        }

        // 2. 回退到内置分析器
        log.info("[LspService] 回退到内置分析器, languageId: {}", languageId);
        return fallbackAnalyzer.analyze(code, languageId);
    }

    /**
     * 获取指定位置的悬停信息
     */
    public String hover(String code, String languageId, int line, int character) {
        LspClient client = getOrCreateClient(languageId);
        if (client == null) {
            return "（内建分析器不支持悬停信息，请安装对应 Language Server）";
        }

        try {
            String uri = openDocument(client, code, languageId);
            LspMessage.Hover hover = client.hover(uri, line, character);
            if (hover != null && hover.getContents() != null) {
                return hover.getContents().getValue();
            }
            return "（该位置无悬停信息）";
        } catch (Exception e) {
            log.warn("[LspService] Hover 失败: {}", e.getMessage());
            return "（悬停查询失败: " + e.getMessage() + "）";
        }
    }

    // ==================== 内部实现 ====================

    /**
     * 尝试通过真实 Language Server 分析
     *
     * @return 诊断列表，如果 LS 不可用返回 null
     */
    private List<LspMessage.Diagnostic> analyzeWithRealServer(String code, String languageId) {
        try {
            LspClient client = getOrCreateClient(languageId);
            if (client == null) return null;

            String uri = openDocument(client, code, languageId);

            // 等待诊断通知（publishDiagnostics 是异步推送的）
            Thread.sleep(500);

            List<LspMessage.Diagnostic> diagnostics = client.getDiagnostics(uri);
            return diagnostics != null ? diagnostics : Collections.emptyList();
        } catch (Exception e) {
            log.warn("[LspService] 真实 Language Server 分析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取或创建 LSP 客户端（如果支持该语言）
     */
    private LspClient getOrCreateClient(String languageId) {
        List<String> command = SERVER_COMMANDS.get(languageId.toLowerCase());
        if (command == null) {
            return null; // 不支持的语言
        }

        synchronized (lock) {
            // 复用已有客户端（同语言）
            if (activeClient != null && activeClient.isInitialized()
                    && languageId.equalsIgnoreCase(activeLanguageId)) {
                return activeClient;
            }

            // 关闭旧客户端
            closeClient();

            // 创建新客户端
            try {
                LspClient client = new LspClient();
                client.start(command, "file:///project");
                activeClient = client;
                activeLanguageId = languageId;
                return client;
            } catch (IOException e) {
                log.warn("[LspService] 无法启动 Language Server ({}): {}",
                        String.join(" ", command), e.getMessage());
                return null;
            }
        }
    }

    /**
     * 打开文档并返回 URI
     */
    private String openDocument(LspClient client, String code, String languageId) {
        String uri = FILE_URI_PREFIX + UUID.randomUUID() + resolveExtension(languageId);
        client.didOpen(uri, languageId, code);
        return uri;
    }

    /**
     * 根据 languageId 返回文件扩展名
     */
    private String resolveExtension(String languageId) {
        return switch (languageId.toLowerCase()) {
            case "typescript" -> ".ts";
            case "javascript" -> ".js";
            case "typescriptreact" -> ".tsx";
            case "javascriptreact" -> ".jsx";
            case "java" -> ".java";
            case "python" -> ".py";
            default -> ".txt";
        };
    }

    private void closeClient() {
        if (activeClient != null) {
            try {
                activeClient.close();
            } catch (Exception e) {
                log.warn("[LspService] 关闭客户端异常: {}", e.getMessage());
            }
            activeClient = null;
            activeLanguageId = null;
        }
    }
}
