package com.kama.jchatmind.agent.tools;

import com.kama.jchatmind.lsp.LspMessage;
import com.kama.jchatmind.lsp.LspService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * LSP 代码分析工具
 * <p>
 * 将 LSP 能力以 Tool 的形式接入 JChatMind Agent 体系。
 * Agent（LLM）可以自主决定何时调用本工具来分析用户提供的代码。
 * <p>
 * 实现模式与 {@link DataBaseTools} 一致：
 * <ul>
 *   <li>实现 {@link Tool} 接口 → 纳入 JChatMind 工具管理</li>
 *   <li>{@code @Component} → Spring 自动注入到 {@code ToolFacadeService}</li>
 *   <li>{@code @Tool} 注解方法 → Spring AI 自动生成 {@code ToolCallback}</li>
 * </ul>
 * <p>
 * 支持策略：
 * <ol>
 *   <li>真实 Language Server（如 typescript-language-server）—— 完整 LSP 诊断</li>
 *   <li>内建正则分析器 —— 无外部依赖回退方案</li>
 * </ol>
 *
 * @see LspService
 * @see DataBaseTools
 */
@Component
@Slf4j
public class LspTool implements Tool {

    private final LspService lspService;

    public LspTool(LspService lspService) {
        this.lspService = lspService;
    }

    @Override
    public String getName() {
        return "lspTool";
    }

    @Override
    public String getDescription() {
        return "代码分析工具，基于 LSP（Language Server Protocol）协议对代码进行诊断分析。" +
                "可以检测代码中的问题（警告、错误、提示），以及查询特定位置的符号信息。";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL; // Agent 可选择是否启用
    }

    /**
     * 分析代码并返回诊断信息
     * <p>
     * 此方法由 LLM 通过 function calling 调用。
     * 返回格式化的 Markdown 诊断报告，便于直接展示给用户。
     *
     * @param code       要分析的源代码（完整内容）
     * @param languageId 编程语言标识（如 "typescript", "javascript", "java", "python"）
     * @return Markdown 格式的诊断报告
     */
    @org.springframework.ai.tool.annotation.Tool(
            name = "analyzeCode",
            description = "使用 LSP 协议分析代码。" +
                    "参数 code: 要分析的完整源代码文本。" +
                    "参数 languageId: 编程语言，支持 typescript/javascript/java/python。" +
                    "返回诊断结果（警告、错误、提示）的 Markdown 报告。"
    )
    public String analyzeCode(String code, String languageId) {
        log.info("[LspTool] analyzeCode 被调用, languageId: {}, codeLength: {}", languageId, code.length());

        try {
            List<LspMessage.Diagnostic> diagnostics = lspService.analyzeDiagnostics(code, languageId);

            if (diagnostics.isEmpty()) {
                return "✅ **代码分析完成**\n\n" +
                        "语言: `" + languageId + "`\n" +
                        "未发现任何问题，代码看起来不错！";
            }

            // 按严重程度分组统计
            long errors = diagnostics.stream().filter(d -> d.getSeverity() == 1).count();
            long warnings = diagnostics.stream().filter(d -> d.getSeverity() == 2).count();
            long infos = diagnostics.stream().filter(d -> d.getSeverity() >= 3).count();

            StringBuilder report = new StringBuilder();
            report.append("## 📋 代码诊断报告\n\n");
            report.append("**语言**: `").append(languageId).append("`  \n");
            report.append("**统计**: ");
            if (errors > 0) report.append("🔴 ").append(errors).append(" 个错误 ");
            if (warnings > 0) report.append("🟡 ").append(warnings).append(" 个警告 ");
            if (infos > 0) report.append("🔵 ").append(infos).append(" 个提示 ");
            report.append("\n\n---\n\n");

            for (int i = 0; i < diagnostics.size(); i++) {
                LspMessage.Diagnostic d = diagnostics.get(i);
                String icon = switch (d.getSeverity()) {
                    case 1 -> "🔴";
                    case 2 -> "🟡";
                    case 3 -> "🔵";
                    case 4 -> "💡";
                    default -> "❓";
                };

                report.append("### ").append(i + 1).append(". ").append(icon).append(" ")
                        .append(LspMessage.severityLabel(d.getSeverity())).append("\n");

                if (d.getRange() != null) {
                    int startLine = d.getRange().getStart().getLine() + 1; // LSP 行号从 0 开始
                    int startChar = d.getRange().getStart().getCharacter();
                    report.append("- **位置**: 第 ").append(startLine).append(" 行, 第 ")
                            .append(startChar).append(" 列\n");
                }
                report.append("- **描述**: ").append(d.getMessage()).append("\n");
                if (d.getCode() != null) {
                    report.append("- **规则**: `").append(d.getCode()).append("`\n");
                }
                if (d.getSource() != null) {
                    report.append("- **来源**: ").append(d.getSource()).append("\n");
                }
                report.append("\n");
            }

            return report.toString();
        } catch (Exception e) {
            log.error("[LspTool] 代码分析异常", e);
            return "❌ **代码分析失败**\n\n" +
                    "错误信息: " + e.getMessage() + "\n" +
                    "请检查 languageId 是否正确，支持的类型: typescript, javascript, java, python";
        }
    }

    /**
     * 查询指定位置的悬停信息（类型、文档等）
     *
     * @param code       源代码内容
     * @param languageId 编程语言
     * @param line       行号（从 1 开始）
     * @param character  列号（从 0 开始）
     * @return 悬停信息
     */
    @org.springframework.ai.tool.annotation.Tool(
            name = "getHoverInfo",
            description = "查询代码中指定位置的符号信息（类型、文档注释等），基于 LSP Hover 请求。" +
                    "参数 code: 源代码文本。" +
                    "参数 languageId: 编程语言。" +
                    "参数 line: 行号（从 1 开始）。" +
                    "参数 character: 列号（从 0 开始）。" +
                    "返回该位置的类型信息或文档说明。"
    )
    public String getHoverInfo(String code, String languageId, int line, int character) {
        log.info("[LspTool] getHoverInfo 被调用, languageId: {}, line: {}, char: {}", languageId, line, character);

        try {
            // LSP 行号从 0 开始
            String result = lspService.hover(code, languageId, line - 1, character);
            return "## 🔍 悬停信息\n\n" +
                    "**位置**: 第 " + line + " 行, 第 " + character + " 列  \n" +
                    "**语言**: `" + languageId + "`\n\n" +
                    result;
        } catch (Exception e) {
            log.error("[LspTool] Hover 查询异常", e);
            return "❌ **悬停查询失败**: " + e.getMessage();
        }
    }
}
