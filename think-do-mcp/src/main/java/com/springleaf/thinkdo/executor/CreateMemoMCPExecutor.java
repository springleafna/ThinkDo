package com.springleaf.thinkdo.executor;

import com.springleaf.thinkdo.core.MCPToolDefinition;
import com.springleaf.thinkdo.core.MCPToolExecutor;
import com.springleaf.thinkdo.core.MCPToolRequest;
import com.springleaf.thinkdo.core.MCPToolResponse;
import com.springleaf.thinkdo.entity.Memo;
import com.springleaf.thinkdo.mapper.MemoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 工具：创建便签
 * <p>
 * 触发场景：用户说「记录一下灵感」「写一个便签」「帮我记下来」等
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateMemoMCPExecutor implements MCPToolExecutor {

    private static final String TOOL_ID = "create_memo";

    private static final List<String> VALID_COLORS =
            List.of("#fef9e3", "#f4f8fe", "#f9f3ff", "#fff1f2", "#e8fcf2");

    private final MemoMapper memoMapper;

    @Override
    public MCPToolDefinition getToolDefinition() {
        Map<String, MCPToolDefinition.ParameterDef> params = new LinkedHashMap<>();
        params.put("userId", MCPToolDefinition.ParameterDef.builder()
                .type("string")
                .description("用户ID")
                .required(true)
                .build());
        params.put("title", MCPToolDefinition.ParameterDef.builder()
                .type("string")
                .description("便签标题，可选，根据内容自动生成")
                .required(false)
                .build());
        params.put("content", MCPToolDefinition.ParameterDef.builder()
                .type("string")
                .description("便签内容")
                .required(true)
                .build());
        params.put("tag", MCPToolDefinition.ParameterDef.builder()
                .type("string")
                .description("便签标签，可选")
                .required(false)
                .build());
        params.put("backgroundColor", MCPToolDefinition.ParameterDef.builder()
                .type("string")
                .description("背景颜色，可选值：#fef9e3(黄), #f4f8fe(蓝), #f9f3ff(紫), #fff1f2(红), #e8fcf2(绿)，默认 #fef9e3")
                .required(false)
                .defaultValue("#fef9e3")
                .enumValues(VALID_COLORS)
                .build());

        return MCPToolDefinition.builder()
                .toolId(TOOL_ID)
                .description("为用户创建一条便签。当用户说'记录一下'、'写个便签'、'帮我记下这个灵感'、'记一下'等时调用此工具。")
                .parameters(params)
                .requireUserId(true)
                .build();
    }

    @Override
    public MCPToolResponse execute(MCPToolRequest request) {
        String userIdStr = request.getStringParameter("userId");
        if (userIdStr == null || userIdStr.isBlank()) {
            return MCPToolResponse.error(TOOL_ID, "INVALID_PARAMS", "缺少必要参数: userId");
        }

        String content = request.getStringParameter("content");
        if (content == null || content.isBlank()) {
            return MCPToolResponse.error(TOOL_ID, "INVALID_PARAMS", "缺少必要参数: content");
        }

        long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            return MCPToolResponse.error(TOOL_ID, "INVALID_PARAMS", "userId 格式错误，必须为数字");
        }

        String title = request.getStringParameter("title");
        String tag = request.getStringParameter("tag");
        String backgroundColor = request.getStringParameter("backgroundColor");
        if (backgroundColor == null || !VALID_COLORS.contains(backgroundColor)) {
            backgroundColor = "#fef9e3";
        }

        try {
            Memo memo = new Memo();
            memo.setUserId(userId);
            memo.setTitle(title);
            memo.setContent(content);
            memo.setTag(tag);
            memo.setBackgroundColor(backgroundColor);
            memo.setPinned(0);

            memoMapper.insert(memo);

            String result = String.format("便签已创建成功！\n标题：%s\n内容：%s",
                    title != null ? title : "（无标题）", content);
            return MCPToolResponse.success(TOOL_ID, result,
                    Map.of("memoId", memo.getId()));
        } catch (Exception e) {
            log.error("创建便签失败, userId={}", userId, e);
            return MCPToolResponse.error(TOOL_ID, "DB_ERROR", "创建便签失败: " + e.getMessage());
        }
    }
}
