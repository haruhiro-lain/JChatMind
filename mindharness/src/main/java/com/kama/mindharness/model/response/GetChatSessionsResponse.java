package com.kama.mindharness.model.response;

import com.kama.mindharness.model.vo.ChatSessionVO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetChatSessionsResponse {
    private ChatSessionVO[] chatSessions;
}
