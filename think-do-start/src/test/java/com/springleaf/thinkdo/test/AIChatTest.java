package com.springleaf.thinkdo.test;

import com.springleaf.thinkdo.chat.ChatClient;
import com.springleaf.thinkdo.chat.ChatMessage;
import com.springleaf.thinkdo.chat.ChatRequest;
import com.springleaf.thinkdo.chat.SiliconFlowChatClient;
import com.springleaf.thinkdo.config.AIModelProperties;
import com.springleaf.thinkdo.model.ModelTarget;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI 聊天客户端测试
 * 测试与 SiliconFlow 和 Bailian 提供商的对话功能
 */
@Slf4j
@SpringBootTest(classes = AIChatTest.Config.class,
        args = "--spring.config.import=optional:file:../.env[.properties]")
@TestPropertySource(locations = "classpath:application.yml")
public class AIChatTest {

    @Qualifier("ai-com.springleaf.thinkdo.config.AIModelProperties")
    @Autowired
    private AIModelProperties aiModelProperties;

    @Qualifier("modelStreamExecutor")
    @Autowired
    private Executor modelStreamExecutor;

    private ChatClient chatClient;

    /**
     * 测试前准备：创建 ChatClient 实例
     */
    @BeforeEach
    public void setUp() {
        // 创建 OkHttpClient 实例（30秒超时）
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(30))
                .build();

        // 创建 SiliconFlowChatClient 实例
        chatClient = new SiliconFlowChatClient(httpClient, modelStreamExecutor);
    }

    @Test
    @DisplayName("测试 SiliconFlow GLM-4.7 模型对话")
    public void testSiliconFlowGLMChat() {
        // 准备 ModelTarget
        ModelTarget target = buildModelTarget("glm-4.7");

        // 构建请求
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.system("你是一个友好的AI助手，请用简洁的语言回答问题。"),
                        ChatMessage.user("请用一句话介绍一下 Java 编程语言。")
                ))
                .temperature(0.7)
                .maxTokens(200)
                .build();

        // 发送请求
        String response = chatClient.chat(request, target);

        // 验证响应
        assertNotNull(response, "响应不应为空");
        assertFalse(response.isBlank(), "响应不应为空字符串");
        log.info("GLM-4.7 响应: {}", response);
    }

    @Test
    @DisplayName("测试 Bailian Qwen3-Max 模型对话")
    public void testBailianQwen3MaxChat() {
        // 准备 ModelTarget
        ModelTarget target = buildModelTarget("qwen3-max");

        // 构建请求
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.system("你是一个专业的代码助手。"),
                        ChatMessage.user("如何理解 Java 中的多态性？请用简短的语言解释。")
                ))
                .temperature(0.5)
                .maxTokens(300)
                .build();

        // 发送请求
        String response = chatClient.chat(request, target);

        // 验证响应
        assertNotNull(response, "响应不应为空");
        assertFalse(response.isBlank(), "响应不应为空字符串");
        log.info("Qwen3-Max 响应: {}", response);
    }

    @Test
    @DisplayName("测试多轮对话")
    public void testMultiTurnConversation() {
        ModelTarget target = buildModelTarget("glm-4.7");

        // 多轮对话
        List<ChatMessage> messages = List.of(
                ChatMessage.system("你是一个编程导师。"),
                ChatMessage.user("什么是 Spring Boot？"),
                ChatMessage.assistant("Spring Boot 是基于 Spring 框架的开发工具，简化了 Spring 应用的配置和部署。"),
                ChatMessage.user("它有哪些主要优点？")
        );

        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .temperature(0.6)
                .build();

        String response = chatClient.chat(request, target);

        assertNotNull(response);
        assertFalse(response.isBlank());
        log.info("多轮对话响应: {}", response);
    }

    @Test
    @DisplayName("测试流式参数（当前实现为同步）")
    public void testChatWithTopP() {
        ModelTarget target = buildModelTarget("qwen-plus");

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.user("给我讲一个不超过50字的短故事。")
                ))
                .temperature(0.8)
                .topP(0.9)
                .maxTokens(100)
                .build();

        String response = chatClient.chat(request, target);

        assertNotNull(response);
        log.info("Top-P 测试响应: {}", response);
    }

    @Test
    @DisplayName("测试思考模式（thinking=true）")
    public void testChatWithThinking() {
        ModelTarget target = buildModelTarget("qwen3-max"); // supports-thinking: true

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.user("解释一下递归的概念，并举一个简单例子。")
                ))
                .temperature(0.3)
                .thinking(true)
                .build();

        String response = chatClient.chat(request, target);

        assertNotNull(response);
        log.info("思考模式响应: {}", response);
    }

    /**
     * 构建 ModelTarget 对象
     * 根据 modelId 从配置中查找对应的模型和提供商信息
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

        if (provider.getApiKey() == null || provider.getApiKey().isBlank()) {
            throw new IllegalStateException("提供商 " + providerName + " 的 API Key 未配置，请检查环境变量");
        }

        return new ModelTarget(modelId, candidate, provider);
    }

    /**
     * 最小化测试配置
     */
    @EnableConfigurationProperties(AIModelProperties.class)
    public static class Config {
    }
}
