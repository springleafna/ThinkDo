package com.springleaf.thinkdo.service;

import com.springleaf.thinkdo.chat.ChatMessage;
import com.springleaf.thinkdo.domain.entity.MessageEntity;
import com.springleaf.thinkdo.domain.response.MessageInfoResp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息Service接口
 */
public interface MessageService {

    /**
     * 根据会话ID获取历史消息
     *
     * @param conversationId 会话ID
     * @return 消息列表
     */
    List<MessageInfoResp> getMessagesByConversationId(String conversationId);

    /**
     * 追加消息到对话历史
     *
     * @param conversationId 对话ID
     * @param userId         用户ID
     * @param message        要追加的消息
     * @return 消息ID
     */
    Long append(String conversationId, Long userId, ChatMessage message);

    /**
     * 加载对话历史记录
     *
     * @param conversationId 对话ID
     * @param userId         用户ID
     * @return 对话历史消息列表（包含摘要和历史记录）
     */
    List<ChatMessage> load(String conversationId, Long userId);


    /**
     * 统计用户在指定对话中的消息数量
     *
     * @param conversationId 对话ID
     * @param userId         用户ID
     * @return 用户消息总数
     */
    long countUserMessages(String conversationId, Long userId);

    /**
     * 获取指定对话中最新的用户消息列表
     *
     * @param conversationId 对话ID
     * @param userId         用户ID
     * @param limit          返回的消息数量限制
     * @return 用户消息列表，按时间倒序排列
     */
    List<MessageEntity> listLatestUserOnlyMessages(String conversationId, Long userId, int limit);

    /**
     * 查找指定时间点之前或当时的最大消息ID
     *
     * @param conversationId 对话ID
     * @param userId         用户ID
     * @param at             指定的时间点
     * @return 最大消息ID，如果不存在则返回null
     */
    Long findMaxMessageIdAtOrBefore(String conversationId, Long userId, LocalDateTime at);

    /**
     * 获取指定ID范围内的消息列表
     *
     * @param conversationId 对话ID
     * @param userId         用户ID
     * @param afterId        起始消息ID（不包含）
     * @param beforeId       结束消息ID（不包含）
     * @return 指定范围内的消息列表
     */
    List<MessageEntity> listMessagesBetweenIds(String conversationId, Long userId, Long afterId, Long beforeId);


}
