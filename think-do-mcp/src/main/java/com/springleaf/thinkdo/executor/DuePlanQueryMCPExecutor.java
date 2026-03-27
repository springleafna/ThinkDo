package com.springleaf.thinkdo.executor;

import com.springleaf.thinkdo.core.MCPToolDefinition;
import com.springleaf.thinkdo.core.MCPToolExecutor;
import com.springleaf.thinkdo.core.MCPToolRequest;
import com.springleaf.thinkdo.core.MCPToolResponse;
import com.springleaf.thinkdo.entity.Plan;
import com.springleaf.thinkdo.mapper.PlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 工具：查询即将截止的计划
 * <p>
 * 触发场景：用户提问「我有哪些计划要截止」「最近有什么任务到期」等
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DuePlanQueryMCPExecutor implements MCPToolExecutor {

    private static final String TOOL_ID = "due_plan_query";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final PlanMapper planMapper;

    @Override
    public MCPToolDefinition getToolDefinition() {
        Map<String, MCPToolDefinition.ParameterDef> params = new LinkedHashMap<>();
        params.put("userId", MCPToolDefinition.ParameterDef.builder()
                .type("string")
                .description("用户ID")
                .required(true)
                .build());
        params.put("days", MCPToolDefinition.ParameterDef.builder()
                .type("integer")
                .description("查询未来几天内截止的计划，默认7天")
                .required(false)
                .defaultValue(7)
                .build());

        return MCPToolDefinition.builder()
                .toolId(TOOL_ID)
                .description("查询指定用户即将截止的计划列表。当用户询问'我有哪些计划要截止'、'最近有什么任务到期'、'快到期的计划'等时调用此工具。")
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

        long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            return MCPToolResponse.error(TOOL_ID, "INVALID_PARAMS", "userId 格式错误，必须为数字");
        }

        int days = 7;
        Object daysParam = request.getParameter("days");
        if (daysParam != null) {
            try {
                days = Integer.parseInt(daysParam.toString());
            } catch (NumberFormatException e) {
                log.warn("days 参数格式错误，使用默认值 7");
            }
        }

        try {
            LocalDateTime from = LocalDateTime.now();
            LocalDateTime to = from.plusDays(days);
            List<Plan> plans = planMapper.selectDueSoonPlans(userId, from, to);

            if (plans.isEmpty()) {
                return MCPToolResponse.success(TOOL_ID,
                        String.format("未来 %d 天内没有即将截止的计划。", days));
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("未来 %d 天内共有 %d 个即将截止的计划：\n\n", days, plans.size()));
            for (int i = 0; i < plans.size(); i++) {
                Plan plan = plans.get(i);
                sb.append(String.format("%d. 【%s】%s\n",
                        i + 1,
                        priorityLabel(plan.getPriority()),
                        plan.getTitle()));
                sb.append(String.format("   截止时间：%s\n",
                        plan.getDueTime().format(FORMATTER)));
                if (plan.getDescription() != null && !plan.getDescription().isBlank()) {
                    sb.append(String.format("   描述：%s\n", plan.getDescription()));
                }
                sb.append("\n");
            }

            return MCPToolResponse.success(TOOL_ID, sb.toString().trim());
        } catch (Exception e) {
            log.error("查询即将截止计划失败, userId={}", userId, e);
            return MCPToolResponse.error(TOOL_ID, "DB_ERROR", "查询失败: " + e.getMessage());
        }
    }

    private String priorityLabel(Integer priority) {
        if (priority == null) return "中";
        return switch (priority) {
            case 1 -> "低优先级";
            case 3 -> "高优先级";
            default -> "中优先级";
        };
    }
}
