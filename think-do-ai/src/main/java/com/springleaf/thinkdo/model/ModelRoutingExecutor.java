package com.springleaf.thinkdo.model;

import com.springleaf.thinkdo.enums.ModelCapability;
import com.springleaf.thinkdo.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * 模型路由执行器
 * 负责在多个模型候选者之间进行调度执行，并提供故障转移（Fallback）和健康检查机制
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelRoutingExecutor {

    private final ModelHealthStore healthStore;

    /**
     * 执行模型调用并支持故障转移。遍历所有可用的模型候选，依次尝试调用，当某个模型调用失败时自动切换到下一个可用模型。
     * 如果所有模型都失败，则抛出业务异常。
     *
     * @param <C>            客户端类型参数
     * @param <T>            返回值类型参数
     * @param capability     模型能力定义，用于获取模型显示名称和标识
     * @param targets        模型目标列表，包含所有可用的模型候选方案
     * @param clientResolver 客户端解析函数，根据模型目标创建对应的客户端实例
     * @param caller         模型调用器，负责执行实际的模型调用逻辑
     * @return 模型调用成功的响应结果
     * @throws BusinessException 当没有可用的模型候选或所有模型候选都调用失败时抛出
     */
    public <C, T> T executeWithFallback(
            ModelCapability capability,
            List<ModelTarget> targets,
            Function<ModelTarget, C> clientResolver,
            ModelCaller<C, T> caller) {
        String label = capability.getDisplayName();
        if (targets == null || targets.isEmpty()) {
            throw new BusinessException("No " + label + " model candidates available");
        }

        Throwable last = null;
        // 遍历所有模型候选，依次尝试调用
        for (ModelTarget target : targets) {
            C client = clientResolver.apply(target);
            if (client == null) {
                log.warn("{} provider client missing: provider={}, modelId={}", label, target.candidate().getProvider(), target.id());
                continue;
            }
            // 检查健康状态，只允许调用健康的模型
            if (!healthStore.allowCall(target.id())) {
                continue;
            }

            // 尝试执行模型调用，成功则标记健康并返回结果
            try {
                T response = caller.call(client, target);
                healthStore.markSuccess(target.id());
                return response;
            } catch (Exception e) {
                last = e;
                healthStore.markFailure(target.id());
                log.warn("{} model failed, fallback to next. modelId={}, provider={}", label, target.id(), target.candidate().getProvider(), e);
            }
        }

        // 所有模型候选均失败，抛出异常
        throw new BusinessException(
                "All " + label + " model candidates failed: " + (last == null ? "unknown" : last.getMessage()),
                last
        );
    }
}

