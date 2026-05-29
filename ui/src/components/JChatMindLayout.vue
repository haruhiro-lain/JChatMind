<template>
  <Layout>
    <Sidebar v-if="sidebarVisible">
      <SideMenu />
    </Sidebar>
    <Content>
      <!-- 顶部导航栏 -->
      <header class="h-12 flex items-center px-5 bg-white dark:bg-[#090d17]/90 border-b border-gray-100 dark:border-[rgba(0,229,255,0.2)] shrink-0 transition-colors duration-300">
        <button
          v-if="!sidebarVisible"
          @click="toggleSidebar"
          class="mr-3 w-7 h-7 flex items-center justify-center rounded-md text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors cursor-pointer"
          title="显示侧边栏"
        >
          ≡
        </button>
        <span class="text-xs text-gray-400 dark:text-[#5a7090] font-medium tracking-wide">
          AI Agent 对话平台
        </span>
        <div class="flex-1" />
        <a
          href="https://github.com"
          target="_blank"
          rel="noopener noreferrer"
          class="text-xs text-gray-400 dark:text-[#5a7090] hover:text-gray-600 dark:hover:text-[#00e5ff] transition-colors"
        >
          GitHub ↗
        </a>
      </header>
      <div class="flex-1 min-h-0">
        <router-view />
      </div>
      <!-- 底部信息栏 -->
      <footer class="h-8 flex items-center justify-center px-5 bg-white dark:bg-[#090d17]/90 border-t border-gray-100 dark:border-[rgba(0,229,255,0.2)] shrink-0 transition-colors duration-300">
        <span class="text-xs text-gray-300 dark:text-[#3a5070]">
          © {{ currentYear }} JChatMind · Powered by Spring AI & Vue 3
        </span>
      </footer>
    </Content>

    <!-- 右下角浮动按钮 -->
    <FloatingActions
      :sidebar-visible="sidebarVisible"
      :is-dark="isDark"
      @toggle-sidebar="toggleSidebar"
      @toggle-theme="toggleTheme"
    />
  </Layout>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from "vue";
import Layout from "../layout/Layout.vue";
import Sidebar from "../layout/Sidebar.vue";
import SideMenu from "./SideMenu.vue";
import Content from "../layout/Content.vue";
import FloatingActions from "./FloatingActions.vue";

const currentYear = new Date().getFullYear();

const isDark = ref(true);
const sidebarVisible = ref(true);

onMounted(() => {
  const saved = localStorage.getItem("jchatmind-theme");
  if (saved !== null) {
    isDark.value = saved === "dark";
  }
});

watch(isDark, (val) => {
  document.documentElement.setAttribute("data-theme", val ? "dark" : "light");
  localStorage.setItem("jchatmind-theme", val ? "dark" : "light");
}, { immediate: true });

function toggleTheme() {
  isDark.value = !isDark.value;
}

function toggleSidebar() {
  sidebarVisible.value = !sidebarVisible.value;
}
</script>
