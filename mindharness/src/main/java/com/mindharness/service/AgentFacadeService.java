package com.mindharness.service;

import com.mindharness.model.request.CreateAgentRequest;
import com.mindharness.model.request.UpdateAgentRequest;
import com.mindharness.model.response.CreateAgentResponse;
import com.mindharness.model.response.GetAgentsResponse;

public interface AgentFacadeService {
    GetAgentsResponse getAgents();

    CreateAgentResponse createAgent(CreateAgentRequest request);

    void deleteAgent(String agentId);

    void updateAgent(String agentId, UpdateAgentRequest request);
}
