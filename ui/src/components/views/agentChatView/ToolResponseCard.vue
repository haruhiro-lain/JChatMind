<template>
  <div class="my-1.5 text-xs">
    <div
      class="flex items-center gap-2 text-gray-500 dark:text-[#8ba4c0] cursor-pointer hover:text-gray-700 dark:hover:text-[#ecf1fa] transition-colors"
      @click="expanded = !expanded"
    >
      <DownOutlined v-if="expanded" class="text-gray-400" />
      <RightOutlined v-else class="text-gray-400" />
      <CheckCircleOutlined class="text-green-500" />
      <span class="font-mono text-green-600 dark:text-green-400">{{ toolResponse.name }}</span>
      <span class="text-gray-400">·</span>
      <span class="text-gray-500 dark:text-[#5a7090] truncate flex-1">{{ dataPreview }}</span>
    </div>
    <div v-if="expanded" class="ml-5 mt-1.5 p-2 bg-gray-50 dark:bg-[#141b2d] rounded border border-gray-200 dark:border-[rgba(0,229,255,0.15)]">
      <pre class="text-xs font-mono whitespace-pre-wrap break-words overflow-x-auto max-h-60 overflow-y-auto text-gray-700 dark:text-[#8ba4c0]">{{ formattedData }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { CheckCircleOutlined, DownOutlined, RightOutlined } from "@ant-design/icons-vue";
import type { ToolResponse } from "../../../types";

const props = defineProps<{
  toolResponse: ToolResponse;
}>();

const expanded = ref(false);

const parsed = computed(() => {
  try {
    return JSON.parse(props.toolResponse.responseData);
  } catch {
    return null;
  }
});

const dataPreview = computed(() => {
  if (parsed.value) {
    const s = JSON.stringify(parsed.value);
    return s.length > 100 ? s.slice(0, 100) + "..." : s;
  }
  const d = props.toolResponse.responseData;
  return d.length > 100 ? d.slice(0, 100) + "..." : d;
});

const formattedData = computed(() => {
  if (parsed.value) {
    return JSON.stringify(parsed.value, null, 2);
  }
  return props.toolResponse.responseData;
});
</script>
