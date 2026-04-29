package com.springleaf.thinkdo.controller;

import com.springleaf.thinkdo.common.PageResp;
import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.context.UserContext;
import com.springleaf.thinkdo.domain.request.AdminKnowledgeBaseQueryReq;
import com.springleaf.thinkdo.domain.request.AdminKnowledgeDocumentQueryReq;
import com.springleaf.thinkdo.domain.response.AdminKnowledgeBaseInfoResp;
import com.springleaf.thinkdo.domain.response.AdminKnowledgeDocumentInfoResp;
import com.springleaf.thinkdo.domain.response.KnowledgeChunkResp;
import com.springleaf.thinkdo.domain.response.KnowledgeDocumentChunkLogResp;
import com.springleaf.thinkdo.domain.response.KnowledgeDocumentResp;
import com.springleaf.thinkdo.enums.ResultCodeEnum;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.service.KnowledgeBaseService;
import com.springleaf.thinkdo.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员-知识库管理接口
 */
@RestController
@RequestMapping("/admin/knowledge")
@RequiredArgsConstructor
public class AdminKnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeDocumentService knowledgeDocumentService;

    // ==================== 知识库管理 ====================

    /**
     * 分页查询知识库列表
     */
    @GetMapping("/base/list")
    public Result<PageResp<AdminKnowledgeBaseInfoResp>> listKnowledgeBases(AdminKnowledgeBaseQueryReq queryReq) {
        checkAdmin();
        return Result.success(knowledgeBaseService.adminListKnowledgeBases(queryReq));
    }

    /**
     * 获取知识库详情
     */
    @GetMapping("/base/{kbId}")
    public Result<AdminKnowledgeBaseInfoResp> getKnowledgeBaseDetail(@PathVariable String kbId) {
        checkAdmin();
        return Result.success(knowledgeBaseService.adminGetKnowledgeBaseDetail(kbId));
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/base/{kbId}")
    public Result<Void> deleteKnowledgeBase(@PathVariable String kbId) {
        checkAdmin();
        knowledgeBaseService.adminDeleteKnowledgeBase(kbId);
        return Result.success();
    }

    // ==================== 文档管理 ====================

    /**
     * 分页查询文档列表（支持跨知识库）
     */
    @GetMapping("/doc/list")
    public Result<PageResp<AdminKnowledgeDocumentInfoResp>> listDocuments(AdminKnowledgeDocumentQueryReq queryReq) {
        checkAdmin();
        return Result.success(knowledgeDocumentService.adminListDocuments(queryReq));
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/doc/{docId}")
    public Result<KnowledgeDocumentResp> getDocumentDetail(@PathVariable String docId) {
        checkAdmin();
        return Result.success(knowledgeDocumentService.adminGetDocumentDetail(docId));
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/doc/{docId}")
    public Result<Void> deleteDocument(@PathVariable String docId) {
        checkAdmin();
        knowledgeDocumentService.adminDeleteDocument(docId);
        return Result.success();
    }

    /**
     * 启用/禁用文档
     */
    @PatchMapping("/doc/{docId}/enable")
    public Result<Void> enableDocument(@PathVariable String docId, @RequestParam("value") boolean enabled) {
        checkAdmin();
        knowledgeDocumentService.adminEnableDocument(docId, enabled);
        return Result.success();
    }

    // ==================== 分块信息（复用） ====================

    /**
     * 查询文档分块日志
     */
    @GetMapping("/doc/{docId}/chunk-logs")
    public Result<List<KnowledgeDocumentChunkLogResp>> getChunkLogs(@PathVariable String docId) {
        checkAdmin();
        return Result.success(knowledgeDocumentService.getChunkLogs(docId));
    }

    /**
     * 查询文档分块详情列表
     */
    @GetMapping("/doc/{docId}/chunks")
    public Result<List<KnowledgeChunkResp>> getChunks(@PathVariable String docId) {
        checkAdmin();
        return Result.success(knowledgeDocumentService.getChunks(docId));
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "无管理员权限");
        }
    }
}
