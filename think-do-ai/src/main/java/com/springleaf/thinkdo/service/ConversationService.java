package com.springleaf.thinkdo.service;

import com.springleaf.thinkdo.domain.entity.ConversationEntity;
import com.springleaf.thinkdo.domain.response.ConversationInfoResp;
import com.springleaf.thinkdo.domain.request.CreateConversationReq;
import com.springleaf.thinkdo.domain.request.UpdateConversationReq;

import java.util.List;

/**
 * 会话Service接口
 */
public interface ConversationService {

    /**
     * 创建会话
     *
     * @param createConversationReq 创建请求
     * @return 会话ID
     */
    String createConversation(CreateConversationReq createConversationReq);

    /**
     * 更新会话标题
     *
     * @param updateConversationReq 更新请求
     */
    void updateConversation(UpdateConversationReq updateConversationReq);

    /**
     * 删除会话
     *
     * @param conversationId 会话ID
     */
    void deleteConversation(String conversationId);

    /**
     * 获取会话详情
     *
     * @param conversationId 会话ID
     * @return 会话信息
     */
    ConversationInfoResp getConversationById(String conversationId);

    /**
     * 获取会话列表
     *
     * @return 会话列表
     */
    List<ConversationInfoResp> getConversationList();

    /**
     * 查找指定的对话信息
     *
     * @param conversationId 对话ID
     * @param userId         用户ID
     * @return 对话信息，如果不存在则返回null
     */
    ConversationEntity findConversation(String conversationId, Long userId);
}
