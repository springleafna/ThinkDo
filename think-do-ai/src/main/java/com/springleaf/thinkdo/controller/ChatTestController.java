package com.springleaf.thinkdo.controller;

import com.springleaf.thinkdo.chat.*;
import com.springleaf.thinkdo.chat.stream.StreamCallback;
import com.springleaf.thinkdo.chat.stream.StreamCancellationHandle;
import com.springleaf.thinkdo.config.AIModelProperties;
import com.springleaf.thinkdo.model.ModelTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * AI 聊天控制器
 * 以下接口仅作为测试使用
 */
@Slf4j
@RestController
@RequestMapping("/aiTest")
@RequiredArgsConstructor
public class ChatTestController {

    private final AIModelProperties aiModelProperties;
    private final OkHttpClient okHttpClient;
    @Qualifier("modelStreamExecutor")
    private final Executor modelStreamExecutor;
    private final RoutingLLMService routingLLMService;

    @GetMapping(value = "/chat/test/v2")
    public String chatTestV2(@RequestParam(defaultValue = "你好，请介绍一下你自己") String message) {
        return routingLLMService.chat(message);
    }

    @GetMapping(value = "/chat/stream/v2", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter chat(@RequestParam String message) {
        SseEmitter emitter = new SseEmitter(0L);

        StreamCallback callback = new StreamCallback() {
            @Override
            public void onContent(String content) {
                try {
                    emitter.send(SseEmitter.event().data(content));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onComplete() {
                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                emitter.completeWithError(error);
            }
        };

        // 构建请求
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.system("你是一个友好的AI助手，请用简洁的语言回答问题。"),
                        ChatMessage.user(message)
                ))
                .temperature(0.7)
                .maxTokens(500)
                .build();

        routingLLMService.streamChat(request, callback);
        return emitter;
    }

    @GetMapping("/chat/stream/v1")
    public SseEmitter streamChat(@RequestParam String message) {
        SseEmitter emitter = new SseEmitter(30_000L);

        // 获取 GLM-4.7 模型配置
        ModelTarget target = buildModelTarget("glm-4.7");

        // 使用注入的 OkHttpClient 创建 ChatClient
        ChatClient chatClient = new SiliconFlowChatClient(okHttpClient, modelStreamExecutor);

        StreamCallback callback = new StreamCallback() {
            @Override
            public void onContent(String content) {
                try {
                    emitter.send(SseEmitter.event().data(content));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onComplete() {
                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                emitter.completeWithError(error);
            }
        };

        // 构建请求
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.system("你是一个友好的AI助手，请用简洁的语言回答问题。"),
                        ChatMessage.user(message)
                ))
                .temperature(0.7)
                .maxTokens(500)
                .build();

        // 启动流式请求，获取取消句柄
        StreamCancellationHandle handle = chatClient.streamChat(
                request,
                callback,
                target
        );

        // 可选：保存句柄用于后续取消
        // emitter.onCompletion(() -> handle.cancel());

        return emitter;
    }

    /**
     * 简单对话测试接口
     * 使用 GLM-4.7 模型进行测试
     *
     * @param message 用户消息
     * @return AI 回复
     */
    @GetMapping("/chat/test")
    public String chatTest(@RequestParam(defaultValue = "你好，请介绍一下你自己") String message) {
        try {
            // 获取 GLM-4.7 模型配置
            ModelTarget target = buildModelTarget("glm-4.7");

            // 使用注入的 OkHttpClient 创建 ChatClient
            ChatClient chatClient = new SiliconFlowChatClient(okHttpClient, modelStreamExecutor);

            // 构建请求
            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(
                            ChatMessage.system("你是一个友好的AI助手，请用简洁的语言回答问题。"),
                            ChatMessage.user(message)
                    ))
                    .temperature(0.7)
                    .maxTokens(500)
                    .build();

            // 发送请求
            String response = chatClient.chat(request, target);

            log.info("用户消息: {}, AI 回复: {}", message, response);
            return response;

        } catch (Exception e) {
            log.error("对话请求失败", e);
            return "请求失败: " + e.getMessage();
        }
    }

    /**
     * 构建 ModelTarget 对象
     */
    private ModelTarget buildModelTarget(String modelId) {
        AIModelProperties.ModelCandidate candidate = aiModelProperties.getChat().getCandidates().stream()
                .filter(c -> c.getId().equals(modelId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到模型: " + modelId));

        String providerName = candidate.getProvider();
        AIModelProperties.ProviderConfig provider = aiModelProperties.getProviders().get(providerName);

        if (provider == null) {
            throw new IllegalArgumentException("未找到提供商: " + providerName);
        }

        return new ModelTarget(modelId, candidate, provider);
    }
}
