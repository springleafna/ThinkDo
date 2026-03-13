package com.springleaf.thinkdo.model;

import cn.hutool.core.util.StrUtil;
import com.springleaf.thinkdo.config.AIModelProperties;
import com.springleaf.thinkdo.enums.ModelProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 模型选择器
 * 负责根据配置和当前需求（如普通对话、深度思考、Embedding 等）选择合适的模型候选列表
 * 并结合健康状态（熔断机制）来过滤不可用的模型。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelSelector {

    private final AIModelProperties properties;
    private final ModelHealthStore healthStore;

    /**
     * 选择聊天模型的候选列表
     *
     * 根据是否启用深度思考模式，从配置中解析并返回合适的模型候选列表。
     * 会优先使用配置的首选模型，并按优先级排序所有可用候选。
     *
     * @param deepThinking 是否启用深度思考模式，true 表示使用深度思考模型，false 或 null 表示使用默认模型
     * @return 模型目标列表，按优先级排序，如果配置为空则返回空列表
     */
    public List<ModelTarget> selectChatCandidates(Boolean deepThinking) {
        AIModelProperties.ModelGroup group = properties.getChat();
        if (group == null) {
            return List.of();
        }

        String firstChoiceModelId = resolveFirstChoiceModel(group, deepThinking);
        return selectCandidates(group, firstChoiceModelId, deepThinking);
    }

    /**
     * 选择 Embedding 模型的候选列表
     *
     * 从配置中获取并返回所有可用的 Embedding 模型候选。
     *
     * @return Embedding 模型目标列表，如果配置为空则返回空列表
     */
    public List<ModelTarget> selectEmbeddingCandidates() {
        return selectCandidates(properties.getEmbedding());
    }

    /**
     * 选择 Rerank 模型的候选列表
     *
     * 从配置中获取并返回所有可用的 Rerank 模型候选。
     *
     * @return Rerank 模型目标列表，如果配置为空则返回空列表
     */
    public List<ModelTarget> selectRerankCandidates() {
        return selectCandidates(properties.getRerank());
    }

    /**
     * 选择默认的 Embedding 模型
     *
     * 从 Embedding 候选列表中选择第一个可用的模型作为默认模型。
     *
     * @return 默认 Embedding 模型目标，如果没有可用候选则返回 null
     */
    public ModelTarget selectDefaultEmbedding() {
        List<ModelTarget> targets = selectEmbeddingCandidates();
        return targets.isEmpty() ? null : targets.get(0);
    }

    /**
     * 根据模式解析首选模型
     *
     * 根据深度思考模式的开关，决定返回哪个模型 ID 作为首选：
     * - 深度思考模式开启：优先返回 deep-thinking-model 配置
     * - 深度思考模式关闭：返回 default-model 配置
     *
     * @param group 模型组配置，包含各种模式下的模型配置
     * @param deepThinking 是否启用深度思考模式
     * @return 首选模型 ID，如果对应配置为空则返回 default-model
     */
    private String resolveFirstChoiceModel(AIModelProperties.ModelGroup group, Boolean deepThinking) {
        if (Boolean.TRUE.equals(deepThinking)) {
            String deepModel = group.getDeepThinkingModel();
            if (StrUtil.isNotBlank(deepModel)) {
                return deepModel;
            }
        }
        return group.getDefaultModel();
    }

    /**
     * 选择模型候选列表（简化版本）
     *
     * 基于模型组配置和其默认模型 ID，构建候选列表。
     *
     * @param group 模型组配置
     * @return 模型目标列表，如果配置为空则返回空列表
     */
    private List<ModelTarget> selectCandidates(AIModelProperties.ModelGroup group) {
        if (group == null) {
            return List.of();
        }
        return selectCandidates(group, group.getDefaultModel(), null);
    }

    /**
     * 选择模型候选列表（完整版本）
     *
     * 根据模型组配置、首选模型 ID 和深度思考模式，准备并返回排序后的候选列表。
     * 该方法会过滤启用的候选、按优先级排序，并将首选模型提升到列表首位。
     *
     * @param group 模型组配置
     * @param firstChoiceModelId 首选模型 ID，用于提升到候选列表首位
     * @param deepThinking 是否启用深度思考模式，用于过滤支持该功能的模型
     * @return 模型目标列表，已按优先级排序且通过熔断检查，如果配置为空则返回空列表
     */
    private List<ModelTarget> selectCandidates(AIModelProperties.ModelGroup group, String firstChoiceModelId, Boolean deepThinking) {
        if (group == null || group.getCandidates() == null) {
            return List.of();
        }

        List<AIModelProperties.ModelCandidate> orderedCandidates =
                prepareOrderedCandidates(group.getCandidates(), firstChoiceModelId, deepThinking);

        return buildAvailableTargets(orderedCandidates);
    }

    /**
     * 准备排序后的候选模型列表
     *
     * 对候选模型进行过滤和排序：
     * 1. 过滤掉未启用的候选（enabled 为 false 或 null 的保留）
     * 2. 在深度思考模式下，只保留支持深度思考的模型
     * 3. 按优先级（priority）升序排序，优先级相同时按 ID 排序
     *
     * @param candidates 原始候选模型列表
     * @param firstChoiceModelId 首选模型 ID（该方法中未使用，但保留用于后续扩展）
     * @param deepThinking 是否启用深度思考模式
     * @return 过滤并排序后的候选模型列表
     */
    private List<AIModelProperties.ModelCandidate> prepareOrderedCandidates(
            List<AIModelProperties.ModelCandidate> candidates,
            String firstChoiceModelId,
            Boolean deepThinking) {
        List<AIModelProperties.ModelCandidate> enabled = candidates.stream()
                .filter(c -> c != null && !Boolean.FALSE.equals(c.getEnabled()))
                .filter(c -> !Boolean.TRUE.equals(deepThinking) || Boolean.TRUE.equals(c.getSupportsThinking()))
                .sorted(Comparator
                        .comparing(AIModelProperties.ModelCandidate::getPriority,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(AIModelProperties.ModelCandidate::getId,
                                Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toCollection(ArrayList::new));

        if (Boolean.TRUE.equals(deepThinking) && enabled.isEmpty()) {
            log.warn("深度思考模式没有可用候选模型");
            return enabled;
        }

        promoteFirstChoiceModel(enabled, firstChoiceModelId);

        return enabled;
    }

    /**
     * 将首选模型提升到候选列表首位
     *
     * 如果指定了首选模型 ID，则在候选列表中找到该模型并将其移动到列表的第一个位置，
     * 确保它会被优先选择。
     *
     * @param candidates 候选模型列表
     * @param firstChoiceModelId 首选模型 ID，如果为空则不执行任何操作
     */
    private void promoteFirstChoiceModel(
            List<AIModelProperties.ModelCandidate> candidates,
            String firstChoiceModelId) {

        if (StrUtil.isBlank(firstChoiceModelId)) {
            return;
        }

        AIModelProperties.ModelCandidate firstChoice = findCandidate(candidates, firstChoiceModelId);
        candidates.remove(firstChoice);
        candidates.add(0, firstChoice);
    }

    /**
     * 构建可用的模型目标列表
     *
     * 将候选模型转换为目标模型，过程中会：
     * 1. 检查熔断状态，熔断的模型会被过滤掉
     * 2. 验证 Provider 配置是否存在
     *
     * @param candidates 已排序的候选模型列表
     * @return 可用的模型目标列表，已过滤掉熔断和配置缺失的模型
     */
    private List<ModelTarget> buildAvailableTargets(
            List<AIModelProperties.ModelCandidate> candidates) {

        Map<String, AIModelProperties.ProviderConfig> providers = properties.getProviders();

        return candidates.stream()
                .map(candidate -> buildModelTarget(candidate, providers))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 构建单个模型目标
     *
     * 将候选模型转换为模型目标，执行以下检查：
     * 1. 检查熔断状态，如果熔断则返回 null
     * 2. 验证 Provider 配置是否存在（NOOP provider 除外）
     *
     * @param candidate 候选模型
     * @param providers 所有 Provider 配置映射表
     * @return 模型目标对象，如果熔断或配置缺失则返回 null
     */
    private ModelTarget buildModelTarget(AIModelProperties.ModelCandidate candidate, Map<String, AIModelProperties.ProviderConfig> providers) {
        String modelId = resolveId(candidate);

        // 检查熔断状态
        if (healthStore.isOpen(modelId)) {
            return null;
        }

        // 验证 provider 配置
        AIModelProperties.ProviderConfig provider = providers.get(candidate.getProvider());
        if (provider == null && !ModelProvider.NOOP.matches(candidate.getProvider())) {
            log.warn("Provider 配置缺失：provider={}, modelId={}",
                    candidate.getProvider(), modelId);
            return null;
        }

        return new ModelTarget(modelId, candidate, provider);
    }

    /**
     * 根据 ID 查找候选模型
     *
     * 在候选列表中查找指定 ID 的模型。
     *
     * @param candidates 候选模型列表
     * @param id 要查找的模型 ID
     * @return 找到的候选模型，如果不存在则返回 null
     */
    private AIModelProperties.ModelCandidate findCandidate(
            List<AIModelProperties.ModelCandidate> candidates,
            String id) {

        return candidates.stream()
                .filter(c -> id.equals(c.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析模型 ID
     *
     * 从候选模型中解析出唯一的模型标识符：
     * - 如果显式配置了 id，则直接使用
     * - 否则使用 "provider::model" 格式组合生成
     *
     * @param candidate 候选模型
     * @return 模型唯一标识符，如果候选为 null 则返回 null
     */
    private String resolveId(AIModelProperties.ModelCandidate candidate) {
        if (candidate == null) {
            return null;
        }
        if (StrUtil.isNotBlank(candidate.getId())) {
            return candidate.getId();
        }
        return String.format("%s::%s",
                Objects.toString(candidate.getProvider(), "unknown"),
                Objects.toString(candidate.getModel(), "unknown"));
    }
}

