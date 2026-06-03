<template>
  <MindHarnessLayout />
</template>

<script setup lang="ts">
import MindHarnessLayout from "./components/MindHarnessLayout.vue";
import { provideChatSessions } from "./composables/useChatSessions";
provideChatSessions();
</script>
