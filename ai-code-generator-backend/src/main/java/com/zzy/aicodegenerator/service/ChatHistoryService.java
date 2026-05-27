package com.zzy.aicodegenerator.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.zzy.aicodegenerator.model.dto.chathistory.ChatHistoryQueryRequest;
import com.zzy.aicodegenerator.model.entity.ChatHistory;
import com.zzy.aicodegenerator.model.entity.User;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author zzy
 */
public interface ChatHistoryService extends IService<ChatHistory> {
    /**
     * 添加对话消息（内部调用）
     *
     * @param appId       应用Id
     * @param userId      用户Id
     * @param message     对话消息
     * @param messageType 消息类型
     * @return 是否添加成功
     */
    boolean addChatMessage(Long appId, Long userId, String message, String messageType);

    /**
     * 根据应用Id删除对话历史
     *
     * @param appId 应用Id
     * @return 是否删除成功
     */
    boolean deleteByAppId(Long appId);

    /**
     * 分页查询某个应用的对话历史（游标查询）
     *
     * @param appId          应用Id
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param loginUser      登录用户
     * @return 对话历史分页
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);
    /**
     * 加载对话历史到内存中
     *
     * @param appId       应用Id
     * @param chatMemory  聊天内存
     * @param maxCount    最大数量
     * @return 加载数量
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);

    /**
     * 获取查询包装类
     *
     * @param chatHistoryQueryRequest 查询条件
     * @return 查询结果
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);
}
