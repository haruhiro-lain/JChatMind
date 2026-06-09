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
      :streaming-message-id="streamingMessageId"
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
import { createChatMessage, createChatSession, getChatSession, updateChatSession } from "../../api/api";
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

/** 当前正在流式输出的消息 ID（无流式时为 null） */
const streamingMessageId = ref<string | null>(null);

const chatSessionId = ref<string | undefined>(undefined);

// 智能体切换
const switchingAgent = ref(false);

/** 用于取消正在进行的 fetch 请求，防止切换会话时的竞态条件 */
let fetchAbortController: AbortController | null = null;

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

/** 清除 Agent 状态指示器 */
function clearAgentStatus() {
  displayAgentStatus.value = false;
  agentStatusText.value = "";
  agentStatusType.value = undefined;
}

/**
 * 取消正在进行的 fetch 请求
 */
function cancelPendingFetch() {
  if (fetchAbortController) {
    fetchAbortController.abort();
    fetchAbortController = null;
  }
}

async function fetchMessages() {
  if (!chatSessionId.value) return;

  // 如果正在流式输出，跳过全量刷新以免覆盖流式内容
  if (streamingMessageId.value) return;

  // 取消之前的请求，防止旧数据覆盖新数据
  cancelPendingFetch();
  fetchAbortController = new AbortController();

  const currentSessionId = chatSessionId.value;
  loading.value = true;
  try {
    // 使用自定义 fetch 以便支持 AbortController
    const url = `${BASE_URL}/chat-messages/session/${currentSessionId}`;
    const response = await fetch(url, {
      headers: { "Content-Type": "application/json" },
      cache: "no-cache",
      signal: fetchAbortController.signal,
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const apiResp = await response.json();
    if (apiResp.code !== 200) {
      throw new Error(apiResp.message || "请求失败");
    }

    // 只有当前会话 ID 没变时才更新数据
    if (chatSessionId.value !== currentSessionId) return;
    messages.value = apiResp.data.chatMessages;

    // 同时获取会话信息
    const sessionResp = await getChatSession(currentSessionId);
    if (chatSessionId.value !== currentSessionId) return;
    agentId.value = sessionResp.chatSession.agentId;
  } catch (err: unknown) {
    if (err instanceof DOMException && err.name === "AbortError") {
      // 请求被取消，忽略
      return;
    }
    // 会话不存在或已删除，回到首页
    if (chatSessionId.value === currentSessionId) {
      message.warning("会话不存在");
      chatSessionId.value = undefined;
      router.replace("/");
    }
  } finally {
    if (chatSessionId.value === currentSessionId) {
      loading.value = false;
    }
    if (fetchAbortController?.signal.aborted === false) {
      fetchAbortController = null;
    }
  }
}

watch(chatSessionId, (newId, oldId) => {
  // 切换会话时先清空消息列表，避免闪现旧数据
  if (oldId !== newId) {
    messages.value = [];
    agentId.value = "";
    streamingMessageId.value = null;
    displayAgentStatus.value = false;
    agentStatusText.value = "";
    agentStatusType.value = undefined;
  }
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
      const resp = await createChatMessage({
        agentId: agentId.value ?? "",
        sessionId: chatSessionId.value,
        role: "user",
        content: text,
      });
      // 先本地追加用户消息获得即时反馈，再刷新消息列表作为安全回退
      // SSE 会实时推送 AI 回复，fetchMessages 确保 SSE 断开时数据不丢失
      addMessage({
        id: resp.chatMessageId,
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
/** 当前 SSE 连接的会话 ID，用于过滤过期事件 */
let sseSessionId: string | null = null;

function closeSseConnection() {
  if (es) {
    es.close();
    es = null;
  }
  sseSessionId = null;
}

watch(chatSessionId, (newId) => {
  // 关闭旧连接
  closeSseConnection();

  if (!newId) return;

  // 记录当前 SSE 连接的会话 ID
  sseSessionId = newId;
  const currentSseSessionId = newId;

  const sseBaseUrl = BASE_URL.replace(/\/api$/, "");
  es = new EventSource(`${sseBaseUrl}/sse/connect/${newId}`);

  es.addEventListener("message", (event) => {
    // 忽略来自旧会话的事件
    if (sseSessionId !== currentSseSessionId) return;

    try {
      const msg = JSON.parse(event.data) as SseMessage;
      handleSseMessage(msg, currentSseSessionId);
    } catch {
      // JSON 解析失败，忽略
    }
  });

  es.onerror = () => {
    // SSE 连接出错，如果是当前会话则尝试重连
    if (sseSessionId === currentSseSessionId && es && es.readyState === EventSource.CLOSED) {
      // EventSource 已关闭，延迟后重连
      setTimeout(() => {
        if (chatSessionId.value === currentSseSessionId) {
          closeSseConnection();
          sseSessionId = currentSseSessionId;
          es = new EventSource(`${sseBaseUrl}/sse/connect/${currentSseSessionId}`);
          es.addEventListener("message", (event) => {
            if (sseSessionId !== currentSseSessionId) return;
            try {
              const msg = JSON.parse(event.data) as SseMessage;
              handleSseMessage(msg, currentSseSessionId);
            } catch { /* ignore */ }
          });
          es.onerror = () => {
            if (sseSessionId === currentSseSessionId) {
              closeSseConnection();
            }
          };
        }
      }, 2000);
    }
  };
});

/**
 * 处理 SSE 消息（提取为独立函数，便于重连时复用）
 */
function handleSseMessage(msg: SseMessage, sessionId: string) {
  if (msg.type === "AI_STREAMING") {
    // 流式增量：追加文本到现有消息或创建新泡泡
    displayAgentStatus.value = false;
    const delta = msg.payload.delta || "";
    const mId = msg.payload.messageId || "";
    streamingMessageId.value = mId || null;
    const existing = messages.value.find((m) => m.id === mId);
    if (existing) {
      existing.content += delta;
    } else if (delta) {
      messages.value = [...messages.value, {
        id: mId,
        sessionId: sessionId,
        role: "assistant",
        content: delta,
      }];
    }
  } else if (msg.type === "AI_GENERATED_CONTENT") {
    // 完整消息：先移除流式泡泡，再添加/替换真实消息
    const currentStreamingId = streamingMessageId.value;
    streamingMessageId.value = null;

    // 移除流式临时泡泡
    if (currentStreamingId) {
      const streamIdx = messages.value.findIndex((m) => m.id === currentStreamingId);
      if (streamIdx >= 0) {
        messages.value.splice(streamIdx, 1);
      }
    }

    const mId = msg.metadata?.chatMessageId || msg.payload.message?.id;
    if (mId && msg.payload.message) {
      const idx = messages.value.findIndex((m) => m.id === mId);
      if (idx >= 0) {
        messages.value[idx] = msg.payload.message;
      } else {
        addMessage(msg.payload.message);
      }
    } else if (msg.payload.message) {
      addMessage(msg.payload.message);
    }
  } else if (msg.type === "AI_PLANNING") {
    if (msg.payload.done) {
      clearAgentStatus();
    } else {
      displayAgentStatus.value = true;
      agentStatusText.value = msg.payload.statusText;
      agentStatusType.value = "AI_PLANNING";
    }
  } else if (msg.type === "AI_THINKING") {
    if (msg.payload.done) {
      clearAgentStatus();
    } else {
      displayAgentStatus.value = true;
      agentStatusText.value = msg.payload.statusText;
      agentStatusType.value = "AI_THINKING";
    }
  } else if (msg.type === "AI_EXECUTING") {
    if (msg.payload.done) {
      clearAgentStatus();
    } else {
      displayAgentStatus.value = true;
      agentStatusText.value = msg.payload.statusText;
      agentStatusType.value = "AI_EXECUTING";
    }
  } else if (msg.type === "AI_DONE") {
    clearAgentStatus();
  } else if (msg.type === "AI_ERROR") {
    displayAgentStatus.value = true;
    agentStatusText.value = msg.payload.statusText || "AI 服务异常";
    agentStatusType.value = "AI_ERROR";
  }
}

onUnmounted(() => {
  cancelPendingFetch();
  closeSseConnection();
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
