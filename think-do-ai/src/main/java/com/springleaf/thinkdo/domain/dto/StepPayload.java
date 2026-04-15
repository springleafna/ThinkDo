package com.springleaf.thinkdo.domain.dto;

/**
 * SSE 步骤进度事件数据
 *
 * @param step    步骤标识（如 rewrite, intent, retrieve, mcp）
 * @param message 步骤描述（如 "正在进行意图识别..."）
 */
public record StepPayload(String step, String message) {
}
