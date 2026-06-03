package com.zzy.aicodegenerator.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zzy.aicodegenerator.ai.guardrail.PromptSafetyInputGuardrail;
import com.zzy.aicodegenerator.ai.tools.*;
import com.zzy.aicodegenerator.model.enums.CodeGenTypeEnum;
import com.zzy.aicodegenerator.service.ChatHistoryService;
import com.zzy.aicodegenerator.utils.SpringContextUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
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
 *
 */
@Slf4j
@Configuration
public class AICodeGeneratorServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * AI 服务缓存
     * 缓存策略：缓存 1000 个实例，每个实例缓存 30 分钟，每次访问 10 分钟后过期
     */
    private final Cache<String, AICodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除： 缓存键：{} ,原因：{}", key, cause);
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

        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据 appId 获取 AI 代码生成器服务
     *
     * @param appId 应用 ID
     * @return AICodeGeneratorService 实例
     */
    public AICodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 创建 AI 代码生成器服务实例
     * <p>
     * 根据 appId 构建独立的对话记忆窗口，并创建包含聊天模型和流式聊天模型的 AI 服务实例。
     * 每个应用 ID 拥有独立的对话历史记忆，支持最多保留 20 条消息。
     * </p>
     *
     * @param appId       应用唯一标识符，用于隔离不同应用的对话上下文
     * @param codeGenType 代码生成类型
     * @return AICodeGeneratorService 配置完成的 AI 代码生成器服务实例
     */
    public AICodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
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
        return switch (codeGenType) {
            // 生成 HTML、多文件代码，使用流式对话模型
            case HTML, MULTI_FILE -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel streamingChatModel = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AICodeGeneratorService.class)
                        .chatModel(chatModel)
                        .chatMemory(chatMemory)
                        .streamingChatModel(streamingChatModel)
                        .inputGuardrails(new PromptSafetyInputGuardrail()) // 对用户输入进行安全检查
//                        .outputGuardrails(new RetryOutputGuardrail())   // 对模型输出进行检查
                        .build();
            }
            // 生成 Vue 项目，使用工具调用和推理模型
            case VUE_PROJECT -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AICodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(reasoningStreamingChatModel)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools(
                                new FileWriteTool(),
                                new FileReadTool(),
                                new FileModifyTool(),
                                new FileDirReadTool(),
                                new FileDeleteTool())
                        .hallucinatedToolNameStrategy(
                                toolExecutionRequest -> ToolExecutionResultMessage.from(toolExecutionRequest,
                                        "Error: there is not tool called" + toolExecutionRequest.name()))
                        .inputGuardrails(new PromptSafetyInputGuardrail()) // 对用户输入进行安全检查
//                        .outputGuardrails(new RetryOutputGuardrail())   // 对模型输出进行检查
                        .build();
            }
            default -> throw new IllegalArgumentException("不支持的代码生成类型：" + codeGenType);
        };
    }

    /**
     * 构建缓存键
     *
     * @param appId       应用唯一标识符
     * @param codeGenType 码生成类型
     * @return 缓存键
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType;
    }
}
