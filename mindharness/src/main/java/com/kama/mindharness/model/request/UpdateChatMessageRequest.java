package com.kama.mindharness.model.request;

import com.kama.mindharness.model.dto.ChatMessageDTO;
import lombok.Data;

@Data
public class UpdateChatMessageRequest {
    private String content;
    private ChatMessageDTO.MetaData metadata;
}

