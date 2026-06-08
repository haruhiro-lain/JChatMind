package com.mindharness.model.request;

import com.mindharness.model.dto.AgentDTO;
import lombok.Data;

import java.util.List;

@Data
public class CreateAgentRequest {
    private String name;
    private String description;
    private String systemPrompt;
    private String model;
    private List<String> allowedTools;
    private AgentDTO.ChatOptions chatOptions;
    private String apiKey;
    /** 头像访问路径（如 /avatars/xxx.png） */
    private String avatar;
}
