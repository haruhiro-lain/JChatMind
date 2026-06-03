package com.kama.mindharness.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kama.mindharness.agent.tools.Tool;
import com.kama.mindharness.config.ChatClientRegistry;
import com.kama.mindharness.config.DynamicChatClientFactory;
import com.kama.mindharness.converter.AgentConverter;
import com.kama.mindharness.converter.ChatMessageConverter;
import com.kama.mindharness.mapper.AgentMapper;
import com.kama.mindharness.model.dto.AgentDTO;
import com.kama.mindharness.model.dto.ChatMessageDTO;
import com.kama.mindharness.model.entity.Agent;
import com.kama.mindharness.service.ChatMessageFacadeService;
import com.kama.mindharness.service.SseService;
import com.kama.mindharness.service.ToolFacadeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.aop.support.AopUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MindHarnessFactory {

    private static final Logger log = LoggerFactory.getLogger(MindHarnessFactory.class);
    private final ChatClientRegistry chatClientRegistry;
    private final DynamicChatClientFactory dynamicChatClientFactory;
    private final SseService sseService;
    private final AgentMapper agentMapper;
    private final AgentConverter agentConverter;
    private final ToolFacadeService toolFacadeService;
    private final ChatMessageFacadeService chatMessageFacadeService;
    private final ChatMessageConverter chatMessageConverter;

    // 运行时 Agent 配置
    private AgentDTO agentConfig;

    public MindHarnessFactory(
            ChatClientRegistry chatClientRegistry,
            DynamicChatClientFactory dynamicChatClientFactory,
            SseService sseService,
            AgentMapper agentMapper,
            AgentConverter agentConverter,
            ToolFacadeService toolFacadeService,
            ChatMessageFacadeService chatMessageFacadeService,
            ChatMessageConverter chatMessageConverter
    ) {
        this.chatClientRegistry = chatClientRegistry;
        this.dynamicChatClientFactory = dynamicChatClientFactory;
        this.sseService = sseService;
        this.agentMapper = agentMapper;
        this.agentConverter = agentConverter;
        this.toolFacadeService = toolFacadeService;
        this.chatMessageFacadeService = chatMessageFacadeService;
        this.chatMessageConverter = chatMessageConverter;
    }

    private Agent loadAgent(String agentId) {
        return agentMapper.selectById(agentId);
    }

    /**
     * 将数据库中存储的记忆恢复成 List<Message> 结构
     */
    private List<Message> loadMemory(String chatSessionId) {
        int messageLength = agentConfig.getChatOptions().getMessageLength();
        List<ChatMessageDTO> chatMessages = chatMessageFacadeService.getChatMessagesBySessionIdRecently(chatSessionId, messageLength);
        List<Message> memory = new ArrayList<>();
        for (ChatMessageDTO chatMessageDTO : chatMessages) {
            switch (chatMessageDTO.getRole()) {
                case SYSTEM:
                    if (!StringUtils.hasLength(chatMessageDTO.getContent())) continue;
                    memory.add(0, new SystemMessage(Objects.requireNonNull(chatMessageDTO.getContent())));
                    break;
                case USER:
                    if (!StringUtils.hasLength(chatMessageDTO.getContent())) continue;
                    memory.add(new UserMessage(Objects.requireNonNull(chatMessageDTO.getContent())));
                    break;
                case ASSISTANT:
                    memory.add(AssistantMessage.builder()
                            .content(Objects.requireNonNull(chatMessageDTO.getContent()))
                            .toolCalls(Objects.requireNonNull(chatMessageDTO.getMetadata()
                                    .getToolCalls()))
                            .build());
                    break;
                case TOOL:
                    memory.add(ToolResponseMessage.builder()
                            .responses(Objects.requireNonNull(List.of(Objects.requireNonNull(chatMessageDTO
                                    .getMetadata()
                                    .getToolResponse()))))
                            .build());
                    break;
                default:
                    log.error("不支持的 Message 类型: {}, content = {}",
                            chatMessageDTO.getRole().getRole(),
                            chatMessageDTO.getContent()
                    );
                    throw new IllegalStateException("不支持的 Message 类型");
            }
        }
        return memory;
    }

    private AgentDTO toAgentConfig(Agent agent) {
        try {
            agentConfig = agentConverter.toDTO(agent);
            return agentConfig;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("解析 Agent 配置失败", e);
        }
    }

    private List<Tool> resolveRuntimeTools(AgentDTO agentConfig) {
        // 固定工具（系统强制）
        List<Tool> runtimeTools = new ArrayList<>(toolFacadeService.getFixedTools());

        // 可选工具（按 Agent 配置）
        List<String> allowedToolNames = agentConfig.getAllowedTools();
        if (allowedToolNames == null || allowedToolNames.isEmpty()) {
            return runtimeTools;
        }

        Map<String, Tool> optionalToolMap = toolFacadeService.getOptionalTools()
                .stream()
                .collect(Collectors.toMap(Tool::getName, Function.identity()));

        for (String toolName : allowedToolNames) {
            Tool tool = optionalToolMap.get(toolName);
            if (tool != null) {
                runtimeTools.add(tool);
            }
        }
        return runtimeTools;
    }

    private List<ToolCallback> buildToolCallbacks(List<Tool> runtimeTools) {
        List<ToolCallback> callbacks = new ArrayList<>();
        for (Tool tool : runtimeTools) {
            Object target = resolveToolTarget(Objects.requireNonNull(tool));
            ToolCallback[] toolCallbacks = MethodToolCallbackProvider.builder()
                    .toolObjects(target)
                    .build()
                    .getToolCallbacks();
            callbacks.addAll(Arrays.asList(toolCallbacks));
        }
        return callbacks;
    }

    private Object resolveToolTarget(@NonNull Tool tool) {
        try {
            return AopUtils.isAopProxy(tool)
                    ? AopUtils.getTargetClass(tool)
                    : tool;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "解析工具目标对象失败: " + tool.getName(), e);
        }
    }

    private MindHarness buildAgentRuntime(
            Agent agent,
            @NonNull List<Message> memory,
            @NonNull List<ToolCallback> toolCallbacks,
            @NonNull String chatSessionId,
            String apiKey
    ) {
        ChatClient chatClient;
        if (apiKey != null && !apiKey.isBlank()) {
            // 用户提供了自定义 API Key，使用动态工厂创建
            log.info("使用用户自定义 API Key 创建 ChatClient，模型: {}", agent.getModel());
            chatClient = dynamicChatClientFactory.create(agent.getModel(), apiKey);
        } else {
            // 使用预设的 ChatClient
            chatClient = chatClientRegistry.get(agent.getModel());
        }
        if (Objects.isNull(chatClient)) {
            throw new IllegalStateException("未找到对应的 ChatClient: " + agent.getModel());
        }
        return new MindHarness(
                agent.getId(),
                agent.getName(),
                agent.getDescription(),
                agent.getSystemPrompt(),
                chatClient,
                agentConfig.getChatOptions().getMessageLength(),
                memory,
                toolCallbacks,
                chatSessionId,
                sseService,
                chatMessageFacadeService,
                chatMessageConverter
        );
    }

    /**
     * 创建一个 MindHarness 实例
     *
     * @param agentId       Agent ID
     * @param chatSessionId 聊天会话 ID
     * @param apiKey        用户自定义 API Key（可选，为空时使用预设 Key）
     */
    public MindHarness create(String agentId, @NonNull String chatSessionId, String apiKey) {
        Agent agent = loadAgent(agentId);
        AgentDTO agentConfig = toAgentConfig(agent);
        List<Message> memory = loadMemory(chatSessionId);

        // 优先使用 Agent 中配置的 apiKey，其次使用消息中携带的 apiKey
        String effectiveApiKey = (agent.getApiKey() != null && !agent.getApiKey().isBlank())
                ? agent.getApiKey()
                : apiKey;

        // 解析 agent 支持的工具调用
        List<Tool> runtimeTools = resolveRuntimeTools(agentConfig);
        // 将工具调用转换成 ToolCallback 的形式
        List<ToolCallback> toolCallbacks = buildToolCallbacks(runtimeTools);

        return buildAgentRuntime(
                agent,
                Objects.requireNonNull(memory),
                Objects.requireNonNull(toolCallbacks),
                chatSessionId,
                effectiveApiKey
        );
    }
}
