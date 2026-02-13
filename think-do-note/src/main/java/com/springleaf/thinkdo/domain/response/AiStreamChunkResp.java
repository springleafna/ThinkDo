package com.springleaf.thinkdo.domain.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI流式响应块
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiStreamChunkResp {

    /**
     * 增量内容（仅在数据块时有值）
     */
    private String delta;

    /**
     * 是否完成
     */
    private Boolean done;

    /**
     * 是否为HTML格式（仅done=true时有意义）
     */
    private Boolean isHtml;

    /**
     * 创建数据块（包含增量内容）
     */
    public static AiStreamChunkResp data(String delta) {
        return new AiStreamChunkResp(delta, false, null);
    }

    /**
     * 创建结束块（表示流式输出完成）
     */
    public static AiStreamChunkResp done(boolean isHtml) {
        return new AiStreamChunkResp(null, true, isHtml);
    }
}
