import React, { useState, useMemo } from "react";
import { Typography, Select } from "antd";
import {
  BulbOutlined,
  MessageOutlined,
  RobotOutlined,
  DownOutlined,
} from "@ant-design/icons";
import { Sender } from "@ant-design/x";
import { useNavigate } from "react-router-dom";
import {
  type AgentVO,
  createChatMessage,
  createChatSession,
} from "../../../api/api.ts";
import { getAgentEmoji } from "../../../utils";
import { useChatSessions } from "../../../hooks/useChatSessions.ts";

const { Title, Text } = Typography;

interface DefaultAgentChatViewProps {
  handleSendMessage: (message: string) => void;
  loading: boolean;
  agents: AgentVO[];
}

const EmptyAgentChatView: React.FC<DefaultAgentChatViewProps> = ({
  loading,
  agents,
}) => {
  const [message, setMessage] = useState("");
  const [selectedAgentId, setSelectedAgentId] = useState<string | null>(null);

  const navigate = useNavigate();
  const { refreshChatSessions } = useChatSessions();

  // 为每个 agent 生成 emoji
  const agentsWithEmoji = useMemo(() => {
    return agents.map((agent) => ({
      ...agent,
      emoji: getAgentEmoji(agent.id),
    }));
  }, [agents]);

  // 计算实际选中的 agent ID（如果用户没有选择，则使用默认的第一个）
  const effectiveAgentId = useMemo(() => {
    if (selectedAgentId) {
      return selectedAgentId;
    }
    return agents.length > 0 ? agents[0].id : null;
  }, [selectedAgentId, agents]);

  return (
    <div className="flex flex-col h-full">
      {/* Agent 选择器 - 顶部 */}
      {agents.length > 0 && (
        <div className="border-b border-gray-100 dark:border-[rgba(0,229,255,0.2)] bg-white dark:bg-[#090d17]/90 px-5 py-3 transition-colors duration-300">
          <div className="flex items-center justify-start">
            <Select
              value={effectiveAgentId}
              onChange={(value) => setSelectedAgentId(value)}
              style={{ width: 200 }}
              className="agent-selector"
              suffixIcon={<DownOutlined className="text-gray-400" />}
              placeholder="选择智能体助手"
              optionRender={(option) => (
                <div className="flex items-center gap-2">
                  <span className="text-lg">
                    {agentsWithEmoji.find((a) => a.id === option.value)?.emoji}
                  </span>
                  <span className="text-sm">{option.label}</span>
                </div>
              )}
              options={agentsWithEmoji.map((agent) => ({
                value: agent.id,
                label: agent.name,
              }))}
            />
          </div>
        </div>
      )}
      <div className="flex-1 flex items-center justify-center p-6">
        <div className="max-w-xl w-full space-y-5">
          {/* 标题区域 - 模仿 haruhiro-lain 的简洁风格 */}
          <div className="text-center mb-6">
            <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center mx-auto mb-4 shadow-lg shadow-indigo-200">
              <RobotOutlined className="text-white text-2xl" />
            </div>
            <Title level={2} className="!mb-1 !text-[22px] !font-bold !text-gray-900 dark:!text-[#ecf1fa] !tracking-tight">
              开始新的对话?
            </Title>
            <Text className="!text-[14px] !text-gray-400 dark:!text-[#5a7090]">
              选择一个智能体助手开始聊天，或直接发送消息息创建新会话
            </Text>
          </div>

          {/* 特性卡片 - 模仿 haruhiro-lain 的文章卡片风格 */}
          <div className="space-y-3">
            <div className="p-4 rounded-xl border border-gray-100 dark:border-[rgba(0,229,255,0.22)] bg-white dark:bg-[#0e1422]/70 card-hover cursor-default transition-colors duration-300 hover:dark:border-[rgba(0,229,255,0.4)] hover:dark:shadow-[0_0_12px_rgba(0,229,255,0.08)]">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-50 to-indigo-50 dark:from-[rgba(0,229,255,0.1)] dark:to-[rgba(255,107,203,0.1)] flex items-center justify-center border border-blue-100 dark:border-[rgba(0,229,255,0.15)]">
                  <RobotOutlined className="text-indigo-500 dark:text-[#00e5ff] text-lg" />
                </div>
                <div className="flex-1">
                  <div className="text-[14px] font-semibold text-gray-900 dark:text-[#ecf1fa]">智能对话</div>
                  <div className="text-[12px] text-gray-400 dark:text-[#5a7090] mt-0.5">
                    与 AI 助手进行智能对话，获取帮助和建议
                  </div>
                </div>
              </div>
            </div>

            <div className="p-4 rounded-xl border border-gray-100 dark:border-[rgba(0,229,255,0.22)] bg-white dark:bg-[#0e1422]/70 card-hover cursor-default transition-colors duration-300 hover:dark:border-[rgba(0,229,255,0.4)] hover:dark:shadow-[0_0_12px_rgba(0,229,255,0.08)]">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-green-50 to-teal-50 dark:from-[rgba(0,229,255,0.1)] dark:to-[rgba(0,229,255,0.05)] flex items-center justify-center border border-green-100 dark:border-[rgba(0,229,255,0.15)]">
                  <BulbOutlined className="text-teal-500 dark:text-[#00e5ff] text-lg" />
                </div>
                <div className="flex-1">
                  <div className="text-[14px] font-semibold text-gray-900 dark:text-[#ecf1fa]">工具调用</div>
                  <div className="text-[12px] text-gray-400 dark:text-[#5a7090] mt-0.5">
                    Agent 可自主调用工具完成任务，如数据库查询
                  </div>
                </div>
              </div>
            </div>

            <div className="p-4 rounded-xl border border-gray-100 dark:border-[rgba(0,229,255,0.22)] bg-white dark:bg-[#0e1422]/70 card-hover cursor-default transition-colors duration-300 hover:dark:border-[rgba(0,229,255,0.4)] hover:dark:shadow-[0_0_12px_rgba(0,229,255,0.08)]">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-orange-50 to-amber-50 dark:from-[rgba(255,107,203,0.1)] dark:to-[rgba(255,107,203,0.05)] flex items-center justify-center border border-orange-100 dark:border-[rgba(255,107,203,0.15)]">
                  <MessageOutlined className="text-orange-500 dark:text-[#ff6bcb] text-lg" />
                </div>
                <div className="flex-1">
                  <div className="text-[14px] font-semibold text-gray-900 dark:text-[#ecf1fa]">快速开始</div>
                  <div className="text-[12px] text-gray-400 dark:text-[#5a7090] mt-0.5">
                    在下方输入框输入消息，立即开始对话?
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div className="border-t border-gray-100 dark:border-[rgba(0,229,255,0.22)] bg-white dark:bg-[#090d17]/90 transition-colors duration-300">
        {/* 输入�?*/}
        <div className="px-4 pb-4 pt-4">
          <Sender
            onSubmit={async () => {
              if (!effectiveAgentId) return;
              console.log("发送消息", message);
              const response = await createChatSession({
                agentId: effectiveAgentId,
                title: message.slice(0, 20),
              });
              await createChatMessage({
                sessionId: response.chatSessionId ?? "",
                content: message,
                role: "user",
                agentId: effectiveAgentId,
              });
              // 刷新聊天会话列表
              await refreshChatSessions();
              setMessage("");
              navigate(`/chat/${response.chatSessionId}`);
            }}
            value={message}
            loading={loading}
            placeholder="输入消息开始对话..."
            onChange={(value) => {
              setMessage(value);
            }}
          />
        </div>
      </div>
    </div>
  );
};

export default EmptyAgentChatView;
