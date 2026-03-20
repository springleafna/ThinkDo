package com.springleaf.thinkdo.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.springleaf.thinkdo.enums.KnowledgeScopeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("tb_knowledge_base")
public class KnowledgeBaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 知识库作用域：SYSTEM-系统知识库，USER-用户知识库
     */
    private KnowledgeScopeEnum scope;

    /**
     * 嵌入模型标识，如：qwen3-embedding:8b-fp16
     */
    private String embeddingModel;

    /**
     * Milvus Collection 名称（创建后禁止修改）
     */
    private String collectionName;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 修改人
     */
    private Long updatedBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 是否删除：0-正常，1-删除
     */
    @TableLogic
    private Integer deleted;
}

