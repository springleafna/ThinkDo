package com.springleaf.thinkdo.domain.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员-知识库列表项响应
 */
@Data
public class AdminKnowledgeBaseInfoResp {

    private String id;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 作用域（SYSTEM/USER）
     */
    private String scope;

    /**
     * 嵌入模型
     */
    private String embeddingModel;

    /**
     * Collection 名称
     */
    private String collectionName;

    /**
     * 文档数量
     */
    private Long documentCount;

    /**
     * 创建人用户ID
     */
    private Long createdBy;

    /**
     * 创建人用户名
     */
    private String username;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
