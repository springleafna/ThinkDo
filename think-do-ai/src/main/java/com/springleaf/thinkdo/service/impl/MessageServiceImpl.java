package com.springleaf.thinkdo.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.thinkdo.chat.ChatMessage;
import com.springleaf.thinkdo.chat.ChatRequest;
import com.springleaf.thinkdo.chat.LLMService;
import com.springleaf.thinkdo.config.MemoryProperties;
import com.springleaf.thinkdo.domain.entity.ConversationEntity;
import com.springleaf.thinkdo.domain.entity.MessageEntity;
import com.springleaf.thinkdo.domain.response.MessageInfoResp;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.ConversationMapper;
import com.springleaf.thinkdo.mapper.MessageMapper;
import com.springleaf.thinkdo.prompt.PromptTemplateLoader;
import com.springleaf.thinkdo.service.ConversationSummaryService;
import com.springleaf.thinkdo.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.springleaf.thinkdo.constant.ChatConstant.CONVERSATION_TITLE_PROMPT_PATH;

/**
 * 消息Service实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, MessageEntity> implements MessageService {

    private final MessageMapper messageMapper;
    private final ConversationMapper conversationMapper;
    private final LLMService llmService;
    private final PromptTemplateLoader promptTemplateLoader;
    private final MemoryProperties memoryProperties;
    @Lazy
    private final ConversationSummaryService summaryService;

    @Override
    public List<MessageInfoResp> getMessagesByConversationId(String conversationId) {
        // 验证会话是否存在
        ConversationEntity conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException("会话不存在");
        }

        LambdaQueryWrapper<MessageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageEntity::getConversationId, conversationId)
                .orderByAsc(MessageEntity::getCreatedAt);

        List<MessageEntity> messageList = messageMapper.selectList(wrapper);

        return messageList.stream()
                .map(this::convertToResp)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long append(String conversationId, Long userId, ChatMessage message) {
        if (StrUtil.isBlank(conversationId) || userId == null) {
            return null;
        }

        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setConversationId(conversationId);
        messageEntity.setUserId(userId);
        messageEntity.setContent(message.getContent());
        messageEntity.setRole(message.getRole().name().toLowerCase());
        messageMapper.insert(messageEntity);

        Long messageId = messageEntity.getId();
        log.info("追加消息成功, conversationId={}, messageId={}", conversationId, messageId);

        // 更新会话
        if (message.getRole() == ChatMessage.Role.USER) {
            ConversationEntity existing = conversationMapper.selectOne(
                    Wrappers.lambdaQuery(ConversationEntity.class)
                            .eq(ConversationEntity::getConversationId, conversationId)
                            .eq(ConversationEntity::getUserId, userId)
                            .eq(ConversationEntity::getDeleted, 0)
            );

            if (existing == null) {
                ConversationEntity record = new ConversationEntity();
                record.setConversationId(conversationId);
                record.setUserId(userId);
                record.setTitle("新对话");
                record.setLastTime(LocalDateTime.now());
                conversationMapper.insert(record);

                // 异步生成会话标题，避免阻塞主流程
                CompletableFuture.runAsync(() -> {
                    try {
                        String title = generateTitleFromQuestion(message.getContent());
                        ConversationEntity update = new ConversationEntity();
                        update.setConversationId(conversationId);
                        update.setTitle(title);
                        conversationMapper.updateById(update);
                        log.info("异步生成会话标题成功, conversationId={}", conversationId);
                    } catch (Exception ex) {
                        log.warn("异步生成会话标题失败, conversationId={}", conversationId, ex);
                    }
                });
            }else {
                existing.setLastTime(LocalDateTime.now());
                conversationMapper.updateById(existing);
            }
        }
        // 判断是否需要生成摘要
        summaryService.compressIfNeeded(conversationId, userId, message);
        return messageId;
    }

    @Override
    public List<ChatMessage> load(String conversationId, Long userId) {
        // 参数校验
        if (StrUtil.isBlank(conversationId) || userId == null) {
            return List.of();
        }

        // 验证会话是否存在且属于当前用户
        ConversationEntity conversation = conversationMapper.selectOne(
                Wrappers.lambdaQuery(ConversationEntity.class)
                        .eq(ConversationEntity::getConversationId, conversationId)
                        .eq(ConversationEntity::getUserId, userId)
                        .eq(ConversationEntity::getDeleted, 0)
        );
        if (conversation == null) {
            return List.of();
        }

        long startTime = System.currentTimeMillis();
        try {
            // 并行加载摘要和历史记录
            CompletableFuture<ChatMessage> summaryFuture = CompletableFuture.supplyAsync(
                    () -> loadSummaryWithFallback(conversationId, userId)
            );
            CompletableFuture<List<ChatMessage>> historyFuture = CompletableFuture.supplyAsync(
                    () -> loadHistoryWithFallback(conversationId, userId)
            );

            // 等待所有任务完成后合并结果
            return CompletableFuture.allOf(summaryFuture, historyFuture)
                    .thenApply(v -> {
                        ChatMessage summary = summaryFuture.join();
                        List<ChatMessage> history = historyFuture.join();
                        log.debug("加载对话记忆 - conversationId: {}, userId: {}, 摘要: {}, 历史消息数: {}, 耗时: {}ms",
                                conversationId, userId, summary != null, history.size(), System.currentTimeMillis() - startTime);
                        return attachSummary(summary, history);
                    })
                    .join();
        } catch (Exception e) {
            log.error("加载对话记忆失败 - conversationId: {}, userId: {}", conversationId, userId, e);
            return List.of();
        }
    }

    private List<ChatMessage> attachSummary(ChatMessage summary, List<ChatMessage> messages) {
        // 确保返回值不为 null
        if (CollUtil.isEmpty(messages)) {
            return List.of();
        }
        if (summary == null) {
            return messages;
        }
        List<ChatMessage> result = new ArrayList<>();
        result.add(summaryService.decorateIfNeeded(summary));
        result.addAll(messages);
        return result;
    }

    private List<ChatMessage> loadHistoryWithFallback(String conversationId, Long userId) {
        // 查询该会话的消息记录
        int maxMessages = memoryProperties.getHistoryKeepTurns() * 2;
        LambdaQueryWrapper<MessageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageEntity::getConversationId, conversationId)
                .orderByDesc(MessageEntity::getCreatedAt)
                .last("LIMIT " + maxMessages);

        List<MessageEntity> messageList = messageMapper.selectList(wrapper);

        // 转换为 ChatMessage 列表并按时间升序排列
        return messageList.stream()
                .sorted(Comparator.comparing(MessageEntity::getCreatedAt))
                .map(messageEntity -> {
                    ChatMessage.Role role = ChatMessage.Role.fromString(messageEntity.getRole());
                    return new ChatMessage(role, messageEntity.getContent());
                })
                .collect(Collectors.toList());
    }

    private ChatMessage loadSummaryWithFallback(String conversationId, Long userId) {
        try {
            return summaryService.loadLatestSummary(conversationId, userId);
        } catch (Exception e) {
            log.warn("加载摘要失败，将跳过摘要 - conversationId: {}, userId: {}", conversationId, userId, e);
            return null;
        }
    }

    @Override
    public long countUserMessages(String conversationId, Long userId) {
        if (StrUtil.isBlank(conversationId) || userId == null) {
            return 0;
        }
        return messageMapper.selectCount(
                Wrappers.lambdaQuery(MessageEntity.class)
                        .eq(MessageEntity::getConversationId, conversationId)
                        .eq(MessageEntity::getUserId, userId)
                        .eq(MessageEntity::getRole, "user")
                        .eq(MessageEntity::getDeleted, 0)
        );
    }

    @Override
    public List<MessageEntity> listLatestUserOnlyMessages(String conversationId, Long userId, int limit) {
        if (StrUtil.isBlank(conversationId) || userId == null || limit <= 0) {
            return List.of();
        }
        return messageMapper.selectList(
                Wrappers.lambdaQuery(MessageEntity.class)
                        .eq(MessageEntity::getConversationId, conversationId)
                        .eq(MessageEntity::getUserId, userId)
                        .eq(MessageEntity::getRole, "user")
                        .eq(MessageEntity::getDeleted, 0)
                        .orderByDesc(MessageEntity::getCreatedAt)
                        .last("limit " + limit)
        );
    }

    @Override
    public Long findMaxMessageIdAtOrBefore(String conversationId, Long userId, LocalDateTime at) {
        if (StrUtil.isBlank(conversationId) || userId == null || at == null) {
            return null;
        }
        MessageEntity record = messageMapper.selectOne(
                Wrappers.lambdaQuery(MessageEntity.class)
                        .eq(MessageEntity::getConversationId, conversationId)
                        .eq(MessageEntity::getUserId, userId)
                        .eq(MessageEntity::getDeleted, 0)
                        .le(MessageEntity::getCreatedAt, at)
                        .orderByDesc(MessageEntity::getId)
                        .last("limit 1")
        );
        return record == null ? null : record.getId();
    }

    @Override
    public List<MessageEntity> listMessagesBetweenIds(String conversationId, Long userId, Long afterId, Long beforeId) {
        if (StrUtil.isBlank(conversationId) || userId == null) {
            return List.of();
        }
        var query = Wrappers.lambdaQuery(MessageEntity.class)
                .eq(MessageEntity::getConversationId, conversationId)
                .eq(MessageEntity::getUserId, userId)
                .in(MessageEntity::getRole, "user", "assistant")
                .eq(MessageEntity::getDeleted, 0);
        if (afterId != null) {
            query.gt(MessageEntity::getId, afterId);
        }
        if (beforeId != null) {
            query.lt(MessageEntity::getId, beforeId);
        }
        return messageMapper.selectList(
                query.orderByAsc(MessageEntity::getId)
        );
    }

    /**
     * 转换为消息信息响应对象
     */
    private MessageInfoResp convertToResp(MessageEntity message) {
        MessageInfoResp resp = new MessageInfoResp();
        BeanUtils.copyProperties(message, resp);
        return resp;
    }

    /**
     * 根据用户问题生成会话标题
     */
    private String generateTitleFromQuestion(String question) {
        String prompt = promptTemplateLoader.render(
                CONVERSATION_TITLE_PROMPT_PATH,
                Map.of(
                        "question", question
                )
        );
        try {
            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(ChatMessage.user(prompt)))
                    .temperature(0.7D)
                    .topP(0.3D)
                    .thinking(false)
                    .build();

            return llmService.chat(request);
        } catch (Exception ex) {
            log.warn("生成会话标题失败", ex);
            return "新对话";
        }
    }
}
