package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.thinkdo.common.PageResp;
import com.springleaf.thinkdo.domain.response.ConversationInfoResp;
import com.springleaf.thinkdo.domain.request.AdminConversationQueryReq;
import com.springleaf.thinkdo.domain.request.CreateConversationReq;
import com.springleaf.thinkdo.domain.request.UpdateConversationReq;
import com.springleaf.thinkdo.domain.response.AdminConversationDetailResp;
import com.springleaf.thinkdo.domain.response.AdminConversationInfoResp;
import com.springleaf.thinkdo.domain.entity.ConversationEntity;
import com.springleaf.thinkdo.domain.entity.ConversationSummaryEntity;
import com.springleaf.thinkdo.domain.entity.MessageEntity;
import com.springleaf.thinkdo.domain.entity.UserEntity;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.ConversationMapper;
import com.springleaf.thinkdo.mapper.ConversationSummaryMapper;
import com.springleaf.thinkdo.mapper.MessageMapper;
import com.springleaf.thinkdo.mapper.UserMapper;
import com.springleaf.thinkdo.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 会话Service实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, ConversationEntity> implements ConversationService {

    private final ConversationMapper conversationMapper;
    private final ConversationSummaryMapper conversationSummaryMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createConversation(CreateConversationReq createConversationReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        ConversationEntity conversation = new ConversationEntity();
        conversation.setUserId(userId);
        conversation.setTitle(createConversationReq.getTitle());

        conversationMapper.insert(conversation);
        log.info("创建会话成功, userId={}, conversationId={}", userId, conversation.getConversationId());

        return conversation.getConversationId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConversation(UpdateConversationReq updateConversationReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        ConversationEntity conversation = conversationMapper.selectById(updateConversationReq.getConversationId());
        if (conversation == null) {
            throw new BusinessException("会话不存在");
        }

        // 验证是否为当前用户的会话
        if (!conversation.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此会话");
        }

        conversation.setTitle(updateConversationReq.getTitle());
        conversationMapper.updateById(conversation);
        log.info("更新会话成功, userId={}, conversationId={}", userId, conversation.getConversationId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(String conversationId) {
        Long userId = StpUtil.getLoginIdAsLong();

        ConversationEntity conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException("会话不存在");
        }

        // 验证是否为当前用户的会话
        if (!conversation.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此会话");
        }

        // 逻辑删除会话（会自动级联删除相关消息和摘要）
        conversationMapper.deleteById(conversationId);
        log.info("删除会话成功, userId={}, conversationId={}", userId, conversationId);
    }

    @Override
    public ConversationInfoResp getConversationById(String conversationId) {
        Long userId = StpUtil.getLoginIdAsLong();

        ConversationEntity conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException("会话不存在");
        }

        // 验证是否为当前用户的会话
        if (!conversation.getUserId().equals(userId)) {
            throw new BusinessException("无权查看此会话");
        }

        return convertToResp(conversation);
    }

    @Override
    public List<ConversationInfoResp> getConversationList() {
        Long userId = StpUtil.getLoginIdAsLong();

        LambdaQueryWrapper<ConversationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationEntity::getUserId, userId)
                .orderByDesc(ConversationEntity::getUpdatedAt);

        List<ConversationEntity> conversationList = conversationMapper.selectList(wrapper);

        // 获取所有会话ID，查询对应的摘要
        List<String> conversationIds = conversationList.stream()
                .map(ConversationEntity::getConversationId)
                .collect(Collectors.toList());

        Map<String, String> summaryMap = Map.of();
        if (!conversationIds.isEmpty()) {
            LambdaQueryWrapper<ConversationSummaryEntity> summaryWrapper = new LambdaQueryWrapper<>();
            summaryWrapper.in(ConversationSummaryEntity::getConversationId, conversationIds)
                    .select(ConversationSummaryEntity::getConversationId, ConversationSummaryEntity::getContent);
            List<ConversationSummaryEntity> summaries = conversationSummaryMapper.selectList(summaryWrapper);
            // 处理可能存在的重复 conversationId，保留最后一条记录
            summaryMap = summaries.stream()
                    .collect(Collectors.toMap(
                            ConversationSummaryEntity::getConversationId,
                            ConversationSummaryEntity::getContent,
                            (existing, replacement) -> replacement
                    ));
        }

        Map<String, String> finalSummaryMap = summaryMap;

        return conversationList.stream()
                .map(conversation -> {
                    ConversationInfoResp resp = convertToResp(conversation);
                    resp.setSummary(finalSummaryMap.get(conversation.getConversationId()));
                    return resp;
                })
                .collect(Collectors.toList());
    }

    @Override
    public ConversationEntity findConversation(String conversationId, Long userId) {
        if (StrUtil.isBlank(conversationId) || userId == null) {
            return null;
        }
        return conversationMapper.selectOne(
                Wrappers.lambdaQuery(ConversationEntity.class)
                        .eq(ConversationEntity::getConversationId, conversationId)
                        .eq(ConversationEntity::getUserId, userId)
                        .eq(ConversationEntity::getDeleted, 0)
        );
    }

    @Override
    public Long countTotal() {
        return conversationMapper.selectCount(
                new LambdaQueryWrapper<ConversationEntity>().eq(ConversationEntity::getDeleted, 0)
        );
    }

    @Override
    public Long countByDate(java.time.LocalDate date) {
        return conversationMapper.selectCount(
                new LambdaQueryWrapper<ConversationEntity>()
                        .eq(ConversationEntity::getDeleted, 0)
                        .ge(ConversationEntity::getCreatedAt, date.atStartOfDay())
                        .lt(ConversationEntity::getCreatedAt, date.plusDays(1).atStartOfDay())
        );
    }

    /**
     * 转换为会话信息响应对象
     */
    private ConversationInfoResp convertToResp(ConversationEntity conversation) {
        ConversationInfoResp resp = new ConversationInfoResp();
        BeanUtils.copyProperties(conversation, resp);
        return resp;
    }

    // ==================== 管理员接口实现 ====================

    @Override
    public PageResp<AdminConversationInfoResp> adminListConversations(AdminConversationQueryReq queryReq) {
        LambdaQueryWrapper<ConversationEntity> wrapper = new LambdaQueryWrapper<>();

        // 用户ID筛选
        if (queryReq.getUserId() != null) {
            wrapper.eq(ConversationEntity::getUserId, queryReq.getUserId());
        }

        // 用户名模糊搜索
        if (StringUtils.hasText(queryReq.getUsername())) {
            List<Long> matchedUserIds = userMapper.selectList(
                    new LambdaQueryWrapper<UserEntity>().like(UserEntity::getUsername, queryReq.getUsername())
            ).stream().map(UserEntity::getId).collect(Collectors.toList());
            if (matchedUserIds.isEmpty()) {
                return PageResp.of(List.of(), 0L, queryReq.getPageNum(), queryReq.getPageSize());
            }
            wrapper.in(ConversationEntity::getUserId, matchedUserIds);
        }

        // 关键词搜索（标题模糊匹配）
        if (StringUtils.hasText(queryReq.getKeyword())) {
            wrapper.like(ConversationEntity::getTitle, queryReq.getKeyword());
        }

        // 创建时间范围
        if (queryReq.getStartTime() != null) {
            wrapper.ge(ConversationEntity::getCreatedAt, queryReq.getStartTime());
        }
        if (queryReq.getEndTime() != null) {
            wrapper.le(ConversationEntity::getCreatedAt, queryReq.getEndTime());
        }

        wrapper.orderByDesc(ConversationEntity::getLastTime);

        IPage<ConversationEntity> page = conversationMapper.selectPage(
                new Page<>(queryReq.getPageNum(), queryReq.getPageSize()), wrapper
        );

        // 批量获取用户名
        Set<Long> userIds = page.getRecords().stream()
                .map(ConversationEntity::getUserId).collect(Collectors.toSet());
        Map<Long, String> usernameMap = batchGetUsernames(userIds);

        // 批量获取每个会话的消息数量
        Set<String> conversationIds = page.getRecords().stream()
                .map(ConversationEntity::getConversationId).collect(Collectors.toSet());
        Map<String, Long> messageCountMap = batchGetMessageCounts(conversationIds);

        return PageResp.of(page, entity -> {
            AdminConversationInfoResp resp = new AdminConversationInfoResp();
            BeanUtils.copyProperties(entity, resp);
            resp.setUsername(usernameMap.getOrDefault(entity.getUserId(), ""));
            resp.setMessageCount(messageCountMap.getOrDefault(entity.getConversationId(), 0L));
            return resp;
        });
    }

    @Override
    public AdminConversationDetailResp adminGetConversationDetail(String conversationId) {
        ConversationEntity conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException("会话不存在");
        }

        UserEntity user = userMapper.selectById(conversation.getUserId());

        // 查询摘要
        String summary = null;
        ConversationSummaryEntity summaryEntity = conversationSummaryMapper.selectOne(
                new LambdaQueryWrapper<ConversationSummaryEntity>()
                        .eq(ConversationSummaryEntity::getConversationId, conversationId)
                        .orderByDesc(ConversationSummaryEntity::getId)
                        .last("LIMIT 1")
        );
        if (summaryEntity != null) {
            summary = summaryEntity.getContent();
        }

        // 查询消息列表
        List<MessageEntity> messages = messageMapper.selectList(
                new LambdaQueryWrapper<MessageEntity>()
                        .eq(MessageEntity::getConversationId, conversationId)
                        .orderByAsc(MessageEntity::getCreatedAt)
        );

        AdminConversationDetailResp resp = new AdminConversationDetailResp();
        resp.setConversationId(conversation.getConversationId());
        resp.setUserId(conversation.getUserId());
        resp.setUsername(user != null ? user.getUsername() : "");
        resp.setTitle(conversation.getTitle());
        resp.setSummary(summary);
        resp.setLastTime(conversation.getLastTime());
        resp.setCreatedAt(conversation.getCreatedAt());
        resp.setMessages(messages.stream().map(msg -> {
            AdminConversationDetailResp.MessageItem item = new AdminConversationDetailResp.MessageItem();
            item.setId(msg.getId());
            item.setRole(msg.getRole());
            item.setContent(msg.getContent());
            item.setCreatedAt(msg.getCreatedAt());
            return item;
        }).collect(Collectors.toList()));

        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteConversation(String conversationId) {
        ConversationEntity conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException("会话不存在");
        }

        // 删除消息
        messageMapper.delete(
                new LambdaQueryWrapper<MessageEntity>()
                        .eq(MessageEntity::getConversationId, conversationId)
        );

        // 删除摘要
        conversationSummaryMapper.delete(
                new LambdaQueryWrapper<ConversationSummaryEntity>()
                        .eq(ConversationSummaryEntity::getConversationId, conversationId)
        );

        // 删除会话
        conversationMapper.deleteById(conversationId);
        log.info("管理员删除会话成功, conversationId={}", conversationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminBatchDeleteConversations(List<String> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            throw new BusinessException("请选择要删除的会话");
        }

        for (String conversationId : conversationIds) {
            adminDeleteConversation(conversationId);
        }
        log.info("管理员批量删除会话成功, count={}", conversationIds.size());
    }

    private Map<Long, String> batchGetUsernames(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        List<UserEntity> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getUsername));
    }

    private Map<String, Long> batchGetMessageCounts(Set<String> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) return Map.of();
        // 按会话ID分组统计消息数量
        List<MessageEntity> messages = messageMapper.selectList(
                new LambdaQueryWrapper<MessageEntity>()
                        .in(MessageEntity::getConversationId, conversationIds)
                        .select(MessageEntity::getConversationId)
        );
        return messages.stream().collect(Collectors.groupingBy(MessageEntity::getConversationId, Collectors.counting()));
    }
}
