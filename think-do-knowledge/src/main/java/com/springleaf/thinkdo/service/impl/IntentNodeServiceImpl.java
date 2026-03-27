package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.gson.Gson;
import com.springleaf.thinkdo.domain.entity.IntentNodeEntity;
import com.springleaf.thinkdo.domain.request.IntentNodeCreateReq;
import com.springleaf.thinkdo.domain.response.IntentNodeTreeResp;
import com.springleaf.thinkdo.enums.IntentLevel;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.IntentNodeMapper;
import com.springleaf.thinkdo.service.IntentNodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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
