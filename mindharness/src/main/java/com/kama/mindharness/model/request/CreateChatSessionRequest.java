package com.kama.mindharness.model.request;

import lombok.Data;

@Data
public class CreateChatSessionRequest {
    private String agentId;
    private String title;
}
