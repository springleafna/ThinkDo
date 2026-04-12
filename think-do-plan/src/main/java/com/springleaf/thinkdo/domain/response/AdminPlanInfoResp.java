package com.springleaf.thinkdo.domain.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员-计划列表项响应
 */
@Data
public class AdminPlanInfoResp {

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
    private Integer status;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
