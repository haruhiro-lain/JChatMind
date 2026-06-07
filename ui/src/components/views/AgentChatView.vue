<template>
  <!-- 无 chatSessionId 时显示提示界面 -->
  <EmptyAgentChatView
    v-if="!chatSessionId"
    :agents="agents"
    :loading="loading"
    :chat-mode="chatMode"
    @update:chat-mode="chatMode = $event"
  />

  <!-- 有 chatSessionId 时显示正常聊天界面 -->
  <div v-else class="flex flex-col h-full">
    <!-- 顶部工具栏：智能体选择 -->
    <div class="flex items-center px-4 py-2 border-b border-[rgba(0,229,255,0.08)] bg-[rgba(0,229,255,0.02)] shrink-0">
      <!-- 智能体选择器（Trae 风格） -->
      <a-dropdown :trigger="['click']" placement="bottomLeft" overlay-class-name="agent-selector-dropdown">
        <div class="flex items-center gap-2 cursor-pointer group px-2 py-1.5 -ml-2 rounded-lg hover:bg-[rgba(0,229,255,0.06)] transition-colors">
          <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-500/20 to-purple-500/20 flex items-center justify-center shrink-0 border border-[rgba(0,229,255,0.15)] overflow-hidden">
            <img v-if="currentAgent?.avatar" :src="currentAgent.avatar" class="w-full h-full object-cover" :alt="currentAgent?.name" />
            <span v-else class="text-sm">{{ currentAgentEmoji }}</span>
          </div>
          <div class="flex flex-col min-w-0">
            <span class="text-[10px] text-[#5a7090] leading-none uppercase tracking-wider">当前智能体</span>
            <span class="text-[13px] font-semibold text-[#ecf1fa] truncate max-w-[140px] leading-tight">{{ currentAgent?.name || '选择智能体' }}</span>
          </div>
          <DownOutlined class="text-[10px] text-[#5a7090] group-hover:text-[#00e5ff] transition-colors" />
        </div>
        <template #overlay>
          <div class="agent-dropdown-panel">
            <div class="px-3 py-2 text-[11px] text-[#5a7090] font-medium uppercase tracking-wider">选择智能体</div>
            <div class="max-h-[280px] overflow-y-auto px-2 pb-2">
              <div
                v-for="agent in agentsWithEmoji"
                :key="agent.id"
                class="flex items-center gap-3 px-3 py-2.5 rounded-lg cursor-pointer transition-colors mb-0.5"
                :class="agent.id === agentId ? 'bg-[rgba(0,229,255,0.08)] border border-[rgba(0,229,255,0.2)]' : 'hover:bg-[rgba(0,229,255,0.04)] border border-transparent'"
                @click="handleAgentSelectClick(agent.id)"
              >
                <div class="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-500/20 to-purple-500/20 flex items-center justify-center shrink-0 border border-[rgba(0,229,255,0.15)] overflow-hidden">
                  <img v-if="agent.avatar" :src="agent.avatar" class="w-full h-full object-cover" :alt="agent.name" />
                  <span v-else class="text-base">{{ agent.emoji }}</span>
                </div>
                <div class="flex-1 min-w-0">
                  <div class="text-[13px] font-semibold text-[#ecf1fa] truncate">{{ agent.name }}</div>
                  <div v-if="agent.description" class="text-[11px] text-[#5a7090] truncate mt-0.5">{{ agent.description }}</div>
                </div>
                <CheckCircleFilled v-if="agent.id === agentId" class="text-[#00e5ff] text-sm shrink-0" />
              </div>
            </div>
          </div>
        </template>
      </a-dropdown>
    </div>

    <AgentChatHistory
      :messages="messages"
      :agent-name="currentAgent?.name"
      :agent-avatar="currentAgent?.avatar"
      :display-agent-status="displayAgentStatus"
      :agent-status-text="agentStatusText"
      :agent-status-type="agentStatusType"
    />
    <AgentChatInput
      :chat-mode="chatMode"
      :sending="sending"
      @send="handleSendMessage"
      @update:chat-mode="chatMode = $event"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { message } from "ant-design-vue";
import { DownOutlined, CheckCircleFilled } from "@ant-design/icons-vue";
import AgentChatHistory from "./agentChatView/AgentChatHistory.vue";
import AgentChatInput from "./agentChatView/AgentChatInput.vue";
import EmptyAgentChatView from "./agentChatView/EmptyAgentChatView.vue";
import { createChatMessage, createChatSession, getChatMessagesBySessionId, getChatSession, updateChatSession } from "../../api/api";
import { BASE_URL } from "../../api/http";
import { useAgents } from "../../composables/useAgents";
import { useChatSessions } from "../../composables/useChatSessions";
import { getAgentEmoji } from "../../utils";
import type { ChatMessageVO, SseMessage, SseMessageType, ChatMode } from "../../types";

const route = useRoute();
const router = useRouter();
const { agents } = useAgents();
const { refreshChatSessions } = useChatSessions();

// ========== 模式 ==========
const chatMode = ref<ChatMode>("agent");

// ========== 智能体 ==========
const agentsWithEmoji = computed(() =>
  agents.value.map((a) => ({
    ...a,
    emoji: getAgentEmoji(a.id),
  }))
);

const currentAgent = computed(() =>
  agents.value.find((a) => a.id === agentId.value)
);

const currentAgentEmoji = computed(() =>
  currentAgent.value ? getAgentEmoji(currentAgent.value.id) : "🤖"
);

const loading = ref(false);
const sending = ref(false);
const messages = ref<ChatMessageVO[]>([]);
const agentId = ref("");

const displayAgentStatus = ref(false);
const agentStatusText = ref("");
const agentStatusType = ref<SseMessageType | undefined>(undefined);

const chatSessionId = ref<string | undefined>(undefined);

// 智能体切换
const switchingAgent = ref(false);

async function handleAgentSelectClick(newAgentId: string) {
  if (!chatSessionId.value || newAgentId === agentId.value) return;
  switchingAgent.value = true;
  try {
    await updateChatSession(chatSessionId.value, { agentId: newAgentId });
    await refreshChatSessions();
    agentId.value = newAgentId;
    message.success("已切换智能体");
  } catch {
    message.error("切换智能体失败");
    const sessionResp = await getChatSession(chatSessionId.value);
    agentId.value = sessionResp.chatSession.agentId;
  } finally {
    switchingAgent.value = false;
  }
}

// 监听路由参数变化
watch(
  () => route.params.chatSessionId,
  (newId) => {
    chatSessionId.value = newId as string | undefined;
  },
  { immediate: true }
);

function addMessage(message: ChatMessageVO) {
  messages.value = [...messages.value, message];
}

async function fetchMessages() {
  if (!chatSessionId.value) return;
  try {
    const resp = await getChatMessagesBySessionId(chatSessionId.value);
    messages.value = resp.chatMessages;
    const sessionResp = await getChatSession(chatSessionId.value);
    agentId.value = sessionResp.chatSession.agentId;
  } catch {
    // 会话不存在或已删除，回到首页
    message.warning("会话不存在");
    chatSessionId.value = undefined;
    router.replace("/");
  }
}

watch(chatSessionId, (newId) => {
  if (newId) fetchMessages();
});

async function handleSendMessage(data: { text: string }) {
  const text = data.text;
  if (!text || !text.trim()) return;
  sending.value = true;

  if (!chatSessionId.value) {
    if (!agentId.value) {
      if (agents.value.length > 0) {
        agentId.value = agents.value[0].id;
      } else {
        message.warning("请先创建一个智能体助手");
        sending.value = false;
        return;
      }
    }
    loading.value = true;
    try {
      const resp = await createChatSession({ agentId: agentId.value, title: text.slice(0, 20) });
      const newSessionId = resp.chatSessionId;

      await createChatMessage({
        agentId: agentId.value ?? "",
        sessionId: newSessionId,
        role: "user",
        content: text,
      });

      await refreshChatSessions();
      chatSessionId.value = newSessionId;
      router.replace(`/chat/${newSessionId}`);
      await fetchMessages();
    } catch {
      message.error("发送消息失败，请重试");
    } finally {
      loading.value = false;
      sending.value = false;
    }
  } else {
    try {
      await createChatMessage({
        agentId: agentId.value ?? "",
        sessionId: chatSessionId.value,
        role: "user",
        content: text,
      });
      await fetchMessages();
    } catch {
      message.error("发送消息失败，请重试");
    } finally {
      sending.value = false;
    }
  }
}

// SSE 连接
let es: EventSource | null = null;

watch(chatSessionId, (newId, oldId) => {
  if (oldId && es) {
    es.close();
    es = null;
  }
  if (!newId) return;

  const sseBaseUrl = BASE_URL.replace(/\/api$/, "");
  es = new EventSource(`${sseBaseUrl}/sse/connect/${newId}`);

  es.addEventListener("message", (event) => {
    const msg = JSON.parse(event.data) as SseMessage;
    if (msg.type === "AI_GENERATED_CONTENT") {
      addMessage(msg.payload.message);
    } else if (msg.type === "AI_PLANNING") {
      displayAgentStatus.value = true;
      agentStatusText.value = msg.payload.statusText;
      agentStatusType.value = "AI_PLANNING";
    } else if (msg.type === "AI_THINKING") {
      displayAgentStatus.value = true;
      agentStatusText.value = msg.payload.statusText;
      agentStatusType.value = "AI_THINKING";
    } else if (msg.type === "AI_EXECUTING") {
      displayAgentStatus.value = true;
      agentStatusText.value = msg.payload.statusText;
      agentStatusType.value = "AI_EXECUTING";
    } else if (msg.type === "AI_DONE") {
      displayAgentStatus.value = false;
      agentStatusText.value = "";
      agentStatusType.value = undefined;
    } else if (msg.type === "AI_ERROR") {
      displayAgentStatus.value = true;
      agentStatusText.value = msg.payload.statusText || "AI 服务异常";
      agentStatusType.value = "AI_ERROR";
    }
  });

  es.onerror = (error) => {
    console.error("SSE error:", error);
  };
});

onUnmounted(() => {
  if (es) {
    es.close();
  }
});
</script>

<style scoped>
@reference "tailwindcss";

/* Agent 下拉面板样式（Trae 风格） */
:deep(.agent-selector-dropdown) {
  @apply !p-0;
}
:deep(.agent-selector-dropdown .ant-dropdown-menu) {
  @apply !p-0 !rounded-xl;
}

.agent-dropdown-panel {
  @apply bg-[#0e1422] border border-[rgba(0,229,255,0.15)] rounded-xl shadow-2xl shadow-black/40 w-[280px];
}

.mode-dropdown-panel {
  @apply bg-[#0e1422] border border-[rgba(0,229,255,0.15)] rounded-xl shadow-2xl shadow-black/40 w-[220px] py-1;
}
</style>
