import React from "react";
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  VerticalAlignTopOutlined,
  VerticalAlignBottomOutlined,
  SunOutlined,
  MoonOutlined,
} from "@ant-design/icons";

interface FloatingActionsProps {
  sidebarVisible: boolean;
  onToggleSidebar: () => void;
  isDark: boolean;
  onToggleTheme: () => void;
}

const FloatingActions: React.FC<FloatingActionsProps> = ({
  sidebarVisible,
  onToggleSidebar,
  isDark,
  onToggleTheme,
}) => {
  const scrollToTop = () => {
    const mainContent = document.querySelector("[data-scroll-container]");
    if (mainContent) {
      mainContent.scrollTo({ top: 0, behavior: "smooth" });
    }
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const scrollToBottom = () => {
    const mainContent = document.querySelector("[data-scroll-container]");
    if (mainContent) {
      mainContent.scrollTo({
        top: mainContent.scrollHeight,
        behavior: "smooth",
      });
    }
  };

  const buttonClass =
    "w-9 h-9 flex items-center justify-center rounded-lg text-sm transition-all duration-200 border border-gray-200 dark:border-[rgba(0,229,255,0.2)] bg-white dark:bg-[#0e1422]/80 text-gray-500 dark:text-[#5a7090] hover:text-gray-900 dark:hover:text-[#00e5ff] hover:border-gray-300 dark:hover:border-[rgba(0,229,255,0.4)] hover:shadow-sm dark:hover:shadow-[0_0_12px_rgba(0,229,255,0.12)] cursor-pointer";

  return (
    <div className="fixed bottom-6 right-6 flex flex-col gap-2 z-50">
      {/* 侧边栏切换 */}
      <button
        className={buttonClass}
        onClick={onToggleSidebar}
        title={sidebarVisible ? "隐藏侧边栏" : "显示侧边栏"}
      >
        {sidebarVisible ? <MenuFoldOutlined /> : <MenuUnfoldOutlined />}
      </button>

      {/* 回到顶部 */}
      <button className={buttonClass} onClick={scrollToTop} title="回到顶部">
        <VerticalAlignTopOutlined />
      </button>

      {/* 滚动到底部 */}
      <button
        className={buttonClass}
        onClick={scrollToBottom}
        title="滚动到底部"
      >
        <VerticalAlignBottomOutlined />
      </button>

      {/* 深色/浅色切换 */}
      <button
        className={buttonClass}
        onClick={onToggleTheme}
        title={isDark ? "切换到浅色模式" : "切换到深色模式"}
      >
        {isDark ? <SunOutlined /> : <MoonOutlined />}
      </button>
    </div>
  );
};

export default FloatingActions;
