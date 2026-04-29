package com.springleaf.thinkdo.service;

import com.springleaf.thinkdo.common.PageResp;
import com.springleaf.thinkdo.domain.entity.ConversationEntity;
import com.springleaf.thinkdo.domain.request.AdminConversationQueryReq;
import com.springleaf.thinkdo.domain.request.CreateConversationReq;
import com.springleaf.thinkdo.domain.request.UpdateConversationReq;
import com.springleaf.thinkdo.domain.response.AdminConversationDetailResp;
import com.springleaf.thinkdo.domain.response.AdminConversationInfoResp;
import com.springleaf.thinkdo.domain.response.ConversationInfoResp;

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

    /**
     * 统计会话总数
     * @return 会话总数
     */
    Long countTotal();

    /**
     * 统计指定日期创建的会话数
     * @param date 日期
     * @return 会话数
     */
    Long countByDate(java.time.LocalDate date);

    // ==================== 管理员接口 ====================

    /**
     * 管理员-分页查询会话列表
     */
    PageResp<AdminConversationInfoResp> adminListConversations(AdminConversationQueryReq queryReq);

    /**
     * 管理员-获取会话详情（含消息记录）
     */
    AdminConversationDetailResp adminGetConversationDetail(String conversationId);

    /**
     * 管理员-删除会话
     */
    void adminDeleteConversation(String conversationId);

    /**
     * 管理员-批量删除会话
     */
    void adminBatchDeleteConversations(List<String> conversationIds);
}
