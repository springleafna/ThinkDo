package com.springleaf.thinkdo.domain.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 便签信息Response
 */
@Data
public class MemoInfoResp {

    /**
     * 便签ID
     */
    private Long id;

    /**
     * 便签标题
     */
    private String title;

    /**
     * 便签内容
     */
    private String content;

    /**
     * 便签标签
     */
    private String tag;

    /**
     * 背景颜色
     */
    private String backgroundColor;

    /**
     * 是否置顶(0:否 1:是)
     */
    private Integer pinned;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
