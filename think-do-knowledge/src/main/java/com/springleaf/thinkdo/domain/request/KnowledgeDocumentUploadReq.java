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
}
