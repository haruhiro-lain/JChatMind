import React, { useState } from "react";
import { Sender } from "@ant-design/x";

export interface SendMessageData {
  text: string;
}

interface AgentChatInputProps {
  onSend: (data: SendMessageData) => void;
}

const AgentChatInput: React.FC<AgentChatInputProps> = ({ onSend }) => {
  const [message, setMessage] = useState("");

  return (
    <Sender
      onSubmit={() => {
        const trimmed = message.trim();
        if (!trimmed) return;
        onSend({ text: trimmed });
        setMessage("");
      }}
      placeholder="输入消息..."
      value={message}
      onChange={setMessage}
    />
  );
};

export default AgentChatInput;
