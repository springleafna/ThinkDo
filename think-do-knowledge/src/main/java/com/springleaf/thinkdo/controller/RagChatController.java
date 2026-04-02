package com.springleaf.thinkdo.controller;

import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.domain.request.UpdateConversationReq;
import com.springleaf.thinkdo.domain.response.ConversationInfoResp;
import com.springleaf.thinkdo.domain.response.MessageInfoResp;
import com.springleaf.thinkdo.service.ConversationService;
import com.springleaf.thinkdo.service.MessageService;
import com.springleaf.thinkdo.service.RagChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 聊天会话Controller
 */
@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class RagChatController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final RagChatService ragChatService;

    /**
     * 发起流式对话
     */
    @GetMapping(value = "/rag", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter chat(@RequestParam String question,
                           @RequestParam(required = false) String conversationId,
                           @RequestParam(required = false, defaultValue = "false") Boolean deepThinking) {
        SseEmitter emitter = new SseEmitter(0L);
        ragChatService.streamChat(question, conversationId, deepThinking, emitter);
        return emitter;
    }

    /**
     * 停止指定的对话任务
     */
    @PostMapping(value = "/stop")
    public Result<Void> stop(@RequestParam String taskId) {
        ragChatService.stopTask(taskId);
        return Result.success();
    }

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
        return Result.success(messageService.getMessagesByConversationId(conversationId));
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
