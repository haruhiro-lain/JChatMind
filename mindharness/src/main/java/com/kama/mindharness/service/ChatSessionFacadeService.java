package com.kama.mindharness.service;

import com.kama.mindharness.model.request.CreateChatSessionRequest;
import com.kama.mindharness.model.request.UpdateChatSessionRequest;
import com.kama.mindharness.model.response.CreateChatSessionResponse;
import com.kama.mindharness.model.response.GetChatSessionResponse;
import com.kama.mindharness.model.response.GetChatSessionsResponse;

public interface ChatSessionFacadeService {
    GetChatSessionsResponse getChatSessions();

    GetChatSessionResponse getChatSession(String chatSessionId);

    GetChatSessionsResponse getChatSessionsByAgentId(String agentId);

    CreateChatSessionResponse createChatSession(CreateChatSessionRequest request);

    void deleteChatSession(String chatSessionId);

    void updateChatSession(String chatSessionId, UpdateChatSessionRequest request);
}
