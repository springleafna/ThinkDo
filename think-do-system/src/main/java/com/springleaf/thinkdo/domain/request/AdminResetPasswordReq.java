package com.springleaf.thinkdo.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员-重置用户密码请求
 */
@Data
public class AdminResetPasswordReq {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
