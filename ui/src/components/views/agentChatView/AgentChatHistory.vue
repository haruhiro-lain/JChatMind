<template>
  <div ref="scrollContainerRef" class="flex-1 overflow-y-auto py-4 px-4 md:px-8">
    <div v-for="message in messages" :key="message.id" class="mb-5">
      <!-- Assistant 消息 (SillyTavern 风格) -->
      <template v-if="message.role === 'assistant'">
        <div class="flex gap-3">
          <!-- 头像 -->
          <div class="w-11 h-11 rounded-full overflow-hidden shrink-0 border-2 border-[rgba(0,229,255,0.3)] shadow-[0_0_12px_rgba(0,229,255,0.15)]">
            <img v-if="agentAvatar" :src="agentAvatar" class="w-full h-full object-cover" :alt="agentName" />
            <div v-else class="w-full h-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center">
              <span class="text-white text-sm font-bold">{{ (agentName || 'AI').charAt(0) }}</span>
            </div>
          </div>
          <div class="flex-1 min-w-0">
            <!-- 角色名 -->
            <div class="text-xs font-semibold text-[#00e5ff] mb-1 ml-0.5">{{ agentName || 'Assistant' }}</div>
            <!-- 工具调用 -->
            <div v-if="message.metadata?.toolCalls?.length" class="mb-2 flex flex-wrap gap-2">
              <div
                v-for="tc in message.metadata.toolCalls"
                :key="tc.id"
                class="text-xs text-[#8ba4c0] flex items-center gap-1.5 px-2 py-1 rounded-md bg-[rgba(0,229,255,0.06)] border border-[rgba(0,229,255,0.12)]"
              >
                <ToolOutlined class="text-[#00e5ff]" />
                <span class="font-mono text-[#00e5ff]">{{ tc.name }}</span>
                <span v-if="tc.arguments" class="text-[#5a7090]">·</span>
                <span class="text-[#5a7090] truncate max-w-[200px]">{{ formatArgs(tc.arguments) }}</span>
              </div>
            </div>
            <!-- 消息内容 -->
            <div v-if="message.content" class="prose prose-sm max-w-none text-[#c8d6e5] leading-relaxed">
              <div v-html="renderMarkdown(message.content)" />
            </div>
          </div>
        </div>
      </template>

      <!-- Tool 消息 -->
      <template v-else-if="message.role === 'tool' && message.metadata?.toolResponse">
        <div class="flex gap-3">
          <div class="w-11 h-11 shrink-0" />
          <div class="flex-1 min-w-0">
            <ToolResponseCard :tool-response="message.metadata.toolResponse" />
          </div>
        </div>
      </template>

      <!-- User 消息 (SillyTavern 风格 - 右对齐，简洁) -->
      <template v-else-if="message.role === 'user'">
        <div class="flex justify-end gap-3">
          <div class="max-w-[75%]">
            <div class="text-xs font-semibold text-[#ff6bcb] mb-1 text-right mr-0.5">You</div>
            <div class="px-4 py-2.5 rounded-2xl bg-[rgba(255,107,203,0.1)] border border-[rgba(255,107,203,0.2)] text-[#c8d6e5] text-sm leading-relaxed">
              {{ message.content }}
            </div>
          </div>
        </div>
      </template>

      <!-- System 消息 -->
      <template v-else-if="message.role === 'system'">
        <div class="flex justify-center">
          <div class="px-4 py-1.5 bg-[rgba(0,229,255,0.06)] border border-[rgba(0,229,255,0.1)] text-[#8ba4c0] text-xs rounded-full flex items-center gap-1.5">
            <RobotOutlined class="text-[#00e5ff]" />
            <span>{{ message.content }}</span>
          </div>
        </div>
      </template>
    </div>

    <!-- Agent 状态 (SillyTavern 风格 "正在输入...") -->
    <div v-if="displayAgentStatus" class="mb-5 animate-pulse">
      <div class="flex gap-3">
        <div class="w-11 h-11 rounded-full overflow-hidden shrink-0 border-2 border-[rgba(0,229,255,0.3)] shadow-[0_0_12px_rgba(0,229,255,0.15)]">
          <img v-if="agentAvatar" :src="agentAvatar" class="w-full h-full object-cover" :alt="agentName" />
          <div v-else class="w-full h-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center">
            <span class="text-white text-sm font-bold">{{ (agentName || 'AI').charAt(0) }}</span>
          </div>
        </div>
        <div class="flex-1 min-w-0">
          <div class="text-xs font-semibold text-[#00e5ff] mb-1 ml-0.5">{{ agentName || 'Assistant' }}</div>
          <div class="px-4 py-3 rounded-2xl bg-[rgba(0,229,255,0.04)] border border-[rgba(0,229,255,0.1)]">
            <span class="text-sm text-[#8ba4c0] flex items-center gap-2">
              <span class="font-semibold text-[#00e5ff]">✨ {{ getStatusLabel() }}</span>
              <span class="text-[#5a7090]">·</span>
              <span>{{ agentStatusText }}</span>
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted, onUnmounted } from "vue";
import { RobotOutlined, ToolOutlined } from "@ant-design/icons-vue";
import { marked } from "marked";
import ToolResponseCard from "./ToolResponseCard.vue";
import type { ChatMessageVO, SseMessageType } from "../../../types";

const props = defineProps<{
  messages: ChatMessageVO[];
  agentName?: string;
  agentAvatar?: string;
  displayAgentStatus?: boolean;
  agentStatusText?: string;
  agentStatusType?: SseMessageType;
}>();

const scrollContainerRef = ref<HTMLDivElement | null>(null);
const isNearBottom = ref(true);
const prevMessagesLength = ref(0);
const SCROLL_THRESHOLD = 20;

function renderMarkdown(content: string): string {
  return marked.parse(content) as string;
}

function formatArgs(args: string): string {
  try {
    const parsed = JSON.parse(args);
    const keys = Object.keys(parsed);
    if (keys.length === 0) return args.slice(0, 50);
    return keys.slice(0, 2).join(", ") + (keys.length > 2 ? "..." : "");
  } catch {
    return args.slice(0, 50) + (args.length > 50 ? "..." : "");
  }
}

function checkIfNearBottom(): boolean {
  const container = scrollContainerRef.value;
  if (!container) return false;
  const { scrollTop, clientHeight, scrollHeight } = container;
  return scrollHeight - scrollTop - clientHeight <= SCROLL_THRESHOLD;
}

function scrollToBottom() {
  requestAnimationFrame(() => {
    if (scrollContainerRef.value) {
      scrollContainerRef.value.scrollTop = scrollContainerRef.value.scrollHeight;
    }
  });
}

function handleScroll() {
  isNearBottom.value = checkIfNearBottom();
}

onMounted(() => {
  const container = scrollContainerRef.value;
  if (!container) return;
  container.addEventListener("scroll", handleScroll, { passive: true });
  setTimeout(() => { isNearBottom.value = checkIfNearBottom(); }, 0);
});

onUnmounted(() => {
  scrollContainerRef.value?.removeEventListener("scroll", handleScroll);
});

watch(() => props.messages.length, (newLen) => {
  const hasNew = newLen > prevMessagesLength.value;
  prevMessagesLength.value = newLen;
  if (hasNew && isNearBottom.value) {
    nextTick(() => scrollToBottom());
  }
});

watch(() => props.displayAgentStatus, (val) => {
  if (val && isNearBottom.value) {
    nextTick(() => scrollToBottom());
  }
});

function getStatusLabel() {
  switch (props.agentStatusType) {
    case "AI_PLANNING": return "规划中";
    case "AI_THINKING": return "思考中";
    case "AI_EXECUTING": return "执行中";
    default: return "处理中";
  }
}
</script>
