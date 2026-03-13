package com.springleaf.thinkdo.model;

import com.springleaf.thinkdo.config.AIModelProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型健康状态存储器
 * 用于管理和跟踪各个 AI 模型的健康状况，实现断路器模式
 * 
 * 支持三种状态：CLOSED（正常）、OPEN（熔断）和HALF_OPEN（半开），并提供相应的状态转换机制。
 */
@Component
@RequiredArgsConstructor
public class ModelHealthStore {

    private final AIModelProperties properties;

    /**
     * 模型健康状态映射表
     * Key: 模型唯一标识符 (id)
     * Value: 对应模型的健康状态信息 (ModelHealth)
     */
    private final Map<String, ModelHealth> healthById = new ConcurrentHashMap<>();

    /**
     * 检查指定ID的模型是否处于熔断开启状态
     * 
     * 当模型因连续失败达到阈值而被熔断时，此方法返回true，表示在一定时间内拒绝对该模型的调用
     * 
     * @param id 模型唯一标识符
     * @return 如果模型处于OPEN状态且仍在熔断时间内返回true，否则返回false
     */
    public boolean isOpen(String id) {
        ModelHealth health = healthById.get(id);
        if (health == null) {
            return false;
        }
        return health.state == State.OPEN && health.openUntil > System.currentTimeMillis();
    }

    /**
     * 判断是否允许对指定ID的模型进行调用
     * 
     * 实现了断路器模式的状态机逻辑，根据当前模型状态决定是否允许调用：
     * - CLOSED状态：始终允许调用
     * - OPEN状态：如果已过熔断时间，则进入HALF_OPEN状态并允许一次试探性调用
     * - HALF_OPEN状态：只允许一个并发的试探性调用
     * 
     * @param id 模型唯一标识符
     * @return 如果允许调用返回true，否则返回false
     */
    public boolean allowCall(String id) {
        if (id == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        final boolean[] allowed = {false};
        healthById.compute(id, (k, v) -> {
            if (v == null) {
                v = new ModelHealth();
            }
            if (v.state == State.OPEN) {
                if (v.openUntil > now) {
                    return v;
                }
                v.state = State.HALF_OPEN;
                v.halfOpenInFlight = true;
                allowed[0] = true;
                return v;
            }
            if (v.state == State.HALF_OPEN) {
                if (v.halfOpenInFlight) {
                    return v;
                }
                v.halfOpenInFlight = true;
                allowed[0] = true;
                return v;
            }
            allowed[0] = true;
            return v;
        });
        return allowed[0];
    }

    /**
     * 标记对指定ID模型的调用成功
     * 
     * 当对模型的调用成功完成时调用此方法，会将模型状态重置为CLOSED，
     * 清除所有失败计数和熔断相关状态
     * 
     * @param id 模型唯一标识符
     */
    public void markSuccess(String id) {
        if (id == null) {
            return;
        }
        healthById.compute(id, (k, v) -> {
            if (v == null) {
                return new ModelHealth();
            }
            v.state = State.CLOSED;
            v.consecutiveFailures = 0;
            v.openUntil = 0L;
            v.halfOpenInFlight = false;
            return v;
        });
    }

    /**
     * 标记对指定ID模型的调用失败
     * 
     * 当对模型的调用失败时调用此方法，会增加连续失败计数。
     * 如果连续失败次数达到配置的阈值，则将模型状态设置为OPEN（熔断），
     * 并设置熔断结束时间。对于HALF_OPEN状态下的失败，会立即重新进入OPEN状态。
     * 
     * @param id 模型唯一标识符
     */
    public void markFailure(String id) {
        if (id == null) {
            return;
        }
        long now = System.currentTimeMillis();
        healthById.compute(id, (k, v) -> {
            if (v == null) {
                v = new ModelHealth();
            }
            if (v.state == State.HALF_OPEN) {
                v.state = State.OPEN;
                v.openUntil = now + properties.getSelection().getOpenDurationMs();
                v.consecutiveFailures = 0;
                v.halfOpenInFlight = false;
                return v;
            }
            v.consecutiveFailures++;
            if (v.consecutiveFailures >= properties.getSelection().getFailureThreshold()) {
                v.state = State.OPEN;
                v.openUntil = now + properties.getSelection().getOpenDurationMs();
                v.consecutiveFailures = 0;
            }
            return v;
        });
    }

    /**
     * 模型健康状态内部类
     * 
     * 封装单个模型的健康状态信息，包括连续失败次数、熔断截止时间、
     * 半开状态下的飞行标志和当前状态。
     */
    private static class ModelHealth {
        private int consecutiveFailures;
        private long openUntil;
        private boolean halfOpenInFlight;
        private State state;

        private ModelHealth() {
            this.consecutiveFailures = 0;
            this.openUntil = 0L;
            this.halfOpenInFlight = false;
            this.state = State.CLOSED;
        }
    }

    /**
     * 断路器状态枚举
     * 
     * 定义了断路器的三种可能状态：
     * - CLOSED: 正常状态，允许所有请求
     * - OPEN: 熔断状态，拒绝所有请求
     * - HALF_OPEN: 半开状态，允许有限数量的请求以测试服务是否恢复
     */
    private enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }
}
