package com.springleaf.thinkdo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.springleaf.thinkdo.common.PageResp;
import com.springleaf.thinkdo.domain.entity.UserEntity;
import com.springleaf.thinkdo.domain.request.AdminChangeUserRoleReq;
import com.springleaf.thinkdo.domain.request.AdminUserQueryReq;
import com.springleaf.thinkdo.domain.request.UserLoginReq;
import com.springleaf.thinkdo.domain.request.UserRegisterReq;
import com.springleaf.thinkdo.domain.request.UserUpdatePasswordReq;
import com.springleaf.thinkdo.domain.request.UserUpdateUsernameReq;
import com.springleaf.thinkdo.domain.response.AdminUserInfoResp;
import com.springleaf.thinkdo.domain.response.UserInfoResp;
import com.springleaf.thinkdo.enums.UserRoleEnum;

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
     * 用户登录（带角色校验）
     * @param loginReq 用户名+密码
     * @param requiredRole 要求的角色（USER / ADMIN）
     * @return 登录成功的token
     */
    String login(UserLoginReq loginReq, UserRoleEnum requiredRole);

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
     * 用户修改用户名
     * @param updateUsernameReq 新用户名+当前密码
     */
    void updateUsername(UserUpdateUsernameReq updateUsernameReq);

    /**
     * 获取用户个人信息
     * @return 用户个人信息
     */
    UserInfoResp getUserInfo();

    /**
     * 管理员-分页查询用户列表
     * @param queryReq 查询条件
     * @return 分页用户列表
     */
    PageResp<AdminUserInfoResp> adminListUsers(AdminUserQueryReq queryReq);

    /**
     * 管理员-修改用户角色
     * @param req 修改角色请求
     */
    void adminChangeUserRole(AdminChangeUserRoleReq req);

    /**
     * 管理员-删除用户
     * @param userId 用户ID
     */
    void adminDeleteUser(Long userId);

    /**
     * 统计用户总数
     * @return 用户总数
     */
    Long countTotal();

    /**
     * 统计指定日期注册的用户数
     * @param date 日期
     * @return 用户数
     */
    Long countByDate(java.time.LocalDate date);
}
