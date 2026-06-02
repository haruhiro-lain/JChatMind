<template>
  <div class="flex flex-col h-full">
    <!-- Agent 选择器 -->
    <div class="border-b border-[rgba(0,229,255,0.08)] bg-[rgba(0,229,255,0.02)] px-4 py-2 transition-colors duration-300">
      <!-- Agent 选择器（Trae 风格） -->
      <a-dropdown v-if="agents.length > 0" :trigger="['click']" placement="bottomLeft">
        <div class="flex items-center gap-2 cursor-pointer group px-2 py-1.5 -ml-2 rounded-lg hover:bg-[rgba(0,229,255,0.06)] transition-colors">
          <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-500/20 to-purple-500/20 flex items-center justify-center shrink-0 border border-[rgba(0,229,255,0.15)]">
            <RobotOutlined class="text-[#00e5ff] text-sm" />
          </div>
          <span class="text-[13px] font-semibold text-[#ecf1fa]">{{ selectedAgentLabel || '选择智能体' }}</span>
          <DownOutlined class="text-[10px] text-[#5a7090] group-hover:text-[#00e5ff] transition-colors" />
        </div>
        <template #overlay>
          <div class="agent-dropdown-panel">
            <div class="px-3 py-2 text-[11px] text-[#5a7090] font-medium uppercase tracking-wider">选择智能体</div>
            <div class="max-h-[240px] overflow-y-auto px-2 pb-2">
              <div
                v-for="agent in agentsWithEmoji"
                :key="agent.id"
                class="flex items-center gap-3 px-3 py-2.5 rounded-lg cursor-pointer transition-colors mb-0.5"
                :class="agent.id === selectedAgentId ? 'bg-[rgba(0,229,255,0.08)] border border-[rgba(0,229,255,0.2)]' : 'hover:bg-[rgba(0,229,255,0.04)] border border-transparent'"
                @click="selectedAgentId = agent.id"
              >
                <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-500/20 to-purple-500/20 flex items-center justify-center shrink-0 border border-[rgba(0,229,255,0.15)] overflow-hidden">
                  <img v-if="agent.avatar" :src="agent.avatar" class="w-full h-full object-cover" :alt="agent.name" />
                  <span v-else class="text-sm">{{ agent.emoji }}</span>
                </div>
                <div class="flex-1 min-w-0">
                  <div class="text-[13px] font-semibold text-[#ecf1fa] truncate">{{ agent.name }}</div>
                  <div v-if="agent.description" class="text-[11px] text-[#5a7090] truncate mt-0.5">{{ agent.description }}</div>
                </div>
                <CheckCircleFilled v-if="agent.id === selectedAgentId" class="text-[#00e5ff] text-sm shrink-0" />
              </div>
            </div>
          </div>
        </template>
      </a-dropdown>
    </div>

    <div class="flex-1 flex items-center justify-center p-6">
      <div class="max-w-xl w-full space-y-5">
        <!-- 标题区域 -->
        <div class="text-center mb-6">
          <div class="w-16 h-16 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center mx-auto mb-4 shadow-lg shadow-indigo-500/20">
            <RobotOutlined class="text-white text-2xl" />
          </div>
          <h2 class="!mb-1 !text-[22px] !font-bold !text-[#ecf1fa] !tracking-tight">
            {{ modeTitle }}
          </h2>
          <p class="!text-[14px] !text-[#5a7090]">
            {{ modeSubtitle }}
          </p>
        </div>

        <!-- 特性卡片（根据模式变化） -->
        <div class="space-y-3">
          <div
            v-for="card in modeCards"
            :key="card.title"
            class="p-4 rounded-xl border border-[rgba(0,229,255,0.1)] bg-[#0e1422]/70 card-hover cursor-default transition-colors duration-300"
          >
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-xl flex items-center justify-center border shrink-0"
                :class="card.iconBg">
                <component :is="card.icon" :class="card.iconColor + ' text-lg'" />
              </div>
              <div>
                <div class="text-[14px] font-semibold text-[#ecf1fa]">{{ card.title }}</div>
                <div class="text-[12px] text-[#5a7090] mt-0.5">{{ card.desc }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="border-t border-[rgba(0,229,255,0.1)] bg-[#090d17]/95 transition-colors duration-300">
      <div class="px-4 pt-3">
        <a-textarea
          v-model:value="messageText"
          :placeholder="modePlaceholder"
          :auto-size="{ minRows: 1, maxRows: 6 }"
          @keydown.enter.exact.prevent="handleSend"
          class="empty-chat-input"
        />
      </div>
      <div class="flex items-center justify-between px-4 pb-3 pt-2">
        <!-- 模式切换下拉列表 -->
        <a-dropdown :trigger="['click']" placement="topLeft">
          <div class="flex items-center gap-1.5 cursor-pointer px-3 py-1.5 rounded-md border border-[rgba(0,229,255,0.1)] bg-[rgba(0,229,255,0.04)] hover:bg-[rgba(0,229,255,0.08)] transition-colors">
            <component :is="currentModeOption.icon" class="text-[#00e5ff] text-[13px]" />
            <span class="text-xs font-medium text-[#8ba4c0]">{{ currentModeOption.label }}</span>
            <DownOutlined class="text-[9px] text-[#5a7090]" />
          </div>
          <template #overlay>
            <div class="mode-dropdown-panel">
              <div
                v-for="mode in modeOptions"
                :key="mode.key"
                class="flex items-center gap-2.5 px-3 py-2 rounded-lg cursor-pointer transition-colors"
                :class="chatMode === mode.key ? 'bg-[rgba(0,229,255,0.06)]' : 'hover:bg-[rgba(0,229,255,0.03)]'"
                @click="$emit('update:chatMode', mode.key)"
              >
                <component :is="mode.icon" class="text-[14px]" :class="chatMode === mode.key ? 'text-[#00e5ff]' : 'text-[#5a7090]'" />
                <div class="flex-1">
                  <div class="text-[13px] font-medium" :class="chatMode === mode.key ? 'text-[#00e5ff]' : 'text-[#ecf1fa]'">{{ mode.label }}</div>
                  <div class="text-[11px] text-[#5a7090] mt-0.5">{{ mode.desc }}</div>
                </div>
                <CheckCircleFilled v-if="chatMode === mode.key" class="text-[#00e5ff] text-xs shrink-0" />
              </div>
            </div>
          </template>
        </a-dropdown>
        <a-button
          type="primary"
          :loading="loading"
          :disabled="!messageText.trim() || !effectiveAgentId"
          @click="handleSend"
          class="send-btn"
        >
          <SendOutlined class="text-sm" />
          <span class="ml-1.5 text-xs font-medium">发送</span>
        </a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import { RobotOutlined, BulbOutlined, SearchOutlined, MessageOutlined, SendOutlined, DownOutlined, CheckCircleFilled, ThunderboltOutlined, CompassOutlined, ExperimentOutlined } from "@ant-design/icons-vue";
import type { AgentVO } from "../../../api/api";
import type { ChatMode } from "../../../types";
import { createChatMessage, createChatSession } from "../../../api/api";
import { useChatSessions } from "../../../composables/useChatSessions";
import { getAgentEmoji } from "../../../utils";

const props = defineProps<{
  agents: AgentVO[];
  loading: boolean;
  chatMode: ChatMode;
}>();

defineEmits<{
  "update:chatMode": [mode: ChatMode];
}>();

const router = useRouter();
const { refreshChatSessions } = useChatSessions();

const messageText = ref("");
const selectedAgentId = ref<string | null>(null);

const modeOptions = [
  { key: "ask" as ChatMode, label: "Ask", icon: SearchOutlined, desc: "快速问答，不调用工具" },
  { key: "agent" as ChatMode, label: "Agent", icon: RobotOutlined, desc: "智能体自主调用工具完成任务" },
  { key: "plan" as ChatMode, label: "Plan", icon: BulbOutlined, desc: "先生成计划，确认后再执行" },
];

const currentModeOption = computed(() =>
  modeOptions.find((m) => m.key === props.chatMode) || modeOptions[1]
);

const agentsWithEmoji = computed(() =>
  props.agents.map((a) => ({
    ...a,
    emoji: getAgentEmoji(a.id),
  }))
);

const selectedAgentLabel = computed(() => {
  if (!selectedAgentId.value) return null;
  const agent = props.agents.find((a) => a.id === selectedAgentId.value);
  return agent?.name || null;
});

const effectiveAgentId = computed(() => {
  if (selectedAgentId.value) return selectedAgentId.value;
  return props.agents.length > 0 ? props.agents[0].id : null;
});

const modeTitle = computed(() => {
  switch (props.chatMode) {
    case "ask": return "快速问答";
    case "agent": return "智能体对话";
    case "plan": return "规划模式";
    default: return "开始新的对话";
  }
});

const modeSubtitle = computed(() => {
  switch (props.chatMode) {
    case "ask": return "快速提问，获取精准答案 — AI 不会调用工具";
    case "agent": return "AI 可自主调用工具，完成复杂任务";
    case "plan": return "AI 先生成执行计划，确认后再逐步执行";
    default: return "选择模式开始对话";
  }
});

const modePlaceholder = computed(() => {
  switch (props.chatMode) {
    case "ask": return "输入你的问题... (Enter 发送)";
    case "agent": return "描述你想完成的任务... (Enter 发送)";
    case "plan": return "描述你的目标，AI 将先生成计划... (Enter 发送)";
    default: return "输入消息开始对话... (Enter 发送)";
  }
});

const modeCards = computed(() => {
  switch (props.chatMode) {
    case "ask":
      return [
        { icon: SearchOutlined, iconBg: 'bg-[rgba(0,229,255,0.08)] border-[rgba(0,229,255,0.15)]', iconColor: 'text-[#00e5ff]', title: '即时问答', desc: '提出问题，AI 基于知识直接回答，不调用外部工具' },
        { icon: ThunderboltOutlined, iconBg: 'bg-[rgba(255,107,203,0.08)] border-[rgba(255,107,203,0.15)]', iconColor: 'text-[#ff6bcb]', title: '快速响应', desc: '无需规划，直接获取答案，适合知识问答和咨询' },
        { icon: MessageOutlined, iconBg: 'bg-[rgba(139,92,246,0.08)] border-[rgba(139,92,246,0.15)]', iconColor: 'text-[#8b5cf6]', title: '多轮对话', desc: '支持连续提问，AI 会记住上下文给出连贯回答' },
      ];
    case "agent":
      return [
        { icon: RobotOutlined, iconBg: 'bg-[rgba(0,229,255,0.08)] border-[rgba(0,229,255,0.15)]', iconColor: 'text-[#00e5ff]', title: '智能体自主执行', desc: 'AI 智能体可调用数据库、API 等工具，自主完成任务' },
        { icon: ThunderboltOutlined, iconBg: 'bg-[rgba(255,107,203,0.08)] border-[rgba(255,107,203,0.15)]', iconColor: 'text-[#ff6bcb]', title: '工具调用', desc: '支持多种工具扩展，Agent 根据任务自动选择和调用' },
        { icon: MessageOutlined, iconBg: 'bg-[rgba(139,92,246,0.08)] border-[rgba(139,92,246,0.15)]', iconColor: 'text-[#8b5cf6]', title: '多步推理', desc: '复杂任务自动拆解为多步执行，逐步完成并反馈结果' },
      ];
    case "plan":
      return [
        { icon: CompassOutlined, iconBg: 'bg-[rgba(0,229,255,0.08)] border-[rgba(0,229,255,0.15)]', iconColor: 'text-[#00e5ff]', title: '先生成计划', desc: 'AI 先分析目标，生成详细的执行计划供你审阅' },
        { icon: ExperimentOutlined, iconBg: 'bg-[rgba(255,107,203,0.08)] border-[rgba(255,107,203,0.15)]', iconColor: 'text-[#ff6bcb]', title: '确认后执行', desc: '审阅并调整计划后，AI 严格按步骤执行，安全可控' },
        { icon: BulbOutlined, iconBg: 'bg-[rgba(139,92,246,0.08)] border-[rgba(139,92,246,0.15)]', iconColor: 'text-[#8b5cf6]', title: '复杂任务规划', desc: '适合多步骤、高复杂度任务，确保每一步都经过思考' },
      ];
    default:
      return [];
  }
});

async function handleSend() {
  if (!effectiveAgentId.value || !messageText.value.trim()) return;
  try {
    const resp = await createChatSession({
      agentId: effectiveAgentId.value,
      title: messageText.value.slice(0, 20),
    });
    await createChatMessage({
      sessionId: resp.chatSessionId,
      content: messageText.value,
      role: "user",
      agentId: effectiveAgentId.value,
    });
    await refreshChatSessions();
    messageText.value = "";
    router.push(`/chat/${resp.chatSessionId}`);
  } catch {
    message.error("发送失败，请重试");
  }
}
</script>

<style scoped>
@reference "tailwindcss";

.empty-chat-input {
  @apply !bg-transparent !border-none !shadow-none !text-[#ecf1fa] !text-sm;
}
.empty-chat-input:deep(textarea) {
  @apply !bg-transparent !resize-none;
}
.empty-chat-input:deep(textarea)::placeholder {
  @apply !text-[#3a5070];
}

.send-btn {
  @apply !h-8 !px-3 !flex !items-center !justify-center !rounded-lg;
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.2), rgba(139, 92, 246, 0.2)) !important;
  border: 1px solid rgba(0, 229, 255, 0.3) !important;
}
.send-btn:hover {
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.35), rgba(139, 92, 246, 0.35)) !important;
  border-color: rgba(0, 229, 255, 0.5) !important;
}
.send-btn:disabled {
  @apply !opacity-30 !cursor-not-allowed;
}

.agent-dropdown-panel {
  @apply bg-[#0e1422] border border-[rgba(0,229,255,0.15)] rounded-xl shadow-2xl shadow-black/40 w-[260px];
}

.card-hover {
  @apply transition-all duration-300;
}
.card-hover:hover {
  @apply border-[rgba(0,229,255,0.25)];
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(0, 229, 255, 0.06);
}

.mode-dropdown-panel {
  @apply bg-[#0e1422] border border-[rgba(0,229,255,0.15)] rounded-xl shadow-2xl shadow-black/40 w-[220px] py-1;
}
</style>
