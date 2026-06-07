<template>
  <a-modal
    :open="open"
    @cancel="$emit('close')"
    :title="isEditMode ? '编辑智能体' : '智能体助手'"
    :footer="null"
    width="800px"
    centered
  >
    <div class="flex h-[500px]">
      <!-- 左侧菜单 -->
      <div class="w-[150px] h-full border-r border-gray-200 dark:border-[rgba(0,229,255,0.22)] pr-2">
        <div class="flex flex-col gap-0.5 select-none cursor-pointer">
          <div
            v-for="item in menuItems"
            :key="item.key"
            @click="selectedKey = item.key"
            :class="[
              'px-3 py-2 rounded-lg hover:bg-gray-100 dark:hover:bg-[rgba(0,229,255,0.06)]',
              selectedKey === item.key
                ? 'bg-gray-100 dark:bg-[rgba(0,229,255,0.08)] text-gray-900 dark:text-[#ecf1fa] font-medium'
                : 'text-gray-600 dark:text-[#8ba4c0]'
            ]"
          >
            {{ item.label }}
          </div>
        </div>
      </div>

      <!-- 右侧内容 -->
      <div class="flex-1 h-full relative">
        <div class="px-4 pb-4 overflow-y-auto h-full">
          <!-- 基础设置 -->
          <template v-if="selectedKey === 'base'">
            <!-- 角色卡导入 -->
            <div v-if="!isEditMode" class="mb-3">
              <a-upload
                :show-upload-list="false"
                :before-upload="handleImportCard"
                accept=".png,.json"
              >
                <a-button :loading="importLoading">
                  <template #icon><UploadOutlined /></template>
                  导入角色卡 (PNG/JSON)
                </a-button>
              </a-upload>
            </div>
            <div class="mb-3">
              <label class="block text-gray-700 dark:text-[#ecf1fa] font-medium mb-1">名称</label>
              <a-input v-model:value="formData.name" placeholder="请输入智能体名称" />
            </div>
            <div class="mb-3">
              <label class="block text-gray-700 dark:text-[#ecf1fa] font-medium mb-1">描述</label>
              <a-textarea v-model:value="formData.description" placeholder="请输入智能体描述" :rows="2" />
            </div>
            <div class="mb-3">
              <label class="block text-gray-700 dark:text-[#ecf1fa] font-medium mb-1">提示词</label>
              <a-textarea v-model:value="formData.systemPrompt" placeholder="默认提示词" :rows="10" />
            </div>
          </template>

          <!-- 模型设置 -->
          <template v-if="selectedKey === 'model'">
            <div class="mb-4">
              <label class="block text-gray-700 dark:text-[#ecf1fa] font-medium mb-1">选择模型</label>
              <a-select
                v-model:value="formData.model"
                :options="modelOptions"
                placeholder="请选择模型"
                style="width: 300px"
              />
            </div>
            <div class="mb-4">
              <label class="block text-gray-700 dark:text-[#ecf1fa] font-medium mb-2">API Key（可选）</label>
              <a-input-password v-model:value="formData.apiKey" placeholder="输入自定义 API Key，留空使用系统预设" />
              <p class="text-xs text-gray-400 mt-1 mb-4">该助手将使用此 Key 调用模型，留空则使用系统预设 Key</p>
            </div>
            <div class="mb-4">
              <label class="block text-gray-700 dark:text-[#ecf1fa] font-medium mb-2">模型参数</label>
              <div class="space-y-4">
                <div>
                  <div class="flex items-center justify-between mb-2">
                    <label class="block text-sm text-gray-600 dark:text-[#8ba4c0]">
                      Temperature（温度）<span class="text-gray-400 ml-1 text-xs">(0.0 - 2.0)</span>
                    </label>
                    <span class="text-sm font-medium text-gray-700 dark:text-[#ecf1fa] min-w-[40px] text-right">
                      {{ formData.chatOptions?.temperature?.toFixed(1) }}
                    </span>
                  </div>
                  <a-slider :min="0" :max="2" :step="0.1" v-model:value="formData.chatOptions!.temperature" />
                </div>
                <div>
                  <div class="flex items-center justify-between mb-2">
                    <label class="block text-sm text-gray-600 dark:text-[#8ba4c0]">
                      Top P（核采样）<span class="text-gray-400 ml-1 text-xs">(0.0 - 1.0)</span>
                    </label>
                    <span class="text-sm font-medium text-gray-700 dark:text-[#ecf1fa] min-w-[40px] text-right">
                      {{ formData.chatOptions?.topP?.toFixed(1) }}
                    </span>
                  </div>
                  <a-slider :min="0" :max="1" :step="0.1" v-model:value="formData.chatOptions!.topP" />
                </div>
                <div>
                  <div class="flex items-center justify-between mb-2">
                    <label class="block text-sm text-gray-600 dark:text-[#8ba4c0]">
                      消息窗口长度<span class="text-gray-400 ml-1 text-xs">(1 - 100)</span>
                    </label>
                    <span class="text-sm font-medium text-gray-700 dark:text-[#ecf1fa] min-w-[40px] text-right">
                      {{ formData.chatOptions?.messageLength }}
                    </span>
                  </div>
                  <a-slider :min="1" :max="100" :step="1" v-model:value="formData.chatOptions!.messageLength" />
                </div>
              </div>
            </div>
          </template>

          <!-- 工具调用 -->
          <template v-if="selectedKey === 'tools'">
            <div class="mb-4">
              <label class="block text-gray-700 dark:text-[#ecf1fa] font-medium mb-3">工具调用</label>
              <p class="text-sm text-gray-500 dark:text-[#8ba4c0] mb-4">选择智能体可以使用的工具</p>
              <div v-if="tools.length === 0" class="text-center py-8 text-gray-500">
                <p>暂无可用工具</p>
              </div>
              <div v-else class="space-y-3">
                <div
                  v-for="tool in tools"
                  :key="tool.name"
                  :class="[
                    'border rounded-lg p-4 cursor-pointer transition-all',
                    isToolSelected(tool.name)
                      ? 'border-blue-500 dark:border-[#00e5ff] bg-blue-50 dark:bg-[rgba(0,229,255,0.08)]'
                      : 'border-gray-200 dark:border-[rgba(0,229,255,0.22)] hover:border-blue-400 dark:hover:border-[rgba(0,229,255,0.35)] hover:bg-blue-50 dark:hover:bg-[rgba(0,229,255,0.06)]'
                  ]"
                  @click="toggleTool(tool.name)"
                >
                  <div class="flex items-start gap-2">
                    <a-checkbox :checked="isToolSelected(tool.name)" />
                    <div class="flex-1">
                      <span class="font-medium text-gray-900 dark:text-[#ecf1fa]">{{ tool.name }}</span>
                      <p class="text-sm text-gray-600 dark:text-[#8ba4c0]">{{ tool.description }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>

        <!-- 底部按钮 -->
        <div class="absolute bottom-0 left-0 right-0 px-4 py-3 border-t border-gray-200 dark:border-[rgba(0,229,255,0.22)] flex justify-end gap-2 bg-white dark:bg-[#0e1422f2]">
          <a-button @click="$emit('close')">取消</a-button>
          <a-button type="primary" :loading="createAgentLoading" @click="handleSubmit">
            <template #icon><SaveOutlined /></template>
            {{ isEditMode ? '保存' : '创建' }}
          </a-button>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from "vue";
import { SaveOutlined, UploadOutlined } from "@ant-design/icons-vue";
import { message } from "ant-design-vue";
import type { CreateAgentRequest, UpdateAgentRequest, AgentVO, ModelType } from "../../api/api";
import { getOptionalTools, importCard, type ToolVO } from "../../api/api";

const props = defineProps<{
  open: boolean;
  editingAgent?: AgentVO | null;
}>();

const emit = defineEmits<{
  close: [];
  create: [request: CreateAgentRequest];
  update: [agentId: string, request: UpdateAgentRequest];
}>();

const menuItems = [
  { key: "base", label: "基础设置" },
  { key: "model", label: "模型设置" },
  { key: "tools", label: "工具调用" },
];

const selectedKey = ref("base");
const tools = ref<ToolVO[]>([]);
const createAgentLoading = ref(false);
const modelOptions = [
  { value: "deepseek-pro", label: "DeepSeek V4 Pro" },
  { value: "deepseek-flash", label: "DeepSeek V4 Flash" },
  { value: "glm-4.6", label: "GLM-4.6 (智谱)" },
];

const defaultForm = (): CreateAgentRequest => ({
  name: "智能体助手",
  description: "",
  systemPrompt: "你是一个很有用的智能体助手",
  model: "deepseek-pro" as ModelType,
  allowedTools: [],
  chatOptions: { temperature: 0.7, topP: 1.0, messageLength: 20 },
  apiKey: "",
});

const formData = ref<CreateAgentRequest>(defaultForm());

const isEditMode = ref(false);

watch(() => props.open, (val) => {
  if (val) {
    createAgentLoading.value = false;
    if (props.editingAgent) {
      isEditMode.value = true;
      formData.value = {
        name: props.editingAgent.name,
        description: props.editingAgent.description || "",
        systemPrompt: props.editingAgent.systemPrompt || "",
        model: props.editingAgent.model,
        allowedTools: props.editingAgent.allowedTools || [],
        chatOptions: props.editingAgent.chatOptions || { temperature: 0.7, topP: 1.0, messageLength: 10 },
        apiKey: props.editingAgent.apiKey || "",
      };
    } else {
      isEditMode.value = false;
      formData.value = defaultForm();
      cardAvatarFileName.value = null;
    }
    selectedKey.value = "base";
  }
});

onMounted(async () => {
  try {
    const resp = await getOptionalTools();
    tools.value = resp.tools;
  } catch { /* ignore */ }
});

function isToolSelected(toolName: string): boolean {
  return formData.value.allowedTools?.includes(toolName) ?? false;
}

function toggleTool(toolName: string) {
  const current = formData.value.allowedTools || [];
  if (current.includes(toolName)) {
    formData.value.allowedTools = current.filter((t) => t !== toolName);
  } else {
    formData.value.allowedTools = [...current, toolName];
  }
}

const importLoading = ref(false);
const cardAvatarFileName = ref<string | null>(null);

async function handleImportCard(file: File): Promise<boolean> {
  importLoading.value = true;
  try {
    const card = await importCard(file);
    formData.value.name = card.name || "未命名角色";
    formData.value.description = card.description || "";
    formData.value.systemPrompt = card.systemPrompt || "";
    cardAvatarFileName.value = card.avatarFileName || null;
    message.success(`已导入角色卡: ${card.name}`);
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : "导入失败";
    message.error(msg);
  } finally {
    importLoading.value = false;
  }
  return false; // 阻止 a-upload 自动上传
}

async function handleSubmit() {
  if (!formData.value.name.trim()) {
    message.warning("请输入智能体名称");
    return;
  }
  createAgentLoading.value = true;
  try {
    const request = {
      ...formData.value,
      avatar: cardAvatarFileName.value
        ? `/avatars/${cardAvatarFileName.value}`
        : undefined,
    };
    if (isEditMode.value && props.editingAgent) {
      emit("update", props.editingAgent.id, request);
    } else {
      emit("create", request);
    }
    // 不在此处重置 loading，由父组件关闭弹窗时通过 watch(open) 重置
  } catch {
    createAgentLoading.value = false;
  }
}
</script>
