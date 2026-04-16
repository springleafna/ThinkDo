package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.thinkdo.common.PageResp;
import com.springleaf.thinkdo.domain.entity.RoleEntity;
import com.springleaf.thinkdo.domain.entity.UserEntity;
import com.springleaf.thinkdo.domain.entity.UserRoleEntity;
import com.springleaf.thinkdo.domain.request.AdminChangeUserRoleReq;
import com.springleaf.thinkdo.domain.request.AdminUserQueryReq;
import com.springleaf.thinkdo.domain.request.UserLoginReq;
import com.springleaf.thinkdo.domain.request.UserRegisterReq;
import com.springleaf.thinkdo.domain.request.UserUpdatePasswordReq;
import com.springleaf.thinkdo.domain.request.UserUpdateUsernameReq;
import com.springleaf.thinkdo.domain.response.AdminUserInfoResp;
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
import org.springframework.util.StringUtils;

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
    public String login(UserLoginReq loginReq, UserRoleEnum requiredRole) {
        String username = loginReq.getUsername();
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username)
        );

        if (user == null || !PasswordUtil.verifyPassword(loginReq.getPassword(), user.getPassword())) {
            log.warn("登录失败, username={}", username);
            throw new BusinessException("用户名或密码错误");
        }

        // 校验角色
        RoleEntity role = roleMapper.selectOne(
                new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getName, requiredRole.getValue())
        );
        if (role == null) {
            throw new BusinessException("系统角色不存在，请联系管理员");
        }

        UserRoleEntity userRole = userRoleMapper.selectOne(
                new LambdaQueryWrapper<UserRoleEntity>()
                        .eq(UserRoleEntity::getUserId, user.getId())
                        .eq(UserRoleEntity::getRoleId, role.getId())
        );
        if (userRole == null) {
            throw new BusinessException("该账号没有" + requiredRole.getDescription() + "权限");
        }

        // 执行登录
        StpUtil.login(user.getId());

        // 将用户信息存储到 Session 中，方便后续使用
        StpUtil.getSession().set("username", user.getUsername());
        StpUtil.getSession().set("userId", user.getId());

        log.info("用户登录成功, username={}, role={}", username, requiredRole.getValue());

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
    @Transactional(rollbackFor = Exception.class)
    public void updateUsername(UserUpdateUsernameReq updateUsernameReq) {
        long userId = StpUtil.getLoginIdAsLong();
        String newUsername = updateUsernameReq.getNewUsername();
        String password = updateUsernameReq.getPassword();

        // 获取当前用户
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证当前密码
        if (!PasswordUtil.verifyPassword(password, user.getPassword())) {
            throw new BusinessException("当前密码输入错误");
        }

        // 新旧用户名不能相同
        if (user.getUsername().equals(newUsername)) {
            throw new BusinessException("新用户名与当前用户名相同");
        }

        // 检查新用户名是否已被占用
        UserEntity existUser = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, newUsername)
        );
        if (existUser != null) {
            throw new BusinessException("该用户名已被占用");
        }

        // 更新用户名
        user.setUsername(newUsername);
        userMapper.updateById(user);

        // 更新 Session 中的用户名
        StpUtil.getSession().set("username", newUsername);

        log.info("用户 {} 修改用户名成功，新用户名为 {}", user.getUsername(), newUsername);
    }

    @Override
    public UserInfoResp getUserInfo() {
        return null;
    }

    @Override
    public PageResp<AdminUserInfoResp> adminListUsers(AdminUserQueryReq queryReq) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();

        // 用户名模糊搜索
        if (StringUtils.hasText(queryReq.getUsername())) {
            wrapper.like(UserEntity::getUsername, queryReq.getUsername());
        }

        // 角色筛选：先查出符合角色的用户ID集合，再作为IN条件
        if (StringUtils.hasText(queryReq.getRole())) {
            RoleEntity role = roleMapper.selectOne(
                    new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getName, queryReq.getRole())
            );
            if (role == null) {
                return PageResp.of(java.util.List.of(), 0L, queryReq.getPageNum(), queryReq.getPageSize());
            }
            java.util.List<UserRoleEntity> userRoles = userRoleMapper.selectList(
                    new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getRoleId, role.getId())
            );
            java.util.List<Long> userIds = userRoles.stream()
                    .map(UserRoleEntity::getUserId)
                    .collect(java.util.stream.Collectors.toList());
            if (userIds.isEmpty()) {
                return PageResp.of(java.util.List.of(), 0L, queryReq.getPageNum(), queryReq.getPageSize());
            }
            wrapper.in(UserEntity::getId, userIds);
        }

        wrapper.orderByDesc(UserEntity::getCreatedAt);

        IPage<UserEntity> page = new Page<>(queryReq.getPageNum(), queryReq.getPageSize());
        IPage<UserEntity> result = userMapper.selectPage(page, wrapper);

        return PageResp.of(result, user -> {
            AdminUserInfoResp resp = new AdminUserInfoResp();
            resp.setId(user.getId());
            resp.setUsername(user.getUsername());
            resp.setCreatedAt(user.getCreatedAt());
            resp.setUpdatedAt(user.getUpdatedAt());

            String roleName = userRoleMapper.getUserRoleName(user.getId());
            if (roleName != null) {
                resp.setRole(roleName);
                UserRoleEnum roleEnum = UserRoleEnum.fromValue(roleName);
                resp.setRoleDescription(roleEnum != null ? roleEnum.getDescription() : roleName);
            }
            return resp;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminChangeUserRole(AdminChangeUserRoleReq req) {
        Long userId = req.getUserId();
        String roleName = req.getRoleName();

        // 校验角色名称
        UserRoleEnum targetRole = UserRoleEnum.fromValue(roleName);
        if (targetRole == null) {
            throw new BusinessException("无效的角色名称: " + roleName);
        }

        // 校验用户存在
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 不能修改自己的角色
        if (userId.equals(StpUtil.getLoginIdAsLong())) {
            throw new BusinessException("不能修改自己的角色");
        }

        // 查找目标角色
        RoleEntity role = roleMapper.selectOne(
                new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getName, roleName)
        );
        if (role == null) {
            throw new BusinessException("系统角色不存在");
        }

        // 更新用户角色
        UserRoleEntity userRole = userRoleMapper.selectOne(
                new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, userId)
        );
        if (userRole == null) {
            throw new BusinessException("用户角色记录不存在");
        }
        userRole.setRoleId(role.getId());
        userRoleMapper.updateById(userRole);

        // 踢出该用户会话，使其重新登录生效
        StpUtil.kickout(userId);

        log.info("管理员修改用户角色成功, userId={}, newRole={}", userId, roleName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteUser(Long userId) {
        // 不能删除自己
        if (userId.equals(StpUtil.getLoginIdAsLong())) {
            throw new BusinessException("不能删除自己的账号");
        }

        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 软删除用户
        userMapper.deleteById(userId);

        // 踢出该用户所有会话
        StpUtil.kickout(userId);

        log.info("管理员删除用户成功, userId={}, username={}", userId, user.getUsername());
    }

    @Override
    public Long countTotal() {
        return userMapper.selectCount(null);
    }

    @Override
    public Long countByDate(java.time.LocalDate date) {
        return userMapper.selectCount(
                new LambdaQueryWrapper<UserEntity>()
                        .ge(UserEntity::getCreatedAt, date.atStartOfDay())
                        .lt(UserEntity::getCreatedAt, date.plusDays(1).atStartOfDay())
        );
    }
}
