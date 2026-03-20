package com.springleaf.thinkdo.test;

import com.springleaf.thinkdo.ThinkDoApplication;
import com.springleaf.thinkdo.intent.DefaultIntentClassifier;
import com.springleaf.thinkdo.intent.IntentNode;
import com.springleaf.thinkdo.intent.NodeScore;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 意图树构建与分类测试
 */
@Slf4j
@SpringBootTest(classes = ThinkDoApplication.class,
        args = "--spring.config.import=optional:file:../.env[.properties]")
public class IntentTreeTest {

    @Autowired
    private DefaultIntentClassifier classifier;

    // 预设测试参数
    private static final Long TEST_USER_ID = 10001L;
    private static final String TEST_QUESTION = "解释一下SpringBoot";

    @Test
    public void testLoadIntentTreeFromDB() {
        log.info("========== 测试 loadIntentTreeFromDB ==========");

        List<IntentNode> roots = classifier.loadIntentTreeFromDB(TEST_USER_ID);

        log.info("根节点数量: {}", roots.size());
        printIntentTree(roots, 0);

        log.info("========== loadIntentTreeFromDB 测试完成 ==========\n");
    }

    @Test
    public void testLoadIntentTreeData() {
        log.info("========== 测试 loadIntentTreeData ==========");

        var result = classifier.loadIntentTreeData(TEST_USER_ID);

        log.info("总节点数: {}", result.allNodes().size());
        log.info("叶子节点数: {}", result.leafNodes().size());
        log.info("ID映射数: {}", result.id2Node().size());

        log.info("========== loadIntentTreeData 测试完成 ==========\n");
    }

    @Test
    public void testClassifyTargets() {
        log.info("========== 测试 classifyTargets ==========");
        log.info("测试问题: {}", TEST_QUESTION);

        List<NodeScore> scores = classifier.classifyTargets(TEST_QUESTION, TEST_USER_ID);

        log.info("分类结果数量: {}", scores.size());
        for (int i = 0; i < scores.size(); i++) {
            NodeScore score = scores.get(i);
            IntentNode node = score.getNode();
            log.info("{}. {} | {} | 分数: {}",
                    i + 1, node.getName(), node.getFullPath(), score.getScore());
        }

        log.info("========== classifyTargets 测试完成 ==========\n");
    }

    private void printIntentTree(List<IntentNode> nodes, int level) {
        for (IntentNode node : nodes) {
            String indent = "  ".repeat(level);
            log.info("{}├─ [{}] {} - {}",
                    indent, node.getId(), node.getName(), node.getFullPath());
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                printIntentTree(node.getChildren(), level + 1);
            }
        }
    }
}
