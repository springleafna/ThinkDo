package com.springleaf.thinkdo.intent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springleaf.thinkdo.enums.IntentKind;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 意图树缓存管理器
 * 负责意图树在 Redis 中的按用户缓存管理
 * <p>
 * 缓存策略：
 * - 每个用户独立缓存，key: thinkdo:intent:tree:user:{userId}
 * - 使用 Redis SET 记录所有有缓存的用户，便于全量失效
 * - TTL: 7 天
 * <p>
 * 失效规则：
 * - SYSTEM/MCP 节点变更 → 清除所有用户缓存
 * - SYSTEM 作用域 KB 节点变更 → 清除所有用户缓存
 * - USER 作用域 KB 节点变更 → 仅清除对应用户缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentTreeCacheManager {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY_PREFIX = "thinkdo:intent:tree:user:";
    private static final String CACHE_USER_SET_KEY = "thinkdo:intent:tree:users";
    private static final long CACHE_EXPIRE_DAYS = 7;

    private String userCacheKey(Long userId) {
        return CACHE_KEY_PREFIX + userId;
    }

    /**
     * 从 Redis 获取指定用户的意图树缓存
     *
     * @param userId 用户 ID
     * @return 意图树根节点列表，如果缓存不存在则返回 null
     */
    public List<IntentNode> getIntentTreeFromCache(Long userId) {
        try {
            String cacheJson = stringRedisTemplate.opsForValue().get(userCacheKey(userId));
            if (cacheJson == null) {
                return null;
            }
            return objectMapper.readValue(cacheJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("从 Redis 读取用户 {} 意图树缓存失败", userId, e);
            return null;
        }
    }

    /**
     * 将指定用户的意图树保存到 Redis 缓存
     *
     * @param userId 用户 ID
     * @param roots  意图树根节点列表
     */
    public void saveIntentTreeToCache(Long userId, List<IntentNode> roots) {
        try {
            String cacheJson = objectMapper.writeValueAsString(roots);
            stringRedisTemplate.opsForValue().set(userCacheKey(userId), cacheJson, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            // 注册到追踪集合
            stringRedisTemplate.opsForSet().add(CACHE_USER_SET_KEY, userId.toString());
            log.info("用户 {} 意图树已缓存到 Redis，根节点数: {}", userId, roots.size());
        } catch (Exception e) {
            log.error("保存用户 {} 意图树到 Redis 缓存失败", userId, e);
        }
    }

    /**
     * 清除指定用户的意图树缓存
     *
     * @param userId 用户 ID
     */
    public void clearUserCache(Long userId) {
        try {
            Boolean deleted = stringRedisTemplate.delete(userCacheKey(userId));
            stringRedisTemplate.opsForSet().remove(CACHE_USER_SET_KEY, userId.toString());
            if (deleted) {
                log.info("用户 {} 意图树缓存已清除", userId);
            }
        } catch (Exception e) {
            log.error("清除用户 {} 意图树缓存失败", userId, e);
        }
    }

    /**
     * 清除所有用户的意图树缓存（系统级变更时调用）
     * <p>
     * 通过追踪 SET 获取所有有缓存的用户 ID，批量删除对应的缓存 key
     */
    public void clearAllUserCaches() {
        try {
            Set<String> userIds = stringRedisTemplate.opsForSet().members(CACHE_USER_SET_KEY);
            if (userIds == null || userIds.isEmpty()) {
                return;
            }
            List<String> keys = userIds.stream()
                    .map(uid -> CACHE_KEY_PREFIX + uid)
                    .collect(Collectors.toList());
            stringRedisTemplate.delete(keys);
            stringRedisTemplate.delete(CACHE_USER_SET_KEY);
            log.info("已清除所有用户意图树缓存，共 {} 个用户", userIds.size());
        } catch (Exception e) {
            log.error("清除所有用户意图树缓存失败", e);
        }
    }

    /**
     * 检查指定用户的缓存是否存在
     *
     * @param userId 用户 ID
     * @return true 表示缓存存在
     */
    public boolean isCacheExists(Long userId) {
        try {
            return stringRedisTemplate.hasKey(userCacheKey(userId));
        } catch (Exception e) {
            log.error("检查用户 {} 意图树缓存是否存在失败", userId, e);
            return false;
        }
    }

    /**
     * 根据节点属性判断失效范围并执行缓存清除
     * <p>
     * - kind != KB (SYSTEM/MCP): 清除所有用户缓存
     * - kind == KB && scope == SYSTEM: 清除所有用户缓存
     * - kind == KB && scope == USER: 清除 created_by 用户缓存
     *
     * @param kind      节点类型 (0=KB, 1=SYSTEM, 2=MCP)
     * @param scope     知识库作用域 ("SYSTEM" / "USER")
     * @param createdBy 节点创建者 ID
     */
    public void invalidateByScope(Integer kind, String scope, Long createdBy) {
        if (kind != null && kind != IntentKind.KB.getCode()) {
            // SYSTEM 或 MCP 节点变更 → 所有用户失效
            clearAllUserCaches();
        } else if ("SYSTEM".equals(scope)) {
            // 系统 KB 节点变更 → 所有用户失效
            clearAllUserCaches();
        } else {
            // 用户 KB 节点变更 → 仅该用户失效
            if (createdBy != null) {
                clearUserCache(createdBy);
            } else {
                log.warn("无法确定用户意图树缓存失效范围 (createdBy 为空)，清除所有缓存");
                clearAllUserCaches();
            }
        }
    }
}
