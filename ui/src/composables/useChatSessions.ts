import { ref, readonly, type InjectionKey, provide, inject } from "vue";
import {
  type ChatSessionVO,
  getChatSessions,
  deleteChatSession,
} from "../api/api.ts";

interface ChatSessionsState {
  chatSessions: ReturnType<typeof ref<ChatSessionVO[]>>;
  loading: ReturnType<typeof ref<boolean>>;
  refreshChatSessions: () => Promise<void>;
  deleteChatSessionHandle: (chatSessionId: string) => Promise<void>;
}

const ChatSessionsKey: InjectionKey<ChatSessionsState> = Symbol("ChatSessions");

export function provideChatSessions() {
  const chatSessions = ref<ChatSessionVO[]>([]);
  const loading = ref(false);

  async function refreshChatSessions() {
    loading.value = true;
    try {
      const resp = await getChatSessions();
      chatSessions.value = resp.chatSessions;
    } finally {
      loading.value = false;
    }
  }

  async function deleteChatSessionHandle(chatSessionId: string) {
    await deleteChatSession(chatSessionId);
    await refreshChatSessions();
  }

  refreshChatSessions();

  provide(ChatSessionsKey, {
    chatSessions,
    loading,
    refreshChatSessions,
    deleteChatSessionHandle,
  });
}

export function useChatSessions() {
  const ctx = inject(ChatSessionsKey);
  if (!ctx) {
    throw new Error("useChatSessions must be used within <App> (provideChatSessions called)");
  }
  return {
    chatSessions: readonly(ctx.chatSessions),
    loading: readonly(ctx.loading),
    refreshChatSessions: ctx.refreshChatSessions,
    deleteChatSession: ctx.deleteChatSessionHandle,
  };
}
