package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.springleaf.thinkdo.domain.entity.KnowledgeBaseEntity;
import com.springleaf.thinkdo.domain.entity.KnowledgeDocumentEntity;
import com.springleaf.thinkdo.domain.request.KnowledgeBaseCreateReq;
import com.springleaf.thinkdo.domain.request.KnowledgeBasePageReq;
import com.springleaf.thinkdo.domain.request.KnowledgeBaseUpdateReq;
import com.springleaf.thinkdo.domain.response.KnowledgeBaseResp;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.KnowledgeBaseMapper;
import com.springleaf.thinkdo.mapper.KnowledgeDocumentMapper;
import com.springleaf.thinkdo.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    // private final VectorStoreAdmin vectorStoreAdmin;
    private final S3Client s3Client;

    @Transactional
    @Override
    public String create(KnowledgeBaseCreateReq requestParam) {
        // 名称重复校验
        String name = requestParam.getName().replaceAll("\\s+", "");
        Long count = knowledgeBaseMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeBaseEntity>()
                        .eq(KnowledgeBaseEntity::getName, name)
        );
        if (count > 0) {
            throw new BusinessException("知识库名称已存在：" + requestParam.getName());
        }

        KnowledgeBaseEntity kb = KnowledgeBaseEntity.builder()
                .name(requestParam.getName())
                .embeddingModel(requestParam.getEmbeddingModel())
                .collectionName(requestParam.getCollectionName())
                .createdBy(StpUtil.getLoginIdAsLong())
                .updatedBy(StpUtil.getLoginIdAsLong())
                .build();

        knowledgeBaseMapper.insert(kb);

        String bucketName = requestParam.getCollectionName();
        try {
            s3Client.createBucket(builder -> builder.bucket(bucketName));
            log.info("成功创建RestFS存储桶，Bucket名称: {}", bucketName);
        } catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException e) {
            if (e instanceof BucketAlreadyOwnedByYouException) {
                log.error("RestFS存储桶已存在，Bucket名称: {}", bucketName, e);
            } else {
                log.error("RestFS存储桶已存在但由其他账户拥有，Bucket名称: {}", bucketName, e);
            }
            throw new BusinessException("存储桶名称已被占用：" + bucketName);
        }

        // TODO：确保向量空间存在
        /*VectorSpaceSpec spaceSpec = VectorSpaceSpec.builder()
                .spaceId(VectorSpaceId.builder()
                        .logicalName(requestParam.getCollectionName())
                        .build())
                .remark(requestParam.getName())
                .build();
        vectorStoreAdmin.ensureVectorSpace(spaceSpec);*/

        return String.valueOf(kb.getId());
    }

    @Override
    public void update(KnowledgeBaseUpdateReq requestParam) {
        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(requestParam.getId());
        if (kb == null || kb.getDeleted() != null && kb.getDeleted() == 1) {
            throw new IllegalArgumentException("知识库不存在：" + requestParam.getId());
        }

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

        kb.setUpdatedBy(StpUtil.getLoginIdAsLong());
        knowledgeBaseMapper.updateById(kb);
    }

    @Override
    public void rename(String kbId, KnowledgeBaseUpdateReq requestParam) {
        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || kb.getDeleted() != null && kb.getDeleted() == 1) {
            throw new BusinessException("知识库不存在");
        }

        if (!StringUtils.hasText(requestParam.getName())) {
            throw new BusinessException("知识库名称不能为空");
        }

        // 名称重复校验（排除当前知识库）
        String name = requestParam.getName().replaceAll("\\s+", "");
        Long count = knowledgeBaseMapper.selectCount(
                Wrappers.lambdaQuery(KnowledgeBaseEntity.class)
                        .eq(KnowledgeBaseEntity::getName, name)
                        .ne(KnowledgeBaseEntity::getId, kbId)
        );
        if (count > 0) {
            throw new BusinessException("知识库名称已存在：" + requestParam.getName());
        }

        kb.setName(requestParam.getName());
        kb.setUpdatedBy(StpUtil.getLoginIdAsLong());
        knowledgeBaseMapper.updateById(kb);

        log.info("成功重命名知识库, kbId={}, newName={}", kbId, requestParam.getName());
    }

    @Override
    public void delete(String kbId) {
        // 限制删除前需要确保没有文档
        Long docCount = knowledgeDocumentMapper.selectCount(
                Wrappers.lambdaQuery(KnowledgeDocumentEntity.class)
                        .eq(KnowledgeDocumentEntity::getKbId, kbId)
        );
        if (docCount > 0) {
            throw new BusinessException("知识库下仍有关联文档，无法删除");
        }

        knowledgeBaseMapper.deleteById(kbId);
    }

    @Override
    public KnowledgeBaseResp queryById(String kbId) {
        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || kb.getDeleted() != null && kb.getDeleted() == 1) {
            throw new BusinessException("知识库不存在");
        }
        return BeanUtil.toBean(kb, KnowledgeBaseResp.class);
    }

    @Override
    public IPage<KnowledgeBaseResp> pageQuery(KnowledgeBasePageReq requestParam) {
        LambdaQueryWrapper<KnowledgeBaseEntity> queryWrapper = Wrappers.lambdaQuery(KnowledgeBaseEntity.class)
                .like(StringUtils.hasText(requestParam.getName()), KnowledgeBaseEntity::getName, requestParam.getName())
                .eq(KnowledgeBaseEntity::getDeleted, 0)
                .orderByDesc(KnowledgeBaseEntity::getUpdatedAt);

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
}
