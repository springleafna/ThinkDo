package com.springleaf.thinkdo.domain.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息信息响应
 */
@Data
public class MessageInfoResp {

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 角色：user-用户, assistant-助手, system-系统
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
