package com.springleaf.thinkdo.domain.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新计划分类Request
 */
@Data
public class UpdatePlanCategoryReq {

    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long id;

    /**
     * 分类名称
     */
    @Size(max = 50, message = "分类名称长度不能超过50个字符")
    private String name;
}
