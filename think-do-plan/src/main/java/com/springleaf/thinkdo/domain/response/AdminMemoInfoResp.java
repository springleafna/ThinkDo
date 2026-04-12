package com.springleaf.thinkdo.domain.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员-便签信息响应
 */
@Data
public class AdminMemoInfoResp {

    private Long id;
    private Long userId;
    private String username;
    private String title;
    private String content;
    private String tag;
    private String backgroundColor;
    private Integer pinned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
