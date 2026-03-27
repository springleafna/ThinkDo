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
 * 即将截止计划查询 MCP 工具测试
 * 依赖真实数据库，请确保 application.yml 中数据源配置正确，并将 TEST_USER_ID 替换为真实用户ID
 */
@Slf4j
@SpringBootTest(classes = MCPServerApplication.class)
public class DuePlanQueryMCPExecutorTest {

    @Autowired
    private DuePlanQueryMCPExecutor executor;

    @Autowired
    private MCPToolRegistry toolRegistry;

    // ===== 替换为数据库中真实存在的 userId =====
    private static final String TEST_USER_ID = "4";

    @Test
    @DisplayName("测试工具已注册")
    public void testToolRegistered() {
        assertTrue(toolRegistry.getExecutor("due_plan_query").isPresent(),
                "due_plan_query 工具应该被注册");
        log.info("工具描述: {}", executor.getToolDefinition().getDescription());
    }

    @Test
    @DisplayName("查询未来7天即将截止的计划（默认）")
    public void testQueryDuePlansDefault() {
        MCPToolRequest request = MCPToolRequest.builder()
                .toolId("due_plan_query")
                .parameters(Map.of("userId", TEST_USER_ID))
                .build();

        MCPToolResponse response = executor.execute(request);

        assertTrue(response.isSuccess(), "查询应该成功，错误: " + response.getErrorMessage());
        assertNotNull(response.getTextResult());
        log.info("查询结果:\n{}", response.getTextResult());
    }

    @Test
    @DisplayName("查询未来3天即将截止的计划")
    public void testQueryDuePlans3Days() {
        MCPToolRequest request = MCPToolRequest.builder()
                .toolId("due_plan_query")
                .parameters(Map.of("userId", TEST_USER_ID, "days", 3))
                .build();

        MCPToolResponse response = executor.execute(request);

        assertTrue(response.isSuccess(), "查询应该成功，错误: " + response.getErrorMessage());
        log.info("未来3天截止计划:\n{}", response.getTextResult());
    }

    @Test
    @DisplayName("缺少 userId 参数")
    public void testMissingUserId() {
        MCPToolRequest request = MCPToolRequest.builder()
                .toolId("due_plan_query")
                .parameters(Map.of())
                .build();

        MCPToolResponse response = executor.execute(request);

        assertFalse(response.isSuccess());
        assertEquals("INVALID_PARAMS", response.getErrorCode());
        log.info("错误信息: {}", response.getErrorMessage());
    }

    @Test
    @DisplayName("userId 格式错误")
    public void testInvalidUserId() {
        MCPToolRequest request = MCPToolRequest.builder()
                .toolId("due_plan_query")
                .parameters(Map.of("userId", "not-a-number"))
                .build();

        MCPToolResponse response = executor.execute(request);

        assertFalse(response.isSuccess());
        assertEquals("INVALID_PARAMS", response.getErrorCode());
        log.info("错误信息: {}", response.getErrorMessage());
    }
}
