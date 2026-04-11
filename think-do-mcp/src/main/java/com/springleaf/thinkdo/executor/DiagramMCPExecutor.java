package com.springleaf.thinkdo.executor;

import com.springleaf.thinkdo.core.MCPToolDefinition;
import com.springleaf.thinkdo.core.MCPToolExecutor;
import com.springleaf.thinkdo.core.MCPToolRequest;
import com.springleaf.thinkdo.core.MCPToolResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 图表生成 MCP 工具
 * <p>
 * 接收 Mermaid 语法的图表描述，调用 Kroki 渲染服务生成 PNG 图片，
 * 返回 base64 编码的图片数据，可在前端直接展示
 */
@Slf4j
@Component
public class DiagramMCPExecutor implements MCPToolExecutor {

    private static final String TOOL_ID = "diagram_generate";

    @Value("${kroki.url:https://kroki.io}")
    private String krokiUrl;

    @Value("${kroki.timeout:30}")
    private int timeoutSeconds;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public MCPToolDefinition getToolDefinition() {
        Map<String, MCPToolDefinition.ParameterDef> parameters = new LinkedHashMap<>();

        parameters.put("content", MCPToolDefinition.ParameterDef.builder()
                .description("Mermaid 语法的图表内容，例如流程图、时序图、类图等")
                .type("string")
                .required(true)
                .build());

        parameters.put("diagramType", MCPToolDefinition.ParameterDef.builder()
                .description("图表类型提示，帮助理解图表用途")
                .type("string")
                .required(false)
                .enumValues(List.of(
                        "flowchart", "sequence", "classDiagram", "stateDiagram",
                        "erDiagram", "gantt", "pie", "mindmap", "gitgraph"))
                .build());

        parameters.put("title", MCPToolDefinition.ParameterDef.builder()
                .description("图表标题，可选")
                .type("string")
                .required(false)
                .build());

        parameters.put("format", MCPToolDefinition.ParameterDef.builder()
                .description("输出图片格式，png 或 svg，默认 png")
                .type("string")
                .required(false)
                .defaultValue("png")
                .enumValues(List.of("png", "svg"))
                .build());

        return MCPToolDefinition.builder()
                .toolId(TOOL_ID)
                .description("根据 Mermaid 语法生成图表图片。支持流程图(flowchart)、时序图(sequence)、类图(classDiagram)、状态图(stateDiagram)、ER图(erDiagram)、甘特图(gantt)、饼图(pie)、思维导图(mindmap)等。"
                        + "返回 base64 编码的图片，可直接在前端通过 <img src='data:image/png;base64,...'> 展示。"
                        + "当用户要求画图、绘制图表、可视化流程时调用此工具。")
                .parameters(parameters)
                .requireUserId(false)
                .build();
    }

    @Override
    public MCPToolResponse execute(MCPToolRequest request) {
        long start = System.currentTimeMillis();
        try {
            String content = request.getStringParameter("content");
            String format = request.getStringParameter("format");
            String title = request.getStringParameter("title");

            if (content == null || content.isBlank()) {
                return MCPToolResponse.error(TOOL_ID, "INVALID_PARAMS", "请提供 Mermaid 图表内容(content参数)");
            }
            if (format == null || format.isBlank()) {
                format = "png";
            }

            log.info("开始渲染图表, format={}, content长度={}", format, content.length());

            // 调用 Kroki 渲染
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(krokiUrl + "/mermaid/" + format))
                    .header("Content-Type", "text/plain")
                    .POST(HttpRequest.BodyPublishers.ofString(content))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            HttpResponse<byte[]> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            long costMs = System.currentTimeMillis() - start;

            if (response.statusCode() != 200) {
                String errorMsg = new String(response.body());
                log.error("Kroki 渲染失败, status={}, body={}", response.statusCode(), errorMsg);
                return MCPToolResponse.error(TOOL_ID, "RENDER_FAILED",
                        "图表渲染失败(状态码:" + response.statusCode() + ")，请检查 Mermaid 语法是否正确。错误: " + errorMsg);
            }

            byte[] imageBytes = response.body();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            String mimeType = "png".equals(format) ? "image/png" : "image/svg+xml";

            // 构建返回数据
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("imageBase64", base64Image);
            data.put("mimeType", mimeType);
            data.put("imageSize", imageBytes.length);
            data.put("format", format);
            if (title != null && !title.isBlank()) {
                data.put("title", title);
            }

            String textResult = String.format("图表渲染成功！格式: %s, 大小: %d bytes, 耗时: %dms",
                    format, imageBytes.length, costMs);
            if (title != null && !title.isBlank()) {
                textResult = "【" + title + "】" + textResult;
            }

            MCPToolResponse result = MCPToolResponse.success(TOOL_ID, textResult, data);
            result.setCostMs(costMs);
            return result;

        } catch (Exception e) {
            long costMs = System.currentTimeMillis() - start;
            log.error("图表生成失败", e);
            MCPToolResponse error = MCPToolResponse.error(TOOL_ID, "EXECUTION_ERROR", "图表生成失败: " + e.getMessage());
            error.setCostMs(costMs);
            return error;
        }
    }
}
