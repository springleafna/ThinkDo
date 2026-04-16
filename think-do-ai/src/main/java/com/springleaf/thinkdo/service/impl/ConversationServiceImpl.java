package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.thinkdo.domain.response.ConversationInfoResp;
import com.springleaf.thinkdo.domain.request.CreateConversationReq;
import com.springleaf.thinkdo.domain.request.UpdateConversationReq;
import com.springleaf.thinkdo.domain.entity.ConversationEntity;
import com.springleaf.thinkdo.domain.entity.ConversationSummaryEntity;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.ConversationMapper;
import com.springleaf.thinkdo.mapper.ConversationSummaryMapper;
import com.springleaf.thinkdo.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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
}
