package com.springleaf.thinkdo.domain.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 更新每日计划Request
 */
@Data
public class UpdatePlanExecutionReq {

    /**
     * 计划ID
     */
    @NotNull(message = "计划ID不能为空")
    private Long id;

    /**
     * 计划标题
     */
    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;

    /**
     * 计划优先级：1-低 2-中 3-高
     */
    @Min(value = 1, message = "优先级只能为1、2或3")
    @Max(value = 3, message = "优先级只能为1、2或3")
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
    @Size(max = 255, message = "标签长度不能超过255个字符")
    private String tags;
}
