package com.springleaf.thinkdo.domain.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 创建每日计划Request
 */
@Data
public class CreatePlanExecutionReq {

    /**
     * 计划标题
     */
    @NotBlank(message = "计划标题不能为空")
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

    /**
     * 执行日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate executeDate;
}
