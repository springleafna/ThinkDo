package com.springleaf.thinkdo.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建笔记Request
 */
@Data
public class CreateNoteReq {

    /**
     * 笔记标题
     */
    @NotBlank(message = "笔记标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    /**
     * 笔记内容（Markdown格式）
     */
    private String content;

    /**
     * 分类ID（NULL表示未分类）
     */
    private Long categoryId;

    /**
     * 笔记标签（逗号分隔）：important,idea,todo
     */
    @Size(max = 255, message = "标签长度不能超过255个字符")
    private String tags;
}
