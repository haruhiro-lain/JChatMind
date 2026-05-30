<template>
  <div class="flex flex-col h-full">
    <a-button type="primary" class="w-full mb-3 h-9 rounded-lg text-[13px] font-medium" @click="$emit('createAgentClick')">
      <template #icon><PlusOutlined /></template>
      新建助手
    </a-button>
    <div class="flex-1 overflow-y-auto rounded-xl bg-gray-50/50 dark:bg-[#0e1422]/60 p-2 transition-colors duration-300">
      <!-- 空状态 -->
      <div v-if="agents.length === 0" class="flex flex-col items-center justify-center h-full text-gray-300 dark:text-[#3a5070]">
        <RobotOutlined class="text-3xl mb-3 opacity-50" />
        <p class="text-[13px] font-medium">暂无智能体</p>
        <p class="text-[11px] mt-1">点击上方按钮添加</p>
      </div>

      <!-- Agent 列表 -->
      <div v-else class="space-y-1.5">
        <a-dropdown v-for="agent in agentsWithEmoji" :key="agent.id" :trigger="['contextmenu']">
          <div
            class="w-full px-3.5 py-3 rounded-xl bg-white dark:bg-[#0e1422]/70 cursor-pointer card-hover border border-gray-100 dark:border-[rgba(0,229,255,0.22)] group relative transition-colors duration-300"
            @click="$emit('selectAgent', agent.id)"
          >
            <div class="flex items-start gap-3">
              <div class="w-9 h-9 rounded-xl bg-gradient-to-br from-blue-50 to-indigo-50 flex items-center justify-center shrink-0 border border-blue-100 overflow-hidden">
                <img v-if="agent.avatar" :src="agent.avatar" class="w-full h-full object-cover" :alt="agent.name" @error="(e) => (e.target as HTMLImageElement).style.display = 'none'" />
                <span v-if="!agent.avatar" class="text-lg">{{ agent.emoji }}</span>
              </div>
              <div class="flex-1 min-w-0">
                <div class="font-semibold text-[13px] text-gray-900 dark:text-[#ecf1fa] truncate leading-tight">
                  {{ agent.name }}
                </div>
                <div v-if="agent.description" class="text-[11px] text-gray-400 dark:text-[#5a7090] mt-0.5 truncate">
                  {{ agent.description }}
                </div>
              </div>
            </div>
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
import { PlusOutlined, EditOutlined, DeleteOutlined, RobotOutlined } from "@ant-design/icons-vue";
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
