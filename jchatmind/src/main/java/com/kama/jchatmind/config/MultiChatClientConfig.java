package com.kama.jchatmind.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        return ChatClient.create(chatModel);
    }

    // DeepSeek Flash (V4)
    @Bean("deepseek-flash")
    public ChatClient deepSeekFlashChatClient() {
        DeepSeekApi api = DeepSeekApi.builder().apiKey(deepSeekApiKey).build();
        DeepSeekChatModel chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(api)
                .defaultOptions(DeepSeekChatOptions.builder().model("deepseek-reasoner").build())
                .build();
        return ChatClient.create(chatModel);
    }

    // ZhiPu AI
    @Bean("glm-4.6")
    public ChatClient zhiPuAiChatClient(ZhiPuAiChatModel zhiPuAiChatModel) {
        return ChatClient.create(zhiPuAiChatModel);
    }
}
