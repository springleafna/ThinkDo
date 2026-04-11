package com.springleaf.thinkdo.executor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * 调用 Kroki 渲染 Mermaid 图表并保存 PNG/SVG 到本地查看效果
 */
public class KrokiSaveImageTest {

    private static final String KROKI_BASE_URL = "https://kroki.io";

    public static void main(String[] args) throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        Path outputDir = Path.of("think-do-mcp/target/kroki-output");
        Files.createDirectories(outputDir);

        // 测试1: 流程图 PNG
        saveImage(httpClient, outputDir, "flowchart", "png", """
                graph TD
                    A[用户访问系统] --> B{已登录?}
                    B -->|是| C[进入首页]
                    B -->|否| D[跳转登录页]
                    D --> E[输入账号密码]
                    E --> F{验证通过?}
                    F -->|是| G[生成Token]
                    F -->|否| H[提示错误]
                    H --> D
                    G --> C
                    C --> I[浏览功能]
                    I --> J[退出登录]
                """);

        // 测试2: 时序图 SVG
        saveImage(httpClient, outputDir, "sequence", "svg", """
                sequenceDiagram
                    participant U as 用户
                    participant F as 前端
                    participant G as 网关
                    participant A as 后端服务
                    participant DB as MySQL
                    participant R as Redis

                    U->>F: 点击登录
                    F->>G: POST /api/login
                    G->>A: 转发请求
                    A->>R: 查询缓存
                    R-->>A: 缓存未命中
                    A->>DB: 查询用户信息
                    DB-->>A: 返回用户数据
                    A->>A: 校验密码
                    A->>R: 缓存Token
                    A-->>G: 返回Token
                    G-->>F: 响应Token
                    F-->>U: 登录成功
                """);

        // 测试3: 类图 PNG
        saveImage(httpClient, outputDir, "class-diagram", "png", """
                classDiagram
                    class MCPToolExecutor {
                        <<interface>>
                        +getToolDefinition() MCPToolDefinition
                        +execute(request) MCPToolResponse
                        +getToolId() String
                    }
                    class WeatherMCPExecutor {
                        +getToolDefinition() MCPToolDefinition
                        +execute(request) MCPToolResponse
                    }
                    class DiagramMCPExecutor {
                        +getToolDefinition() MCPToolDefinition
                        +execute(request) MCPToolResponse
                    }
                    class MCPToolRegistry {
                        <<interface>>
                        +register(executor) void
                        +getExecutor(toolId) Optional
                        +listAllTools() List
                    }
                    class DefaultMCPToolRegistry {
                        -executors Map
                        +register(executor) void
                        +getExecutor(toolId) Optional
                    }
                    MCPToolExecutor <|.. WeatherMCPExecutor
                    MCPToolExecutor <|.. DiagramMCPExecutor
                    MCPToolRegistry <|.. DefaultMCPToolRegistry
                """);

        System.out.println("\n========================================");
        System.out.println("All images saved to: " + outputDir.toAbsolutePath());
        System.out.println("========================================");
    }

    private static void saveImage(HttpClient httpClient, Path outputDir, String name, String format, String mermaid) throws Exception {
        System.out.println("Rendering: " + name + " (" + format + ") ...");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(KROKI_BASE_URL + "/mermaid/" + format))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(mermaid))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            System.err.println("[FAIL] " + name + " status=" + response.statusCode());
            return;
        }

        Path file = outputDir.resolve(name + "." + format);
        Files.write(file, response.body());
        System.out.println("[OK] " + file.getFileName() + " (" + response.body().length + " bytes)");
    }
}
