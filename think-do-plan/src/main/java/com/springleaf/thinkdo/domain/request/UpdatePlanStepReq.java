package com.springleaf.thinkdo.domain.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新计划步骤Request
 */
@Data
public class UpdatePlanStepReq {

    /**
     * 步骤ID
     */
    @NotNull(message = "步骤ID不能为空")
    private Long id;

    /**
     * 步骤标题
     */
    @Size(max = 255, message = "步骤标题长度不能超过255个字符")
    private String title;

    /**
     * 状态：0-未完成 1-已完成
     */
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status;
}
