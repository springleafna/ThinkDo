package com.springleaf.thinkdo.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库文档分块日志实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("tb_knowledge_document_chunk_log")
public class KnowledgeDocumentChunkLogEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 文档 ID
     */
    private Long docId;

    /**
     * 执行状态：running / success / failed
     */
    private String status;

    /**
     * 分块策略
     */
    private String chunkStrategy;

    /**
     * 文本提取耗时（毫秒）
     */
    private Long extractDuration;

    /**
     * 分块耗时（毫秒）
     */
    private Long chunkDuration;

    /**
     * 向量化耗时（毫秒）
     */
    private Long embeddingDuration;

    /**
     * 总耗时（毫秒）
     */
    private Long totalDuration;

    /**
     * 生成的分块数量
     */
    private Integer chunkCount;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
