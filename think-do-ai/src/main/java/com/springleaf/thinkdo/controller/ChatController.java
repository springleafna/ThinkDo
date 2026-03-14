package com.springleaf.thinkdo.controller;

import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.domain.response.ConversationInfoResp;
import com.springleaf.thinkdo.domain.response.MessageInfoResp;
import com.springleaf.thinkdo.domain.request.UpdateConversationReq;
import com.springleaf.thinkdo.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天会话Controller
 */
@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ConversationService conversationService;

    /**
     * 获取会话列表
     */
    @GetMapping("/list")
    public Result<List<ConversationInfoResp>> getConversationList() {
        return Result.success(conversationService.getConversationList());
    }

    /**
     * 根据会话ID获取历史消息
     */
    @GetMapping("/messages/{conversationId}")
    public Result<List<MessageInfoResp>> getMessagesByConversationId(@PathVariable String conversationId) {
        return Result.success(conversationService.getMessagesByConversationId(conversationId));
    }

    /**
     * 修改会话名称
     */
    @PutMapping("/update")
    public Result<Void> updateConversation(@RequestBody @Valid UpdateConversationReq updateConversationReq) {
        conversationService.updateConversation(updateConversationReq);
        return Result.success();
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/delete/{conversationId}")
    public Result<Void> deleteConversation(@PathVariable String conversationId) {
        conversationService.deleteConversation(conversationId);
        return Result.success();
    }
}
