package com.kama.mindharness.event.listener;

import com.kama.mindharness.agent.MindHarness;
import com.kama.mindharness.agent.MindHarnessFactory;
import com.kama.mindharness.event.ChatEvent;
import com.kama.mindharness.message.SseMessage;
import com.kama.mindharness.service.SseService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@AllArgsConstructor
public class ChatEventListener {

    private final MindHarnessFactory mindHarnessFactory;
    private final SseService sseService;

    @Async
    @EventListener
    public void handle(ChatEvent event) {
        try {
            // 创建一个 Agent 实例处理聊天事件，传入用户自定义 API Key（可为空）
            MindHarness mindHarness = mindHarnessFactory.create(event.getAgentId(), Objects.requireNonNull(event.getSessionId()), event.getApiKey());
            mindHarness.run();
        } catch (Exception e) {
            log.error("ChatEventListener 处理失败: sessionId={}", event.getSessionId(), e);
            // 如果 MindHarness 还未发送错误 SSE（比如 MindHarnessFactory.create 阶段就失败了），这里兜底发送
            try {
                SseMessage errorSse = SseMessage.builder()
                        .type(SseMessage.Type.AI_ERROR)
                        .payload(SseMessage.Payload.builder()
                                .statusText("AI 服务异常，请稍后重试")
                                .done(true)
                                .build())
                        .build();
                sseService.send(event.getSessionId(), errorSse);
            } catch (Exception ignored) {
                log.warn("发送错误 SSE 失败", ignored);
            }
        }
    }
}
