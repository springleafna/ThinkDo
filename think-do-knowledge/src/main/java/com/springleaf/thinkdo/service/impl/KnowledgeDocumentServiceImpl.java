package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springleaf.thinkdo.constant.KnowledgeBaseConstant;
import com.springleaf.thinkdo.document.chunk.*;
import com.springleaf.thinkdo.document.parser.DocumentParserSelector;
import com.springleaf.thinkdo.document.parser.ParserType;
import com.springleaf.thinkdo.domain.dto.StoredFileDTO;
import com.springleaf.thinkdo.domain.entity.KnowledgeBaseEntity;
import com.springleaf.thinkdo.domain.entity.KnowledgeDocumentChunkLogEntity;
import com.springleaf.thinkdo.domain.entity.KnowledgeDocumentEntity;
import com.springleaf.thinkdo.domain.request.KnowledgeChunkCreateRequest;
import com.springleaf.thinkdo.domain.request.KnowledgeDocumentUpdateReq;
import com.springleaf.thinkdo.domain.request.KnowledgeDocumentUploadReq;
import com.springleaf.thinkdo.domain.response.KnowledgeDocumentResp;
import com.springleaf.thinkdo.domain.response.KnowledgeDocumentSearchResp;
import com.springleaf.thinkdo.enums.DocumentStatus;
import com.springleaf.thinkdo.enums.KnowledgeScopeEnum;
import com.springleaf.thinkdo.enums.SourceType;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.KnowledgeBaseMapper;
import com.springleaf.thinkdo.mapper.KnowledgeDocumentChunkLogMapper;
import com.springleaf.thinkdo.mapper.KnowledgeDocumentMapper;
import com.springleaf.thinkdo.service.FileStorageService;
import com.springleaf.thinkdo.service.KnowledgeChunkService;
import com.springleaf.thinkdo.service.KnowledgeDocumentService;
import com.springleaf.thinkdo.service.VectorStoreService;
import com.springleaf.thinkdo.util.HttpClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    private final KnowledgeBaseMapper kbMapper;
    private final KnowledgeDocumentMapper docMapper;
    private final FileStorageService fileStorageService;
    private final HttpClientHelper httpClientHelper;
    private final RedissonClient redissonClient;
    private final PlatformTransactionManager transactionManager;
    private final VectorStoreService vectorStoreService;
    private final KnowledgeChunkService knowledgeChunkService;
    private final KnowledgeDocumentChunkLogMapper chunkLogMapper;
    private final DocumentParserSelector parserSelector;
    private final ObjectMapper objectMapper;
    private final ChunkingStrategyFactory chunkingStrategyFactory;
    @Qualifier("knowledgeChunkExecutor")
    private final Executor knowledgeChunkExecutor;

    @Value("${kb.chunk.semantic.targetChars:1400}")
    private int targetChars;
    @Value("${kb.chunk.semantic.maxChars:1800}")
    private int maxChars;
    @Value("${kb.chunk.semantic.minChars:600}")
    private int minChars;
    @Value("${kb.chunk.semantic.overlapChars:0}")
    private int overlapChars;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentResp upload(String kbId, KnowledgeDocumentUploadReq request, MultipartFile file) {
        KnowledgeBaseEntity kbEntity = kbMapper.selectById(kbId);
        if (kbEntity == null) {
            throw new BusinessException("知识库不存在");
        }

        SourceType sourceType = normalizeSourceType(request == null ? null : request.getSourceType(), file);
        String sourceLocation = request == null ? null : request.getSourceLocation();
        if (StringUtils.hasText(sourceLocation)) {
            sourceLocation = sourceLocation.trim();
        }

        if (SourceType.URL == sourceType && !StringUtils.hasText(sourceLocation)) {
            throw new BusinessException("来源地址不能为空");
        }

        // 上传文件并返回StoredFileDTO（根据知识库作用域选择固定bucket）
        String bucketName = (kbEntity.getScope() == KnowledgeScopeEnum.SYSTEM)
                ? KnowledgeBaseConstant.SYSTEM_BUCKET
                : KnowledgeBaseConstant.USER_BUCKET;
        Long userId = StpUtil.getLoginIdAsLong();
        StoredFileDTO stored = resolveStoredFile(bucketName, sourceType, sourceLocation, file, userId, kbId);

        // 解析分块策略和配置
        ChunkingMode chunkingMode = resolveChunkingMode(request == null ? null : request.getChunkStrategy());
        String chunkConfig = buildChunkConfigJson(chunkingMode, request);

        KnowledgeDocumentEntity documentEntity = KnowledgeDocumentEntity.builder()
                .kbId(Long.parseLong(kbId))
                .docName(stored.getOriginalFilename())
                .enabled(1)
                .chunkCount(0)
                .fileUrl(stored.getUrl())
                .fileType(stored.getDetectedType())
                .fileSize(stored.getSize())
                .status(DocumentStatus.PENDING.getCode())
                .sourceType(sourceType.getValue())
                .sourceLocation(SourceType.URL == sourceType ? sourceLocation : null)
                .chunkStrategy(chunkingMode != null ? chunkingMode.getValue() : null)
                .chunkConfig(chunkConfig)
                .createdBy(StpUtil.getLoginIdAsLong())
                .updatedBy(StpUtil.getLoginIdAsLong())
                .build();
        docMapper.insert(documentEntity);

        return BeanUtil.toBean(documentEntity, KnowledgeDocumentResp.class);
    }

    private String buildChunkConfigJson(ChunkingMode mode, KnowledgeDocumentUploadReq request) {
        if (request == null) {
            return null;
        }
        if (StringUtils.hasText(request.getChunkConfig())) {
            return request.getChunkConfig().trim();
        }
        if (mode == null) {
            mode = ChunkingMode.STRUCTURE_AWARE;
        }
        Map<String, Object> params = new HashMap<>();
        if (mode == ChunkingMode.FIXED_SIZE) {
            if (request.getChunkSize() != null) {
                params.put("chunkSize", request.getChunkSize());
            }
            if (request.getOverlapSize() != null) {
                params.put("overlapSize", request.getOverlapSize());
            }
        } else {
            if (request.getTargetChars() != null) {
                params.put("targetChars", request.getTargetChars());
            }
            if (request.getMaxChars() != null) {
                params.put("maxChars", request.getMaxChars());
            }
            if (request.getMinChars() != null) {
                params.put("minChars", request.getMinChars());
            }
            if (request.getOverlapChars() != null) {
                params.put("overlapChars", request.getOverlapChars());
            }
        }
        if (params.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            throw new BusinessException("分块参数序列化失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String docId) {
        KnowledgeDocumentEntity documentEntity = docMapper.selectById(docId);
        if (documentEntity == null) {
            throw new BusinessException("文档不存在");
        }

        documentEntity.setDeleted(1);
        documentEntity.setUpdatedBy(StpUtil.getLoginIdAsLong());
        docMapper.deleteById(documentEntity);
    }

    @Override
    public KnowledgeDocumentResp get(String docId) {
        KnowledgeDocumentEntity documentEntity = docMapper.selectById(docId);
        if (documentEntity == null) {
            throw new BusinessException("文档不存在");
        }
        return BeanUtil.toBean(documentEntity, KnowledgeDocumentResp.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String docId, KnowledgeDocumentUpdateReq requestParam) {
        KnowledgeDocumentEntity documentEntity = docMapper.selectById(docId);
        if (documentEntity == null) {
            throw new BusinessException("文档不存在");
        }

        String docName = requestParam == null ? null : requestParam.getDocName();
        if (!StringUtils.hasText(docName)) {
            throw new BusinessException("文档名称不能为空");
        }

        KnowledgeDocumentEntity update = new KnowledgeDocumentEntity();
        update.setId(documentEntity.getId());
        update.setDocName(docName.trim());
        update.setUpdatedBy(StpUtil.getLoginIdAsLong());
        docMapper.updateById(update);
    }

    @Override
    public IPage<KnowledgeDocumentResp> page(String kbId, Page<KnowledgeDocumentResp> page, String status, String keyword) {
        Page<KnowledgeDocumentEntity> mpPage = new Page<>(page.getCurrent(), page.getSize());
        LambdaQueryWrapper<KnowledgeDocumentEntity> qw = new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getKbId, kbId)
                .eq(KnowledgeDocumentEntity::getDeleted, 0)
                .like(keyword != null && !keyword.isBlank(), KnowledgeDocumentEntity::getDocName, keyword)
                .eq(status != null && !status.isBlank(), KnowledgeDocumentEntity::getStatus, status)
                .orderByDesc(KnowledgeDocumentEntity::getCreatedAt);

        IPage<KnowledgeDocumentEntity> result = docMapper.selectPage(mpPage, qw);

        Page<KnowledgeDocumentResp> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(each -> BeanUtil.toBean(each, KnowledgeDocumentResp.class)).toList());
        return voPage;
    }

    @Override
    public List<KnowledgeDocumentSearchResp> search(String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        int size = Math.min(Math.max(limit, 1), 20);
        Page<KnowledgeDocumentEntity> mpPage = new Page<>(1, size);
        LambdaQueryWrapper<KnowledgeDocumentEntity> qw = new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getDeleted, 0)
                .like(KnowledgeDocumentEntity::getDocName, keyword)
                .orderByDesc(KnowledgeDocumentEntity::getUpdatedAt);

        IPage<KnowledgeDocumentEntity> result = docMapper.selectPage(mpPage, qw);
        List<KnowledgeDocumentSearchResp> records = result.getRecords().stream()
                .map(each -> BeanUtil.toBean(each, KnowledgeDocumentSearchResp.class))
                .toList();
        if (records.isEmpty()) {
            return records;
        }

        Set<Long> kbIds = new HashSet<>();
        for (KnowledgeDocumentSearchResp record : records) {
            if (record.getKbId() != null) {
                kbIds.add(record.getKbId());
            }
        }
        if (kbIds.isEmpty()) {
            return records;
        }

        List<KnowledgeBaseEntity> bases = kbMapper.selectByIds(kbIds);
        Map<Long, String> nameMap = new HashMap<>();
        if (bases != null) {
            for (KnowledgeBaseEntity base : bases) {
                nameMap.put(base.getId(), base.getName());
            }
        }
        for (KnowledgeDocumentSearchResp record : records) {
            record.setKbName(nameMap.get(record.getKbId()));
        }
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable(String docId, boolean enabled) {
        KnowledgeDocumentEntity documentEntity = docMapper.selectById(docId);
        if (documentEntity == null) {
            throw new BusinessException("文档不存在");
        }
        documentEntity.setEnabled(enabled ? 1 : 0);
        documentEntity.setUpdatedBy(StpUtil.getLoginIdAsLong());
        docMapper.updateById(documentEntity);
    }

    @Override
    public void startChunk(String docId) {
        Long userId = StpUtil.getLoginIdAsLong();
        // 使用分布式锁避免同一文档的并发分块
        String lockKey = String.format("knowledge:chunk:lock:%s", docId);
        RLock lock = redissonClient.getLock(lockKey);

        // 尝试获取锁，最多等待5秒，锁自动过期时间30秒
        boolean locked = false;
        try {
            locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException("文档分块操作正在进行中，请稍后再试");
            }

            // 在锁保护下，使用 TransactionTemplate 手动管理事务
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            txTemplate.executeWithoutResult(status -> {
                KnowledgeDocumentEntity document = docMapper.selectById(docId);
                if (document == null) {
                    throw new BusinessException("文档不存在");
                }
                if (DocumentStatus.RUNNING.getCode().equals(document.getStatus())) {
                    throw new BusinessException("文档分块进行中");
                }

                // 允许重复分块：如果已经分块过，先删除历史分块记录
                boolean alreadyChunked = knowledgeChunkService.existsByDocId(docId);
                if (alreadyChunked) {
                    log.info("文档已存在分块记录，将删除历史分块并重新分块: docId={}", docId);
                    // 删除数据库中的历史分块记录
                    knowledgeChunkService.deleteByDocId(docId);
                    // 删除向量库中的历史向量（在事务提交后异步执行分块任务时也会删除，这里提前删除确保一致性）
                    String kbId = String.valueOf(document.getKbId());
                    vectorStoreService.deleteDocumentVectors(kbId, docId);
                }

                // 更新文档状态为 处理中
                patchStatus(document, userId);
                try {
                    // 使用线程池进行 文档提取->分块->向量化 任务
                    knowledgeChunkExecutor.execute(() -> runChunkTask(document, userId));
                } catch (RejectedExecutionException e) {
                    log.error("分块任务提交失败: docId={}", docId, e);
                    throw new BusinessException("分块任务排队失败");
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("获取分块锁被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void runChunkTask(KnowledgeDocumentEntity document, Long userId) {
        String docId = String.valueOf(document.getId());

        // 创建分块日志记录
        KnowledgeDocumentChunkLogEntity chunkLog = KnowledgeDocumentChunkLogEntity.builder()
                .docId(document.getId())
                .status(DocumentStatus.RUNNING.getCode())
                .chunkStrategy(document.getChunkStrategy())
                .startTime(LocalDateTime.now())
                .build();
        chunkLogMapper.insert(chunkLog);

        // 总处理的开始时间
        long totalStartTime = System.currentTimeMillis();
        // 文本提取耗时
        long extractDuration = 0;
        // 文本分块耗时
        long chunkDuration = 0;
        // 向量化耗时
        long embeddingDuration = 0;

        List<VectorChunk> chunkResults;

        try {
            // 使用分块策略处理文档
            ChunkProcessResult result = runChunkProcess(document, userId);
            extractDuration = result.getExtractDuration();
            chunkDuration = result.getChunkDuration();
            chunkResults = result.getChunks();

            if (chunkResults == null) {
                // 处理失败
                updateChunkLog(chunkLog.getId(), DocumentStatus.FAILED.getCode(), 0, extractDuration, chunkDuration, 0,
                        System.currentTimeMillis() - totalStartTime, "分块处理失败");
                return;
            }
            log.info("文档提取->分块完成，docId：{}，分块数量：{}", docId, chunkResults.size());

            // 保存分块到数据库并更新向量库
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            List<VectorChunk> finalChunkResults = chunkResults;
            txTemplate.executeWithoutResult(status -> {
                List<KnowledgeChunkCreateRequest> chunks = finalChunkResults.stream()
                        .map(vectorChunk -> {
                            KnowledgeChunkCreateRequest req = new KnowledgeChunkCreateRequest();
                            req.setChunkId(vectorChunk.getChunkId());
                            req.setIndex(vectorChunk.getIndex());
                            req.setContent(vectorChunk.getContent());
                            return req;
                        })
                        .toList();
                knowledgeChunkService.batchCreate(docId, chunks, userId);

                KnowledgeDocumentEntity update = new KnowledgeDocumentEntity();
                update.setId(document.getId());
                update.setChunkCount(chunks.size());
                update.setStatus(DocumentStatus.SUCCESS.getCode());
                update.setUpdatedBy(userId);
                docMapper.updateById(update);
            });

            // 向量化
            String kbId = String.valueOf(document.getKbId());
            long embeddingStart = System.currentTimeMillis();
            vectorStoreService.deleteDocumentVectors(kbId, docId);
            vectorStoreService.indexDocumentChunks(kbId, docId, chunkResults, userId);
            embeddingDuration = System.currentTimeMillis() - embeddingStart;

            long totalDuration = System.currentTimeMillis() - totalStartTime;

            // 更新日志为成功
            updateChunkLog(chunkLog.getId(), DocumentStatus.SUCCESS.getCode(), chunkResults.size(), extractDuration,
                    chunkDuration, embeddingDuration, totalDuration, null);
            log.info("文档 docId：{} 提取 -> 分块 ->向量化完成，总耗时：{} ms", docId, totalDuration);
        } catch (Exception e) {
            log.error("文件分块失败：docId={}", docId, e);
            // 分块出错时将文档状态更新为失败，由于下面方法的事务传播行为为REQUIRES_NEW，所以下面方法内部的事务和当前事务互不影响，
            // 所以能够确保分块失败后状态更新的行为不会回滚
            markChunkFailed(document.getId(), userId);
            long totalDuration = System.currentTimeMillis() - totalStartTime;
            updateChunkLog(chunkLog.getId(), DocumentStatus.FAILED.getCode(), 0, extractDuration, chunkDuration,
                    embeddingDuration, totalDuration, e.getMessage());
        }
    }

    /**
     * 使用分块策略处理文档
     */
    private ChunkProcessResult runChunkProcess(KnowledgeDocumentEntity document, Long userId) {
        Long docId = document.getId();
        ChunkingMode chunkingMode = resolveChunkingMode(document.getChunkStrategy());
        String embeddingModel = resolveEmbeddingModel(document.getKbId());
        ChunkingOptions config = buildChunkingOptions(chunkingMode, document, embeddingModel);
        long extractStart = System.currentTimeMillis();
        long chunkStart = 0;
        long extractDuration = 0;
        long chunkDuration = 0;

        try (InputStream is = fileStorageService.openStream(document.getFileUrl())) {
            String text = parserSelector.select(ParserType.TIKA.getType()).extractText(is, document.getDocName());
            extractDuration = System.currentTimeMillis() - extractStart;
            ChunkingStrategy chunkingStrategy = chunkingStrategyFactory.requireStrategy(chunkingMode);
            chunkStart = System.currentTimeMillis();
            List<VectorChunk> chunks = chunkingStrategy.chunk(text, config);
            chunkDuration = System.currentTimeMillis() - chunkStart;
            return new ChunkProcessResult(chunks, extractDuration, chunkDuration);
        } catch (Exception e) {
            if (extractStart > 0 && extractDuration == 0) {
                extractDuration = System.currentTimeMillis() - extractStart;
            }
            if (chunkStart > 0 && chunkDuration == 0) {
                chunkDuration = System.currentTimeMillis() - chunkStart;
            }
            log.error("文件分块失败：docId={}", docId, e);
            markChunkFailed(document.getId(), userId);
            return new ChunkProcessResult(null, extractDuration, chunkDuration);
        }
    }

    private ChunkingOptions buildChunkingOptions(ChunkingMode mode, KnowledgeDocumentEntity document, String embeddingModel) {
        if (mode == null) {
            mode = ChunkingMode.STRUCTURE_AWARE;
        }
        Map<String, Object> config = parseChunkConfig(document.getChunkConfig());
        if (mode == ChunkingMode.FIXED_SIZE) {
            Integer chunkSize = getConfigInt(config, "chunkSize", 512);
            Integer overlapSize = getConfigInt(config, "overlapSize", 128);
            Map<String, Object> metadata = new HashMap<>();
            if (StringUtils.hasText(embeddingModel)) {
                metadata.put("embeddingModel", embeddingModel);
            }
            return ChunkingOptions.builder()
                    .chunkSize(chunkSize)
                    .overlapSize(overlapSize)
                    .metadata(metadata)
                    .build();
        }
        Integer target = getConfigInt(config, "targetChars", targetChars);
        Integer max = getConfigInt(config, "maxChars", maxChars);
        Integer min = getConfigInt(config, "minChars", minChars);
        Integer overlap = getConfigInt(config, "overlapChars", overlapChars);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("targetChars", target);
        metadata.put("maxChars", max);
        metadata.put("minChars", min);
        metadata.put("overlapChars", overlap);
        if (StringUtils.hasText(embeddingModel)) {
            metadata.put("embeddingModel", embeddingModel);
        }

        return ChunkingOptions.builder()
                .chunkSize(target)
                .overlapSize(overlap)
                .metadata(metadata)
                .build();
    }

    private Map<String, Object> parseChunkConfig(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("分块参数解析失败: {}", json, e);
            return Map.of();
        }
    }

    private Integer getConfigInt(Map<String, Object> config, String key, Integer defaultValue) {
        if (config == null || config.isEmpty()) {
            return defaultValue;
        }
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String str && StringUtils.hasText(str)) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private String resolveEmbeddingModel(Long kbId) {
        if (kbId == null) {
            return null;
        }
        KnowledgeBaseEntity kb = kbMapper.selectById(kbId);
        return kb != null ? kb.getEmbeddingModel() : null;
    }

    private void markChunkFailed(Long docId, Long userId) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        txTemplate.executeWithoutResult(status -> {
            KnowledgeDocumentEntity update = new KnowledgeDocumentEntity();
            update.setId(docId);
            update.setStatus(DocumentStatus.FAILED.getCode());
            update.setUpdatedBy(userId);
            docMapper.updateById(update);
        });
    }

    private void updateChunkLog(Long logId, String status, int chunkCount, long extractDuration,
                                long chunkDuration, long embeddingDuration, long totalDuration,
                                String errorMessage) {
        KnowledgeDocumentChunkLogEntity update = new KnowledgeDocumentChunkLogEntity();
        update.setId(logId);
        update.setStatus(status);
        update.setChunkCount(chunkCount);
        update.setExtractDuration(extractDuration);
        update.setChunkDuration(chunkDuration);
        update.setEmbeddingDuration(embeddingDuration);
        update.setTotalDuration(totalDuration);
        update.setErrorMessage(errorMessage);
        update.setEndTime(LocalDateTime.now());
        chunkLogMapper.updateById(update);
    }

    private ChunkingMode resolveChunkingMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return ChunkingMode.STRUCTURE_AWARE;
        }
        return ChunkingMode.fromValue(mode);
    }

    private void patchStatus(KnowledgeDocumentEntity doc, Long userId) {
        doc.setStatus(DocumentStatus.RUNNING.getCode());
        doc.setUpdatedBy(userId);
        docMapper.updateById(doc);
    }

    private SourceType normalizeSourceType(String sourceType, MultipartFile file) {
        if (!StringUtils.hasText(sourceType)) {
            return file == null ? SourceType.URL : SourceType.FILE;
        }
        SourceType result = SourceType.fromValue(sourceType);
        if (result == null) {
            throw new BusinessException("不支持的来源类型: " + sourceType);
        }
        return result;
    }

    private static class ChunkProcessResult {
        private final List<VectorChunk> chunks;
        private final long extractDuration;
        private final long chunkDuration;

        private ChunkProcessResult(List<VectorChunk> chunks, long extractDuration, long chunkDuration) {
            this.chunks = chunks;
            this.extractDuration = extractDuration;
            this.chunkDuration = chunkDuration;
        }

        private List<VectorChunk> getChunks() {
            return chunks;
        }

        private long getExtractDuration() {
            return extractDuration;
        }

        private long getChunkDuration() {
            return chunkDuration;
        }
    }

    /**
     * 根据源类型解析并上传文件，返回文件的元数据信息。
     *
     * @param bucketName     目标存储桶名称
     * @param sourceType     文件来源类型（本地文件或远程链接）
     * @param sourceLocation 远程文件的URL地址（当sourceType不为FILE时使用）
     * @param file           本地上传的MultipartFile对象（当sourceType为FILE时使用）
     * @param userId         用户ID，用于构建文件路径前缀
     * @param kbId           知识库ID，用于构建文件路径前缀
     * @return StoredFileDTO 包含文件URL、类型、大小等元数据的对象
     * @throws BusinessException 当sourceType为FILE但file为空时抛出
     */
    private StoredFileDTO resolveStoredFile(String bucketName, SourceType sourceType, String sourceLocation, MultipartFile file, Long userId, String kbId) {
        // 构建路径前缀：{userId}/kb_{kbId}/
        String pathPrefix = userId + "/kb_" + kbId + "/";
        
        // 处理本地文件上传逻辑
        if (SourceType.FILE == sourceType) {
            if (file == null) {
                throw new BusinessException("上传文件不能为空");
            }
            return fileStorageService.upload(bucketName, file, pathPrefix);
        }

        // 处理远程文件抓取逻辑
        HttpClientHelper.HttpFetchResponse response = httpClientHelper.get(sourceLocation, Map.of());
        String fileName = StringUtils.hasText(response.fileName()) ? response.fileName() : "remote-file";
        return fileStorageService.upload(bucketName, response.body(), fileName, response.contentType(), pathPrefix);
    }

}
