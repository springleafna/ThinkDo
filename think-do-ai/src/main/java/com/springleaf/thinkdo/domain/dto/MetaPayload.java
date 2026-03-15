package com.springleaf.thinkdo.domain.dto;

/**
 * 流式对话时的第一个data:
 * @param conversationId 会话ID
 * @param taskId 对话任务ID用于停止本次对话
 */
public record MetaPayload(String conversationId, String taskId) {
}
