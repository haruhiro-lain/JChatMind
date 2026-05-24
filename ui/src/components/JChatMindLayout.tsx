import { useState, useEffect, useCallback } from "react";
import { Routes, Route } from "react-router-dom";
import Layout from "../layout/Layout.tsx";
import Sidebar from "../layout/Sidebar.tsx";
import SideMenu from "./SideMenu.tsx";
import Content from "../layout/Content.tsx";
import AgentChatView from "./views/AgentChatView.tsx";
import FloatingActions from "./FloatingActions.tsx";

export default function JChatMindLayout() {
  // 深色/浅色主题（默认深色）
  const [isDark, setIsDark] = useState(() => {
    const saved = localStorage.getItem("jchatmind-theme");
    if (saved !== null) return saved === "dark";
    return true; // 默认深色
  });

  // 侧边栏可见性（默认显示）
  const [sidebarVisible, setSidebarVisible] = useState(true);

  // 同步主题到 DOM
  useEffect(() => {
    document.documentElement.setAttribute(
      "data-theme",
      isDark ? "dark" : "light",
    );
    localStorage.setItem("jchatmind-theme", isDark ? "dark" : "light");
  }, [isDark]);

  const toggleTheme = useCallback(() => {
    setIsDark((prev) => !prev);
  }, []);

  const toggleSidebar = useCallback(() => {
    setSidebarVisible((prev) => !prev);
  }, []);

  return (
    <Layout>
      {sidebarVisible && (
        <Sidebar>
          <SideMenu />
        </Sidebar>
      )}
      <Content>
        {/* 顶部导航栏 */}
        <header className="h-12 flex items-center px-5 bg-white dark:bg-[#090d17]/90 border-b border-gray-100 dark:border-[rgba(0,229,255,0.2)] shrink-0 transition-colors duration-300">
          {!sidebarVisible && (
            <button
              onClick={toggleSidebar}
              className="mr-3 w-7 h-7 flex items-center justify-center rounded-md text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors cursor-pointer"
              title="显示侧边栏"
            >
              ≡
            </button>
          )}
          <span className="text-xs text-gray-400 dark:text-[#5a7090] font-medium tracking-wide">
            AI Agent 对话平台
          </span>
          <div className="flex-1" />
          <a
            href="https://github.com"
            target="_blank"
            rel="noopener noreferrer"
            className="text-xs text-gray-400 dark:text-[#5a7090] hover:text-gray-600 dark:hover:text-[#00e5ff] transition-colors"
          >
            GitHub ↗
          </a>
        </header>
        <div className="flex-1 min-h-0">
          <Routes>
            <Route path="/" element={<AgentChatView />} />
            <Route path="/agent" element={<AgentChatView />} />
            <Route path="/chat" element={<AgentChatView />} />
            <Route path="/chat/:chatSessionId" element={<AgentChatView />} />
          </Routes>
        </div>
        {/* 底部信息栏 */}
        <footer className="h-8 flex items-center justify-center px-5 bg-white dark:bg-[#090d17]/90 border-t border-gray-100 dark:border-[rgba(0,229,255,0.2)] shrink-0 transition-colors duration-300">
          <span className="text-xs text-gray-300 dark:text-[#3a5070]">
            © {new Date().getFullYear()} JChatMind · Powered by Spring AI &
            React
          </span>
        </footer>
      </Content>

      {/* 右下角浮动按钮 */}
      <FloatingActions
        sidebarVisible={sidebarVisible}
        onToggleSidebar={toggleSidebar}
        isDark={isDark}
        onToggleTheme={toggleTheme}
      />
    </Layout>
  );
}
