package com.kama.mindharness.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

import java.util.Objects;

@Configuration
public class MultiChatClientConfig {

    @Value("${spring.ai.deepseek.api-key}")
    private String deepSeekApiKey;

    // DeepSeek Pro (V4)
    @Bean("deepseek-pro")
    public ChatClient deepSeekProChatClient() {
        DeepSeekApi api = DeepSeekApi.builder().apiKey(deepSeekApiKey).build();
        DeepSeekChatModel chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(api)
                .defaultOptions(DeepSeekChatOptions.builder().model("deepseek-chat").build())
                .build();
        return ChatClient.create(Objects.requireNonNull(chatModel));
    }

    // DeepSeek Flash (V4)
    @Bean("deepseek-flash")
    public ChatClient deepSeekFlashChatClient() {
        DeepSeekApi api = DeepSeekApi.builder().apiKey(deepSeekApiKey).build();
        DeepSeekChatModel chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(api)
                .defaultOptions(DeepSeekChatOptions.builder().model("deepseek-reasoner").build())
                .build();
        return ChatClient.create(Objects.requireNonNull(chatModel));
    }

    // ZhiPu AI（仅当配置了 API Key 时才启用）
    @Bean("glm-4.6")
    @ConditionalOnExpression("not '${spring.ai.zhipuai.api-key:}'.isEmpty()")
    public ChatClient zhiPuAiChatClient(@NonNull ZhiPuAiChatModel zhiPuAiChatModel) {
        return ChatClient.create(zhiPuAiChatModel);
    }
}
