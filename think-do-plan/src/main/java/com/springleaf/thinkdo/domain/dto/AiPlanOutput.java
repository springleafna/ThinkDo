package com.springleaf.thinkdo.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * AI创建计划输出DTO
 */
@Data
public class AiPlanOutput {

    /**
     * 计划标题
     */
    private String title;

    /**
     * 计划详细描述
     */
    private String description;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 计划类型：0-普通计划，1-四象限计划，2-每日计划
     */
    private Integer type;

    /**
     * 优先级：1-低，2-中，3-高
     */
    private Integer priority;

    /**
     * 四象限：0-无，1-重要且紧急，2-重要不紧急，3-紧急不重要，4-不重要不紧急
     */
    private Integer quadrant;

    /**
     * 标签（逗号分隔）
     */
    private String tags;

    /**
     * 开始时间（ISO 8601格式或null）
     */
    private String startTime;

    /**
     * 截止时间（ISO 8601格式或null）
     */
    private String dueTime;

    /**
     * 子计划步骤列表
     */
    private List<String> steps;
}
