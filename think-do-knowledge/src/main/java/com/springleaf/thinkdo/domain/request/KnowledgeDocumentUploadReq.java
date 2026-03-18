package com.springleaf.thinkdo.domain.request;

import lombok.Data;

@Data
public class KnowledgeDocumentUploadReq {

    /**
     * 来源类型：file / url
     */
    private String sourceType;

    /**
     * 来源位置（URL）
     */
    private String sourceLocation;

    /**
     * 分块策略：fixed_size / structure_aware
     */
    private String chunkStrategy;

    /**
     * 分块参数JSON（可选，优先于下面字段）
     */
    private String chunkConfig;

    /**
     * 固定大小分块：块大小
     */
    private Integer chunkSize;

    /**
     * 固定大小分块：重叠大小
     */
    private Integer overlapSize;

    /**
     * 结构感知：理想块大小
     */
    private Integer targetChars;

    /**
     * 结构感知：块上限
     */
    private Integer maxChars;

    /**
     * 结构感知：块下限
     */
    private Integer minChars;

    /**
     * 结构感知：重叠大小
     */
    private Integer overlapChars;
}
