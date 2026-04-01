package com.springleaf.thinkdo.intent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.springleaf.thinkdo.domain.dto.IntentCandidate;
import com.springleaf.thinkdo.domain.dto.IntentGroup;
import com.springleaf.thinkdo.domain.dto.SubQuestionIntent;
import com.springleaf.thinkdo.domain.entity.IntentNodeEntity;
import com.springleaf.thinkdo.enums.IntentKind;
import com.springleaf.thinkdo.enums.IntentLevel;
import com.springleaf.thinkdo.enums.KnowledgeScopeEnum;
import com.springleaf.thinkdo.mapper.IntentNodeMapper;
import com.springleaf.thinkdo.rewrite.RewriteResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static com.springleaf.thinkdo.constant.RAGConstant.INTENT_MIN_SCORE;
import static com.springleaf.thinkdo.constant.RAGConstant.MAX_INTENT_COUNT;

@Slf4j
@Service
public class IntentResolver {

    private final IntentClassifier intentClassifier;
    private final Executor intentClassifyExecutor;
    private final IntentNodeMapper intentNodeMapper;
    private final IntentTreeCacheManager intentTreeCacheManager;

    public IntentResolver(
            @Qualifier("defaultIntentClassifier") IntentClassifier intentClassifier,
            @Qualifier("intentClassifyThreadPoolExecutor") Executor intentClassifyExecutor,
            IntentNodeMapper intentNodeMapper,
            IntentTreeCacheManager intentTreeCacheManager) {
        this.intentClassifier = intentClassifier;
        this.intentClassifyExecutor = intentClassifyExecutor;
        this.intentNodeMapper = intentNodeMapper;
        this.intentTreeCacheManager = intentTreeCacheManager;
    }

    /**
     * 确保用户意图树存在（domain和category级别节点）
     * 如果不存在则创建，存在则跳过
     *
     * @param userId 用户ID
     */
    public void ensureUserIntentTreeExists(Long userId) {
        String domainCode = "root_user_" + userId;
        String categoryCode = "category_user_kb_" + userId;
        boolean created = false;

        // 检查domain节点是否存在
        IntentNodeEntity domainNode = intentNodeMapper.selectOne(
                new LambdaQueryWrapper<IntentNodeEntity>()
                        .eq(IntentNodeEntity::getIntentCode, domainCode)
        );

        // 如果domain节点不存在，则创建
        if (domainNode == null) {
            domainNode = IntentNodeEntity.builder()
                    .kbId(null)
                    .intentCode(domainCode)
                    .scope(KnowledgeScopeEnum.USER.getValue())
                    .name("用户私人知识库")
                    .level(IntentLevel.DOMAIN.getCode())
                    .parentCode(null)
                    .description("用户私有知识库根节点，包含用户上传的所有文件内容")
                    .examples("")
                    .collectionName(null)
                    .topK(null)
                    .kind(IntentKind.KB.getCode())
                    .promptSnippet("请基于用户私有知识库内容回答，这些是用户自己上传的文件。")
                    .sortOrder(1)
                    .enabled(1)
                    .createdBy(userId)
                    .updatedBy(userId)
                    .build();
            intentNodeMapper.insert(domainNode);
            created = true;
            log.info("为用户 {} 创建domain意图节点: {}", userId, domainCode);
        }

        // 检查category节点是否存在
        IntentNodeEntity categoryNode = intentNodeMapper.selectOne(
                new LambdaQueryWrapper<IntentNodeEntity>()
                        .eq(IntentNodeEntity::getIntentCode, categoryCode)
        );

        // 如果category节点不存在，则创建
        if (categoryNode == null) {
            categoryNode = IntentNodeEntity.builder()
                    .kbId(null)
                    .intentCode(categoryCode)
                    .scope(KnowledgeScopeEnum.USER.getValue())
                    .name("我的知识库")
                    .level(IntentLevel.CATEGORY.getCode())
                    .parentCode(domainCode)
                    .description("用户创建的私有知识库集合，包含用户上传的所有文件")
                    .examples("")
                    .collectionName(null)
                    .topK(null)
                    .kind(IntentKind.KB.getCode())
                    .promptSnippet("请基于用户知识库中的私有内容回答问题。")
                    .sortOrder(1)
                    .enabled(1)
                    .createdBy(userId)
                    .updatedBy(userId)
                    .build();
            intentNodeMapper.insert(categoryNode);
            created = true;
            log.info("为用户 {} 创建category意图节点: {}", userId, categoryCode);
        }

        // 只有实际创建了新节点时才需要失效缓存
        if (created) {
            intentTreeCacheManager.clearUserCache(userId);
        }
    }

    /**
     * 解析重写后的问题，识别每个子问题的意图
     * <p>
     * 该方法将输入的问题（可能是多个子问题）并行分类到不同的意图节点，
     * 并限制总意图数量不超过配置的上限。
     *
     * @param rewriteResult 重写结果，包含重写后的问题和子问题列表
     * @return 子问题意图列表，每个子问题对应一个 SubQuestionIntent，包含该子问题匹配的意图节点及分数
     */
    public List<SubQuestionIntent> resolve(RewriteResult rewriteResult, Long userId) {
        List<String> subQuestions = CollUtil.isNotEmpty(rewriteResult.subQuestions())
                ? rewriteResult.subQuestions()
                : List.of(rewriteResult.rewrittenQuestion());
        List<CompletableFuture<SubQuestionIntent>> tasks = subQuestions.stream()
                .map(q -> CompletableFuture.supplyAsync(
                        () -> new SubQuestionIntent(q, classifyIntents(q, userId)),
                        intentClassifyExecutor
                ))
                .toList();
        List<SubQuestionIntent> subIntents = tasks.stream()
                .map(CompletableFuture::join)
                .toList();
        return capTotalIntents(subIntents);
    }

    /**
     * 合并多个子问题的意图为一个统一的意图组
     * <p>
     * 将所有子问题的 MCP 意图和 KB 意图分别聚合到两个列表中，
     * 便于后续的意图处理和路由。
     *
     * @param subIntents 子问题意图列表
     * @return 合并后的意图组，包含 MCP 意图列表和 KB 意图列表
     */
    public IntentGroup mergeIntentGroup(List<SubQuestionIntent> subIntents) {
        List<NodeScore> mcpIntents = new ArrayList<>();
        List<NodeScore> kbIntents = new ArrayList<>();
        for (SubQuestionIntent si : subIntents) {
            mcpIntents.addAll(filterMcpIntents(si.nodeScores()));
            kbIntents.addAll(filterKbIntents(si.nodeScores()));
        }
        return new IntentGroup(mcpIntents, kbIntents);
    }

    /**
     * 判断意图是否仅包含系统节点
     * <p>
     * 当且仅当只有一个意图节点，且该节点类型为 SYSTEM 时返回 true。
     *
     * @param nodeScores 节点评分列表
     * @return 如果仅包含系统节点返回 true，否则返回 false
     */
    public boolean isSystemOnly(List<NodeScore> nodeScores) {
        return nodeScores.size() == 1
                && nodeScores.get(0).getNode() != null
                && nodeScores.get(0).getNode().getKind() == IntentKind.SYSTEM;
    }

    /**
     * 对问题进行意图分类，过滤低分意图并限制最大数量
     *
     * @param question 待分类的问题文本
     * @return 过滤后的意图节点评分列表，只保留分数大于等于最小阈值且数量不超过上限的意图
     */
    private List<NodeScore> classifyIntents(String question, Long userId) {
        List<NodeScore> scores = intentClassifier.classifyTargets(question, userId);
        return scores.stream()
                .filter(ns -> ns.getScore() >= INTENT_MIN_SCORE)
                .limit(MAX_INTENT_COUNT)
                .toList();
    }

    /**
     * 过滤出 MCP 类型的意图节点
     * <p>
     * 只保留节点类型为 MCP 且 MCP 工具 ID 不为空的意图。
     *
     * @param nodeScores 节点评分列表
     * @return 过滤后的 MCP 意图列表
     */
    private List<NodeScore> filterMcpIntents(List<NodeScore> nodeScores) {
        return nodeScores.stream()
                .filter(ns -> ns.getNode() != null && ns.getNode().getKind() == IntentKind.MCP)
                .filter(ns -> StrUtil.isNotBlank(ns.getNode().getMcpToolId()))
                .toList();
    }

    /**
     * 过滤出 KB 类型的意图节点
     * <p>
     * 保留节点类型为 KB 或类型为空（默认为 KB）的意图。
     *
     * @param nodeScores 节点评分列表
     * @return 过滤后的 KB 意图列表
     */
    private List<NodeScore> filterKbIntents(List<NodeScore> nodeScores) {
        return nodeScores.stream()
                .filter(ns -> {
                    IntentNode node = ns.getNode();
                    if (node == null) {
                        return false;
                    }
                    return node.getKind() == null || node.getKind() == IntentKind.KB;
                })
                .toList();
    }

    /**
     * 限制总意图数量不超过 MAX_INTENT_COUNT
     * <p>
     * 策略：
     * 1. 如果总数未超限，直接返回
     * 2. 如果超限，每个子问题至少保留 1 个最高分意图
     * 3. 剩余配额按分数从高到低分配给其他意图
     *
     * @param subIntents 子问题意图列表
     * @return 限制数量后的子问题意图列表
     */
    private List<SubQuestionIntent> capTotalIntents(List<SubQuestionIntent> subIntents) {
        int totalIntents = subIntents.stream()
                .mapToInt(si -> si.nodeScores().size())
                .sum();

        // 未超限，直接返回
        if (totalIntents <= MAX_INTENT_COUNT) {
            return subIntents;
        }

        // 步骤 1：收集所有意图，按子问题索引分组
        List<IntentCandidate> allCandidates = collectAllCandidates(subIntents);

        // 步骤 2：每个子问题保留最高分意图
        List<IntentCandidate> guaranteedIntents = selectTopIntentPerSubQuestion(allCandidates, subIntents.size());

        // 步骤 3：计算剩余配额
        int remaining = MAX_INTENT_COUNT - guaranteedIntents.size();

        // 步骤 4：从剩余候选中按分数选择
        List<IntentCandidate> additionalIntents = selectAdditionalIntents(allCandidates, guaranteedIntents, remaining);

        // 步骤 5：合并并重建结果
        return rebuildSubIntents(subIntents, guaranteedIntents, additionalIntents);
    }

    /**
     * 收集所有意图候选，标记所属子问题索引
     * <p>
     * 遍历所有子问题的意图，将每个意图封装为 IntentCandidate 并记录其所属子问题的索引，
     * 最后按意图分数降序排序。
     *
     * @param subIntents 子问题意图列表
     * @return 按分数降序排列的意图候选列表
     */
    private List<IntentCandidate> collectAllCandidates(List<SubQuestionIntent> subIntents) {
        List<IntentCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < subIntents.size(); i++) {
            List<NodeScore> nodeScores = subIntents.get(i).nodeScores();
            if (CollUtil.isEmpty(nodeScores)) {
                continue;
            }
            for (NodeScore ns : nodeScores) {
                candidates.add(new IntentCandidate(i, ns));
            }
        }
        // 按分数降序排序
        candidates.sort((a, b) -> Double.compare(b.nodeScore().getScore(), a.nodeScore().getScore()));
        return candidates;
    }

    /**
     * 每个子问题选择最高分意图（保底策略）
     * <p>
     * 确保每个子问题至少有一个意图被保留，选择该子问题中分数最高的意图。
     *
     * @param allCandidates 所有意图候选列表，已按分数降序排序
     * @param subQuestionCount 子问题数量
     * @return 每个子问题的最高分意图列表
     */
    private List<IntentCandidate> selectTopIntentPerSubQuestion(List<IntentCandidate> allCandidates, int subQuestionCount) {
        List<IntentCandidate> topIntents = new ArrayList<>();
        boolean[] selected = new boolean[subQuestionCount];

        for (IntentCandidate candidate : allCandidates) {
            int index = candidate.subQuestionIndex();
            if (!selected[index]) {
                topIntents.add(candidate);
                selected[index] = true;
            }
            // 所有子问题都有了保底意图，提前退出
            if (topIntents.size() == subQuestionCount) {
                break;
            }
        }
        return topIntents;
    }

    /**
     * 从剩余候选中选择额外意图
     * <p>
     * 在满足每个子问题的保底意图后，根据剩余配额从所有候选中按分数高低选择额外的意图。
     *
     * @param allCandidates 所有意图候选列表，已按分数降序排序
     * @param guaranteedIntents 已选中的保底意图列表
     * @param remaining 剩余可选配额数量
     * @return 额外选中的意图列表
     */
    private List<IntentCandidate> selectAdditionalIntents(List<IntentCandidate> allCandidates,
                                                          List<IntentCandidate> guaranteedIntents,
                                                          int remaining) {
        if (remaining <= 0) {
            return List.of();
        }

        List<IntentCandidate> additional = new ArrayList<>();
        for (IntentCandidate candidate : allCandidates) {
            // 跳过已经被选为保底的意图
            if (guaranteedIntents.contains(candidate)) {
                continue;
            }
            additional.add(candidate);
            if (additional.size() >= remaining) {
                break;
            }
        }
        return additional;
    }

    /**
     * 根据选中的意图重建 SubQuestionIntent 列表
     * <p>
     * 将选中的意图按子问题索引分组，重建 SubQuestionIntent 对象，保持原有顺序。
     *
     * @param originalSubIntents 原始的子问题意图列表
     * @param guaranteedIntents 保底意图列表
     * @param additionalIntents 额外选中的意图列表
     * @return 重建后的子问题意图列表
     */
    private List<SubQuestionIntent> rebuildSubIntents(List<SubQuestionIntent> originalSubIntents,
                                                      List<IntentCandidate> guaranteedIntents,
                                                      List<IntentCandidate> additionalIntents) {
        // 合并所有选中的意图
        List<IntentCandidate> allSelected = new ArrayList<>(guaranteedIntents);
        allSelected.addAll(additionalIntents);

        // 按子问题索引分组
        Map<Integer, List<NodeScore>> groupedByIndex = new ConcurrentHashMap<>();
        for (IntentCandidate candidate : allSelected) {
            groupedByIndex.computeIfAbsent(candidate.subQuestionIndex(), k -> new ArrayList<>())
                    .add(candidate.nodeScore());
        }

        // 重建结果
        List<SubQuestionIntent> result = new ArrayList<>();
        for (int i = 0; i < originalSubIntents.size(); i++) {
            SubQuestionIntent original = originalSubIntents.get(i);
            List<NodeScore> retained = groupedByIndex.getOrDefault(i, List.of());
            result.add(new SubQuestionIntent(original.subQuestion(), retained));
        }
        return result;
    }
}
