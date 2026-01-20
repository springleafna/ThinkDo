package com.springleaf.thinkdo.domain.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日清单信息Response
 */
@Data
public class PlanExecutionInfoResp {

    /**
     * 每日清单ID
     */
    private Long id;

    /**
     * 计划ID
     */
    private Long planId;

    /**
     * 计划标题
     */
    private String planTitle;

    /**
     * 计划类型：0-普通计划，1-四象限计划，2-每日计划
     */
    private Integer planType;

    /**
     * 计划优先级：1-低 2-中 3-高
     */
    private Integer priority;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 截止时间
     */
    private LocalDateTime dueTime;

    /**
     * 计划标签（逗号分隔）
     */
    private String tags;

    /**
     * 执行日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate executeDate;

    /**
     * 执行状态：0-未完成 1-已完成
     */
    private Integer status;

    /**
     * 当天完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
