import React, { useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { Button, Popconfirm } from "antd";
import {
  PlusOutlined,
  MessageOutlined,
  DeleteOutlined,
} from "@ant-design/icons";
import { useChatSessions } from "../../hooks/useChatSessions.ts";
import { useAgents } from "../../hooks/useAgents.ts";

const ChatTabContent: React.FC = () => {
  const navigate = useNavigate();
  const { chatSessions, loading, deleteChatSession } = useChatSessions();
  const { agents } = useAgents();

  // 创建 agentId 到 agent 的映射
  const agentMap = useMemo(() => {
    const map = new Map<string, string>();
    agents.forEach((agent) => {
      map.set(agent.id, agent.name);
    });
    return map;
  }, [agents]);

  const handleCreateNewChat = () => {
    navigate("/chat");
  };

  const handleSelectChatSession = (chatSessionId: string) => {
    navigate(`/chat/${chatSessionId}`);
  };

  const handleDeleteChatSession = async (chatSessionId: string) => {
    await deleteChatSession(chatSessionId);
  };

  // 格式化标题显示
  const getDisplayTitle = (session: { title?: string; agentId: string }) => {
    if (session.title) {
      return session.title;
    }
    const agentName = agentMap.get(session.agentId);
    return agentName ? `与 ${agentName} 的对话` : "新对话";
  };

  return (
    <div className="flex flex-col h-full">
      <Button
        type="primary"
        icon={<PlusOutlined />}
        onClick={handleCreateNewChat}
        className="w-full mb-3 h-9 rounded-lg text-[13px] font-medium"
      >
        新建会话
      </Button>
      <div className="flex-1 min-h-0 overflow-y-auto rounded-xl bg-gray-50/50 dark:bg-[#0e1422]/60 p-2 transition-colors duration-300">
        {loading ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-300 dark:text-gray-600">
            <p className="text-[13px] font-medium">加载中...</p>
          </div>
        ) : chatSessions.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-300 dark:text-gray-600">
            <MessageOutlined className="text-3xl mb-3 opacity-50" />
            <p className="text-[13px] font-medium">暂无会话记录</p>
            <p className="text-[11px] mt-1">点击上方按钮创建</p>
          </div>
        ) : (
          <div className="space-y-1.5">
            {chatSessions.map((session) => (
              <div
                key={session.id}
                onClick={() => handleSelectChatSession(session.id)}
                className="w-full px-3.5 py-3 rounded-xl bg-white dark:bg-[#0e1422]/70 cursor-pointer card-hover border border-gray-100 dark:border-[rgba(0,229,255,0.22)] group relative transition-colors duration-300 hover:dark:border-[rgba(0,229,255,0.4)] hover:dark:shadow-[0_0_12px_rgba(0,229,255,0.08)]"
              >
                <div className="flex items-start gap-3">
                  <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-blue-50 to-indigo-50 dark:from-[rgba(0,229,255,0.1)] dark:to-[rgba(255,107,203,0.1)] flex items-center justify-center shrink-0 border border-blue-100 dark:border-[rgba(0,229,255,0.15)]">
                    <MessageOutlined className="text-blue-500 dark:text-[#00e5ff] text-sm" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="font-semibold text-[13px] text-gray-900 dark:text-[#ecf1fa] truncate leading-tight">
                      {getDisplayTitle(session)}
                    </div>
                  </div>
                  <div onClick={(e) => e.stopPropagation()}>
                    <Popconfirm
                      title="确定要删除这条聊天记录吗？"
                      description="删除后将无法恢复"
                      onConfirm={() => handleDeleteChatSession(session.id)}
                      okText="确定"
                      cancelText="取消"
                    >
                      <Button
                        type="text"
                        size="small"
                        icon={<DeleteOutlined />}
                        className="opacity-100 transition-opacity shrink-0 text-gray-300 dark:text-[#ecf1fa] hover:text-red-400 dark:hover:text-red-400"
                        onClick={(e) => e.stopPropagation()}
                      />
                    </Popconfirm>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default ChatTabContent;
