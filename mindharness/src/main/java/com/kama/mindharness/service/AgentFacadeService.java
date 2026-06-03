package com.kama.mindharness.service;

import com.kama.mindharness.model.request.CreateAgentRequest;
import com.kama.mindharness.model.request.UpdateAgentRequest;
import com.kama.mindharness.model.response.CreateAgentResponse;
import com.kama.mindharness.model.response.GetAgentsResponse;

public interface AgentFacadeService {
    GetAgentsResponse getAgents();

    CreateAgentResponse createAgent(CreateAgentRequest request);

    void deleteAgent(String agentId);

    void updateAgent(String agentId, UpdateAgentRequest request);
}
