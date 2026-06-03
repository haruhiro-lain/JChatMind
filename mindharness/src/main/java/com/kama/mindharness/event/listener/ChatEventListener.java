package com.kama.mindharness.event.listener;

import com.kama.mindharness.agent.MindHarness;
import com.kama.mindharness.agent.MindHarnessFactory;
import com.kama.mindharness.event.ChatEvent;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@AllArgsConstructor
public class ChatEventListener {

    private final MindHarnessFactory mindHarnessFactory;

    @Async
    @EventListener
    public void handle(ChatEvent event) {
        // 创建一个 Agent 实例处理聊天事件，传入用户自定义 API Key（可为空）
        MindHarness mindHarness = mindHarnessFactory.create(event.getAgentId(), Objects.requireNonNull(event.getSessionId()), event.getApiKey());
        mindHarness.run();
    }
}
