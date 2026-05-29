import { createRouter, createWebHistory } from "vue-router";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", component: () => import("../components/views/AgentChatView.vue") },
    { path: "/agent", component: () => import("../components/views/AgentChatView.vue") },
    { path: "/chat", component: () => import("../components/views/AgentChatView.vue") },
    { path: "/chat/:chatSessionId", component: () => import("../components/views/AgentChatView.vue") },
  ],
});

export default router;
