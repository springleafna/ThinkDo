package com.springleaf.thinkdo.executor;

import com.springleaf.thinkdo.core.DefaultMCPToolRegistry;
import com.springleaf.thinkdo.core.MCPToolDefinition;
import com.springleaf.thinkdo.core.MCPToolRegistry;
import com.springleaf.thinkdo.core.MCPToolRequest;
import com.springleaf.thinkdo.core.MCPToolResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP 天气工具测试
 */
@Slf4j
@SpringBootTest(classes = WeatherMCPExecutorTest.Config.class)
public class WeatherMCPExecutorTest {

    @Autowired
    private MCPToolRegistry toolRegistry;

    @Autowired
    private WeatherMCPExecutor weatherExecutor;

    @Test
    @DisplayName("测试天气工具注册")
    public void testToolRegistered() {
        List<MCPToolDefinition> tools = toolRegistry.listAllTools();
        assertFalse(tools.isEmpty(), "应该至少有一个工具被注册");

        Optional<MCPToolDefinition> weatherTool = tools.stream()
                .filter(t -> "weather_query".equals(t.getToolId()))
                .findFirst();
        assertTrue(weatherTool.isPresent(), "weather_query 工具应该被注册");
        log.info("工具描述: {}", weatherTool.get().getDescription());
    }

    @Test
    @DisplayName("测试查询当前天气 - 北京")
    public void testGetCurrentWeatherBeijing() {
        MCPToolRequest request = MCPToolRequest.builder()
                .toolId("weather_query")
                .parameters(Map.of(
                        "city", "北京",
                        "queryType", "current"
                ))
                .build();

        MCPToolResponse response = weatherExecutor.execute(request);

        assertTrue(response.isSuccess(), "调用应该成功");
        assertNotNull(response.getTextResult(), "结果不应为空");
        assertTrue(response.getTextResult().contains("北京"), "结果应包含城市名");
        log.info("当前天气响应: {}", response.getTextResult());
    }

    @Test
    @DisplayName("测试查询天气预报 - 上海3天")
    public void testGetForecastShanghai() {
        MCPToolRequest request = MCPToolRequest.builder()
                .toolId("weather_query")
                .parameters(Map.of(
                        "city", "上海",
                        "queryType", "forecast",
                        "days", 3
                ))
                .build();

        MCPToolResponse response = weatherExecutor.execute(request);

        assertTrue(response.isSuccess());
        assertTrue(response.getTextResult().contains("上海"));
        assertTrue(response.getTextResult().contains("预报")
                || response.getTextResult().contains("天气"));
        log.info("天气预报响应: {}", response.getTextResult());
    }

    @Test
    @DisplayName("测试通过 Registry 的 getExecutor 调用工具")
    public void testCallViaRegistry() {
        MCPToolRequest request = MCPToolRequest.builder()
                .toolId("weather_query")
                .parameters(Map.of("city", "深圳"))
                .build();

        MCPToolResponse response = toolRegistry.getExecutor("weather_query")
                .orElseThrow(() -> new AssertionError("weather_query 工具未注册"))
                .execute(request);

        assertTrue(response.isSuccess());
        log.info("通过 Registry 调用结果: {}", response.getTextResult());
    }

    @Test
    @DisplayName("测试缺少参数")
    public void testMissingCityParameter() {
        MCPToolRequest request = MCPToolRequest.builder()
                .toolId("weather_query")
                .parameters(Map.of())
                .build();

        MCPToolResponse response = weatherExecutor.execute(request);

        assertFalse(response.isSuccess(), "缺少参数应该失败");
        assertEquals("INVALID_PARAMS", response.getErrorCode());
        log.info("错误信息: {}", response.getErrorMessage());
    }

    @Configuration
    static class Config {

        @Bean
        public WeatherMCPExecutor weatherMCPExecutor() {
            return new WeatherMCPExecutor();
        }

        @Bean
        @Primary
        public MCPToolRegistry mcpToolRegistry(WeatherMCPExecutor weatherMCPExecutor) {
            return new DefaultMCPToolRegistry(List.of(weatherMCPExecutor));
        }
    }
}
