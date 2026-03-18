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
import com.springleaf.thinkdo.domain.dto.RetrievedChunk;
import com.springleaf.thinkdo.retrieve.RetrieverService;
import com.springleaf.thinkdo.service.MessageService;
import com.springleaf.thinkdo.service.RagChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static com.springleaf.thinkdo.constant.RAGConstant.DEFAULT_TOP_K;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagChatServiceImpl implements RagChatService {

    private final LLMService llmService;
    private final StreamCallbackFactory callbackFactory;
    private final MessageService messageService;
    private final StreamTaskManager taskManager;
    private final RetrieverService retrieverService;

    @Override
    public void streamChat(String question, String conversationId, Boolean deepThinking, Boolean useKnowledgeBase, SseEmitter emitter) {
        String actualConversationId = StrUtil.isBlank(conversationId) ? IdUtil.getSnowflakeNextIdStr() : conversationId;
        String taskId = IdUtil.getSnowflakeNextIdStr();
        log.info("开始流式对话，会话ID：{}，任务ID：{}", actualConversationId, taskId);
        boolean thinkingEnabled = Boolean.TRUE.equals(deepThinking);
        boolean knowledgeBaseEnabled = Boolean.TRUE.equals(useKnowledgeBase);

        StreamCallback callback = callbackFactory.createChatEventHandler(emitter, actualConversationId, taskId);
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 保存用户消息
        messageService.append(actualConversationId, userId, new ChatMessage(ChatMessage.Role.USER, question));
        
        // 加载历史消息
        List<ChatMessage> messages = messageService.load(actualConversationId, userId);
        
        // RAG流程：根据检索到的知识构建系统提示词
        if (knowledgeBaseEnabled) {
            // 从向量库检索相关文档
            List<RetrievedChunk> retrievedChunks = retrieverService.retrieve(question, DEFAULT_TOP_K);
            log.info("检索到 {} 条相关文档", retrievedChunks.size());
            
            // 如果检索到了相关文档，构建 RAG 系统提示词
            if (!retrievedChunks.isEmpty()) {
                String ragSystemPrompt = buildRAGSystemPrompt(retrievedChunks);
                
                // 将 RAG 系统提示词插入到消息列表的开头
                messages.add(0, new ChatMessage(ChatMessage.Role.SYSTEM, ragSystemPrompt));
                log.info("已添加 RAG 系统提示词，知识库上下文长度：{} 字符", ragSystemPrompt.length());
            } else {
                log.warn("未检索到相关文档，使用普通对话模式");
            }
        }
        
        // 构建请求并调用 LLM
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)
                .thinking(deepThinking)
                .build();
        llmService.streamChat(chatRequest, callback);
    }
    
    /**
     * 构建 RAG 系统提示词
     * 
     * @param retrievedChunks 检索到的文档块列表
     * @return 系统提示词
     */
    private String buildRAGSystemPrompt(List<RetrievedChunk> retrievedChunks) {
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("你是一个智能助手，需要基于以下知识库内容回答用户问题。\n\n");
        contextBuilder.append("【知识库内容】\n");
        
        for (int i = 0; i < retrievedChunks.size(); i++) {
            RetrievedChunk chunk = retrievedChunks.get(i);
            contextBuilder.append(String.format("[文档%d 相似度: %.2f]\n%s\n\n", 
                    i + 1, chunk.getScore(), chunk.getText()));
        }
        
        contextBuilder.append("【回答要求】\n");
        contextBuilder.append("1. 请优先使用上述知识库内容回答用户问题\n");
        contextBuilder.append("2. 如果知识库内容不足以回答问题，可以基于你的训练知识补充，但要说明\n");
        contextBuilder.append("3. 回答时要准确、客观，引用知识库内容时请标注来源\n");
        contextBuilder.append("4. 如果知识库内容与用户问题无关，请直接说明，不要强行回答\n");
        
        return contextBuilder.toString();
    }

    @Override
    public void stopTask(String taskId) {
        taskManager.cancel(taskId);
    }
}
