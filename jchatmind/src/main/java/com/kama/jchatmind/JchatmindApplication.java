package com.kama.jchatmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(excludeName = {
    "org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiChatAutoConfiguration",
    "org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiEmbeddingAutoConfiguration",
    "org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiImageAutoConfiguration"
})
public class JchatmindApplication {

    public static void main(String[] args) {
        SpringApplication.run(JchatmindApplication.class, args);
    }

}
