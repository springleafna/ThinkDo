package com.springleaf.thinkdo.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.springleaf.thinkdo.document.chunk.VectorChunk;
import com.springleaf.thinkdo.domain.entity.KnowledgeBaseEntity;
import com.springleaf.thinkdo.domain.entity.KnowledgeChunkEntity;
import com.springleaf.thinkdo.domain.entity.KnowledgeDocumentEntity;
import com.springleaf.thinkdo.domain.request.KnowledgeChunkCreateRequest;
import com.springleaf.thinkdo.embedding.EmbeddingService;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.KnowledgeBaseMapper;
import com.springleaf.thinkdo.mapper.KnowledgeChunkMapper;
import com.springleaf.thinkdo.mapper.KnowledgeDocumentMapper;
import com.springleaf.thinkdo.service.KnowledgeChunkService;
import com.springleaf.thinkdo.service.VectorStoreService;
import com.springleaf.thinkdo.token.TokenCounterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeChunkServiceImpl implements KnowledgeChunkService {

    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final EmbeddingService embeddingService;
    private final TokenCounterService tokenCounterService;
    private final VectorStoreService vectorStoreService;
    @Override
    public Boolean existsByDocId(String docId) {
        List<KnowledgeChunkEntity> chunkList = knowledgeChunkMapper.selectList(
                Wrappers.lambdaQuery(KnowledgeChunkEntity.class).eq(KnowledgeChunkEntity::getDocId, docId)
        );
        return chunkList != null && !chunkList.isEmpty();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByDocId(String docId) {
        if (docId == null) {
            return;
        }
        knowledgeChunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunkEntity>().eq(KnowledgeChunkEntity::getDocId, docId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCreate(String docId, List<KnowledgeChunkCreateRequest> requestParams, Long userId) {
        batchCreate(docId, requestParams, false, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCreate(String docId, List<KnowledgeChunkCreateRequest> requestParams, boolean writeVector, Long userId) {
        if (CollUtil.isEmpty(requestParams)) {
            return;
        }

        KnowledgeDocumentEntity document = documentMapper.selectById(docId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }

        boolean needAutoIndex = requestParams.stream().anyMatch(request -> request.getIndex() == null);
        int nextIndex = 0;
        if (needAutoIndex) {
            KnowledgeChunkEntity latest = chunkMapper.selectOne(
                    new LambdaQueryWrapper<KnowledgeChunkEntity>()
                            .eq(KnowledgeChunkEntity::getDocId, docId)
                            .orderByDesc(KnowledgeChunkEntity::getChunkIndex)
                            .last("LIMIT 1")
            );
            nextIndex = latest != null && latest.getChunkIndex() != null ? latest.getChunkIndex() + 1 : 0;
        }

        Long docIdLong = Long.parseLong(docId);
        Long kbId = document.getKbId();
        String embeddingModel = resolveEmbeddingModel(kbId);
        List<KnowledgeChunkEntity> chunkList = new ArrayList<>(requestParams.size());

        for (KnowledgeChunkCreateRequest request : requestParams) {
            String content = request.getContent();
            if (!StringUtils.hasText(content)) {
                throw new BusinessException("Chunk 内容不能为空");
            }

            Integer chunkIndex = request.getIndex();
            if (chunkIndex == null) {
                chunkIndex = nextIndex++;
            }

            String chunkId = request.getChunkId();
            if (!StringUtils.hasText(chunkId)) {
                chunkId = IdUtil.getSnowflakeNextIdStr();
            }

            KnowledgeChunkEntity chunk = KnowledgeChunkEntity.builder()
                    .id(Long.parseLong(chunkId))
                    .kbId(kbId)
                    .docId(docIdLong)
                    .chunkIndex(chunkIndex)
                    .content(content)
                    .contentHash(calculateHash(content))
                    .charCount(content.length())
                    .tokenCount(resolveTokenCount(content))
                    .enabled(1)
                    .createdBy(userId)
                    .build();
            chunkList.add(chunk);
        }

        // 批量写入数据库，向量索引由上层统一处理以避免重复计算
        chunkMapper.insert(chunkList);

        if (writeVector) {
            String kbIdStr = String.valueOf(document.getKbId());
            List<VectorChunk> vectorChunks = chunkList.stream()
                    .map(each -> VectorChunk.builder()
                            .chunkId(String.valueOf(each.getId()))
                            .content(each.getContent())
                            .index(each.getChunkIndex())
                            .build())
                    .toList();
            if (CollUtil.isNotEmpty(vectorChunks)) {
                attachEmbeddings(vectorChunks, embeddingModel);
                vectorStoreService.indexDocumentChunks(kbIdStr, docId, vectorChunks, userId);
            }
        }
    }

    private void attachEmbeddings(List<VectorChunk> chunks, String embeddingModel) {
        if (CollUtil.isEmpty(chunks)) {
            return;
        }
        List<String> texts = chunks.stream().map(VectorChunk::getContent).toList();
        List<List<Float>> vectors = embedBatch(texts, embeddingModel);
        if (vectors == null || vectors.size() != chunks.size()) {
            throw new BusinessException("向量结果数量不匹配");
        }
        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).setEmbedding(toArray(vectors.get(i)));
        }
    }

    /**
     * List<Float> 转 float[]
     */
    private static float[] toArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private List<List<Float>> embedBatch(List<String> texts, String embeddingModel) {
        return StrUtil.isBlank(embeddingModel)
                ? embeddingService.embedBatch(texts)
                : embeddingService.embedBatch(texts, embeddingModel);
    }

    private String resolveEmbeddingModel(Long kbId) {
        if (kbId == null) {
            return null;
        }
        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(kbId);
        return kb != null ? kb.getEmbeddingModel() : null;
    }

    private Integer resolveTokenCount(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        return tokenCounterService.countTokens(content);
    }

    /**
     * 计算内容哈希（SHA-256）
     */
    private String calculateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不可用", e);
        }
    }
}
