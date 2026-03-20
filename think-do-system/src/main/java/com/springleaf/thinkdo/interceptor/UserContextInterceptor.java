package com.springleaf.thinkdo.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.springleaf.thinkdo.context.UserContext;
import com.springleaf.thinkdo.enums.UserRoleEnum;
import com.springleaf.thinkdo.mapper.UserMapper;
import com.springleaf.thinkdo.mapper.UserRoleMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 用户上下文拦截器
 * 在请求处理前设置当前用户上下文，在请求处理后清除用户上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserContextInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String USER_ROLE_CACHE_KEY_PREFIX = "user:role:";
    private static final long CACHE_EXPIRE_HOURS = 1;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // 检查用户是否登录
            if (StpUtil.isLogin()) {
                Long userId = StpUtil.getLoginIdAsLong();

                // 获取用户名：优先从Session获取，如果不存在则从数据库查询
                String username = getUsername(userId);

                // 获取用户角色（从Redis缓存或数据库查询）
                UserRoleEnum role = getUserRole(userId);

                // 设置用户上下文
                UserContext currentUser = UserContext.builder()
                        .userId(userId)
                        .username(username)
                        .role(role)
                        .build();

                UserContext.setCurrentUser(currentUser);
                log.debug("设置用户上下文: userId={}, username={}, role={}", userId, username, role);
            }
        } catch (Exception e) {
            log.error("设置用户上下文失败", e);
            // 不拦截请求，继续执行
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清除用户上下文，避免内存泄漏
        UserContext.clear();
        log.debug("清除用户上下文");
    }

    /**
     * 获取用户名
     * 优先从Session获取，如果不存在则从数据库查询并写入Session
     *
     * @param userId 用户ID
     * @return 用户名
     */
    private String getUsername(Long userId) {
        // 1. 先从Session获取
        String username = StpUtil.getSession().getString("username");
        if (username != null) {
            return username;
        }

        // 2. Session中没有，从数据库查询
        try {
            com.springleaf.thinkdo.domain.entity.UserEntity user = userMapper.selectById(userId);
            if (user != null) {
                username = user.getUsername();
                // 写入Session，下次就不用查数据库了
                StpUtil.getSession().set("username", username);
                StpUtil.getSession().set("userId", userId);
                log.debug("从数据库查询用户名并写入Session: userId={}, username={}", userId, username);
                return username;
            }
        } catch (Exception e) {
            log.error("查询用户名失败: userId={}", userId, e);
        }

        // 3. 查询失败，返回null
        log.warn("用户名查询失败: userId={}", userId);
        return null;
    }

    /**
     * 获取用户角色
     * 优先从Redis缓存获取，如果缓存不存在则从数据库查询并写入缓存
     *
     * @param userId 用户ID
     * @return 用户角色
     */
    private UserRoleEnum getUserRole(Long userId) {
        if (userId == null) {
            return UserRoleEnum.USER;
        }

        // 1. 先从Redis缓存获取用户角色
        String cacheKey = USER_ROLE_CACHE_KEY_PREFIX + userId;
        String cachedRole = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedRole != null) {
            UserRoleEnum role = UserRoleEnum.fromValue(cachedRole);
            if (role != null) {
                log.debug("从缓存获取用户角色: userId={}, role={}", userId, role);
                return role;
            }
        }

        // 2. 缓存不存在，从数据库查询
        try {
            String roleName = userRoleMapper.getUserRoleName(userId);
            if (roleName != null) {
                UserRoleEnum role = UserRoleEnum.fromValue(roleName);
                if (role != null) {
                    // 3. 将查询结果写入Redis缓存，设置1小时过期
                    stringRedisTemplate.opsForValue().set(
                            cacheKey,
                            role.getValue(),
                            CACHE_EXPIRE_HOURS,
                            TimeUnit.HOURS
                    );
                    log.debug("从数据库查询用户角色并写入缓存: userId={}, role={}", userId, role);
                    return role;
                }
            }
        } catch (Exception e) {
            log.error("查询用户角色失败: userId={}", userId, e);
        }

        // 3. 查询失败或用户没有角色，返回默认角色
        log.warn("用户角色查询失败或用户无角色，返回默认角色USER: userId={}", userId);
        return UserRoleEnum.USER;
    }
}
