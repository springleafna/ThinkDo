package com.springleaf.thinkdo.test;

import com.springleaf.thinkdo.config.AIModelProperties;
import com.springleaf.thinkdo.embedding.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 向量化服务测试类
 * 测试文本向量化功能，包括单个文本和批量文本的嵌入操作
 */
@Slf4j
@SpringBootTest(args = "--spring.config.import=optional:file:../.env[.properties]")
@TestPropertySource(locations = "classpath:application.yml")
public class EmbeddingTest {

    @Autowired
    private AIModelProperties aiModelProperties;

    @Autowired(required = false)
    private EmbeddingService embeddingService;

    private int expectedDimension;

    /**
     * 测试前准备
     */
    @BeforeEach
    public void setUp() {
        // 获取配置的向量维度
        AIModelProperties.ModelGroup embeddingGroup = aiModelProperties.getEmbedding();
        String defaultModelId = embeddingGroup.getDefaultModel();

        expectedDimension = embeddingGroup.getCandidates().stream()
                .filter(c -> c.getId().equals(defaultModelId))
                .findFirst()
                .map(AIModelProperties.ModelCandidate::getDimension)
                .orElse(0);

        if (embeddingService == null) {
            log.warn("EmbeddingService 未注入，请检查 Spring 配置");
        } else {
            log.info("Embedding 测试初始化完成，使用模型: {}, 向量维度: {}",
                    defaultModelId, embeddingService.dimension());
        }
    }

    @Test
    @DisplayName("测试单个文本向量化")
    public void testSingleTextEmbedding() {
        assertNotNull(embeddingService, "EmbeddingService 应该已注入");

        // 测试文本
        String text = "Java 是一种广泛使用的编程语言，具有跨平台特性。";

        // 执行向量化
        List<Float> embedding = embeddingService.embed(text);

        // 验证结果
        assertNotNull(embedding, "向量化结果不应为空");
        assertFalse(embedding.isEmpty(), "向量维度应大于0");
        assertEquals(expectedDimension, embedding.size(),
                "向量维度应与配置的维度一致");

        log.info("文本: {}", text);
        log.info("向量维度: {}", embedding.size());
        log.info("向量前5个值: {}", embedding.stream()
                .limit(5)
                .map(v -> String.format("%.6f", v))
                .collect(Collectors.joining(", ")));
    }

    @Test
    @DisplayName("测试批量文本向量化")
    public void testBatchTextEmbedding() {
        assertNotNull(embeddingService, "EmbeddingService 应该已注入");

        // 准备多个文本
        List<String> texts = Arrays.asList(
                "Spring Boot 是基于 Spring 的开发框架",
                "机器学习是人工智能的一个分支",
                "数据库用于存储和管理数据"
        );

        // 执行批量向量化
        List<List<Float>> embeddings = embeddingService.embedBatch(texts);

        // 验证结果
        assertNotNull(embeddings, "批量向量化结果不应为空");
        assertEquals(texts.size(), embeddings.size(), "结果数量应与输入数量一致");

        for (int i = 0; i < embeddings.size(); i++) {
            List<Float> embedding = embeddings.get(i);
            assertNotNull(embedding, "第 " + i + " 个向量化结果不应为空");
            assertEquals(expectedDimension, embedding.size(),
                    "第 " + i + " 个向量维度应与配置的维度一致");
        }

        log.info("批量向量化 {} 个文本完成", texts.size());
        log.info("向量维度: {}", embeddings.get(0).size());
    }

    @Test
    @DisplayName("测试空文本处理")
    public void testEmptyTextEmbedding() {
        assertNotNull(embeddingService, "EmbeddingService 应该已注入");

        // 空列表
        List<List<Float>> emptyBatchResult = embeddingService.embedBatch(Collections.emptyList());
        assertNotNull(emptyBatchResult, "空批量向量化结果不应为null");
        assertTrue(emptyBatchResult.isEmpty(), "空批量向量化结果应为空列表");
    }

    @Test
    @DisplayName("测试大批量文本分批处理")
    public void testLargeBatchEmbedding() {
        assertNotNull(embeddingService, "EmbeddingService 应该已注入");

        // 创建超过 MAX_BATCH (32) 的文本列表
        int totalTexts = 50;
        List<String> texts = IntStream.range(0, totalTexts)
                .mapToObj(i -> "这是第 " + (i + 1) + " 条测试文本")
                .collect(Collectors.toList());

        long startTime = System.currentTimeMillis();
        List<List<Float>> embeddings = embeddingService.embedBatch(texts);
        long duration = System.currentTimeMillis() - startTime;

        // 验证结果
        assertEquals(totalTexts, embeddings.size(), "应返回所有文本的向量化结果");

        for (List<Float> embedding : embeddings) {
            assertNotNull(embedding, "每个向量化结果都不应为null");
            assertEquals(expectedDimension, embedding.size(),
                    "每个向量维度应与配置的维度一致");
        }

        log.info("批量向量化 {} 个文本完成，耗时: {} ms", totalTexts, duration);
        log.info("平均每个文本耗时: {} ms", duration / totalTexts);
    }

    @Test
    @DisplayName("测试向量相似度计算")
    public void testVectorSimilarity() {
        assertNotNull(embeddingService, "EmbeddingService 应该已注入");

        // 两个相似的文本
        String text1 = "Python 是一种流行的编程语言";
        String text2 = "Python 编程在数据科学领域应用广泛";
        String text3 = "今天天气很好";

        List<Float> embedding1 = embeddingService.embed(text1);
        List<Float> embedding2 = embeddingService.embed(text2);
        List<Float> embedding3 = embeddingService.embed(text3);

        // 计算余弦相似度
        double similarity12 = cosineSimilarity(embedding1, embedding2);
        double similarity13 = cosineSimilarity(embedding1, embedding3);
        double similarity23 = cosineSimilarity(embedding2, embedding3);

        log.info(String.format("文本1和文本2的相似度: %.4f", similarity12));
        log.info(String.format("文本1和文本3的相似度: %.4f", similarity13));
        log.info(String.format("文本2和文本3的相似度: %.4f", similarity23));

        // 相似的文本（1和2）应该比不相似的文本（1和3）有更高的相似度
        assertTrue(similarity12 > similarity13,
                "相似文本的相似度应高于不相似文本");
    }

    @Test
    @DisplayName("测试特殊字符处理")
    public void testSpecialCharactersEmbedding() {
        assertNotNull(embeddingService, "EmbeddingService 应该已注入");

        List<String> specialTexts = Arrays.asList(
                "包含中文的文本：你好世界",
                "Contains English: Hello World",
                "Mix混合中英文和数字123",
                "特殊字符: 测试"
        );

        List<List<Float>> embeddings = embeddingService.embedBatch(specialTexts);

        assertEquals(specialTexts.size(), embeddings.size());
        for (int i = 0; i < embeddings.size(); i++) {
            assertNotNull(embeddings.get(i),
                    "特殊字符文本 " + i + " 的向量化结果不应为null");
            assertEquals(expectedDimension, embeddings.get(i).size(),
                    "特殊字符文本 " + i + " 的向量维度应正确");
        }

        log.info("特殊字符文本向量化测试通过");
    }

    @Test
    @DisplayName("测试长文本向量化")
    public void testLongTextEmbedding() {
        assertNotNull(embeddingService, "EmbeddingService 应该已注入");

        // 构建一个较长的文本
        String longText = String.join(" ", Arrays.asList(
                "Java是一种广泛使用的计算机编程语言，拥有跨平台、面向对象、泛型编程的特性。",
                "广泛应用于企业级Web应用开发和移动应用开发。",
                "Java由Sun Microsystems公司于1995年5月推出，后由Oracle公司收购和维护。",
                "Java的语法与C和C++相似，但具有更简单的对象模型和更少的底层功能。",
                "Java应用程序通常编译成可以在任何Java虚拟机上运行的字节码。"
        ));

        List<Float> embedding = embeddingService.embed(longText);

        assertNotNull(embedding, "长文本向量化结果不应为空");
        assertEquals(expectedDimension, embedding.size(),
                "长文本向量维度应与配置的维度一致");

        log.info("长文本向量化测试通过，文本长度: {} 字符", longText.length());
        log.info("向量维度: {}", embedding.size());
    }

    @Test
    @DisplayName("测试指定模型向量化")
    public void testEmbeddingWithSpecificModel() {
        assertNotNull(embeddingService, "EmbeddingService 应该已注入");

        String text = "测试指定模型的向量化功能";

        // 获取配置中的模型ID
        AIModelProperties.ModelGroup embeddingGroup = aiModelProperties.getEmbedding();
        String modelId = embeddingGroup.getDefaultModel();

        // 使用指定模型进行向量化
        List<Float> embedding = embeddingService.embed(text, modelId);

        assertNotNull(embedding, "向量化结果不应为空");
        assertEquals(expectedDimension, embedding.size(),
                "向量维度应与配置的维度一致");

        log.info("使用模型 {} 进行向量化测试完成", modelId);
    }

    @Test
    @DisplayName("测试批量向量化指定模型")
    public void testBatchEmbeddingWithSpecificModel() {
        assertNotNull(embeddingService, "EmbeddingService 应该已注入");

        List<String> texts = Arrays.asList(
                "第一条测试文本",
                "第二条测试文本",
                "第三条测试文本"
        );

        // 获取配置中的模型ID
        AIModelProperties.ModelGroup embeddingGroup = aiModelProperties.getEmbedding();
        String modelId = embeddingGroup.getDefaultModel();

        // 使用指定模型进行批量向量化
        List<List<Float>> embeddings = embeddingService.embedBatch(texts, modelId);

        assertNotNull(embeddings, "批量向量化结果不应为空");
        assertEquals(texts.size(), embeddings.size(),
                "结果数量应与输入数量一致");

        log.info("使用模型 {} 进行批量向量化测试完成", modelId);
    }

    @Test
    @DisplayName("测试获取向量维度")
    public void testDimension() {
        assertNotNull(embeddingService, "EmbeddingService 应该已注入");

        int dimension = embeddingService.dimension();

        assertTrue(dimension > 0, "向量维度应大于0");
        assertEquals(expectedDimension, dimension,
                "返回的维度应与配置的维度一致");

        log.info("向量维度: {}", dimension);
    }

    /**
     * 计算两个向量的余弦相似度
     */
    private double cosineSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1.size() != vector2.size()) {
            throw new IllegalArgumentException("向量长度不一致");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += vector1.get(i) * vector1.get(i);
            norm2 += vector2.get(i) * vector2.get(i);
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
