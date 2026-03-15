package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.springleaf.thinkdo.chat.ChatMessage;
import com.springleaf.thinkdo.chat.ChatRequest;
import com.springleaf.thinkdo.chat.LLMService;
import com.springleaf.thinkdo.chat.stream.StreamCallback;
import com.springleaf.thinkdo.chat.stream.handler.StreamCallbackFactory;
import com.springleaf.thinkdo.chat.stream.handler.StreamTaskManager;
import com.springleaf.thinkdo.service.ChatService;
import com.springleaf.thinkdo.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final LLMService llmService;
    private final StreamCallbackFactory callbackFactory;
    private final MessageService messageService;
    private final StreamTaskManager taskManager;


    @Override
    public void streamChat(String question, String conversationId, Boolean deepThinking, SseEmitter emitter) {
        String actualConversationId = StrUtil.isBlank(conversationId) ? IdUtil.getSnowflakeNextIdStr() : conversationId;
        String taskId = IdUtil.getSnowflakeNextIdStr();
        log.info("开始流式对话，会话ID：{}，任务ID：{}", actualConversationId, taskId);
        boolean thinkingEnabled = Boolean.TRUE.equals(deepThinking);
        StreamCallback callback = callbackFactory.createChatEventHandler(emitter, actualConversationId, taskId);
        long userId = StpUtil.getLoginIdAsLong();
        messageService.append(actualConversationId, userId, new ChatMessage(ChatMessage.Role.USER, question));
        List<ChatMessage> messages = messageService.load(actualConversationId, userId);
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)
                .thinking(deepThinking)
                .build();
        llmService.streamChat(chatRequest, callback);
    }

    @Override
    public void stopTask(String taskId) {
        taskManager.cancel(taskId);
    }
}
