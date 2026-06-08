package com.mindharness.message;

import com.mindharness.model.vo.ChatMessageVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class SseMessage {

    private Type type;
    private Payload payload;
    private Metadata metadata;

    @Data
    @AllArgsConstructor
    @Builder
    public static class Payload {
        private ChatMessageVO message;
        private String statusText;
        private Boolean done;
        /** 流式输出的增量文本 */
        private String delta;
        /** 流式输出对应的消息 ID */
        private String messageId;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class Metadata {
        private String chatMessageId;
    }

    // 自定义消息类型
    // 1. AI 流式增量
    // 2. AI 生成完成
    // 3. AI 规划中
    // 4. AI 思考中
    // 5. AI 执行中
    // 6. AI 完成
    // 7. AI 出错
    public enum Type {
        AI_STREAMING,
        AI_GENERATED_CONTENT,
        AI_PLANNING,
        AI_THINKING,
        AI_EXECUTING,
        AI_DONE,
        AI_ERROR,
    }
}
