<template>
  <!-- 无 chatSessionId 时显示提示界面 -->
  <EmptyAgentChatView
    v-if="!chatSessionId"
    :agents="agents"
    :loading="loading"
  />

  <!-- 有 chatSessionId 时显示正常聊天界面 -->
  <div v-else class="flex flex-col h-full">
    <AgentChatHistory
      :messages="messages"
      :display-agent-status="displayAgentStatus"
      :agent-status-text="agentStatusText"
      :agent-status-type="agentStatusType"
    />
    <AgentChatInput @send="handleSendMessage" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { message } from "ant-design-vue";
import AgentChatHistory from "./agentChatView/AgentChatHistory.vue";
import AgentChatInput from "./agentChatView/AgentChatInput.vue";
import EmptyAgentChatView from "./agentChatView/EmptyAgentChatView.vue";
import { createChatMessage, createChatSession, getChatMessagesBySessionId, getChatSession } from "../../api/api";
import { BASE_URL } from "../../api/http";
import { useAgents } from "../../composables/useAgents";
import { useChatSessions } from "../../composables/useChatSessions";
import type { ChatMessageVO, SseMessage, SseMessageType } from "../../types";

const route = useRoute();
const router = useRouter();
const { agents } = useAgents();
const { refreshChatSessions } = useChatSessions();

const loading = ref(false);
const messages = ref<ChatMessageVO[]>([]);
const agentId = ref("");

const displayAgentStatus = ref(false);
const agentStatusText = ref("");
const agentStatusType = ref<SseMessageType | undefined>(undefined);

const chatSessionId = ref<string | undefined>(undefined);

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
  const resp = await getChatMessagesBySessionId(chatSessionId.value);
  messages.value = resp.chatMessages;
  const sessionResp = await getChatSession(chatSessionId.value);
  agentId.value = sessionResp.chatSession.agentId;
}

watch(chatSessionId, (newId) => {
  if (newId) fetchMessages();
});

async function handleSendMessage(data: { text: string }) {
  const text = data.text;
  if (!text || !text.trim()) return;

  if (!chatSessionId.value) {
    if (!agentId.value) {
      // 取第一个 agent
      if (agents.value.length > 0) {
        agentId.value = agents.value[0].id;
      } else {
        message.warning("请先创建一个智能体助手");
        return;
      }
    }
    loading.value = true;
    try {
      const resp = await createChatSession({ agentId: agentId.value, title: text.slice(0, 20) });
      await refreshChatSessions();
      router.replace(`/chat/${resp.chatSessionId}`);
    } catch {
      message.error("创建聊天会话失败，请重试");
    } finally {
      loading.value = false;
    }
  } else {
    await createChatMessage({
      agentId: agentId.value ?? "",
      sessionId: chatSessionId.value,
      role: "user",
      content: text,
    });
    await fetchMessages();
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
