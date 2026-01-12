package com.springleaf.thinkdo.domain.request;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 计划查询Request
 */
@Data
public class PlanQueryReq {

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 计划标题关键词
     */
    private String keyword;

    /**
     * 计划优先级：1-低 2-中 3-高
     */
    private Integer priority;

    /**
     * 四象限状态：0-无, 1-重要且紧急, 2-重要不紧急, 3-紧急不重要, 4-不重要不紧急
     */
    private Integer quadrant;

    /**
     * 计划标签
     */
    private String tags;

    /**
     * 计划状态：0-未完成 1-已完成
     */
    private Integer status;

    /**
     * 重复类型：0-不重复, 1-每天, 2-每周, 3-每月, 4-每年, 5-工作日
     */
    private Integer repeatType;

    /**
     * 开始时间-查询开始
     */
    private LocalDateTime startTimeFrom;

    /**
     * 开始时间-查询结束
     */
    private LocalDateTime startTimeTo;

    /**
     * 截止时间-查询开始
     */
    private LocalDateTime dueTimeFrom;

    /**
     * 截止时间-查询结束
     */
    private LocalDateTime dueTimeTo;
}
