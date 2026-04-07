package com.springleaf.thinkdo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.domain.request.KnowledgeDocumentUpdateReq;
import com.springleaf.thinkdo.domain.request.KnowledgeDocumentUploadReq;
import com.springleaf.thinkdo.domain.response.KnowledgeChunkResp;
import com.springleaf.thinkdo.domain.response.KnowledgeDocumentChunkLogResp;
import com.springleaf.thinkdo.domain.response.KnowledgeDocumentResp;
import com.springleaf.thinkdo.domain.response.KnowledgeDocumentSearchResp;
import com.springleaf.thinkdo.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


/**
 * 知识库文档管理控制器
 * 提供文档的上传、分块、删除、查询、启用/禁用等功能
 */
@RestController
@RequiredArgsConstructor
@Validated
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService documentService;


    /**
     * 上传文档：入库记录 + 文件落盘，返回文档ID
     */
    @PostMapping(value = "/knowledge-base/{kb-id}/docs/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<KnowledgeDocumentResp> upload(@PathVariable("kb-id") String kbId,
                                                @RequestPart(value = "file", required = false) MultipartFile file,
                                                @ModelAttribute KnowledgeDocumentUploadReq requestParam) {
        return Result.success(documentService.upload(kbId, requestParam, file));
    }

    /**
     * 开始分块：抽取文本 -> 分块 -> 嵌入并写入向量库
     */
    @PostMapping("/knowledge-base/docs/{doc-id}/chunk")
    public Result<Void> startChunk(@PathVariable(value = "doc-id") String docId) {
        documentService.startChunk(docId);
        return Result.success();
    }

    /**
     * 删除文档：逻辑删除。可选同时删除向量库中该文档的所有 chunk
     */
    @DeleteMapping("/knowledge-base/docs/{doc-id}")
    public Result<Void> delete(@PathVariable(value = "doc-id") String docId) {
        documentService.delete(docId);
        return Result.success();
    }

    /**
     * 查询文档详情
     */
    @GetMapping("/knowledge-base/docs/{docId}")
    public Result<KnowledgeDocumentResp> get(@PathVariable String docId) {
        return Result.success(documentService.get(docId));
    }

    /**
     * 更新文档信息
     */
    @PutMapping("/knowledge-base/docs/{docId}")
    public Result<Void> update(@PathVariable String docId,
                               @RequestBody KnowledgeDocumentUpdateReq requestParam) {
        documentService.update(docId, requestParam);
        return Result.success();
    }

    /**
     * 分页查询文档列表（支持状态/关键字过滤）
     */
    @GetMapping("/knowledge-base/{kb-id}/docs")
    public Result<IPage<KnowledgeDocumentResp>> page(@PathVariable(value = "kb-id") String kbId,
                                                   @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                                   @RequestParam(value = "status", required = false) String status,
                                                   @RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(documentService.page(kbId, new Page<>(pageNo, pageSize), status, keyword));
    }

    /**
     * 搜索文档（全局检索建议）
     */
    @GetMapping("/knowledge-base/docs/search")
    public Result<List<KnowledgeDocumentSearchResp>> search(@RequestParam(value = "keyword", required = false) String keyword,
                                                            @RequestParam(value = "limit", defaultValue = "8") int limit) {
        return Result.success(documentService.search(keyword, limit));
    }

    /**
     * 启用/禁用文档
     */
    @PatchMapping("/knowledge-base/docs/{docId}/enable")
    public Result<Void> enable(@PathVariable String docId,
                               @RequestParam("value") boolean enabled) {
        documentService.enable(docId, enabled);
        return Result.success();
    }

    /**
     * 查询文档分块日志
     */
    @GetMapping("/knowledge-base/docs/{docId}/chunk-logs")
    public Result<List<KnowledgeDocumentChunkLogResp>> getChunkLogs(@PathVariable String docId) {
        return Result.success(documentService.getChunkLogs(docId));
    }

    /**
     * 查询文档分块详情列表
     */
    @GetMapping("/knowledge-base/docs/{docId}/chunks")
    public Result<List<KnowledgeChunkResp>> getChunks(@PathVariable String docId) {
        return Result.success(documentService.getChunks(docId));
    }
}
