package com.springleaf.thinkdo.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建计划步骤Request
 */
@Data
public class CreatePlanStepReq {

    /**
     * 关联的父计划ID
     */
    @NotNull(message = "计划ID不能为空")
    private Long planId;

    /**
     * 步骤标题
     */
    @NotBlank(message = "步骤标题不能为空")
    @Size(max = 255, message = "步骤标题长度不能超过255个字符")
    private String title;
}
