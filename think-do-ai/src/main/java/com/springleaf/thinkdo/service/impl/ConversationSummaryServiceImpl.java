package com.springleaf.thinkdo.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.springleaf.thinkdo.chat.ChatMessage;
import com.springleaf.thinkdo.chat.ChatRequest;
import com.springleaf.thinkdo.chat.LLMService;
import com.springleaf.thinkdo.config.MemoryProperties;
import com.springleaf.thinkdo.domain.entity.ConversationSummaryEntity;
import com.springleaf.thinkdo.domain.entity.MessageEntity;
import com.springleaf.thinkdo.mapper.ConversationSummaryMapper;
import com.springleaf.thinkdo.prompt.PromptTemplateLoader;
import com.springleaf.thinkdo.service.ConversationSummaryService;
import com.springleaf.thinkdo.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.springleaf.thinkdo.constant.RAGConstant.CONVERSATION_SUMMARY_PROMPT_PATH;

/**
 * 会话摘要 Service实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConversationSummaryServiceImpl implements ConversationSummaryService {

    private static final String SUMMARY_PREFIX = "对话摘要：";
    private static final String SUMMARY_LOCK_PREFIX = "thinkdo:memory:summary:lock:";
    private static final Duration SUMMARY_LOCK_TTL = Duration.ofMinutes(5);

    private final RedissonClient redissonClient;
    private final LLMService llmService;
    private final PromptTemplateLoader promptTemplateLoader;
    private final MemoryProperties memoryProperties;
    @Qualifier("memorySummaryThreadPoolExecutor")
    private final Executor memorySummaryExecutor;
    private final MessageService messageService;
    private final ConversationSummaryMapper conversationSummaryMapper;

    @Override
    public void compressIfNeeded(String conversationId, Long userId, ChatMessage message) {
        if (!memoryProperties.getSummaryEnabled()) {
            return;
        }
        if (message.getRole() != ChatMessage.Role.ASSISTANT) {
            return;
        }
        CompletableFuture.runAsync(() -> doCompressIfNeeded(conversationId, userId), memorySummaryExecutor)
                .exceptionally(ex -> {
                    log.error("对话记忆摘要异步任务失败 - conversationId: {}, userId: {}",
                            conversationId, userId, ex);
                    return null;
                });
    }

    @Override
    public ChatMessage loadLatestSummary(String conversationId, Long userId) {
        if (StrUtil.isBlank(conversationId) || userId == null) {
            return null;
        }
        ConversationSummaryEntity record = conversationSummaryMapper.selectOne(
                Wrappers.lambdaQuery(ConversationSummaryEntity.class)
                        .eq(ConversationSummaryEntity::getConversationId, conversationId)
                        .eq(ConversationSummaryEntity::getUserId, userId)
                        .eq(ConversationSummaryEntity::getDeleted, 0)
                        .orderByDesc(ConversationSummaryEntity::getId)
                        .last("limit 1")
        );
        if (record == null || StrUtil.isBlank(record.getContent())) {
            return null;
        }
        return new ChatMessage(ChatMessage.Role.SYSTEM, record.getContent());
    }

    @Override
    public ChatMessage decorateIfNeeded(ChatMessage summary) {
        if (summary == null || StrUtil.isBlank(summary.getContent())) {
            return summary;
        }

        String content = summary.getContent().trim();
        if (content.startsWith(SUMMARY_PREFIX) || content.startsWith("摘要：")) {
            return summary;
        }
        return ChatMessage.system(SUMMARY_PREFIX + content);
    }

    private void doCompressIfNeeded(String conversationId, Long userId) {
        long startTime = System.currentTimeMillis();
        int triggerTurns = memoryProperties.getSummaryStartTurns();
        int maxTurns = memoryProperties.getHistoryKeepTurns();
        if (maxTurns <= 0 || triggerTurns <= 0) {
            return;
        }

        // 加分布式锁防止重复生成摘要
        String lockKey = SUMMARY_LOCK_PREFIX + buildLockKey(conversationId, userId);
        RLock lock = redissonClient.getLock(lockKey);
        if (!tryLock(lock)) {
            return;
        }
        try {
            long total = messageService.countUserMessages(conversationId, userId);
            if (total < triggerTurns) {
                return;
            }

            // 获取最新的摘要
            ConversationSummaryEntity latestSummary = conversationSummaryMapper.selectOne(
                    Wrappers.lambdaQuery(ConversationSummaryEntity.class)
                            .eq(ConversationSummaryEntity::getConversationId, conversationId)
                            .eq(ConversationSummaryEntity::getUserId, userId)
                            .eq(ConversationSummaryEntity::getDeleted, 0)
                            .orderByDesc(ConversationSummaryEntity::getId)
                            .last("limit 1"));

            // 获取最近的需要完整作为上下文消息的几轮对话
            List<MessageEntity> latestUserTurns = messageService.listLatestUserOnlyMessages(
                    conversationId,
                    userId,
                    maxTurns
            );
            if (latestUserTurns.isEmpty()) {
                return;
            }
            // 获取完整对话最早的一个消息 ID，作为摘要结束位置的消息 ID（该ID之前的一个ID）
            Long cutoffId = resolveCutoffId(latestUserTurns);
            if (cutoffId == null) {
                return;
            }

            // 获取摘要起始位置的消息 ID（该ID之后的一个ID）
            Long afterId = resolveSummaryStartId(conversationId, userId, latestSummary);
            if (afterId != null && afterId >= cutoffId) {
                return;
            }

            // 根据起始 ID 和 结束 ID 获取要进行摘要的消息
            List<MessageEntity> toSummarize = messageService.listMessagesBetweenIds(
                    conversationId,
                    userId,
                    afterId,
                    cutoffId
            );
            if (CollUtil.isEmpty(toSummarize)) {
                return;
            }

            // 获取进行摘要的最后一个消息ID作为该次摘要的最后消息ID
            Long lastMessageId = resolveLastMessageId(toSummarize);
            if (lastMessageId == null) {
                return;
            }

            // 拼接消息生成摘要
            String existingSummary = latestSummary == null ? "" : latestSummary.getContent();
            String summary = summarizeMessages(toSummarize, existingSummary);
            if (StrUtil.isBlank(summary)) {
                return;
            }

            // 创建新的摘要记录到数据库
            createSummary(conversationId, userId, summary, lastMessageId);
            log.info("摘要成功 - conversationId：{}，userId：{}，消息数：{}，耗时：{}ms",
                    conversationId, userId, toSummarize.size(),
                    System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("摘要失败 - conversationId：{}，userId：{}", conversationId, userId, e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void createSummary(String conversationId,
                               Long userId,
                               String content,
                               Long lastMessageId) {
        ConversationSummaryEntity summaryEntity = new ConversationSummaryEntity();
        summaryEntity.setConversationId(conversationId);
        summaryEntity.setUserId(userId);
        summaryEntity.setContent(content);
        summaryEntity.setLastMessageId(lastMessageId);
        conversationSummaryMapper.insert(summaryEntity);
    }

    /**
     * 使用 LLM 对消息列表进行摘要生成
     *
     * @param messages        待摘要的消息实体列表
     * @param existingSummary 已存在的历史摘要
     * @return 生成的摘要内容，失败时返回原摘要
     */
    private String summarizeMessages(List<MessageEntity> messages, String existingSummary) {
        // 转换消息格式
        List<ChatMessage> histories = toHistoryMessages(messages);
        if (CollUtil.isEmpty(histories)) {
            return existingSummary;
        }

        // 构建系统提示词
        int summaryMaxChars = memoryProperties.getSummaryMaxChars();
        List<ChatMessage> summaryMessages = new ArrayList<>();
        String summaryPrompt = promptTemplateLoader.render(
                CONVERSATION_SUMMARY_PROMPT_PATH,
                Map.of("summary_max_chars", String.valueOf(summaryMaxChars))
        );
        summaryMessages.add(ChatMessage.system(summaryPrompt));

        // 添加历史摘要（如有）
        if (StrUtil.isNotBlank(existingSummary)) {
            summaryMessages.add(ChatMessage.assistant(
                    "历史摘要（仅用于合并去重，不得作为事实新增来源；若与本轮对话冲突，以本轮对话为准）：\n"
                            + existingSummary.trim()
            ));
        }
        summaryMessages.addAll(histories);
        summaryMessages.add(ChatMessage.user(
                "合并以上对话与历史摘要，去重后输出更新摘要。要求：严格≤" + summaryMaxChars + "字符；仅一行。"
        ));

        // 构建 LLM 请求并调用
        ChatRequest request = ChatRequest.builder()
                .messages(summaryMessages)
                .temperature(0.3D)
                .topP(0.9D)
                .thinking(false)
                .build();
        try {
            String result = llmService.chat(request);
            log.info("对话摘要生成 - resultChars: {}", result.length());

            return result;
        } catch (Exception e) {
            log.error("对话记忆摘要生成失败，conversationId 相关消息数：{}", messages.size(), e);
            return existingSummary;
        }
    }

    private List<ChatMessage> toHistoryMessages(List<MessageEntity> messages) {
        if (CollUtil.isEmpty(messages)) {
            return List.of();
        }
        return messages.stream()
                .filter(item -> item != null
                        && StrUtil.isNotBlank(item.getContent())
                        && StrUtil.isNotBlank(item.getRole()))
                .map(item -> {
                    String role = item.getRole().toLowerCase();
                    if ("user".equals(role)) {
                        return ChatMessage.user(item.getContent());
                    } else if ("assistant".equals(role)) {
                        return ChatMessage.assistant(item.getContent());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 解析待摘要消息列表中的最后一条消息 ID
     *
     * @param toSummarize 待摘要的消息列表
     * @return 最后一条有效消息的 ID，如果不存在则返回 null
     */
    private Long resolveLastMessageId(List<MessageEntity> toSummarize) {
        // 倒序遍历，找到第一条有效的消息 ID
        for (int i = toSummarize.size() - 1; i >= 0; i--) {
            MessageEntity item = toSummarize.get(i);
            if (item != null && item.getId() != null) {
                return item.getId();
            }
        }
        return null;
    }

    /**
     * 解析摘要起始位置的消息 ID
     *
     * @param conversationId 会话 ID
     * @param userId         用户 ID
     * @param summary        最新的摘要记录
     * @return 摘要起始消息 ID，如果不存在则返回 null
     */
    private Long resolveSummaryStartId(String conversationId, Long userId, ConversationSummaryEntity summary) {
        if (summary == null) {
            return null;
        }
        // 优先使用摘要中记录的最后消息 ID
        if (summary.getLastMessageId() != null) {
            return summary.getLastMessageId();
        }

        // 使用时间戳查找对应的消息 ID
        LocalDateTime after = summary.getUpdatedAt();
        if (after == null) {
            after = summary.getCreatedAt();
        }
        return messageService.findMaxMessageIdAtOrBefore(conversationId, userId, after);
    }

    /**
     * 解析截止位置的消息 ID（基于最早的用户消息）
     *
     * @param latestUserTurns 最近的用户消息列表（按创建时间倒序排列）
     * @return 最早消息的 ID，如果列表为空则返回 null
     */
    private Long resolveCutoffId(List<MessageEntity> latestUserTurns) {
        if (CollUtil.isEmpty(latestUserTurns)) {
            return null;
        }

        // 倒序列表的最后一个就是最早的
        MessageEntity oldest = latestUserTurns.get(latestUserTurns.size() - 1);
        return oldest == null ? null : oldest.getId();
    }

    private boolean tryLock(RLock lock) {
        try {
            return lock.tryLock(0, SUMMARY_LOCK_TTL.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String buildLockKey(String conversationId, Long userId) {
        return String.valueOf(userId).trim() + ":" + conversationId.trim();
    }
}
