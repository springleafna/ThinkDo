package com.springleaf.thinkdo.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员-修改用户角色请求
 */
@Data
public class AdminChangeUserRoleReq {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;
}
