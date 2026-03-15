package com.springleaf.thinkdo.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.thinkdo.chat.ChatMessage;
import com.springleaf.thinkdo.chat.ChatRequest;
import com.springleaf.thinkdo.chat.LLMService;
import com.springleaf.thinkdo.domain.entity.ConversationEntity;
import com.springleaf.thinkdo.domain.entity.MessageEntity;
import com.springleaf.thinkdo.domain.response.MessageInfoResp;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.ConversationMapper;
import com.springleaf.thinkdo.mapper.MessageMapper;
import com.springleaf.thinkdo.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
                String title = generateTitleFromQuestion(message.getContent());
                ConversationEntity record = new ConversationEntity();
                record.setConversationId(conversationId);
                record.setUserId(userId);
                record.setTitle(title);
                record.setLastTime(LocalDateTime.now());
                conversationMapper.insert(record);
            }else {
                existing.setLastTime(LocalDateTime.now());
                conversationMapper.updateById(existing);
            }
        }
        // TODO: 生成摘要

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

        // 查询该会话的所有消息记录
        LambdaQueryWrapper<MessageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageEntity::getConversationId, conversationId)
                .orderByAsc(MessageEntity::getCreatedAt);

        List<MessageEntity> messageList = messageMapper.selectList(wrapper);

        // 转换为 ChatMessage 列表
        return messageList.stream()
                .map(messageEntity -> {
                    ChatMessage.Role role = ChatMessage.Role.fromString(messageEntity.getRole());
                    return new ChatMessage(role, messageEntity.getContent());
                })
                .collect(Collectors.toList());
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
        // TODO: 提示词待优化
        String prompt = "根据用户提问生成一个简短的标题，用户提问如下：" + question;
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
