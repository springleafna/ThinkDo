package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.thinkdo.domain.entity.RoleEntity;
import com.springleaf.thinkdo.domain.entity.UserEntity;
import com.springleaf.thinkdo.domain.entity.UserRoleEntity;
import com.springleaf.thinkdo.domain.request.UserLoginReq;
import com.springleaf.thinkdo.domain.request.UserRegisterReq;
import com.springleaf.thinkdo.domain.request.UserUpdatePasswordReq;
import com.springleaf.thinkdo.domain.response.UserInfoResp;
import com.springleaf.thinkdo.enums.UserRoleEnum;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.RoleMapper;
import com.springleaf.thinkdo.mapper.UserMapper;
import com.springleaf.thinkdo.mapper.UserRoleMapper;
import com.springleaf.thinkdo.service.UserService;
import com.springleaf.thinkdo.utils.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户Service实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterReq registerReq) {
        String username = registerReq.getUsername();

        // 查询用户名是否重复
        UserEntity existUser = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username)
        );
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        // 创建用户
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encryptPassword(registerReq.getPassword()));
        userMapper.insert(user);

        // 分配用户角色
        RoleEntity role = roleMapper.selectOne(
                new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getName, UserRoleEnum.USER.getValue())
        );
        if (role == null) {
            throw new BusinessException("系统角色不存在，请联系管理员");
        }

        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);

        log.info("用户注册成功, username={}", username);
    }

    @Override
    public String login(UserLoginReq loginReq) {
        String username = loginReq.getUsername();
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username)
        );

        if (user == null || !PasswordUtil.verifyPassword(loginReq.getPassword(), user.getPassword())) {
            log.warn("登录失败, username={}", username);
            throw new BusinessException("用户名或密码错误");
        }

        StpUtil.login(user.getId());
        log.info("用户登录成功, username={}", username);

        return StpUtil.getTokenValue();
    }

    @Override
    public void logout() {
        long userId = StpUtil.getLoginIdAsLong();
        StpUtil.logout();
        log.info("用户 {} 退出登录", userId);
    }

    @Override
    public void updatePassword(UserUpdatePasswordReq updatePasswordReq) {
        long userId = StpUtil.getLoginIdAsLong();
        String oldPassword = updatePasswordReq.getOldPassword();
        String newPassword = updatePasswordReq.getNewPassword();
        String confirmPassword = updatePasswordReq.getConfirmPassword();

        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException("两次输入的密码不一致");
        }

        // 新旧密码不能相同
        if (oldPassword.equals(newPassword)) {
            throw new BusinessException("新旧密码不能相同");
        }

        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证旧密码
        if (!PasswordUtil.verifyPassword(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码输入错误");
        }

        // 更新密码
        user.setPassword(PasswordUtil.encryptPassword(newPassword));
        userMapper.updateById(user);

        // 踢出该用户所有会话
        StpUtil.kickout(userId);

        log.info("用户 {} 修改密码成功", user.getUsername());
    }

    @Override
    public UserInfoResp getUserInfo() {
        return null;
    }
}
