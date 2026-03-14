package com.springleaf.thinkdo.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新会话请求
 */
@Data
public class UpdateConversationReq {

    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String conversationId;

    /**
     * 会话标题
     */
    @NotBlank(message = "会话标题不能为空")
    private String title;
}
