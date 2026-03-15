package com.springleaf.thinkdo.chat.stream.handler;

import com.springleaf.thinkdo.config.AIModelProperties;
import com.springleaf.thinkdo.service.ConversationService;
import com.springleaf.thinkdo.service.MessageService;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * StreamChatEventHandler 构建参数
 * 使用参数对象模式，将多个参数封装成一个对象
 */
@Getter
@Builder
public class StreamChatHandlerParams {

    /**
     * SSE 发射器
     */
    private final SseEmitter emitter;

    /**
     * 会话ID
     */
    private final String conversationId;

    /**
     * 任务ID
     */
    private final String taskId;

    /**
     * 模型配置
     */
    private final AIModelProperties modelProperties;

    /**
     * 会话服务
     */
    private final ConversationService conversationService;

    /**
     * 消息服务
     */
    private final MessageService messageService;

    /**
     * 任务管理器
     */
    private final StreamTaskManager taskManager;
}
