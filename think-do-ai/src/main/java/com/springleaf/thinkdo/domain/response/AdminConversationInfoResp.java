package com.springleaf.thinkdo.domain.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员-会话列表项响应
 */
@Data
public class AdminConversationInfoResp {

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
     * 消息数量
     */
    private Long messageCount;

    /**
     * 最近消息时间
     */
    private LocalDateTime lastTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
