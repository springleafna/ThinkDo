package com.springleaf.thinkdo.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息通知实体
 */
@Data
@TableName("tb_notification")
public class NotificationEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 接收消息的用户ID
     */
    private Long userId;

    /**
     * 业务类型：1-计划 2-知识库
     */
    private Integer bizType;

    /**
     * 事件类型：1-处理完成，10-1天提醒，20-3天提醒，30-已逾期
     */
    private Integer eventType;

    /**
     * 关联的业务表ID（如 plan_id 或 file_id）
     */
    private Long bizId;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 扩展数据（存储JSON格式，如跳转路由、附加参数等）
     */
    private String extraData;

    /**
     * 是否已读：0-未读 1-已读
     */
    private Integer readStatus;

    /**
     * 读取时间
     */
    private LocalDateTime readAt;

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
     * 删除标志（0正常 1删除）
     */
    @TableLogic
    private Integer deleted;
}
