<template>
  <div class="flex flex-col h-full">
    <a-button type="primary" class="w-full mb-3 h-10 rounded-lg text-[13px] font-semibold create-btn" @click="$emit('createAgentClick')">
      <template #icon><PlusOutlined /></template>
      新建助手
    </a-button>
    <div class="flex-1 overflow-y-auto rounded-xl bg-[#0e1422]/40 p-2 transition-colors duration-300">
      <!-- 空状态 -->
      <div v-if="agents.length === 0" class="flex flex-col items-center justify-center h-full text-[#3a5070]">
        <div class="w-16 h-16 rounded-2xl bg-[rgba(0,229,255,0.04)] flex items-center justify-center mb-4 border border-[rgba(0,229,255,0.08)]">
          <RobotOutlined class="text-2xl opacity-40" />
        </div>
        <p class="text-[13px] font-semibold text-[#5a7090]">暂无智能体</p>
        <p class="text-[11px] mt-1 text-[#3a5070]">点击上方按钮创建你的第一个 AI 助手</p>
      </div>

      <!-- Agent 列表（Trae 风格） -->
      <div v-else class="space-y-1.5">
        <a-dropdown v-for="agent in agentsWithEmoji" :key="agent.id" :trigger="['contextmenu']">
          <div
            class="agent-card group"
            @click="$emit('selectAgent', agent.id)"
          >
            <!-- 左侧头像 -->
            <div class="agent-avatar">
              <span v-if="!agent.avatar" class="text-lg">{{ agent.emoji }}</span>
              <img
                v-else
                :src="agent.avatar"
                class="w-full h-full object-cover"
                :alt="agent.name"
                @error="(e) => (e.target as HTMLImageElement).style.display = 'none'"
              />
            </div>

            <!-- 中间信息 -->
            <div class="flex-1 min-w-0">
              <div class="agent-name">
                {{ agent.name }}
              </div>
              <div v-if="agent.description" class="agent-desc">
                {{ agent.description }}
              </div>
              <div v-else class="agent-desc italic">
                暂无描述
              </div>
            </div>

            <!-- 右侧箭头指示 -->
            <RightOutlined class="text-[10px] text-[#3a5070] group-hover:text-[#00e5ff] transition-all duration-300 group-hover:translate-x-0.5 shrink-0" />
          </div>
          <template #overlay>
            <a-menu>
              <a-menu-item v-if="$attrs.onEditAgent" key="edit" @click="() => $emit('editAgent', agent)">
                <EditOutlined /> 编辑
              </a-menu-item>
              <a-menu-item key="delete" danger @click="() => confirmDelete(agent)">
                <DeleteOutlined /> 删除
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { PlusOutlined, EditOutlined, DeleteOutlined, RobotOutlined, RightOutlined } from "@ant-design/icons-vue";
import { Modal } from "ant-design-vue";
import type { AgentVO } from "../../api/api.ts";
import { getAgentEmoji } from "../../utils";

const props = defineProps<{
  agents: AgentVO[];
}>();

const emit = defineEmits<{
  createAgentClick: [];
  selectAgent: [agentId: string];
  editAgent: [agent: AgentVO];
  deleteAgent: [agentId: string];
}>();

const agentsWithEmoji = computed(() => {
  return props.agents.map((agent) => ({
    ...agent,
    emoji: getAgentEmoji(agent.id),
  }));
});

function confirmDelete(agent: AgentVO) {
  Modal.confirm({
    title: "确定要删除这个智能体吗？",
    content: "删除后将无法恢复",
    okText: "确定",
    cancelText: "取消",
    okType: "danger",
    onOk: () => {
      emit("deleteAgent", agent.id);
    },
  });
}
</script>

<style scoped>
@reference "tailwindcss";

.create-btn {
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.15), rgba(139, 92, 246, 0.15)) !important;
  border: 1px solid rgba(0, 229, 255, 0.2) !important;
  color: #00e5ff !important;
  transition: all 0.3s ease !important;
}
.create-btn:hover {
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.25), rgba(139, 92, 246, 0.25)) !important;
  border-color: rgba(0, 229, 255, 0.4) !important;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 229, 255, 0.15);
}

/* Trae 风格 Agent 卡片 */
.agent-card {
  @apply w-full px-3.5 py-3 rounded-xl cursor-pointer
    bg-[rgba(14,20,34,0.6)] border border-[rgba(0,229,255,0.06)]
    flex items-center gap-3
    transition-all duration-300;
}
.agent-card:hover {
  @apply bg-[rgba(0,229,255,0.04)] border-[rgba(0,229,255,0.15)];
  transform: translateX(2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.agent-avatar {
  @apply w-10 h-10 rounded-xl shrink-0
    bg-gradient-to-br from-[rgba(0,229,255,0.1)] to-[rgba(139,92,246,0.1)]
    flex items-center justify-center
    border border-[rgba(0,229,255,0.12)]
    overflow-hidden;
}

.agent-name {
  @apply font-semibold text-[13px] text-[#ecf1fa] truncate leading-tight;
}

.agent-desc {
  @apply text-[11px] text-[#5a7090] mt-0.5 truncate leading-relaxed;
}
</style>
