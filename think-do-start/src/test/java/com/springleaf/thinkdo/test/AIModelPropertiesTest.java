package com.springleaf.thinkdo.test;

import com.springleaf.thinkdo.config.AIModelProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Comparator;

/**
 * AI 模型配置属性测试
 * 只加载配置属性类，不加载完整的应用上下文
 */
@SpringBootTest(classes = AIModelPropertiesTest.Config.class,
        args = "--spring.config.import=optional:file:../.env[.properties]")
@TestPropertySource(locations = "classpath:application.yml")
public class AIModelPropertiesTest {

    @Qualifier("ai-com.springleaf.thinkdo.config.AIModelProperties")
    @Autowired
    private AIModelProperties aiModelProperties;

    @Test
    public void testPrintAllConfigurations() {
        System.out.println("\n========================================");
        System.out.println("      AI Model Properties Configuration");
        System.out.println("========================================\n");

        // 1. 输出 Providers 配置
        System.out.println("Providers (提供商配置):");
        System.out.println("----------------------------------------");
        aiModelProperties.getProviders().forEach((name, provider) -> {
            System.out.println("  [" + name + "]");
            System.out.println("    URL: " + provider.getUrl());
            System.out.println("    API Key: " + maskApiKey(provider.getApiKey()));
            System.out.println("    Endpoints:");
            provider.getEndpoints().forEach((type, path) -> {
                System.out.println("      " + type + " -> " + path);
            });
            System.out.println();
        });

        // 2. 输出 Chat 配置
        System.out.println("Chat (聊天模型配置):");
        System.out.println("----------------------------------------");
        printModelGroup("chat", aiModelProperties.getChat());

        // 3. 输出 Embedding 配置
        System.out.println("Embedding (向量嵌入模型配置):");
        System.out.println("----------------------------------------");
        printModelGroup("embedding", aiModelProperties.getEmbedding());

        // 4. 输出 Rerank 配置
        System.out.println("Rerank (重排序模型配置):");
        System.out.println("----------------------------------------");
        printModelGroup("rerank", aiModelProperties.getRerank());

        // 5. 输出 Selection 配置
        System.out.println("Selection (模型选择策略配置):");
        System.out.println("----------------------------------------");
        System.out.println("  Failure Threshold: " + aiModelProperties.getSelection().getFailureThreshold());
        System.out.println("  Open Duration (ms): " + aiModelProperties.getSelection().getOpenDurationMs());
        System.out.println();

        // 6. 输出 Stream 配置
        System.out.println("Stream (流式响应配置):");
        System.out.println("----------------------------------------");
        System.out.println("  Message Chunk Size: " + aiModelProperties.getStream().getMessageChunkSize());
        System.out.println();

        System.out.println("========================================");
        System.out.println("           Configuration Loaded ✓");
        System.out.println("========================================\n");
    }

    private void printModelGroup(String groupName, AIModelProperties.ModelGroup modelGroup) {
        System.out.println("  Default Model: " + modelGroup.getDefaultModel());
        System.out.println("  Deep Thinking Model: " + modelGroup.getDeepThinkingModel());
        System.out.println("  Candidates (" + modelGroup.getCandidates().size() + "):");

        // 按优先级排序输出
        modelGroup.getCandidates().stream()
                .sorted(Comparator.comparingInt(c -> c.getPriority() == null ? 100 : c.getPriority()))
                .forEach(candidate -> {
                    System.out.printf("    - %-20s | provider: %-15s | model: %-35s | priority: %3d | enabled: %5s | thinking: %5s%n",
                            candidate.getId(),
                            candidate.getProvider(),
                            candidate.getModel(),
                            candidate.getPriority() == null ? 100 : candidate.getPriority(),
                            candidate.getEnabled(),
                            candidate.getSupportsThinking());
                    if (candidate.getDimension() != null) {
                        System.out.println("      dimension: " + candidate.getDimension());
                    }
                });
        System.out.println();
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null) {
            return "null";
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * 最小化测试配置，只启用 AIModelProperties
     */
    @EnableConfigurationProperties(AIModelProperties.class)
    public static class Config {
    }
}
