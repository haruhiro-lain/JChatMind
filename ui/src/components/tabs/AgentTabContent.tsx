import React, { useMemo } from "react";
import { Button, Dropdown, Modal } from "antd";
import type { MenuProps } from "antd";
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  MoreOutlined,
  RobotOutlined,
} from "@ant-design/icons";
import type { AgentVO } from "../../api/api.ts";
import { formatDateTime, getAgentEmoji } from "../../utils";

interface AgentTabContentProps {
  agents: AgentVO[];
  onCreateAgentClick: () => void;
  onSelectAgent: (agentId: string) => void;
  onEditAgent?: (agent: AgentVO) => void;
  onDeleteAgent?: (agentId: string) => void;
}

const AgentTabContent: React.FC<AgentTabContentProps> = ({
  agents,
  onCreateAgentClick,
  onSelectAgent,
  onEditAgent,
  onDeleteAgent,
}) => {
  // 为每个 agent 生成 emoji
  const agentsWithEmoji = useMemo(() => {
    return agents.map((agent) => ({
      ...agent,
      emoji: getAgentEmoji(agent.id),
    }));
  }, [agents]);

  // 创建右键菜单
  const getContextMenuItems = (agent: AgentVO): MenuProps["items"] => {
    const items: MenuProps["items"] = [];

    if (onEditAgent) {
      items.push({
        key: "edit",
        label: "编辑",
        icon: <EditOutlined />,
        onClick: (e) => {
          e.domEvent.stopPropagation();
          onEditAgent(agent);
        },
      });
    }

    if (onDeleteAgent) {
      items.push({
        key: "delete",
        label: "删除",
        icon: <DeleteOutlined />,
        danger: true,
        onClick: (e) => {
          e.domEvent.stopPropagation();
          Modal.confirm({
            title: "确定要删除这个智能体吗？",
            content: "删除后将无法恢复",
            okText: "确定",
            cancelText: "取消",
            okType: "danger",
            onOk: () => {
              onDeleteAgent(agent.id);
            },
          });
        },
      });
    }

    return items;
  };

  return (
    <div className="flex flex-col h-full">
      <Button
        type="primary"
        icon={<PlusOutlined />}
        onClick={onCreateAgentClick}
        className="w-full mb-3 h-9 rounded-lg text-[13px] font-medium"
      >
        新建助手
      </Button>
      <div className="flex-1 overflow-y-auto rounded-xl bg-gray-50/50 dark:bg-[#0e1422]/60 p-2 transition-colors duration-300">
        {agents.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-300 dark:text-[#3a5070]">
            <RobotOutlined className="text-3xl mb-3 opacity-50" />
            <p className="text-[13px] font-medium">暂无智能体</p>
            <p className="text-[11px] mt-1">点击上方按钮添加</p>
          </div>
        ) : (
          <div className="space-y-1.5">
            {agentsWithEmoji.map((agent) => {
              const menuItems = getContextMenuItems(agent);
              const hasMenu = menuItems && menuItems.length > 0;
              return (
                <div
                  key={agent.id}
                  onClick={() => onSelectAgent(agent.id)}
                  className="w-full px-3.5 py-3 rounded-xl bg-white dark:bg-[#0e1422]/70 cursor-pointer card-hover border border-gray-100 dark:border-[rgba(0,229,255,0.22)] group relative transition-colors duration-300 hover:dark:border-[rgba(0,229,255,0.4)] hover:dark:shadow-[0_0_12px_rgba(0,229,255,0.08)]"
                >
                  <div className="flex items-start gap-3">
                    <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-50 to-purple-50 dark:from-[rgba(0,229,255,0.1)] dark:to-[rgba(255,107,203,0.1)] flex items-center justify-center shrink-0 text-lg border border-indigo-100 dark:border-[rgba(0,229,255,0.15)]">
                      {agent.emoji}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="font-semibold text-[13px] text-gray-900 dark:text-[#ecf1fa] truncate leading-tight">
                        {agent.name}
                      </div>
                      {agent.description && (
                        <div className="text-[11px] text-gray-400 dark:text-[#5a7090] mt-1 line-clamp-1 leading-relaxed">
                          {agent.description}
                        </div>
                      )}
                      {agent.updatedAt && (
                        <div className="text-[10px] text-gray-300 dark:text-[#3a5070] mt-1.5 font-medium">
                          {formatDateTime(agent.updatedAt)}
                        </div>
                      )}
                    </div>
                    {hasMenu && (
                      <div
                        onClick={(e) => e.stopPropagation()}
                        onContextMenu={(e) => e.stopPropagation()}
                        className="shrink-0"
                      >
                        <Dropdown
                          menu={{ items: menuItems }}
                          trigger={["contextMenu", "click"]}
                          placement="bottomRight"
                        >
                          <Button
                            type="text"
                            size="small"
                            icon={<MoreOutlined />}
                            onClick={(e) => e.stopPropagation()}
                            className="text-gray-300 dark:text-[#ecf1fa] hover:text-gray-500 dark:hover:text-[#00e5ff] cursor-pointer"
                          />
                        </Dropdown>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default AgentTabContent;
