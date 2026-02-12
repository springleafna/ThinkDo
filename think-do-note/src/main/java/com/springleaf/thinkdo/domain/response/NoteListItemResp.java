package com.springleaf.thinkdo.domain.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记列表项Response
 */
@Data
public class NoteListItemResp {

    /**
     * 笔记ID
     */
    private Long id;

    /**
     * 笔记标题
     */
    private String title;

    /**
     * 笔记预览（纯文本前100字符）
     */
    private String preview;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 笔记标签（逗号分隔）
     */
    private String tags;

    /**
     * 是否收藏：0-否, 1-是
     */
    private Integer favorited;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
