package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.springleaf.thinkdo.domain.entity.IntentNodeEntity;
import com.springleaf.thinkdo.domain.request.IntentNodeCreateReq;
import com.springleaf.thinkdo.domain.request.IntentNodePageReq;
import com.springleaf.thinkdo.domain.request.IntentNodeUpdateReq;
import com.springleaf.thinkdo.domain.response.IntentNodeTreeResp;
import com.springleaf.thinkdo.enums.IntentKind;
import com.springleaf.thinkdo.enums.IntentLevel;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.IntentNodeMapper;
import com.springleaf.thinkdo.service.IntentNodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentNodeServiceImpl implements IntentNodeService {

    private final IntentNodeMapper intentNodeMapper;

    @Override
    public List<IntentNodeTreeResp> getFullTree() {
        List<IntentNodeEntity> allNodes = intentNodeMapper.selectList(
                new LambdaQueryWrapper<IntentNodeEntity>()
                        .eq(IntentNodeEntity::getEnabled, 1)
                        .orderByAsc(IntentNodeEntity::getSortOrder)
        );
        return buildTree(allNodes);
    }

    @Override
    public void create(IntentNodeCreateReq requestParam) {
        int level = requestParam.getLevel();
        String parentCode = requestParam.getParentCode();

        if (level == IntentLevel.DOMAIN.getCode()) {
            // DOMAIN 节点不能有父节点
            if (parentCode != null && !parentCode.isEmpty()) {
                throw new BusinessException("DOMAIN 根节点不能设置父节点");
            }
        } else {
            // CATEGORY 和 TOPIC 必须指定父节点
            if (parentCode == null || parentCode.isEmpty()) {
                throw new BusinessException(IntentLevel.fromCode(level) + " 节点必须指定父节点");
            }
            // 校验父节点存在
            IntentNodeEntity parentNode = intentNodeMapper.selectOne(
                    new LambdaQueryWrapper<IntentNodeEntity>().eq(IntentNodeEntity::getIntentCode, parentCode)
            );
            if (parentNode == null) {
                throw new BusinessException("父节点不存在: " + parentCode);
            }
            // CATEGORY 的父节点必须是 DOMAIN，TOPIC 的父节点必须是 CATEGORY
            int expectedParentLevel = level - 1;
            if (parentNode.getLevel() != expectedParentLevel) {
                throw new BusinessException(
                        IntentLevel.fromCode(level) + " 节点的父节点必须是 " + IntentLevel.fromCode(expectedParentLevel));
            }
        }

        if (requestParam.getKind() == IntentKind.MCP.getCode()
                && requestParam.getLevel() == IntentLevel.TOPIC.getCode()
                && (requestParam.getMcpToolId() == null || requestParam.getMcpToolId().isEmpty())) {
            throw new BusinessException("MCP 类型且 TOPIC 层级的节点必须填写 mcpToolId");
        }

        IntentNodeEntity entity = IntentNodeEntity.builder()
                .kbId(requestParam.getKbId())
                .intentCode(requestParam.getIntentCode())
                .scope(requestParam.getScope())
                .name(requestParam.getName())
                .level(requestParam.getLevel())
                .parentCode(requestParam.getParentCode())
                .description(requestParam.getDescription())
                .examples(requestParam.getExamples() == null ? null : new Gson().toJson(requestParam.getExamples()))
                .collectionName(requestParam.getCollectionName())
                .mcpToolId(requestParam.getMcpToolId())
                .topK(requestParam.getTopK())
                .kind(requestParam.getKind())
                .sortOrder(requestParam.getSortOrder())
                .promptSnippet(requestParam.getPromptSnippet())
                .promptTemplate(requestParam.getPromptTemplate())
                .paramPromptTemplate(requestParam.getParamPromptTemplate())
                .enabled(1)
                .createdBy(StpUtil.getLoginIdAsLong())
                .deleted(0)
                .build();
        intentNodeMapper.insert(entity);
    }

    @Override
    public void update(IntentNodeUpdateReq requestParam) {
        IntentNodeEntity entity = intentNodeMapper.selectById(requestParam.getId());
        if (entity == null) {
            throw new BusinessException("节点不存在: " + requestParam.getId());
        }

        LambdaUpdateWrapper<IntentNodeEntity> updateWrapper = new LambdaUpdateWrapper<IntentNodeEntity>()
                .eq(IntentNodeEntity::getId, requestParam.getId())
                .set(IntentNodeEntity::getUpdatedBy, StpUtil.getLoginIdAsLong());

        if (requestParam.getName() != null) {
            updateWrapper.set(IntentNodeEntity::getName, requestParam.getName());
        }
        if (requestParam.getDescription() != null) {
            updateWrapper.set(IntentNodeEntity::getDescription, requestParam.getDescription());
        }
        if (requestParam.getExamples() != null) {
            updateWrapper.set(IntentNodeEntity::getExamples, requestParam.getExamples());
        }
        if (requestParam.getMcpToolId() != null) {
            updateWrapper.set(IntentNodeEntity::getMcpToolId, requestParam.getMcpToolId());
        }
        if (requestParam.getTopK() != null) {
            updateWrapper.set(IntentNodeEntity::getTopK, requestParam.getTopK());
        }
        if (requestParam.getSortOrder() != null) {
            updateWrapper.set(IntentNodeEntity::getSortOrder, requestParam.getSortOrder());
        }
        if (requestParam.getPromptSnippet() != null) {
            updateWrapper.set(IntentNodeEntity::getPromptSnippet, requestParam.getPromptSnippet());
        }
        if (requestParam.getPromptTemplate() != null) {
            updateWrapper.set(IntentNodeEntity::getPromptTemplate, requestParam.getPromptTemplate());
        }
        if (requestParam.getParamPromptTemplate() != null) {
            updateWrapper.set(IntentNodeEntity::getParamPromptTemplate, requestParam.getParamPromptTemplate());
        }

        intentNodeMapper.update(null, updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        IntentNodeEntity entity = intentNodeMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("节点不存在: " + id);
        }

        // 递归收集所有子节点 ID
        List<Long> allIds = new ArrayList<>();
        allIds.add(id);
        collectChildIds(entity.getIntentCode(), allIds);

        // 批量逻辑删除
        intentNodeMapper.delete(
                new LambdaQueryWrapper<IntentNodeEntity>()
                        .in(IntentNodeEntity::getId, allIds)
        );
    }

    @Override
    public void toggleEnabled(Long id) {
        IntentNodeEntity entity = intentNodeMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("节点不存在: " + id);
        }

        int newEnabled = (entity.getEnabled() != null && entity.getEnabled() == 1) ? 0 : 1;
        intentNodeMapper.update(null,
                new LambdaUpdateWrapper<IntentNodeEntity>()
                        .eq(IntentNodeEntity::getId, id)
                        .set(IntentNodeEntity::getEnabled, newEnabled)
                        .set(IntentNodeEntity::getUpdatedBy, StpUtil.getLoginIdAsLong())
        );
    }

    @Override
    public IPage<IntentNodeTreeResp> pageQuery(IntentNodePageReq requestParam) {
        LambdaQueryWrapper<IntentNodeEntity> queryWrapper = new LambdaQueryWrapper<IntentNodeEntity>()
                .and(StringUtils.hasText(requestParam.getKeyword()), w -> w
                        .like(IntentNodeEntity::getName, requestParam.getKeyword())
                        .or()
                        .like(IntentNodeEntity::getIntentCode, requestParam.getKeyword())
                )
                .eq(requestParam.getLevel() != null, IntentNodeEntity::getLevel, requestParam.getLevel())
                .eq(requestParam.getKind() != null, IntentNodeEntity::getKind, requestParam.getKind())
                .eq(requestParam.getEnabled() != null, IntentNodeEntity::getEnabled, requestParam.getEnabled())
                .orderByAsc(IntentNodeEntity::getSortOrder)
                .orderByDesc(IntentNodeEntity::getUpdatedAt);

        Page<IntentNodeEntity> page = new Page<>(requestParam.getCurrent(), requestParam.getSize());
        IPage<IntentNodeEntity> result = intentNodeMapper.selectPage(page, queryWrapper);

        return result.convert(this::toTreeResp);
    }

    /**
     * 递归收集所有子节点 ID
     */
    private void collectChildIds(String parentCode, List<Long> ids) {
        List<IntentNodeEntity> children = intentNodeMapper.selectList(
                new LambdaQueryWrapper<IntentNodeEntity>()
                        .eq(IntentNodeEntity::getParentCode, parentCode)
        );
        for (IntentNodeEntity child : children) {
            ids.add(child.getId());
            collectChildIds(child.getIntentCode(), ids);
        }
    }

    private List<IntentNodeTreeResp> buildTree(List<IntentNodeEntity> allNodes) {
        Map<String, List<IntentNodeEntity>> childrenMap = allNodes.stream()
                .filter(n -> n.getParentCode() != null)
                .collect(Collectors.groupingBy(IntentNodeEntity::getParentCode));

        List<IntentNodeTreeResp> roots = allNodes.stream()
                .filter(n -> n.getParentCode() == null || n.getParentCode().isEmpty())
                .map(this::toTreeResp)
                .collect(Collectors.toList());

        roots.forEach(root -> fillChildren(root, childrenMap));
        return roots;
    }

    private IntentNodeTreeResp toTreeResp(IntentNodeEntity entity) {
        return IntentNodeTreeResp.builder()
                .id(String.valueOf(entity.getId()))
                .intentCode(entity.getIntentCode())
                .name(entity.getName())
                .scope(entity.getScope())
                .level(entity.getLevel())
                .parentCode(entity.getParentCode())
                .description(entity.getDescription())
                .examples(entity.getExamples())
                .collectionName(entity.getCollectionName())
                .topK(entity.getTopK())
                .kind(entity.getKind())
                .sortOrder(entity.getSortOrder())
                .enabled(entity.getEnabled())
                .mcpToolId(entity.getMcpToolId())
                .promptSnippet(entity.getPromptSnippet())
                .promptTemplate(entity.getPromptTemplate())
                .paramPromptTemplate(entity.getParamPromptTemplate())
                .build();
    }

    private void fillChildren(IntentNodeTreeResp parent, Map<String, List<IntentNodeEntity>> childrenMap) {
        List<IntentNodeEntity> children = childrenMap.get(parent.getIntentCode());
        if (children != null) {
            List<IntentNodeTreeResp> childResps = children.stream()
                    .map(this::toTreeResp)
                    .collect(Collectors.toList());
            parent.setChildren(childResps);
            childResps.forEach(child -> fillChildren(child, childrenMap));
        }
    }
}
