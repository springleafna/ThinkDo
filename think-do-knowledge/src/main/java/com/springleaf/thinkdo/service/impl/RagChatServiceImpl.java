package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.springleaf.thinkdo.chat.ChatMessage;
import com.springleaf.thinkdo.chat.ChatRequest;
import com.springleaf.thinkdo.chat.LLMService;
import com.springleaf.thinkdo.chat.stream.StreamCallback;
import com.springleaf.thinkdo.chat.stream.StreamCancellationHandle;
import com.springleaf.thinkdo.chat.stream.handler.StreamCallbackFactory;
import com.springleaf.thinkdo.chat.stream.handler.StreamTaskManager;
import com.springleaf.thinkdo.domain.dto.IntentGroup;
import com.springleaf.thinkdo.domain.dto.RetrievalContext;
import com.springleaf.thinkdo.domain.dto.SubQuestionIntent;
import com.springleaf.thinkdo.intent.IntentResolver;
import com.springleaf.thinkdo.prompt.PromptTemplateLoader;
import com.springleaf.thinkdo.retrieve.RetrievalEngine;
import com.springleaf.thinkdo.retrieve.prompt.PromptContext;
import com.springleaf.thinkdo.retrieve.prompt.RAGPromptService;
import com.springleaf.thinkdo.rewrite.QueryRewriteService;
import com.springleaf.thinkdo.rewrite.RewriteResult;
import com.springleaf.thinkdo.service.MessageService;
import com.springleaf.thinkdo.service.RagChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.springleaf.thinkdo.constant.RAGConstant.CHAT_SYSTEM_PROMPT_PATH;
import static com.springleaf.thinkdo.constant.RAGConstant.DEFAULT_TOP_K;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagChatServiceImpl implements RagChatService {

    private final LLMService llmService;
    private final StreamCallbackFactory callbackFactory;
    private final MessageService messageService;
    private final StreamTaskManager taskManager;
    private final QueryRewriteService queryRewriteService;
    private final IntentResolver intentResolver;
    private final RetrievalEngine retrievalEngine;
    private final PromptTemplateLoader promptTemplateLoader;
    private final RAGPromptService promptBuilder;

    @Override
    public void streamChat(String question, String conversationId, Boolean deepThinking, SseEmitter emitter) {
        String actualConversationId = StrUtil.isBlank(conversationId) ? IdUtil.getSnowflakeNextIdStr() : conversationId;
        String taskId = IdUtil.getSnowflakeNextIdStr();
        log.info("开始流式对话，会话ID：{}，任务ID：{}", actualConversationId, taskId);
        boolean thinkingEnabled = Boolean.TRUE.equals(deepThinking);

        // 在请求线程中创建 callback，发送 meta 事件并获取 userId
        StreamCallback callback = callbackFactory.createChatEventHandler(emitter, actualConversationId, taskId);
        Long userId = StpUtil.getLoginIdAsLong();

        // 异步执行预处理，使 Controller 立即返回 SseEmitter，SSE 连接先建立
        // 这样 step 事件才能实时推送到前端
        CompletableFuture.runAsync(() -> {
            try {
                doStreamChat(question, actualConversationId, thinkingEnabled, callback, userId, taskId);
            } catch (Exception e) {
                log.error("流式对话异常, conversationId={}, taskId={}", actualConversationId, taskId, e);
                callback.onError(e);
            }
        });
    }

    private void doStreamChat(String question, String conversationId, boolean thinkingEnabled,
                              StreamCallback callback, Long userId, String taskId) {
        // 保存用户消息
        messageService.append(conversationId, userId, new ChatMessage(ChatMessage.Role.USER, question));

        // 加载历史消息
        List<ChatMessage> history = messageService.load(conversationId, userId);

        // 确保用户意图树存在（domain和category级别节点）
        intentResolver.ensureUserIntentTreeExists(userId);

        // 问题拆分
        callback.onStep("rewrite", "正在理解您的问题...");
        RewriteResult rewriteResult = queryRewriteService.rewriteWithSplit(question, history);

        // 获取每个子问题及其意图结果
        callback.onStep("intent", "正在进行意图识别...");
        List<SubQuestionIntent> subIntents = intentResolver.resolve(rewriteResult, userId);

        // 如果全是SYSTEM类型的意图，则不走向量检索和MCP
        boolean allSystemOnly = subIntents.stream()
                .allMatch(si -> intentResolver.isSystemOnly(si.nodeScores()));
        if (allSystemOnly) {
            String customPrompt = subIntents.stream()
                    .flatMap(si -> si.nodeScores().stream())
                    .map(ns -> ns.getNode().getPromptTemplate())
                    .filter(StrUtil::isNotBlank)
                    .findFirst()
                    .orElse(null);
            StreamCancellationHandle handle = streamSystemResponse(rewriteResult.rewrittenQuestion(), history, customPrompt, callback);
            taskManager.bindHandle(taskId, handle);
            return;
        }

        // 根据意图节点进行向量化检索
        callback.onStep("retrieve", "正在检索相关知识...");
        RetrievalContext ctx = retrievalEngine.retrieve(subIntents, DEFAULT_TOP_K, userId);
        if (ctx.isEmpty()) {
            String emptyReply = "未检索到与问题相关的文档内容。";
            callback.onContent(emptyReply);
            callback.onComplete();
            return;
        }

        // 聚合所有意图用于 prompt 规划
        IntentGroup mergedGroup = intentResolver.mergeIntentGroup(subIntents);

        StreamCancellationHandle handle = streamLLMResponse(
                rewriteResult,
                ctx,
                mergedGroup,
                history,
                thinkingEnabled,
                callback
        );
        taskManager.bindHandle(taskId, handle);
    }

    private StreamCancellationHandle streamSystemResponse(String question, List<ChatMessage> history,
                                                          String customPrompt, StreamCallback callback) {
        String systemPrompt = StrUtil.isNotBlank(customPrompt)
                ? customPrompt
                : promptTemplateLoader.load(CHAT_SYSTEM_PROMPT_PATH);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(systemPrompt));
        if (CollUtil.isNotEmpty(history)) {
            messages.addAll(history.subList(0, history.size() - 1));
        }
        messages.add(ChatMessage.user(question));

        ChatRequest req = ChatRequest.builder()
                .messages(messages)
                .temperature(0.7D)
                .thinking(false)
                .build();
        return llmService.streamChat(req, callback);
    }

    private StreamCancellationHandle streamLLMResponse(RewriteResult rewriteResult, RetrievalContext ctx,
                                                       IntentGroup intentGroup, List<ChatMessage> history,
                                                       boolean deepThinking, StreamCallback callback) {
        PromptContext promptContext = PromptContext.builder()
                .question(rewriteResult.rewrittenQuestion())
                .mcpContext(ctx.getMcpContext())
                .kbContext(ctx.getKbContext())
                .mcpIntents(intentGroup.mcpIntents())
                .kbIntents(intentGroup.kbIntents())
                .intentChunks(ctx.getIntentChunks())
                .build();

        List<ChatMessage> messages = promptBuilder.buildStructuredMessages(
                promptContext,
                history,
                rewriteResult.rewrittenQuestion(),
                rewriteResult.subQuestions()  // 传入子问题列表
        );
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)
                .thinking(deepThinking)
                .temperature(ctx.hasMcp() ? 0.3D : 0D)  // MCP 场景稍微放宽温度
                .topP(ctx.hasMcp() ? 0.8D : 1D)
                .build();

        return llmService.streamChat(chatRequest, callback);
    }

    @Override
    public void stopTask(String taskId) {
        taskManager.cancel(taskId);
    }
}
