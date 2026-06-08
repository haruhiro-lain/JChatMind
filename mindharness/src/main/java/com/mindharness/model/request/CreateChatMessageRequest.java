package com.mindharness.model.request;

import com.mindharness.model.dto.ChatMessageDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateChatMessageRequest {
    private String agentId;
    private String sessionId;
    private ChatMessageDTO.RoleType role;
    private String content;
    private ChatMessageDTO.MetaData metadata;

    /**
     * 用户自定义的 API Key（可选）。
     * 当用户在前端手动输入 API Key 时，将覆盖 application.yaml 中的预设 Key。
     */
    private String apiKey;
}
