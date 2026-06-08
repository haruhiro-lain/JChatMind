package com.mindharness.controller;

import com.mindharness.card.CardData;
import com.mindharness.card.CharacterCardParser;
import com.mindharness.model.common.ApiResponse;
import com.mindharness.model.request.CreateAgentRequest;
import com.mindharness.model.request.UpdateAgentRequest;
import com.mindharness.model.response.CreateAgentResponse;
import com.mindharness.model.response.GetAgentsResponse;
import com.mindharness.model.response.ImportCardResponse;
import com.mindharness.service.AgentFacadeService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class AgentController {

    private final AgentFacadeService agentFacadeService;

    // 查询 agents
    @GetMapping("/agents")
    public ApiResponse<GetAgentsResponse> getAgents() {
        return ApiResponse.success(agentFacadeService.getAgents());
    }

    // 创建 agent
    @PostMapping("/agents")
    public ApiResponse<CreateAgentResponse> createAgent(@RequestBody CreateAgentRequest request) {
        return ApiResponse.success(agentFacadeService.createAgent(request));
    }

    // 删除 agent
    @DeleteMapping("/agents/{agentId}")
    public ApiResponse<Void> deleteAgent(@PathVariable String agentId) {
        agentFacadeService.deleteAgent(agentId);
        return ApiResponse.success();
    }

    // 更新 agent
    @PatchMapping("/agents/{agentId}")
    public ApiResponse<Void> updateAgent(@PathVariable String agentId, @RequestBody UpdateAgentRequest request) {
        agentFacadeService.updateAgent(agentId, request);
        return ApiResponse.success();
    }

    /**
     * 导入角色卡（PNG / JSON），解析并返回预览数据。
     * PNG 文件同时保存为头像。
     * 前端确认后调用 POST /api/agents 完成创建。
     */
    @PostMapping("/agents/import-card")
    public ApiResponse<ImportCardResponse> importCard(@RequestParam("file") MultipartFile file) {
        try {
            byte[] data = file.getBytes();
            CardData card = CharacterCardParser.parse(data);
            String systemPrompt = CharacterCardParser.buildSystemPrompt(card);

            // 如果是 PNG，保存为头像
            String avatarFileName = null;
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.toLowerCase().endsWith(".png")) {
                avatarFileName = java.util.UUID.randomUUID().toString() + ".png";
                java.nio.file.Path uploadDir = java.nio.file.Path.of("uploads/avatars");
                java.nio.file.Files.createDirectories(uploadDir);
                java.nio.file.Files.write(uploadDir.resolve(avatarFileName), data);
            }

            return ApiResponse.success(ImportCardResponse.builder()
                    .name(card.getName())
                    .description(card.getDescription())
                    .systemPrompt(systemPrompt)
                    .firstMessage(card.getFirstMessage())
                    .avatarFileName(avatarFileName)
                    .build());
        } catch (Exception e) {
            log.error("角色卡解析失败", e);
            return ApiResponse.error("角色卡解析失败: " + e.getMessage());
        }
    }
}
