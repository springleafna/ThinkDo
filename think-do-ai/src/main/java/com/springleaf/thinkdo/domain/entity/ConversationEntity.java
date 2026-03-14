package com.springleaf.thinkdo.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表实体类
 */
@Data
@TableName("tb_conversation")
public class ConversationEntity {

    /**
     * 会话ID（主键）
     */
    @TableId(value = "conversation_id", type = IdType.ASSIGN_ID)
    private String conversationId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话名称
     */
    private String title;

    /**
     * 最近消息时间
     */
    private LocalDateTime lastTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 删除标记(0:正常 1:删除)
     */
    @TableLogic
    private Integer deleted;
}
