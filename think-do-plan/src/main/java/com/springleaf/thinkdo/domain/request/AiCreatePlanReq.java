package com.springleaf.thinkdo.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI创建计划Request
 */
@Data
public class AiCreatePlanReq {

    /**
     * 用户输入的计划描述/目标
     */
    @NotBlank(message = "计划描述不能为空")
    @Size(max = 2000, message = "描述长度不能超过2000个字符")
    private String description;

    /**
     * 计划类型：0-普通计划，1-四象限计划，2-每日计划（可选，不传则由AI推断）
     */
    private Integer type;
}
