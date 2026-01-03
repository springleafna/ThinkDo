package com.springleaf.thinkdo.domain.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 计划步骤信息Response
 */
@Data
public class PlanStepInfoResp {

    /**
     * 步骤ID
     */
    private Long id;

    /**
     * 关联的父计划ID
     */
    private Long planId;

    /**
     * 步骤标题
     */
    private String title;

    /**
     * 状态：0-未完成 1-已完成
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
