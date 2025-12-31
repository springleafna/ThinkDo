package com.springleaf.thinkdo.controller;

import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.domain.request.UserLoginReq;
import com.springleaf.thinkdo.domain.request.UserRegisterReq;
import com.springleaf.thinkdo.domain.request.UserUpdatePasswordReq;
import com.springleaf.thinkdo.domain.response.UserInfoResp;
import com.springleaf.thinkdo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户名+密码 注册
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid UserRegisterReq registerReq) {
        userService.register(registerReq);
        return Result.success();
    }

    /**
     * 用户名+密码 登录
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody @Valid UserLoginReq loginReq) {
        String token = userService.login(loginReq);
        return Result.success(token);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.success();
    }

    /**
     * 用户修改密码
     */
    @PutMapping("/updatePassword")
    public Result<Void> updatePassword(@RequestBody @Valid UserUpdatePasswordReq updatePasswordReq) {
        userService.updatePassword(updatePasswordReq);
        return Result.success();
    }

    /**
     * 获取用户个人信息
     */
    @GetMapping("/info")
    public Result<UserInfoResp> getUserInfo() {
        return Result.success(userService.getUserInfo());
    }
}
