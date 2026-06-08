package com.mindharness.lsp;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简易代码分析器（内建回退方案）
 * <p>
 * 当外部 Language Server 不可用时，使用正则匹配提供基本的诊断能力。
 * 支持的语言：TypeScript/JavaScript, Java, Python
 * <p>
 * 面试价值：此分析器与真实 LSP Client 共享相同的返回格式（{@link LspMessage.Diagnostic}），
 * 体现了"策略模式"设计思想——上层调用方不关心底层是真实 LS 还是正则分析器。
 */
@Slf4j
public class LspSimpleAnalyzer {

    private static final Pattern TS_ERROR_PATTERN = Pattern.compile(
            "\\b(var\\s+\\w+\\s*=\\s*\\d+)\\b"  // var 声明（应使用 const/let）
    );

    @SuppressWarnings("unused")
    private static final Pattern JAVA_SEMICOLON_PATTERN = Pattern.compile(
            "(\\S+\\s+\\w+\\s*=\\s*[^;\\n]+)(?=\\n|$)"  // 赋值语句缺少分号
    );

    private static final Pattern TODO_PATTERN = Pattern.compile(
            "//\\s*TODO", Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CONSOLE_LOG_PATTERN = Pattern.compile(
            "console\\.log"
    );

    /**
     * 根据 languageId 选择分析策略
     */
    public List<LspMessage.Diagnostic> analyze(String code, String languageId) {
        return switch (languageId.toLowerCase()) {
            case "typescript", "javascript" -> analyzeTypeScript(code);
            case "java" -> analyzeJava(code);
            case "python" -> analyzePython(code);
            default -> analyzeGeneric(code);
        };
    }

    /**
     * TypeScript/JavaScript 分析
     */
    private List<LspMessage.Diagnostic> analyzeTypeScript(String code) {
        List<LspMessage.Diagnostic> diagnostics = new ArrayList<>();
        String[] lines = code.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 检测 var 声明（推荐用 const/let）
            Matcher varMatcher = TS_ERROR_PATTERN.matcher(line);
            if (varMatcher.find()) {
                diagnostics.add(LspMessage.Diagnostic.builder()
                        .range(LspMessage.Range.builder()
                                .start(LspMessage.Position.builder().line(i).character(line.indexOf("var")).build())
                                .end(LspMessage.Position.builder().line(i).character(line.indexOf("var") + 3).build())
                                .build())
                        .severity(3) // Info
                        .message("建议使用 'const' 或 'let' 替代 'var'（ES6 最佳实践）")
                        .source("LspSimpleAnalyzer")
                        .build());
            }

            // 检测 console.log
            Matcher consoleMatcher = CONSOLE_LOG_PATTERN.matcher(line);
            if (consoleMatcher.find()) {
                diagnostics.add(LspMessage.Diagnostic.builder()
                        .range(LspMessage.Range.builder()
                                .start(LspMessage.Position.builder().line(i).character(line.indexOf("console")).build())
                                .end(LspMessage.Position.builder().line(i).character(line.indexOf("console") + 11).build())
                                .build())
                        .severity(2) // Warning
                        .message("生产代码中建议移除 console.log，使用统一的日志框架")
                        .source("LspSimpleAnalyzer")
                        .build());
            }
        }

        // 检测 TODO 注释
        Matcher todoMatcher = TODO_PATTERN.matcher(code);
        while (todoMatcher.find()) {
            int line = countLinesBefore(code, todoMatcher.start());
            diagnostics.add(LspMessage.Diagnostic.builder()
                    .range(LspMessage.Range.builder()
                            .start(LspMessage.Position.builder().line(line).character(0).build())
                            .end(LspMessage.Position.builder().line(line).character(0).build())
                            .build())
                    .severity(3) // Info
                    .message("存在 TODO 注释，需要后续处理")
                    .source("LspSimpleAnalyzer")
                    .build());
        }

        return diagnostics;
    }

    /**
     * Java 分析
     */
    private List<LspMessage.Diagnostic> analyzeJava(String code) {
        List<LspMessage.Diagnostic> diagnostics = new ArrayList<>();
        String[] lines = code.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 检测 System.out.println
            if (line.contains("System.out.println") || line.contains("System.out.print")) {
                diagnostics.add(LspMessage.Diagnostic.builder()
                        .range(LspMessage.Range.builder()
                                .start(LspMessage.Position.builder().line(i).character(line.indexOf("System")).build())
                                .end(LspMessage.Position.builder().line(i).character(line.indexOf("System") + 18).build())
                                .build())
                        .severity(2) // Warning
                        .message("建议使用 Slf4j/Log4j 日志框架替代 System.out.println")
                        .source("LspSimpleAnalyzer")
                        .build());
            }
        }

        // 检测 TODO 注释
        Matcher todoMatcher = TODO_PATTERN.matcher(code);
        while (todoMatcher.find()) {
            int line = countLinesBefore(code, todoMatcher.start());
            diagnostics.add(LspMessage.Diagnostic.builder()
                    .range(LspMessage.Range.builder()
                            .start(LspMessage.Position.builder().line(line).character(0).build())
                            .end(LspMessage.Position.builder().line(line).character(0).build())
                            .build())
                    .severity(3) // Info
                    .message("存在 TODO 注释")
                    .source("LspSimpleAnalyzer")
                    .build());
        }

        return diagnostics;
    }

    /**
     * Python 分析
     */
    private List<LspMessage.Diagnostic> analyzePython(String code) {
        List<LspMessage.Diagnostic> diagnostics = new ArrayList<>();
        String[] lines = code.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 检测 print 语句
            if (line.trim().startsWith("print ")) {
                diagnostics.add(LspMessage.Diagnostic.builder()
                        .range(LspMessage.Range.builder()
                                .start(LspMessage.Position.builder().line(i).character(line.indexOf("print")).build())
                                .end(LspMessage.Position.builder().line(i).character(line.indexOf("print") + 5).build())
                                .build())
                        .severity(2) // Warning
                        .message("建议使用 logging 模块替代 print")
                        .source("LspSimpleAnalyzer")
                        .build());
            }
        }

        return diagnostics;
    }

    /**
     * 通用分析
     */
    private List<LspMessage.Diagnostic> analyzeGeneric(String code) {
        List<LspMessage.Diagnostic> diagnostics = new ArrayList<>();

        // 只检测 TODO
        Matcher todoMatcher = TODO_PATTERN.matcher(code);
        while (todoMatcher.find()) {
            int line = countLinesBefore(code, todoMatcher.start());
            diagnostics.add(LspMessage.Diagnostic.builder()
                    .range(LspMessage.Range.builder()
                            .start(LspMessage.Position.builder().line(line).character(0).build())
                            .end(LspMessage.Position.builder().line(line).character(0).build())
                            .build())
                    .severity(3) // Info
                    .message("存在 TODO 注释")
                    .source("LspSimpleAnalyzer")
                    .build());
        }

        return diagnostics;
    }

    /**
     * 计算字符偏移之前的行数
     */
    private int countLinesBefore(String text, int offset) {
        int count = 0;
        for (int i = 0; i < offset && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }
}
