<template>
  <div class="border-t border-[rgba(0,229,255,0.1)] bg-[#090d17]/95 shrink-0 transition-colors duration-300">
    <!-- 输入区域 -->
    <div class="px-4 pt-3">
      <a-textarea
        v-model:value="message"
        :placeholder="modePlaceholder"
        :auto-size="{ minRows: 1, maxRows: 6 }"
        @keydown.enter.exact.prevent="handleSubmit"
        class="chat-input-area"
      />
    </div>

    <!-- 底部工具栏：模式选择 + 发送按钮 -->
    <div class="flex items-center justify-between px-4 pb-3 pt-2">
      <!-- 左侧：模式切换下拉列表 -->
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

      <!-- 右侧：发送按钮 -->
      <a-button
        type="primary"
        :disabled="!message.trim() || sending"
        :loading="sending"
        @click="handleSubmit"
        class="send-btn"
      >
        <SendOutlined class="text-sm" />
        <span class="ml-1.5 text-xs font-medium">{{ sending ? '发送中' : '发送' }}</span>
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { SendOutlined, SearchOutlined, RobotOutlined, BulbOutlined, DownOutlined, CheckCircleFilled } from "@ant-design/icons-vue";
import type { ChatMode } from "../../../types";

const props = defineProps<{
  chatMode: ChatMode;
  sending?: boolean;
}>();

const emit = defineEmits<{
  send: [data: { text: string }];
  "update:chatMode": [mode: ChatMode];
}>();

const message = ref("");

const modeOptions = [
  { key: "ask" as ChatMode, label: "Ask", icon: SearchOutlined, desc: "快速问答，不调用工具" },
  { key: "agent" as ChatMode, label: "Agent", icon: RobotOutlined, desc: "智能体自主调用工具完成任务" },
  { key: "plan" as ChatMode, label: "Plan", icon: BulbOutlined, desc: "先生成计划，确认后再执行" },
];

const currentModeOption = computed(() =>
  modeOptions.find((m) => m.key === props.chatMode) || modeOptions[1]
);

const modePlaceholder = computed(() => {
  switch (props.chatMode) {
    case "ask": return "提问或搜索... (Enter 发送)";
    case "agent": return "让智能体帮你完成任务... (Enter 发送)";
    case "plan": return "描述你的目标，AI 将先规划再执行... (Enter 发送)";
    default: return "输入消息... (Enter 发送)";
  }
});

function handleSubmit() {
  const trimmed = message.value.trim();
  if (!trimmed) return;
  emit("send", { text: trimmed });
  message.value = "";
}
</script>

<style scoped>
@reference "tailwindcss";

.chat-input-area {
  @apply !bg-transparent !border-none !shadow-none !text-[#ecf1fa] !text-sm;
}
.chat-input-area:deep(textarea) {
  @apply !bg-transparent !resize-none;
}
.chat-input-area:deep(textarea)::placeholder {
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

.mode-dropdown-panel {
  @apply bg-[#0e1422] border border-[rgba(0,229,255,0.15)] rounded-xl shadow-2xl shadow-black/40 w-[220px] py-1;
}
</style>
