package com.kama.mindharness.model.response;

import com.kama.mindharness.model.vo.ChatMessageVO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetChatMessagesResponse {
    private ChatMessageVO[] chatMessages;
}

