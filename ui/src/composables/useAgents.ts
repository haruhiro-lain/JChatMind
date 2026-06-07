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

// 模块级共享状态，所有调用者共用同一个 agents 列表
const agents = ref<AgentVO[]>([]);
let initialized = false;

export function useAgents() {

  async function refreshAgents() {
    const resp = await getAgents();
    agents.value = resp.agents;
  }

  if (!initialized) {
    initialized = true;
    refreshAgents();
  }

  async function createAgentHandle(agent: CreateAgentRequest) {
    const resp = await createAgent(agent);
    // 乐观更新：直接用表单数据拼出完整对象，省掉 refreshAgents 的额外请求
    const newAgent: AgentVO = {
      id: resp.agentId,
      name: agent.name,
      description: agent.description,
      systemPrompt: agent.systemPrompt,
      model: agent.model,
      allowedTools: agent.allowedTools,
      chatOptions: agent.chatOptions,
      apiKey: agent.apiKey,
      avatar: agent.avatar,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    agents.value = [newAgent, ...agents.value];
  }

  async function deleteAgentHandle(agentId: string) {
    await deleteAgent(agentId);
    // 乐观更新：直接从本地列表移除
    agents.value = agents.value.filter((a) => a.id !== agentId);
  }

  async function updateAgentHandle(
    agentId: string,
    request: UpdateAgentRequest,
  ) {
    await updateAgent(agentId, request);
    // 乐观更新：直接更新本地列表中的对应项
    agents.value = agents.value.map((a) =>
      a.id === agentId ? { ...a, ...request, updatedAt: new Date().toISOString() } : a
    );
  }

  return {
    agents,
    createAgentHandle,
    deleteAgentHandle,
    updateAgentHandle,
    refreshAgents,
  };
}
