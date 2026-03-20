package com.springleaf.thinkdo.domain.response;

import com.springleaf.thinkdo.enums.KnowledgeScopeEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库前端返回对象
 */
@Data
public class KnowledgeBaseResp {

    /**
     * 知识库ID
     */
    private String id;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 知识库作用域
     */
    private KnowledgeScopeEnum scope;

    /**
     * 嵌入模型标识
     */
    private String embeddingModel;

    /**
     * Milvus Collection 名称
     */
    private String collectionName;

    /**
     * 文档数量
     */
    private Long documentCount;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
