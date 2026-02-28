package com.springleaf.thinkdo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.domain.request.KnowledgeBaseCreateReq;
import com.springleaf.thinkdo.domain.request.KnowledgeBasePageReq;
import com.springleaf.thinkdo.domain.request.KnowledgeBaseUpdateReq;
import com.springleaf.thinkdo.domain.response.KnowledgeBaseResp;
import com.springleaf.thinkdo.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 知识库控制器
 * 提供知识库的增删改查等基础操作接口
 */
@RestController
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 创建知识库
     */
    @PostMapping("/knowledge-base")
    public Result<String> createKnowledgeBase(@RequestBody KnowledgeBaseCreateReq requestParam) {
        return Result.success(knowledgeBaseService.create(requestParam));
    }

    /**
     * 重命名知识库
     */
    @PutMapping("/knowledge-base/{kb-id}")
    public Result<Void> renameKnowledgeBase(@PathVariable("kb-id") String kbId,
                                            @RequestBody KnowledgeBaseUpdateReq requestParam) {
        knowledgeBaseService.rename(kbId, requestParam);
        return Result.success();
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/knowledge-base/{kb-id}")
    public Result<Void> deleteKnowledgeBase(@PathVariable("kb-id") String kbId) {
        knowledgeBaseService.delete(kbId);
        return Result.success();
    }

    /**
     * 查询知识库详情
     */
    @GetMapping("/knowledge-base/{kb-id}")
    public Result<KnowledgeBaseResp> queryKnowledgeBase(@PathVariable("kb-id") String kbId) {
        return Result.success(knowledgeBaseService.queryById(kbId));
    }

    /**
     * 分页查询知识库列表
     */
    @GetMapping("/knowledge-base")
    public Result<IPage<KnowledgeBaseResp>> pageQuery(KnowledgeBasePageReq requestParam) {
        return Result.success(knowledgeBaseService.pageQuery(requestParam));
    }
}
