package com.zzy.aicodegenerator.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zzy.aicodegenerator.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 服务创建工厂
 *
 * @author zzy
 * @date 2024/6/11
 */
@Slf4j
@Configuration
public class AICodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;
    /**
     * AI 服务缓存
     * 缓存策略：缓存 1000 个实例，每个实例缓存 30 分钟，每次访问 10 分钟后过期
     */
    private final Cache<Long, AICodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除： appId：{} ,原因：{}", key, cause);
            })
            .build();

    /**
     * 创建 AI 代码生成器服务
     *
     * @return AICodeGeneratorService 实例
     */
    @Bean
    public AICodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0);
    }

    /**
     * 根据 appId 获取 AI 代码生成器服务
     *
     * @param appId 应用 ID
     * @return AICodeGeneratorService 实例
     */
    public AICodeGeneratorService getAiCodeGeneratorService(long appId) {
        return serviceCache.get(appId, this::createAiCodeGeneratorService);
    }

    /**
     * 创建 AI 代码生成器服务实例
     * <p>
     * 根据 appId 构建独立的对话记忆窗口，并创建包含聊天模型和流式聊天模型的 AI 服务实例。
     * 每个应用 ID 拥有独立的对话历史记忆，支持最多保留 20 条消息。
     * </p>
     *
     * @param appId 应用唯一标识符，用于隔离不同应用的对话上下文
     * @return AICodeGeneratorService 配置完成的 AI 代码生成器服务实例
     */
    public AICodeGeneratorService createAiCodeGeneratorService(long appId) {
        log.info("创建 AI 服务实例： appId：{}", appId);

        // 根据 appId 构建独立的应用对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 从数据库中加载对话历史到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        return AiServices.builder(AICodeGeneratorService.class)
                .chatModel(chatModel)
                .chatMemory(chatMemory)
                .streamingChatModel(streamingChatModel)
                .build();
    }
}
