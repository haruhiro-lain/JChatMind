package com.kama.mindharness.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 动态 ChatClient 工厂，支持用户在前端输入自定义 API Key 来调用模型。
 * 预设的 API Key 依然通过 application.yaml 配置的 Bean 生效。
 */
@Component
public class DynamicChatClientFactory {

    /**
     * 根据模型名称和用户提供的 API Key 动态创建 ChatClient。
     *
     * @param model  模型名称（deepseek-pro / deepseek-flash / glm-4.6）
     * @param apiKey 用户在前端输入的 API Key
     * @return ChatClient 实例
     */
    public ChatClient create(String model, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API Key 不能为空");
        }

        return switch (model) {
            case "deepseek-pro" -> {
                DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                        .apiKey(apiKey)
                        .build();
                DeepSeekChatModel chatModel = DeepSeekChatModel.builder()
                        .deepSeekApi(deepSeekApi)
                        .defaultOptions(DeepSeekChatOptions.builder()
                                .model("deepseek-chat")
                                .build())
                        .build();
                yield ChatClient.create(Objects.requireNonNull(chatModel));
            }
            case "deepseek-flash" -> {
                DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                        .apiKey(apiKey)
                        .build();
                DeepSeekChatModel chatModel = DeepSeekChatModel.builder()
                        .deepSeekApi(deepSeekApi)
                        .defaultOptions(DeepSeekChatOptions.builder()
                                .model("deepseek-reasoner")
                                .build())
                        .build();
                yield ChatClient.create(Objects.requireNonNull(chatModel));
            }
            case "glm-4.6" -> {
                ZhiPuAiApi zhiPuAiApi = ZhiPuAiApi.builder()
                        .apiKey(apiKey)
                        .build();
                ZhiPuAiChatModel chatModel = new ZhiPuAiChatModel(
                        zhiPuAiApi,
                        ZhiPuAiChatOptions.builder()
                                .model("glm-4.6")
                                .build()
                );
                yield ChatClient.create(Objects.requireNonNull(chatModel));
            }
            default -> throw new IllegalArgumentException("不支持的模型: " + model);
        };
    }
}
