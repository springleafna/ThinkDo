package com.springleaf.thinkdo.domain.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员-计划详情响应
 */
@Data
public class AdminPlanDetailResp {

    private Long id;
    private Long userId;
    private String username;
    private Integer type;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String description;
    private Integer priority;
    private Integer quadrant;
    private String tags;
    private LocalDateTime startTime;
    private LocalDateTime dueTime;
    private Integer repeatType;
    private String repeatConf;
    private LocalDate repeatUntil;
    private Integer status;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 计划步骤列表
     */
    private List<PlanStepInfoResp> steps;

    @Data
    public static class PlanStepInfoResp {
        private Long id;
        private String title;
        private Integer status;
    }
}
