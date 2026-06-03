package com.kama.mindharness.lsp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * JSON-RPC 2.0 消息定义（LSP 协议底层通信格式）
 * <p>
 * LSP 基于 JSON-RPC 2.0，消息分为三类：
 * <ul>
 *   <li>Request：有 id，期望回复</li>
 *   <li>Response：携带 id，对应某个 Request 的结果</li>
 *   <li>Notification：无 id，不需要回复</li>
 * </ul>
 */
public class LspMessage {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Request {
        @Builder.Default
        private String jsonrpc = "2.0";
        private Object id;
        private String method;
        private Object params;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        @Builder.Default
        private String jsonrpc = "2.0";
        private Object id;
        private Object result;
        private ResponseError error;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResponseError {
        private int code;
        private String message;
        private Object data;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Notification {
        @Builder.Default
        private String jsonrpc = "2.0";
        private String method;
        private Object params;
    }

    // ==================== LSP 协议参数/结果类型 ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InitializeParams {
        private Integer processId;
        private String rootUri;
        private Map<String, Object> capabilities;
        private String workspaceFolders;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InitializeResult {
        private Map<String, Object> capabilities;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TextDocumentItem {
        private String uri;
        private String languageId;
        private int version;
        private String text;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DidOpenTextDocumentParams {
        private TextDocumentItem textDocument;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Position {
        private int line;
        private int character;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Range {
        private Position start;
        private Position end;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Diagnostic {
        private Range range;
        private int severity;   // 1=Error, 2=Warning, 3=Info, 4=Hint
        private String code;
        private String source;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TextDocumentIdentifier {
        private String uri;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HoverParams {
        private TextDocumentIdentifier textDocument;
        private Position position;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Hover {
        private MarkupContent contents;
        private Range range;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MarkupContent {
        private String kind;    // "markdown" or "plaintext"
        private String value;
    }

    /**
     * LSP code 转可读标签
     */
    public static String severityLabel(int severity) {
        return switch (severity) {
            case 1 -> "Error";
            case 2 -> "Warning";
            case 3 -> "Info";
            case 4 -> "Hint";
            default -> "Unknown";
        };
    }
}
