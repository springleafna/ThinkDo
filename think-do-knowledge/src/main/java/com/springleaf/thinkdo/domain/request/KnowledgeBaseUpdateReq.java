package com.springleaf.thinkdo.domain.request;

import lombok.Data;

@Data
public class KnowledgeBaseUpdateReq {

    private String id;

    /**
     * 知识库名称（可修改）
     */
    private String name;

    /**
     * 嵌入模型（有文档分块后禁止修改）
     */
    private String embeddingModel;
}
