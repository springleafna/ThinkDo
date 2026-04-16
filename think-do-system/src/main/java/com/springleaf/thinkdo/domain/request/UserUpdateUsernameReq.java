package com.springleaf.thinkdo.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户修改用户名Request
 */
@Data
public class UserUpdateUsernameReq {

    /**
     * 新用户名
     */
    @NotBlank(message = "新用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20位之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String newUsername;

    /**
     * 当前密码（用于验证身份）
     */
    @NotBlank(message = "当前密码不能为空")
    private String password;
}
