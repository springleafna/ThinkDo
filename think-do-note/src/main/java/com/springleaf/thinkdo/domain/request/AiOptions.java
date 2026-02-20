package com.springleaf.thinkdo.domain.request;

import lombok.Data;

/**
 * AI转换可选参数
 */
@Data
public class AiOptions {

    /**
     * 语气：neutral, formal, casual, professional, friendly
     */
    private String tone;

    /**
     * 目标长度：light, medium, heavy
     */
    private String targetLength;

    /**
     * 语言：zh, en
     */
    private String language;

    /**
     * 是否保留标记（HTML等）
     */
    private Boolean preserveMarkup;
}
