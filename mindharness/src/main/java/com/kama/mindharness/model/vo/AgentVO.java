package com.kama.mindharness.model.vo;

import com.kama.mindharness.model.dto.AgentDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AgentVO {
    private String id;

    private String name;

    private String description;

    private String systemPrompt;

    private AgentDTO.ModelType model;

    private List<String> allowedTools;

    private AgentDTO.ChatOptions chatOptions;

    private String apiKey;

    /** 头像访问路径 */
    private String avatar;
}
