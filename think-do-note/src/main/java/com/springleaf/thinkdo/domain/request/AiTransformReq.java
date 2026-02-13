package com.springleaf.thinkdo.domain.request;

import com.springleaf.thinkdo.enums.AiActionEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI转换请求
 */
@Data
public class AiTransformReq {

    /**
     * AI操作类型
     */
    @NotNull(message = "操作类型不能为空")
    private AiActionEnum action;

    /**
     * 待处理的文本内容
     */
    @NotBlank(message = "文本内容不能为空")
    private String text;

    /**
     * 可选参数
     */
    private AiOptions options;

    /**
     * 上下文信息（标题、前后句、标签等）
     */
    private String context;
}
