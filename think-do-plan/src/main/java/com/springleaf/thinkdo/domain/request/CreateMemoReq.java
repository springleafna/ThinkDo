package com.springleaf.thinkdo.domain.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建便签Request
 */
@Data
public class CreateMemoReq {

    /**
     * 便签标题
     */
    @NotBlank(message = "便签标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100个字符")
    private String title;

    /**
     * 便签内容
     */
    @NotBlank(message = "便签内容不能为空")
    @Size(max = 5000, message = "内容长度不能超过5000个字符")
    private String content;

    /**
     * 便签标签
     */
    @Size(max = 20, message = "标签长度不能超过20个字符")
    private String tag;

    /**
     * 背景颜色代码
     */
    private String backgroundColor;

    /**
     * 是否置顶(0:否 1:是)
     */
    @Min(value = 0, message = "置顶状态只能为0或1")
    @Max(value = 1, message = "置顶状态只能为0或1")
    private Integer pinned;
}
