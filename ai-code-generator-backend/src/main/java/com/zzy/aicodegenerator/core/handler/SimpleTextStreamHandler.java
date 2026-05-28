package com.zzy.aicodegenerator.core.handler;

import com.zzy.aicodegenerator.model.entity.User;
import com.zzy.aicodegenerator.model.enums.ChatHistoryMessageTypeEnum;
import com.zzy.aicodegenerator.service.ChatHistoryService;
import reactor.core.publisher.Flux;

/**
 * 简单的文本流处理器，用于处理 AI 输出的文本流。
 */
public class SimpleTextStreamHandler {
    /**
     * 处理传统流（HTML，MULTI_FILE）
     * 直接收集完整的文本响应，并保存到数据库中
     *
     * @param originFlux 原始文本流
     * @param chatHistoryService 对话历史服务
     * @param appId 应用 ID
     * @param loginUser 登录用户
     * @return 处理后的文本流
     */
    public Flux<String> handle(Flux<String> originFlux, ChatHistoryService chatHistoryService,
                               Long appId, User loginUser) {
        StringBuilder aiResponseBuilder = new StringBuilder();
        return originFlux
                .map(chunk -> {
                    // 收集 AI 响应内容
                    aiResponseBuilder.append(chunk);
                    return chunk;
                })
                .doOnComplete(() -> {
                    // 流式返回完成后，保存 AI 响应内容
                    String aiResponse = aiResponseBuilder.toString();
                    chatHistoryService.addChatMessage(appId, loginUser.getId(), aiResponse, ChatHistoryMessageTypeEnum.AI.getValue());
                })
                .doOnError(error -> {
                    // 如果流式返回发生错误，保存错误信息
                    String errMessage = "AI 响应错误： " + error.getMessage();
                    chatHistoryService.addChatMessage(appId, loginUser.getId(), errMessage, ChatHistoryMessageTypeEnum.AI.getValue());
                });
    }
}
