<template>
  <div class="fixed bottom-27 right-6 flex flex-col gap-2 z-50">
    <!-- 侧边栏切换 -->
    <button :class="buttonClass" @click="$emit('toggleSidebar')" :title="sidebarVisible ? '隐藏侧边栏' : '显示侧边栏'">
      <MenuFoldOutlined v-if="sidebarVisible" />
      <MenuUnfoldOutlined v-else />
    </button>

    <!-- 回到顶部 -->
    <button :class="buttonClass" @click="scrollToTop" title="回到顶部">
      <VerticalAlignTopOutlined />
    </button>

    <!-- 滚动到底部 -->
    <button :class="buttonClass" @click="scrollToBottom" title="滚动到底部">
      <VerticalAlignBottomOutlined />
    </button>

    <!-- 深色/浅色切换 -->
    <button :class="buttonClass" @click="$emit('toggleTheme')" :title="isDark ? '切换到浅色模式' : '切换到深色模式'">
      <SunOutlined v-if="isDark" />
      <MoonOutlined v-else />
    </button>
  </div>
</template>

<script setup lang="ts">
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  VerticalAlignTopOutlined,
  VerticalAlignBottomOutlined,
  SunOutlined,
  MoonOutlined,
} from "@ant-design/icons-vue";

defineProps<{
  sidebarVisible: boolean;
  isDark: boolean;
}>();

defineEmits<{
  toggleSidebar: [];
  toggleTheme: [];
}>();

const buttonClass =
  "w-9 h-9 flex items-center justify-center rounded-lg text-sm transition-all duration-200 border border-gray-200 dark:border-[rgba(0,229,255,0.2)] bg-white dark:bg-[#0e1422]/80 text-gray-500 dark:text-[#5a7090] hover:text-gray-900 dark:hover:text-[#00e5ff] hover:border-gray-300 dark:hover:border-[rgba(0,229,255,0.4)] hover:shadow-sm dark:hover:shadow-[0_0_12px_rgba(0,229,255,0.12)] cursor-pointer";

function scrollToTop() {
  const mainContent = document.querySelector("[data-scroll-container]");
  if (mainContent) {
    mainContent.scrollTo({ top: 0, behavior: "smooth" });
  }
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function scrollToBottom() {
  const mainContent = document.querySelector("[data-scroll-container]");
  if (mainContent) {
    mainContent.scrollTo({ top: mainContent.scrollHeight, behavior: "smooth" });
  }
}
</script>
