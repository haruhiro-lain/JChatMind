import { ref } from "vue";
import {
  type AgentVO,
  createAgent,
  type CreateAgentRequest,
  getAgents,
  deleteAgent,
  updateAgent,
  type UpdateAgentRequest,
} from "../api/api.ts";

export function useAgents() {
  const agents = ref<AgentVO[]>([]);

  async function refreshAgents() {
    const resp = await getAgents();
    agents.value = resp.agents;
  }

  refreshAgents();

  async function createAgentHandle(agent: CreateAgentRequest) {
    await createAgent(agent);
    await refreshAgents();
  }

  async function deleteAgentHandle(agentId: string) {
    await deleteAgent(agentId);
    await refreshAgents();
  }

  async function updateAgentHandle(
    agentId: string,
    request: UpdateAgentRequest,
  ) {
    await updateAgent(agentId, request);
    await refreshAgents();
  }

  return {
    agents,
    createAgentHandle,
    deleteAgentHandle,
    updateAgentHandle,
    refreshAgents,
  };
}
