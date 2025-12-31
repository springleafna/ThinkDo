package com.springleaf.thinkdo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.springleaf.thinkdo.domain.entity.UserEntity;
import com.springleaf.thinkdo.domain.request.UserLoginReq;
import com.springleaf.thinkdo.domain.request.UserRegisterReq;
import com.springleaf.thinkdo.domain.request.UserUpdatePasswordReq;
import com.springleaf.thinkdo.domain.response.UserInfoResp;
import jakarta.validation.Valid;

/**
 * 用户Service
 */
public interface UserService extends IService<UserEntity> {

    /**
     * 用户注册
     * @param registerReq 用户名+密码
     */
    void register(UserRegisterReq registerReq);

    /**
     * 用户登录
     * @param loginReq 用户名+密码
     * @return 登录成功的token
     */
    String login(UserLoginReq loginReq);

    /**
     * 用户退出登录
     */
    void logout();

    /**
     * 用户修改密码
     * @param updatePasswordReq 旧密码+新密码+确认密码
     */
    void updatePassword(UserUpdatePasswordReq updatePasswordReq);

    /**
     * 获取用户个人信息
     * @return 用户个人信息
     */
    UserInfoResp getUserInfo();
}
