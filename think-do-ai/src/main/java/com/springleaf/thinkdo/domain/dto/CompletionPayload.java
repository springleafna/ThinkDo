package com.springleaf.thinkdo.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 模型回复完成事件载荷
 * 返回标题是为了在会话结束后在前端更新会话的标题，因为这个会话可能是新创建的会话，第一次提问时还没有明确的标题
 *
 * @param messageId 消息ID（字符串，避免前端精度丢失）
 * @param title     会话标题（可选）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompletionPayload(String messageId, String title) {
}
