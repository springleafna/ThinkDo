package com.springleaf.thinkdo.executor;

import com.springleaf.thinkdo.MCPServerApplication;
import com.springleaf.thinkdo.core.MCPToolRegistry;
import com.springleaf.thinkdo.core.MCPToolRequest;
import com.springleaf.thinkdo.core.MCPToolResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 创建便签 MCP 工具测试
 * 依赖真实数据库，请确保 application.yml 中数据源配置正确，并将 TEST_USER_ID 替换为真实用户ID
 */
@Slf4j
@SpringBootTest(classes = MCPServerApplication.class)
public class CreateMemoMCPExecutorTest {

    @Autowired
    private CreateMemoMCPExecutor executor;

    @Autowired
    private MCPToolRegistry toolRegistry;

    // ===== 替换为数据库中真实存在的 userId =====
    private static final String TEST_USER_ID = "4";

    @Test
    @DisplayName("测试工具已注册")
    public void testToolRegistered() {
        assertTrue(toolRegistry.getExecutor("create_memo").isPresent(),
                "create_memo 工具应该被注册");
        log.info("工具描述: {}", executor.getToolDefinition().getDescription());
    }

    @Test
    @DisplayName("创建一条基本便签")
    public void testCreateBasicMemo() {
        MCPToolRequest request = MCPToolRequest.builder()
                .toolId("create_memo")
                .parameters(Map.of(
                        "userId", TEST_USER_ID,
                        "content", "今天突然有个想法：可以用 AI 自动生成每日计划摘要"
                ))
                .build();

        MCPToolResponse response = executor.execute(request);

        assertTrue(response.isSuccess(), "创建应该成功，错误: " + response.getErrorMessage());
        assertNotNull(response.getData().get("memoId"), "应返回新建便签的ID");
        log.info("创建结果:\n{}", response.getTextResult());
        log.info("便签ID: {}", response.getData().get("memoId"));
    }

    @Test
    @DisplayName("创建带标题、标签和颜色的便签")
    public void testCreateMemoWithAllFields() {
        MCPToolRequest request = MCPToolRequest.builder()
                .toolId("create_memo")
                .parameters(Map.of(
                        "userId", TEST_USER_ID,
                        "title", "产品灵感",
                        "content", "支持语音输入便签，方便用户随时记录",
                        "tag", "灵感",
                        "backgroundColor", "#f9f3ff"
                ))
                .build();

        MCPToolResponse response = executor.execute(request);

        assertTrue(response.isSuccess(), "创建应该成功，错误: " + response.getErrorMessage());
        log.info("创建结果:\n{}", response.getTextResult());
    }

    @Test
    @DisplayName("缺少 userId 参数")
    public void testMissingUserId() {
        MCPToolRequest request = MCPToolRequest.builder()
                .toolId("create_memo")
                .parameters(Map.of("content", "测试内容"))
                .build();

        MCPToolResponse response = executor.execute(request);

        assertFalse(response.isSuccess());
        assertEquals("INVALID_PARAMS", response.getErrorCode());
        log.info("错误信息: {}", response.getErrorMessage());
    }

    @Test
    @DisplayName("缺少 content 参数")
    public void testMissingContent() {
        MCPToolRequest request = MCPToolRequest.builder()
                .toolId("create_memo")
                .parameters(Map.of("userId", TEST_USER_ID))
                .build();

        MCPToolResponse response = executor.execute(request);

        assertFalse(response.isSuccess());
        assertEquals("INVALID_PARAMS", response.getErrorCode());
        log.info("错误信息: {}", response.getErrorMessage());
    }

    @Test
    @DisplayName("非法颜色值自动回退为默认颜色")
    public void testInvalidColorFallback() {
        MCPToolRequest request = MCPToolRequest.builder()
                .toolId("create_memo")
                .parameters(Map.of(
                        "userId", TEST_USER_ID,
                        "content", "颜色测试便签",
                        "backgroundColor", "#ffffff"  // 不在枚举中，回退为默认
                ))
                .build();

        MCPToolResponse response = executor.execute(request);

        assertTrue(response.isSuccess(), "非法颜色应回退默认值，不应失败");
        log.info("创建结果:\n{}", response.getTextResult());
    }
}
