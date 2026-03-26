package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.springleaf.thinkdo.constant.KnowledgeBaseConstant;
import com.springleaf.thinkdo.context.UserContext;
import com.springleaf.thinkdo.domain.entity.IntentNodeEntity;
import com.springleaf.thinkdo.domain.entity.KnowledgeBaseEntity;
import com.springleaf.thinkdo.domain.entity.KnowledgeDocumentEntity;
import com.springleaf.thinkdo.domain.request.KnowledgeBaseCreateReq;
import com.springleaf.thinkdo.domain.request.KnowledgeBasePageReq;
import com.springleaf.thinkdo.domain.request.KnowledgeBaseUpdateReq;
import com.springleaf.thinkdo.domain.response.KnowledgeBaseResp;
import com.springleaf.thinkdo.enums.IntentKind;
import com.springleaf.thinkdo.enums.IntentLevel;
import com.springleaf.thinkdo.enums.KnowledgeScopeEnum;
import com.springleaf.thinkdo.enums.UserRoleEnum;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.IntentNodeMapper;
import com.springleaf.thinkdo.mapper.KnowledgeBaseMapper;
import com.springleaf.thinkdo.mapper.KnowledgeDocumentMapper;
import com.springleaf.thinkdo.intent.IntentResolver;
import com.springleaf.thinkdo.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 知识库服务实现
 * 支持基于角色的权限管理：
 * - 管理员(ADMIN)：可以创建、查看、修改、删除所有知识库（系统+用户），创建时默认为SYSTEM知识库
 * - 普通用户(USER)：只能创建、查看、修改、删除自己的用户知识库，创建时为USER知识库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final IntentNodeMapper intentNodeMapper;
    private final IntentResolver intentResolver;

    @Transactional
    @Override
    public String create(KnowledgeBaseCreateReq requestParam) {
        // 获取当前用户信息
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("用户ID为空");
        }
        UserRoleEnum userRole = UserContext.getCurrentUserRole();

        // 根据用户角色确定知识库作用域
        // 管理员创建系统知识库，普通用户创建用户知识库
        KnowledgeScopeEnum scope = (userRole == UserRoleEnum.ADMIN)
                ? KnowledgeScopeEnum.SYSTEM
                : KnowledgeScopeEnum.USER;

        // 名称重复校验（在同一作用域下）
        String name = requestParam.getName().replaceAll("\\s+", "");
        Long count = knowledgeBaseMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeBaseEntity>()
                        .eq(KnowledgeBaseEntity::getName, name)
                        .eq(KnowledgeBaseEntity::getScope, scope)
                        .eq(KnowledgeBaseEntity::getCreatedBy, currentUserId)
        );
        if (count > 0) {
            throw new BusinessException("知识库名称已存在：" + requestParam.getName());
        }

        // 确定使用的固定 collection 名称
        String collectionName = (scope == KnowledgeScopeEnum.SYSTEM)
                ? KnowledgeBaseConstant.SYSTEM_COLLECTION
                : KnowledgeBaseConstant.USER_COLLECTION;

        // 创建知识库
        KnowledgeBaseEntity kb = KnowledgeBaseEntity.builder()
                .name(requestParam.getName())
                .description(requestParam.getDescription())
                .scope(scope)
                .embeddingModel(requestParam.getEmbeddingModel())
                .collectionName(collectionName)
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .build();
        knowledgeBaseMapper.insert(kb);

        // 确保用户意图树存在（domain和category级别节点）
        intentResolver.ensureUserIntentTreeExists(currentUserId);
        // 创建该用户知识库的意图节点
        String intentCode = "user_" + currentUserId + "_kb_" + kb.getId();
        IntentNodeEntity intentNode = IntentNodeEntity.builder()
                .kbId(kb.getId())
                .intentCode(intentCode)
                .scope(scope.getValue())
                .name(kb.getName())
                .level(IntentLevel.TOPIC.getCode())
                .parentCode("category_user_kb_" + currentUserId)
                .description(kb.getDescription())
                .examples(null)
                .collectionName(kb.getCollectionName())
                .kind(IntentKind.KB.getCode())
                .topK(5)
                .promptSnippet("请基于用户知识库 [" + kb.getName() + "] 中的私有内容回答，这些是用户上传的个人文档。")
                .enabled(1)
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .build();
        intentNodeMapper.insert(intentNode);

        log.info("用户 {} (角色:{}) 成功创建 {} 知识库，名称: {}", currentUserId, userRole, scope, requestParam.getName());
        return String.valueOf(kb.getId());
    }

    @Override
    public void update(KnowledgeBaseUpdateReq requestParam) {
        // 获取当前用户信息
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("用户ID为空");
        }

        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(requestParam.getId());
        if (kb == null || kb.getDeleted() != null && kb.getDeleted() == 1) {
            throw new IllegalArgumentException("知识库不存在：" + requestParam.getId());
        }

        // 权限校验
        checkUpdatePermission(kb, currentUserId);

        if (StringUtils.hasText(requestParam.getEmbeddingModel())
                && !requestParam.getEmbeddingModel().equals(kb.getEmbeddingModel())) {

            Long docCount = knowledgeDocumentMapper.selectCount(
                    new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                            .eq(KnowledgeDocumentEntity::getKbId, requestParam.getId())
                            .gt(KnowledgeDocumentEntity::getChunkCount, 0)
                            .eq(KnowledgeDocumentEntity::getDeleted, 0)
            );
            if (docCount > 0) {
                throw new IllegalStateException("知识库已存在向量化文档，不允许修改嵌入模型");
            }

            kb.setEmbeddingModel(requestParam.getEmbeddingModel());
        }

        if (StringUtils.hasText(requestParam.getName())) {
            kb.setName(requestParam.getName());
        }

        if (requestParam.getDescription() != null) {
            kb.setDescription(requestParam.getDescription());
        }

        kb.setUpdatedBy(currentUserId);
        knowledgeBaseMapper.updateById(kb);

        log.info("用户 {} 成功更新知识库 {}", currentUserId, requestParam.getId());
    }

    @Override
    public void rename(String kbId, KnowledgeBaseUpdateReq requestParam) {
        // 获取当前用户信息
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("用户ID为空");
        }

        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || kb.getDeleted() != null && kb.getDeleted() == 1) {
            throw new BusinessException("知识库不存在");
        }

        // 权限校验
        checkUpdatePermission(kb, currentUserId);

        if (!StringUtils.hasText(requestParam.getName())) {
            throw new BusinessException("知识库名称不能为空");
        }

        // 名称重复校验（在同一作用域下，排除当前知识库）
        String name = requestParam.getName().replaceAll("\\s+", "");
        Long count = knowledgeBaseMapper.selectCount(
                Wrappers.lambdaQuery(KnowledgeBaseEntity.class)
                        .eq(KnowledgeBaseEntity::getName, name)
                        .eq(KnowledgeBaseEntity::getScope, kb.getScope())
                        .eq(KnowledgeBaseEntity::getCreatedBy, currentUserId)
                        .ne(KnowledgeBaseEntity::getId, kbId)
        );
        if (count > 0) {
            throw new BusinessException("知识库名称已存在：" + requestParam.getName());
        }

        kb.setName(requestParam.getName());
        kb.setUpdatedBy(currentUserId);
        knowledgeBaseMapper.updateById(kb);

        log.info("用户 {} 成功重命名知识库 {}, 新名称: {}", currentUserId, kbId, requestParam.getName());
    }

    @Override
    public void delete(String kbId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || kb.getDeleted() != null && kb.getDeleted() == 1) {
            throw new BusinessException("知识库不存在");
        }

        // 权限校验
        checkUpdatePermission(kb, currentUserId);

        // 限制删除前需要确保没有文档
        Long docCount = knowledgeDocumentMapper.selectCount(
                Wrappers.lambdaQuery(KnowledgeDocumentEntity.class)
                        .eq(KnowledgeDocumentEntity::getKbId, kbId)
        );
        if (docCount > 0) {
            throw new BusinessException("知识库下仍有关联文档，无法删除");
        }

        knowledgeBaseMapper.deleteById(kbId);
        log.info("用户 {} 成功删除知识库 {}", currentUserId, kbId);
    }

    @Override
    public KnowledgeBaseResp queryById(String kbId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = isAdmin();

        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || kb.getDeleted() != null && kb.getDeleted() == 1) {
            throw new BusinessException("知识库不存在");
        }

        // 权限校验：普通用户只能查看自己的用户知识库
        if (!isAdmin && kb.getScope() == KnowledgeScopeEnum.USER) {
            if (!kb.getCreatedBy().equals(currentUserId)) {
                throw new BusinessException("无权访问该知识库");
            }
        } else if (!isAdmin && kb.getScope() == KnowledgeScopeEnum.SYSTEM) {
            throw new BusinessException("无权访问系统知识库");
        }

        return BeanUtil.toBean(kb, KnowledgeBaseResp.class);
    }

    @Override
    public IPage<KnowledgeBaseResp> pageQuery(KnowledgeBasePageReq requestParam) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = isAdmin();

        LambdaQueryWrapper<KnowledgeBaseEntity> queryWrapper = Wrappers.lambdaQuery(KnowledgeBaseEntity.class)
                .like(StringUtils.hasText(requestParam.getName()), KnowledgeBaseEntity::getName, requestParam.getName())
                .eq(KnowledgeBaseEntity::getDeleted, 0);

        // 权限过滤
        if (!isAdmin) {
            // 普通用户只能查看自己的用户知识库
            queryWrapper.eq(KnowledgeBaseEntity::getScope, KnowledgeScopeEnum.USER)
                    .eq(KnowledgeBaseEntity::getCreatedBy, currentUserId);
        } else if (requestParam.getScope() != null) {
            // 管理员可以根据scope筛选
            queryWrapper.eq(KnowledgeBaseEntity::getScope, requestParam.getScope());
        }

        queryWrapper.orderByDesc(KnowledgeBaseEntity::getUpdatedAt);

        Page<KnowledgeBaseEntity> page = new Page<>(requestParam.getCurrent(), requestParam.getSize());
        IPage<KnowledgeBaseEntity> result = knowledgeBaseMapper.selectPage(page, queryWrapper);
        Map<Long, Long> docCountMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(result.getRecords())) {
            List<Long> kbIds = result.getRecords().stream()
                    .map(KnowledgeBaseEntity::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!kbIds.isEmpty()) {
                List<Map<String, Object>> rows = knowledgeDocumentMapper.selectMaps(
                        Wrappers.query(KnowledgeDocumentEntity.class)
                                .select("kb_id AS kbId", "COUNT(1) AS docCount")
                                .in("kb_id", kbIds)
                                .eq("deleted", 0)
                                .groupBy("kb_id")
                );
                for (Map<String, Object> row : rows) {
                    Object kbIdValue = row.get("kbId");
                    Object countValue = row.get("docCount");
                    if (kbIdValue == null) {
                        continue;
                    }
                    Long kbId = kbIdValue instanceof Number
                            ? ((Number) kbIdValue).longValue()
                            : Long.parseLong(kbIdValue.toString());
                    Long count = countValue instanceof Number
                            ? ((Number) countValue).longValue()
                            : countValue != null ? Long.parseLong(countValue.toString()) : 0L;
                    docCountMap.put(kbId, count);
                }
            }
        }
        return result.convert(each -> {
            KnowledgeBaseResp resp = BeanUtil.toBean(each, KnowledgeBaseResp.class);
            Long docCount = docCountMap.get(each.getId());
            resp.setDocumentCount(docCount != null ? docCount : 0L);
            return resp;
        });
    }

    /**
     * 检查用户是否有更新权限
     * - 管理员可以更新所有知识库
     * - 普通用户只能更新自己创建的用户知识库
     *
     * @param kb           知识库实体
     * @param currentUserId 当前用户ID
     */
    private void checkUpdatePermission(KnowledgeBaseEntity kb, Long currentUserId) {
        boolean isAdmin = isAdmin();

        if (!isAdmin) {
            // 普通用户只能更新自己创建的用户知识库
            if (kb.getScope() == KnowledgeScopeEnum.SYSTEM) {
                throw new BusinessException("无权操作系统知识库");
            }
            if (!kb.getCreatedBy().equals(currentUserId)) {
                throw new BusinessException("无权操作其他用户的知识库");
            }
        }
    }

    /**
     * 判断当前用户是否为管理员
     * 从用户上下文中获取当前用户的角色信息
     *
     * @return 如果是管理员返回true，否则返回false
     */
    private boolean isAdmin() {
        UserRoleEnum role = UserContext.getCurrentUserRole();
        // 如果用户上下文中没有角色信息，默认返回false（普通用户）
        return role == UserRoleEnum.ADMIN;
    }
}
