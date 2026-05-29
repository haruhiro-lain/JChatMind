<template>
  <div ref="scrollContainerRef" class="flex-1 px-6 md:px-16 pt-6 overflow-y-auto">
    <div v-for="message in messages" :key="message.id" class="mb-4">
      <!-- Assistant 消息 -->
      <template v-if="message.role === 'assistant'">
        <div class="flex justify-start">
          <div class="max-w-[85%]">
            <!-- 工具调用展示 -->
            <div v-if="message.metadata?.toolCalls?.length" class="mb-2 flex flex-wrap gap-2">
              <div
                v-for="tc in message.metadata.toolCalls"
                :key="tc.id"
                class="text-xs text-gray-500 dark:text-[#8ba4c0] flex items-center gap-1.5"
              >
                <ToolOutlined class="text-blue-500" />
                <span class="font-mono text-blue-600 dark:text-[#00e5ff]">{{ tc.name }}</span>
                <span v-if="tc.arguments" class="text-gray-400">·</span>
                <span class="text-gray-500 dark:text-[#5a7090] truncate max-w-[200px]">{{ formatArgs(tc.arguments) }}</span>
              </div>
            </div>
            <!-- 消息内容 Markdown -->
            <div v-if="message.content" class="prose prose-sm dark:prose-invert max-w-none bubble-card">
              <div v-html="renderMarkdown(message.content)" />
            </div>
          </div>
        </div>
      </template>

      <!-- Tool 消息 -->
      <template v-else-if="message.role === 'tool' && message.metadata?.toolResponse">
        <div class="flex justify-start">
          <div class="max-w-[85%]">
            <ToolResponseCard :tool-response="message.metadata.toolResponse" />
          </div>
        </div>
      </template>

      <!-- User 消息 -->
      <template v-else-if="message.role === 'user'">
        <div class="flex justify-end">
          <div class="max-w-[80%] px-4 py-2.5 rounded-2xl rounded-br-md bg-blue-500 text-white text-sm">
            {{ message.content }}
          </div>
        </div>
      </template>

      <!-- System 消息 -->
      <template v-else-if="message.role === 'system'">
        <div class="flex justify-center">
          <div class="px-3 py-1 bg-gray-100 dark:bg-[#0e1422] text-gray-600 dark:text-[#8ba4c0] text-xs rounded-full flex items-center gap-1">
            <RobotOutlined />
            <span>{{ message.content }}</span>
          </div>
        </div>
      </template>
    </div>

    <!-- Agent 状态显示 -->
    <div v-if="displayAgentStatus" class="mb-3 animate-pulse">
      <div class="flex justify-start">
        <div class="max-w-[85%] px-4 py-2.5 rounded-2xl rounded-bl-md bg-gray-100 dark:bg-[#0e1422] border border-gray-200 dark:border-[rgba(0,229,255,0.15)]">
          <span class="text-sm text-gray-600 dark:text-[#8ba4c0] flex items-center gap-2">
            <span class="font-semibold text-blue-600 dark:text-[#00e5ff]">✨ {{ getStatusLabel() }}</span>
            <span class="text-gray-400">·</span>
            <span>{{ agentStatusText }}</span>
          </span>
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
