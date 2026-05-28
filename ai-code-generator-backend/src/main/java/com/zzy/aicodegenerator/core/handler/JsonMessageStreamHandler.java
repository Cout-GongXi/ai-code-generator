package com.zzy.aicodegenerator.core.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zzy.aicodegenerator.ai.model.message.*;
import com.zzy.aicodegenerator.model.entity.User;
import com.zzy.aicodegenerator.model.enums.ChatHistoryMessageTypeEnum;
import com.zzy.aicodegenerator.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

/**
 * Json消息流处理器
 * 处理 VUE_PROJECT 类型的复杂流式响应，包含工具调用信息
 *
 * @author zzy
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {

    /**
     * 处理 JSON 消息流
     *
     * @param originFlux         原始消息流
     * @param chatHistoryService 对话历史服务
     * @param appId              应用 ID
     * @param loginUser          登录用户
     * @return 处理后的消息流
     */
    public Flux<String> handle(Flux<String> originFlux, ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        // 创建一个 StringBuilder 用于收集 AI 响应内容
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 用于跟踪已经见过的工具ID，判断是否是第一次调用
        Set<String> seenToolIds = new HashSet<>();
        return originFlux.map(chunk -> {
                    // 解析JSON 消息块
                    return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds);
                })
                .filter(StrUtil::isNotEmpty)
                .doOnComplete(() -> {
                    // 流式响应完成后，添加 AI 消息到对话历史
                    String aiResponse = chatHistoryStringBuilder.toString();
                    chatHistoryService.addChatMessage(appId, loginUser.getId(), aiResponse, ChatHistoryMessageTypeEnum.AI.getValue());
                }).doOnError(error -> {
                    // 流式响应出错时，添加错误消息到对话历史
                    String errMessage = "AI 响应错误： " + error.getMessage();
                    chatHistoryService.addChatMessage(appId, loginUser.getId(), errMessage, ChatHistoryMessageTypeEnum.AI.getValue());
                });
    }

    /**
     * 处理 JSON 消息块
     *
     * @param chunk                    消息块
     * @param chatHistoryStringBuilder 用于收集 AI 响应内容的 StringBuilder
     * @param seenToolIds              用于跟踪已经见过的工具ID的 Set
     * @return 处理后的消息块
     */
    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, Set<String> seenToolIds) {
        // 解析 JSON
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);

        StreamMessageTypeEnum messageType = StreamMessageTypeEnum.getByValue(streamMessage.getType());
        switch (messageType) {
            case AI_RESPONSE -> {
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData();
                // 直接响应
                chatHistoryStringBuilder.append(data);
                return data;
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                // 判断是否是第一次调用该工具
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    //第一次调用工具，记录 Id 并完整返回工具信息
                    seenToolIds.add(toolId);
                    return "\n\n[🔧选择工具] 写入文件\n\n";
                } else {
                    // 不是第一次调用该工具，则返回空
                    return "";
                }
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                String relativeFilePath = jsonObject.getStr("relativeFilePath");
                String suffix = FileUtil.getSuffix(relativeFilePath);
                String content = jsonObject.getStr("content");
                String result = String.format("""
                        [工具调用] 写入文件 %s
                        ```%s
                        %s
                        ```
                        """, relativeFilePath, suffix, content);
                // 输出前端和要持久化的内容
                String output = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(output);
                return output;

            }
            default -> {
                log.error("未知消息类型: {}", messageType);
                return "";
            }
        }
    }

}
