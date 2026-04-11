package com.springleaf.thinkdo.executor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

/**
 * Kroki 公共云服务连通性测试（独立运行，不依赖 Spring/Lombok）
 * 验证 POST Mermaid 语法到 kroki.io 能否正确返回 PNG/SVG 图片
 */
public class KrokiConnectivityTest {

    private static final String KROKI_BASE_URL = "https://kroki.io";

    public static void main(String[] args) throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        System.out.println("========== 测试1: Mermaid -> PNG ==========");
        testMermaidToPng(httpClient);

        System.out.println("\n========== 测试2: Mermaid -> SVG ==========");
        testMermaidToSvg(httpClient);

        System.out.println("\n========== 测试3: JSON 方式调用 ==========");
        testKrokiJsonFormat(httpClient);

        System.out.println("\n========== 测试4: 错误语法处理 ==========");
        testInvalidMermaid(httpClient);

        System.out.println("\n========== 所有测试完成 ==========");
    }

    static void testMermaidToPng(HttpClient httpClient) throws Exception {
        String mermaid = """
                graph TD
                    A[用户访问] --> B{已登录?}
                    B -->|是| C[首页]
                    B -->|否| D[登录页]
                    D --> E[输入账号密码]
                    E --> F{验证通过?}
                    F -->|是| C
                    F -->|否| D
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(KROKI_BASE_URL + "/mermaid/png"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(mermaid))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        assert200(response.statusCode());
        assertNotNull(response.body());

        byte[] body = response.body();
        // 校验 PNG 文件头: 89 50 4E 47
        if (body[0] != (byte) 0x89 || body[1] != 0x50 || body[2] != 0x4E || body[3] != 0x47) {
            throw new AssertionError("返回数据不是有效的 PNG 文件");
        }

        System.out.println("[PASS] PNG 图片大小: " + body.length + " bytes");
        System.out.println("[PASS] Content-Type: " + response.headers().firstValue("Content-Type").orElse("unknown"));

        String base64 = Base64.getEncoder().encodeToString(body);
        System.out.println("[PASS] Base64 前100字符: " + base64.substring(0, Math.min(100, base64.length())) + "...");
    }

    static void testMermaidToSvg(HttpClient httpClient) throws Exception {
        String mermaid = """
                sequenceDiagram
                    participant 用户
                    participant 前端
                    participant 后端
                    participant 数据库
                    用户->>前端: 点击登录
                    前端->>后端: POST /login
                    后端->>数据库: 查询用户
                    数据库-->>后端: 返回用户信息
                    后端-->>前端: 返回 Token
                    前端-->>用户: 登录成功
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(KROKI_BASE_URL + "/mermaid/svg"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(mermaid))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assert200(response.statusCode());
        if (!response.body().contains("<svg") || !response.body().contains("</svg>")) {
            throw new AssertionError("返回数据不是有效的 SVG");
        }

        System.out.println("[PASS] SVG 内容大小: " + response.body().length() + " bytes");
        System.out.println("[PASS] SVG 前200字符: " + response.body().substring(0, Math.min(200, response.body().length())));
    }

    static void testKrokiJsonFormat(HttpClient httpClient) throws Exception {
        String jsonBody = """
                {
                  "diagram_source": "graph LR\\n    A[开始] --> B[处理] --> C[结束]",
                  "diagram_type": "mermaid",
                  "output_format": "png"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(KROKI_BASE_URL + "/"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        assert200(response.statusCode());
        if (response.body().length == 0) {
            throw new AssertionError("图片数据不应为空");
        }

        System.out.println("[PASS] JSON 方式调用成功，图片大小: " + response.body().length + " bytes");
    }

    static void testInvalidMermaid(HttpClient httpClient) throws Exception {
        String invalidMermaid = "this is not valid mermaid syntax !!!";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(KROKI_BASE_URL + "/mermaid/png"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(invalidMermaid))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() < 400) {
            throw new AssertionError("错误语法应返回 4xx 或 5xx，实际: " + response.statusCode());
        }

        System.out.println("[PASS] 错误语法响应状态码: " + response.statusCode());
    }

    private static void assert200(int statusCode) {
        if (statusCode != 200) {
            throw new AssertionError("期望状态码 200，实际: " + statusCode);
        }
    }

    private static void assertNotNull(Object obj) {
        if (obj == null) {
            throw new AssertionError("对象不应为 null");
        }
    }
}
