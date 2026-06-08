package com.mindharness.service;

import com.mindharness.model.request.CreateChatSessionRequest;
import com.mindharness.model.request.UpdateChatSessionRequest;
import com.mindharness.model.response.CreateChatSessionResponse;
import com.mindharness.model.response.GetChatSessionResponse;
import com.mindharness.model.response.GetChatSessionsResponse;

public interface ChatSessionFacadeService {
    GetChatSessionsResponse getChatSessions();

    GetChatSessionResponse getChatSession(String chatSessionId);

    GetChatSessionsResponse getChatSessionsByAgentId(String agentId);

    CreateChatSessionResponse createChatSession(CreateChatSessionRequest request);

    void deleteChatSession(String chatSessionId);

    void updateChatSession(String chatSessionId, UpdateChatSessionRequest request);
}
