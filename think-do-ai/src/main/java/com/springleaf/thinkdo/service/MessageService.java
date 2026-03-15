package com.springleaf.thinkdo.service;

import com.springleaf.thinkdo.chat.ChatMessage;
import com.springleaf.thinkdo.domain.response.MessageInfoResp;

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
}
