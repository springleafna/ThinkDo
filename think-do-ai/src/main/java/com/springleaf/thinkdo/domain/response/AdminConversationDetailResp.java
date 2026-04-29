package com.springleaf.thinkdo.domain.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员-会话详情响应
 */
@Data
public class AdminConversationDetailResp {

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 会话摘要
     */
    private String summary;

    /**
     * 最近消息时间
     */
    private LocalDateTime lastTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 消息列表
     */
    private List<MessageItem> messages;

    @Data
    public static class MessageItem {

        /**
         * 消息ID
         */
        private Long id;

        /**
         * 角色：user/assistant/system
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;

        /**
         * 创建时间
         */
        private LocalDateTime createdAt;
    }
}
