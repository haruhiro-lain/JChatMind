package com.mindharness;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(excludeName = {
    "org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiChatAutoConfiguration",
    "org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiEmbeddingAutoConfiguration",
    "org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiImageAutoConfiguration"
})
public class MindHarnessApplication {

    public static void main(String[] args) {
        SpringApplication.run(MindHarnessApplication.class, args);
    }

}
