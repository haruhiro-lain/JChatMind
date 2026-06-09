package com.mindharness.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindharness.message.SseMessage;
import com.mindharness.service.SseService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@AllArgsConstructor
public class SseServiceImpl implements SseService {

    private final ConcurrentMap<String, SseEmitter> clients = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Override
    public SseEmitter connect(String chatSessionId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        clients.put(chatSessionId, emitter);

        try {
            emitter.send(SseEmitter.event()
                    .name("init")
                    .data("connected")
            );
        } catch (IOException e) {
            clients.remove(chatSessionId);
            throw new RuntimeException(e);
        }

        emitter.onCompletion(() -> clients.remove(chatSessionId));
        emitter.onTimeout(() -> clients.remove(chatSessionId));
        emitter.onError((error) -> clients.remove(chatSessionId));

        return emitter;
    }

    @Override
    public void send(String chatSessionId, SseMessage message) {
        SseEmitter emitter = clients.get(chatSessionId);

        if (emitter == null) {
            log.debug("No SSE client for session {}, skipping message", chatSessionId);
            return;
        }

        try {
            String sseMessageStr = objectMapper.writeValueAsString(message);
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(Objects.requireNonNull(sseMessageStr))
            );
        } catch (IOException e) {
            // 客户端已断开，移除并静默处理
            clients.remove(chatSessionId);
            log.debug("SSE send failed for session {}: client disconnected", chatSessionId);
        }
    }
}
