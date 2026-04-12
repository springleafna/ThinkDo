package com.springleaf.thinkdo.domain.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员-用户信息响应
 */
@Data
public class AdminUserInfoResp {

    private Long id;
    private String username;
    private String role;
    private String roleDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
