package com.springleaf.thinkdo.domain.dto;

import com.springleaf.thinkdo.intent.NodeScore;

import java.util.List;

/**
 * 意图分组（MCP 与 KB）
 *
 * @param mcpIntents MCP 意图列表
 * @param kbIntents  KB 意图列表
 */
public record IntentGroup(List<NodeScore> mcpIntents, List<NodeScore> kbIntents) {
}
