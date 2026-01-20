package com.springleaf.thinkdo.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建四象限计划Request
 */
@Data
public class CreateQuadrantPlanReq {

    /**
     * 计划标题
     */
    @NotBlank(message = "计划标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;

    /**
     * 四象限状态：1-重要且紧急, 2-重要不紧急, 3-紧急不重要, 4-不重要不紧急
     */
    @NotNull(message = "四象限状态不能为空")
    private Integer quadrant;
}
