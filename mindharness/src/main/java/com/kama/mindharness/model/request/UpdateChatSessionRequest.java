package com.kama.mindharness.model.request;

import lombok.Data;

@Data
public class UpdateChatSessionRequest {
    private String title;
    private String agentId;
}
