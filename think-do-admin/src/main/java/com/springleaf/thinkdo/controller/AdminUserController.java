package com.springleaf.thinkdo.controller;

import com.springleaf.thinkdo.common.PageResp;
import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.context.UserContext;
import com.springleaf.thinkdo.domain.request.AdminChangeUserRoleReq;
import com.springleaf.thinkdo.domain.request.AdminResetPasswordReq;
import com.springleaf.thinkdo.domain.request.AdminUserQueryReq;
import com.springleaf.thinkdo.domain.response.AdminUserInfoResp;
import com.springleaf.thinkdo.enums.ResultCodeEnum;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员-用户管理接口
 */
@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    /**
     * 分页查询用户列表
     */
    @GetMapping("/list")
    public Result<PageResp<AdminUserInfoResp>> listUsers(AdminUserQueryReq queryReq) {
        checkAdmin();
        return Result.success(userService.adminListUsers(queryReq));
    }

    /**
     * 修改用户角色
     */
    @PutMapping("/changeRole")
    public Result<Void> changeUserRole(@RequestBody @Valid AdminChangeUserRoleReq req) {
        checkAdmin();
        userService.adminChangeUserRole(req);
        return Result.success();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        checkAdmin();
        userService.adminDeleteUser(id);
        return Result.success();
    }

    /**
     * 重置用户密码
     */
    @PutMapping("/resetPassword")
    public Result<Void> resetPassword(@RequestBody @Valid AdminResetPasswordReq req) {
        checkAdmin();
        userService.adminResetPassword(req.getUserId(), req.getNewPassword());
        return Result.success();
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "无管理员权限");
        }
    }
}
