package com.springleaf.thinkdo.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建会话请求
 */
@Data
public class CreateConversationReq {

    /**
     * 会话标题
     */
    @NotBlank(message = "会话标题不能为空")
    private String title;
}
