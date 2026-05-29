<template>
  <div class="flex flex-col h-full">
    <a-button type="primary" class="w-full mb-3 h-9 rounded-lg text-[13px] font-medium" @click="handleCreateNewChat">
      <template #icon><PlusOutlined /></template>
      新建会话
    </a-button>
    <div class="flex-1 min-h-0 overflow-y-auto rounded-xl bg-gray-50/50 dark:bg-[#0e1422]/60 p-2 transition-colors duration-300">
      <!-- 加载中 -->
      <div v-if="loading" class="flex flex-col items-center justify-center h-full text-gray-300 dark:text-gray-600">
        <p class="text-[13px] font-medium">加载中...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="chatSessions.length === 0" class="flex flex-col items-center justify-center h-full text-gray-300 dark:text-gray-600">
        <MessageOutlined class="text-3xl mb-3 opacity-50" />
        <p class="text-[13px] font-medium">暂无会话记录</p>
        <p class="text-[11px] mt-1">点击上方按钮创建</p>
      </div>

      <!-- 会话列表 -->
      <div v-else class="space-y-1.5">
        <div
          v-for="session in chatSessions"
          :key="session.id"
          @click="handleSelectChatSession(session.id)"
          class="w-full px-3.5 py-3 rounded-xl bg-white dark:bg-[#0e1422]/70 cursor-pointer card-hover border border-gray-100 dark:border-[rgba(0,229,255,0.22)] group relative transition-colors duration-300"
        >
          <div class="flex items-start gap-3">
            <div class="w-9 h-9 rounded-xl bg-gradient-to-br from-blue-50 to-indigo-50 flex items-center justify-center shrink-0 border border-blue-100">
              <MessageOutlined class="text-blue-500 dark:text-[#00e5ff] text-sm" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="font-semibold text-[13px] text-gray-900 dark:text-[#ecf1fa] truncate leading-tight">
                {{ getDisplayTitle(session) }}
              </div>
            </div>
            <a-popconfirm
              title="确定要删除这条聊天记录吗？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="handleDeleteChatSession(session.id)"
            >
              <a-button type="text" size="small" class="shrink-0 opacity-0 group-hover:opacity-100">
                <template #icon><DeleteOutlined /></template>
              </a-button>
            </a-popconfirm>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRouter } from "vue-router";
import { PlusOutlined, MessageOutlined, DeleteOutlined } from "@ant-design/icons-vue";
import { useChatSessions } from "../../composables/useChatSessions";
import { getAgents } from "../../api/api";
import { ref, onMounted } from "vue";
import type { AgentVO } from "../../api/api";

const router = useRouter();
const { chatSessions, loading, deleteChatSession } = useChatSessions();
const agents = ref<AgentVO[]>([]);

onMounted(async () => {
  try {
    const resp = await getAgents();
    agents.value = resp.agents;
  } catch { /* ignore */ }
});

const agentMap = computed(() => {
  const map = new Map<string, string>();
  agents.value.forEach((agent) => {
    map.set(agent.id, agent.name);
  });
  return map;
});

function getDisplayTitle(session: { title?: string; agentId: string }) {
  if (session.title) return session.title;
  const agentName = agentMap.value.get(session.agentId);
  return agentName ? `与 ${agentName} 的对话` : "新对话";
}

function handleCreateNewChat() {
  router.push("/chat");
}

function handleSelectChatSession(chatSessionId: string) {
  router.push(`/chat/${chatSessionId}`);
}

async function handleDeleteChatSession(chatSessionId: string) {
  await deleteChatSession(chatSessionId);
}
</script>

<style scoped>
.group:hover .group-hover\:opacity-100 {
  opacity: 1 !important;
}
</style>
