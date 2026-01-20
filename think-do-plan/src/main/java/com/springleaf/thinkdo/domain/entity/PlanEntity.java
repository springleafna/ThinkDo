package com.springleaf.thinkdo.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 计划实体类
 */
@Data
@TableName("tb_plan")
public class PlanEntity {

    /**
     * 计划ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户
     */
    private Long userId;

    /**
     * 计划类型：0-普通计划，1-四象限计划，2-每日计划
     */
    private Integer type;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 计划标题
     */
    private String title;

    /**
     * 计划描述
     */
    private String description;

    /**
     * 计划优先级：1-低 2-中 3-高
     */
    private Integer priority;

    /**
     * 四象限状态：0-无, 1-重要且紧急, 2-重要不紧急, 3-紧急不重要, 4-不重要不紧急
     */
    private Integer quadrant;

    /**
     * 计划标签（逗号分隔）
     */
    private String tags;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 截止时间
     */
    @TableField("due_time")
    private LocalDateTime dueTime;

    /**
     * 重复类型：0-不重复, 1-每天, 2-每周, 3-每月, 4-每年, 5-工作日
     */
    private Integer repeatType;

    /**
     * 重复配置细节(JSON格式)
     */
    private String repeatConf;

    /**
     * 重复截止日期(空代表无限重复)
     */
    private LocalDate repeatUntil;

    /**
     * 计划状态：0-未完成 1-已完成
     */
    private Integer status;

    /**
     * 计划完成时间
     */
    private LocalDateTime completedAt;

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
