package com.springleaf.thinkdo.service;

import com.springleaf.thinkdo.chat.ChatMessage;

/**
 * 会话摘要服务接口
 */
public interface ConversationSummaryService {

    /**
     * 判断是否需要压缩对话并执行异步摘要任务
     *
     * @param conversationId 会话 ID
     * @param userId         用户 ID
     * @param message        聊天消息对象
     */
    void compressIfNeeded(String conversationId, Long userId, ChatMessage message);

    /**
     * 加载最新的对话摘要
     *
     * @param conversationId 会话 ID
     * @param userId         用户 ID
     * @return 最新的摘要消息，如果不存在则返回 null
     */
    ChatMessage loadLatestSummary(String conversationId, Long userId);

    /**
     * 对摘要消息进行装饰处理（如添加前缀等）
     *
     * @param summary 待装饰的摘要消息
     * @return 装饰后的摘要消息
     */
    ChatMessage decorateIfNeeded(ChatMessage summary);
}
