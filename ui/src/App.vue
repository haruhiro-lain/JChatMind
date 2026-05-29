<template>
  <JChatMindLayout />
</template>

<script setup lang="ts">
import JChatMindLayout from "./components/JChatMindLayout.vue";
import { provideChatSessions } from "./composables/useChatSessions";
provideChatSessions();
</script>
