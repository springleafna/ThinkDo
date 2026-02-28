package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.springleaf.thinkdo.domain.dto.StoredFileDTO;
import com.springleaf.thinkdo.domain.entity.KnowledgeBaseEntity;
import com.springleaf.thinkdo.domain.entity.KnowledgeDocumentEntity;
import com.springleaf.thinkdo.domain.request.KnowledgeDocumentUpdateReq;
import com.springleaf.thinkdo.domain.request.KnowledgeDocumentUploadReq;
import com.springleaf.thinkdo.domain.response.KnowledgeDocumentResp;
import com.springleaf.thinkdo.domain.response.KnowledgeDocumentSearchResp;
import com.springleaf.thinkdo.enums.DocumentStatus;
import com.springleaf.thinkdo.enums.SourceType;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.KnowledgeBaseMapper;
import com.springleaf.thinkdo.mapper.KnowledgeDocumentMapper;
import com.springleaf.thinkdo.service.FileStorageService;
import com.springleaf.thinkdo.service.KnowledgeDocumentService;
import com.springleaf.thinkdo.util.HttpClientHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    private final KnowledgeBaseMapper kbMapper;
    private final KnowledgeDocumentMapper docMapper;
    private final FileStorageService fileStorageService;
    private final HttpClientHelper httpClientHelper;

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

        StoredFileDTO stored = resolveStoredFile(kbEntity.getCollectionName(), sourceType, sourceLocation, file);

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
                .createdBy(StpUtil.getLoginIdAsLong())
                .updatedBy(StpUtil.getLoginIdAsLong())
                .build();
        docMapper.insert(documentEntity);

        return BeanUtil.toBean(documentEntity, KnowledgeDocumentResp.class);
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


    private StoredFileDTO resolveStoredFile(String bucketName, SourceType sourceType, String sourceLocation, MultipartFile file) {
        if (SourceType.FILE == sourceType) {
            if (file == null) {
                throw new BusinessException("上传文件不能为空");
            }
            return fileStorageService.upload(bucketName, file);
        }

        HttpClientHelper.HttpFetchResponse response = httpClientHelper.get(sourceLocation, Map.of());
        String fileName = StringUtils.hasText(response.fileName()) ? response.fileName() : "remote-file";
        return fileStorageService.upload(bucketName, response.body(), fileName, response.contentType());
    }
}
