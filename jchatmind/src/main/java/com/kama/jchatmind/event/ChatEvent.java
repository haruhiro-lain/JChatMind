package com.kama.jchatmind.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatEvent {
    private String agentId;
    private String sessionId;
    private String userInput;

    /**
     * 用户自定义的 API Key（可选），为空时使用预设 Key。
     */
    private String apiKey;
}
