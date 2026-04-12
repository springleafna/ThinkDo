package com.springleaf.thinkdo.domain.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员-笔记详情响应
 */
@Data
public class AdminNoteDetailResp {

    private Long id;
    private Long userId;
    private String username;
    private String title;
    private String content;
    private String preview;
    private Long categoryId;
    private String categoryName;
    private String tags;
    private Integer favorited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
