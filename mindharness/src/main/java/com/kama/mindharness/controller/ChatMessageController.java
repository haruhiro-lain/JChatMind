package com.kama.mindharness.controller;

import com.kama.mindharness.model.common.ApiResponse;
import com.kama.mindharness.model.request.CreateChatMessageRequest;
import com.kama.mindharness.model.request.UpdateChatMessageRequest;
import com.kama.mindharness.model.response.CreateChatMessageResponse;
import com.kama.mindharness.model.response.GetChatMessagesResponse;
import com.kama.mindharness.service.ChatMessageFacadeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class ChatMessageController {

    private final ChatMessageFacadeService chatMessageFacadeService;

    // 根据 sessionId 查询聊天消息
    @GetMapping("/chat-messages/session/{sessionId}")
    public ApiResponse<GetChatMessagesResponse> getChatMessagesBySessionId(@PathVariable String sessionId) {
        return ApiResponse.success(chatMessageFacadeService.getChatMessagesBySessionId(sessionId));
    }

    // 创建聊天消息
    @PostMapping("/chat-messages")
    public ApiResponse<CreateChatMessageResponse> createChatMessage(@RequestBody CreateChatMessageRequest request) {
        return ApiResponse.success(chatMessageFacadeService.createChatMessage(request));
    }

    // 删除聊天消息
    @DeleteMapping("/chat-messages/{chatMessageId}")
    public ApiResponse<Void> deleteChatMessage(@PathVariable String chatMessageId) {
        chatMessageFacadeService.deleteChatMessage(chatMessageId);
        return ApiResponse.success();
    }

    // 更新聊天消息
    @PatchMapping("/chat-messages/{chatMessageId}")
    public ApiResponse<Void> updateChatMessage(@PathVariable String chatMessageId, @RequestBody UpdateChatMessageRequest request) {
        chatMessageFacadeService.updateChatMessage(chatMessageId, request);
        return ApiResponse.success();
    }
}
