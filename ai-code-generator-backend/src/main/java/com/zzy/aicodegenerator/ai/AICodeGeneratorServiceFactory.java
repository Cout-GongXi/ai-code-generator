package com.zzy.aicodegenerator.ai;

import cn.hutool.ai.core.AIService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 服务创建工厂
 *
 * @author zzy
 * @date 2024/6/11
 */
@Configuration
public class AICodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;
    @Resource
    private StreamingChatModel streamingChatModel;

    /**
     * 创建 AI 代码生成器服务
     *
     * @return AICodeGeneratorService 实例
     */
    @Bean
    public AICodeGeneratorService aiCodeGeneratorService() {

        return AiServices.builder(AICodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .build();
    }
}
