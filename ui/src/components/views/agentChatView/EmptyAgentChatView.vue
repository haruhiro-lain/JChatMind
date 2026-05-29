<template>
  <div class="flex flex-col h-full">
    <!-- Agent 选择器 -->
    <div v-if="agents.length > 0" class="border-b border-gray-100 dark:border-[rgba(0,229,255,0.2)] bg-white dark:bg-[#090d17]/90 px-5 py-3 transition-colors duration-300">
      <a-select
        v-model:value="selectedAgentId"
        :options="agentOptions"
        placeholder="选择智能体助手"
        style="width: 200px"
      />
    </div>

    <div class="flex-1 flex items-center justify-center p-6">
      <div class="max-w-xl w-full space-y-5">
        <!-- 标题区域 -->
        <div class="text-center mb-6">
          <div class="w-16 h-16 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center mx-auto mb-4 shadow-lg shadow-indigo-200/50 dark:shadow-indigo-500/20">
            <RobotOutlined class="text-white text-2xl" />
          </div>
          <h2 class="!mb-1 !text-[22px] !font-bold !text-gray-900 dark:!text-[#ecf1fa] !tracking-tight">
            开始新的对话
          </h2>
          <p class="!text-[14px] !text-gray-400 dark:!text-[#5a7090]">
            选择一个智能体助手开始聊天，或直接发送消息创建新会话
          </p>
        </div>

        <!-- 特性卡片 -->
        <div class="space-y-3">
          <div class="p-4 rounded-xl border border-gray-100 dark:border-[rgba(0,229,255,0.22)] bg-white dark:bg-[#0e1422]/70 card-hover cursor-default transition-colors duration-300">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-50 to-indigo-50 dark:from-[rgba(0,229,255,0.1)] dark:to-[rgba(255,107,203,0.1)] flex items-center justify-center border border-blue-100 dark:border-[rgba(0,229,255,0.15)]">
                <RobotOutlined class="text-indigo-500 dark:text-[#00e5ff] text-lg" />
              </div>
              <div>
                <div class="text-[14px] font-semibold text-gray-900 dark:text-[#ecf1fa]">智能对话</div>
                <div class="text-[12px] text-gray-400 dark:text-[#5a7090] mt-0.5">与 AI 助手进行智能对话，获取帮助和建议</div>
              </div>
            </div>
          </div>

          <div class="p-4 rounded-xl border border-gray-100 dark:border-[rgba(0,229,255,0.22)] bg-white dark:bg-[#0e1422]/70 card-hover cursor-default transition-colors duration-300">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-green-50 to-teal-50 dark:from-[rgba(0,229,255,0.1)] dark:to-[rgba(0,229,255,0.05)] flex items-center justify-center border border-green-100 dark:border-[rgba(0,229,255,0.15)]">
                <BulbOutlined class="text-teal-500 dark:text-[#00e5ff] text-lg" />
              </div>
              <div>
                <div class="text-[14px] font-semibold text-gray-900 dark:text-[#ecf1fa]">工具调用</div>
                <div class="text-[12px] text-gray-400 dark:text-[#5a7090] mt-0.5">Agent 可自主调用工具完成任务，如数据库查询</div>
              </div>
            </div>
          </div>

          <div class="p-4 rounded-xl border border-gray-100 dark:border-[rgba(0,229,255,0.22)] bg-white dark:bg-[#0e1422]/70 card-hover cursor-default transition-colors duration-300">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-orange-50 to-amber-50 dark:from-[rgba(255,107,203,0.1)] dark:to-[rgba(255,107,203,0.05)] flex items-center justify-center border border-orange-100 dark:border-[rgba(255,107,203,0.15)]">
                <MessageOutlined class="text-orange-500 dark:text-[#ff6bcb] text-lg" />
              </div>
              <div>
                <div class="text-[14px] font-semibold text-gray-900 dark:text-[#ecf1fa]">快速开始</div>
                <div class="text-[12px] text-gray-400 dark:text-[#5a7090] mt-0.5">在下方输入框输入消息，立即开始对话</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="border-t border-gray-100 dark:border-[rgba(0,229,255,0.22)] bg-white dark:bg-[#090d17]/90 transition-colors duration-300">
      <div class="px-4 pb-4 pt-4">
        <div class="flex items-center gap-2">
          <a-textarea
            v-model:value="message"
            placeholder="输入消息开始对话..."
            :auto-size="{ minRows: 1, maxRows: 6 }"
            @keydown.enter.exact.prevent="handleSend"
          />
          <a-button type="primary" :loading="loading" :disabled="!message.trim() || !effectiveAgentId" @click="handleSend">
            发送
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import { RobotOutlined, BulbOutlined, MessageOutlined } from "@ant-design/icons-vue";
import type { AgentVO } from "../../../api/api";
import { createChatMessage, createChatSession } from "../../../api/api";
import { useChatSessions } from "../../../composables/useChatSessions";

const props = defineProps<{
  agents: AgentVO[];
  loading: boolean;
}>();

const router = useRouter();
const { refreshChatSessions } = useChatSessions();

const messageText = ref("");
const selectedAgentId = ref<string | null>(null);

const agentOptions = computed(() =>
  props.agents.map((a) => ({ value: a.id, label: a.name }))
);

const effectiveAgentId = computed(() => {
  if (selectedAgentId.value) return selectedAgentId.value;
  return props.agents.length > 0 ? props.agents[0].id : null;
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
