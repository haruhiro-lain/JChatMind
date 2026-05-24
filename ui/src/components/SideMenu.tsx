import React, { useState } from "react";
import { RobotOutlined } from "@ant-design/icons";
import { Tabs, type TabsProps } from "antd";
import AgentTabContent from "./tabs/AgentTabContent.tsx";
import AddAgentModal from "./modals/AddAgentModal.tsx";
import ChatTabContent from "./tabs/ChatTabContent.tsx";
import { useAgents } from "../hooks/useAgents.ts";

interface SideMenuProps {
  children?: React.ReactNode;
}

const SideMenu: React.FC<SideMenuProps> = () => {

  const [isAddAgentModalOpen, setIsAddAgentModalOpen] = useState(false);
  const toggleAddAgentModal = () => {
    setIsAddAgentModalOpen(!isAddAgentModalOpen);
    setEditingAgent(null);
  };

  const [editingAgent, setEditingAgent] = useState<
    import("../api/api.ts").AgentVO | null
  >(null);

  const { agents, createAgentHandle, deleteAgentHandle, updateAgentHandle } =
    useAgents();

  const [activeKey, setActiveKey] = useState(() => {
    if (location.pathname.startsWith("/agent")) return "agent";
    if (location.pathname.startsWith("/chat")) return "chat";
    return "agent";
  });

  // 处理标签页切换
  const handleTabChange = (key: string) => {
    setActiveKey(key);
  };

  const items: TabsProps["items"] = [
    {
      key: "agent",
      label: <span className="select-none text-[13px]">助手</span>,
      children: (
        <AgentTabContent
          agents={agents}
          onSelectAgent={() => {}}
          onCreateAgentClick={toggleAddAgentModal}
          onEditAgent={(agent) => {
            setEditingAgent(agent);
            setIsAddAgentModalOpen(true);
          }}
          onDeleteAgent={deleteAgentHandle}
        />
      ),
    },
    {
      key: "chat",
      label: <span className="select-none text-[13px]">会话</span>,
      children: <ChatTabContent />,
    },
  ];

  return (
    <div className="flex flex-col h-full">
      {/* Logo 区域 */}
      <div className="h-14 w-full flex items-center px-5 border-b border-gray-100 dark:border-[rgba(0,229,255,0.2)] transition-colors duration-300">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shadow-sm">
            <RobotOutlined className="text-white text-base" />
          </div>
          <div className="flex flex-col leading-tight">
            <span className="text-[15px] font-semibold text-gray-900 dark:text-[#ecf1fa] tracking-tight">
              JChatMind
            </span>
            <span className="text-[10px] text-gray-400 dark:text-[#5a7090] font-medium tracking-wide">
              AI AGENT PLATFORM
            </span>
          </div>
        </div>
      </div>

      {/* Tabs 区域 */}
      <div className="flex-1 min-h-0 flex flex-col px-4">
        <Tabs
          activeKey={activeKey}
          onChange={handleTabChange}
          items={items}
          className="pt-3"
        />
      </div>
      <AddAgentModal
        open={isAddAgentModalOpen}
        onClose={toggleAddAgentModal}
        createAgentHandle={createAgentHandle}
        updateAgentHandle={updateAgentHandle}
        editingAgent={editingAgent}
      />
    </div>
  );
};

export default SideMenu;
