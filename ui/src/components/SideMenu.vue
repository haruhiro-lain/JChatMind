<template>
  <div class="flex flex-col h-full">
    <!-- Logo 区域 -->
    <div class="h-14 w-full flex items-center px-5 border-b border-gray-100 dark:border-[rgba(0,229,255,0.2)] transition-colors duration-300">
      <div class="flex items-center gap-2.5">
        <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shadow-sm">
          <RobotOutlined class="text-white text-base" />
        </div>
        <div class="flex flex-col leading-tight">
          <span class="text-[15px] font-semibold text-gray-900 dark:text-[#ecf1fa] tracking-tight">JChatMind</span>
          <span class="text-[10px] text-gray-400 dark:text-[#5a7090] font-medium tracking-wide">AI AGENT PLATFORM</span>
        </div>
      </div>
    </div>

    <!-- Tabs 区域 -->
    <div class="flex-1 min-h-0 flex flex-col px-4">
      <a-tabs v-model:activeKey="activeKey" class="pt-3">
        <a-tab-pane key="agent" tab="助手">
          <AgentTabContent
            :agents="agents"
            @create-agent-click="toggleAddAgentModal"
            @edit-agent="onEditAgent"
            @delete-agent="onDeleteAgent"
          />
        </a-tab-pane>
        <a-tab-pane key="chat" tab="会话">
          <ChatTabContent />
        </a-tab-pane>
      </a-tabs>
    </div>

    <AddAgentModal
      :open="isAddAgentModalOpen"
      :editing-agent="editingAgent"
      @close="toggleAddAgentModal"
      @create="onCreateAgent"
      @update="onUpdateAgent"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import { RobotOutlined } from "@ant-design/icons-vue";
import AgentTabContent from "./tabs/AgentTabContent.vue";
import ChatTabContent from "./tabs/ChatTabContent.vue";
import AddAgentModal from "./modals/AddAgentModal.vue";
import { useAgents } from "../composables/useAgents.ts";
import type { AgentVO, CreateAgentRequest, UpdateAgentRequest } from "../api/api.ts";

const route = useRoute();
const { agents, createAgentHandle, deleteAgentHandle, updateAgentHandle } = useAgents();

const isAddAgentModalOpen = ref(false);
const editingAgent = ref<AgentVO | null>(null);

const activeKey = ref("agent");

onMounted(() => {
  if (route.path.startsWith("/agent")) activeKey.value = "agent";
  else if (route.path.startsWith("/chat")) activeKey.value = "chat";
  else activeKey.value = "agent";
});

function toggleAddAgentModal() {
  isAddAgentModalOpen.value = !isAddAgentModalOpen.value;
  if (!isAddAgentModalOpen.value) editingAgent.value = null;
}

function onEditAgent(agent: AgentVO) {
  editingAgent.value = agent;
  isAddAgentModalOpen.value = true;
}

function onDeleteAgent(agentId: string) {
  deleteAgentHandle(agentId);
}

async function onCreateAgent(request: CreateAgentRequest) {
  await createAgentHandle(request);
  isAddAgentModalOpen.value = false;
}

async function onUpdateAgent(agentId: string, request: UpdateAgentRequest) {
  if (updateAgentHandle) {
    await updateAgentHandle(agentId, request);
  }
  isAddAgentModalOpen.value = false;
}
</script>
