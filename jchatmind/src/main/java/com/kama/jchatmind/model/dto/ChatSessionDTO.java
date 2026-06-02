package com.kama.jchatmind.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ChatSessionDTO {
    private String id;

    private String agentId;

    private String title;

    private MetaData metadata;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    @Data
    public static class MetaData {
    }
}
