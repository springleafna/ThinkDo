package com.springleaf.thinkdo.domain.response;

import lombok.Data;

import java.util.List;

/**
 * 计划四象限Response
 */
@Data
public class PlanQuadrantResp {

    /**
     * 重要且紧急
     */
    private List<PlanInfoResp> importantUrgent;

    /**
     * 重要不紧急
     */
    private List<PlanInfoResp> importantNotUrgent;

    /**
     * 紧急不重要
     */
    private List<PlanInfoResp> urgentNotImportant;

    /**
     * 不重要不紧急
     */
    private List<PlanInfoResp> notImportantNotUrgent;

    /**
     * 未分类
     */
    private List<PlanInfoResp> unclassified;
}
