package com.springleaf.thinkdo.context;

import com.springleaf.thinkdo.enums.UserRoleEnum;
import lombok.Data;

/**
 * 用户上下文管理
 * 用于在请求处理过程中存储当前用户信息
 */
@Data
public class UserContext {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户角色
     */
    private UserRoleEnum role;

    /**
     * 线程本地变量，用于存储当前线程的用户上下文
     */
    private static final ThreadLocal<UserContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前用户上下文
     * @param userContext 用户上下文
     */
    public static void setCurrentUser(UserContext userContext) {
        CONTEXT_HOLDER.set(userContext);
    }

    /**
     * 获取当前用户上下文
     * @return 用户上下文，如果未设置返回null
     */
    public static UserContext getCurrentUser() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 清除当前用户上下文
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * 获取当前用户ID
     * @return 用户ID，如果未设置返回null
     */
    public static Long getCurrentUserId() {
        UserContext context = getCurrentUser();
        return context != null ? context.getUserId() : null;
    }

    /**
     * 获取当前用户名
     * @return 用户名，如果未设置返回null
     */
    public static String getCurrentUsername() {
        UserContext context = getCurrentUser();
        return context != null ? context.getUsername() : null;
    }

    /**
     * 获取当前用户角色
     * @return 用户角色，如果未设置返回null
     */
    public static UserRoleEnum getCurrentUserRole() {
        UserContext context = getCurrentUser();
        return context != null ? context.getRole() : null;
    }

    /**
     * 判断当前用户是否为管理员
     * @return 如果当前用户是管理员返回true，否则返回false
     */
    public static boolean isAdmin() {
        UserRoleEnum role = getCurrentUserRole();
        return role != null && role == UserRoleEnum.ADMIN;
    }

    /**
     * 判断当前用户是否为普通用户
     * @return 如果当前用户是普通用户返回true，否则返回false
     */
    public static boolean isUser() {
        UserRoleEnum role = getCurrentUserRole();
        return role != null && role == UserRoleEnum.USER;
    }

    /**
     * 构建器模式创建UserContext
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String username;
        private UserRoleEnum role;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder role(UserRoleEnum role) {
            this.role = role;
            return this;
        }

        public UserContext build() {
            UserContext context = new UserContext();
            context.setUserId(userId);
            context.setUsername(username);
            context.setRole(role);
            return context;
        }
    }
}
