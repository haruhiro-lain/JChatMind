<template>
  <div class="border-t border-gray-100 dark:border-[rgba(0,229,255,0.2)] p-4 bg-white dark:bg-[#090d17]/90 transition-colors duration-300">
    <div class="flex items-center gap-2">
      <a-textarea
        v-model:value="message"
        placeholder="输入消息..."
        :auto-size="{ minRows: 1, maxRows: 6 }"
        @keydown.enter.exact.prevent="handleSubmit"
        class="flex-1"
      />
      <a-button type="primary" :disabled="!message.trim()" @click="handleSubmit">
        发送
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";

const emit = defineEmits<{
  send: [data: { text: string }];
}>();

const message = ref("");

function handleSubmit() {
  const trimmed = message.value.trim();
  if (!trimmed) return;
  emit("send", { text: trimmed });
  message.value = "";
}
</script>
