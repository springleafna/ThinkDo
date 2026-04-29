package com.springleaf.thinkdo.controller;

import com.springleaf.thinkdo.common.PageResp;
import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.context.UserContext;
import com.springleaf.thinkdo.domain.request.AdminConversationQueryReq;
import com.springleaf.thinkdo.domain.response.AdminConversationDetailResp;
import com.springleaf.thinkdo.domain.response.AdminConversationInfoResp;
import com.springleaf.thinkdo.enums.ResultCodeEnum;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员-会话管理接口
 */
@RestController
@RequestMapping("/admin/conversation")
@RequiredArgsConstructor
public class AdminConversationController {

    private final ConversationService conversationService;

    /**
     * 分页查询会话列表
     */
    @GetMapping("/list")
    public Result<PageResp<AdminConversationInfoResp>> listConversations(AdminConversationQueryReq queryReq) {
        checkAdmin();
        return Result.success(conversationService.adminListConversations(queryReq));
    }

    /**
     * 获取会话详情（含消息记录）
     */
    @GetMapping("/detail/{conversationId}")
    public Result<AdminConversationDetailResp> getConversationDetail(@PathVariable String conversationId) {
        checkAdmin();
        return Result.success(conversationService.adminGetConversationDetail(conversationId));
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/delete/{conversationId}")
    public Result<Void> deleteConversation(@PathVariable String conversationId) {
        checkAdmin();
        conversationService.adminDeleteConversation(conversationId);
        return Result.success();
    }

    /**
     * 批量删除会话
     */
    @DeleteMapping("/batchDelete")
    public Result<Void> batchDeleteConversations(@RequestBody List<String> conversationIds) {
        checkAdmin();
        conversationService.adminBatchDeleteConversations(conversationIds);
        return Result.success();
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "无管理员权限");
        }
    }
}
