package com.kama.mindharness.model.request;

import com.kama.mindharness.model.dto.AgentDTO;
import lombok.Data;

import java.util.List;

@Data
public class UpdateAgentRequest {
    private String name;
    private String description;
    private String systemPrompt;
    private String model;
    private List<String> allowedTools;
    private AgentDTO.ChatOptions chatOptions;
    private String apiKey;
}
