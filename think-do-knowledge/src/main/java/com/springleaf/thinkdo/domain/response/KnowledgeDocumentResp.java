package com.springleaf.thinkdo.domain.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档视图对象
 */
@Data
public class KnowledgeDocumentResp {

    /**
     * 文档唯一标识
     */
    private String id;

    /**
     * 知识库ID
     */
    private Long kbId;

    /**
     * 文档名称
     */
    private String docName;

    /**
     * 来源类型
     */
    private String sourceType;

    /**
     * 来源位置
     */
    private String sourceLocation;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 切片数量
     */
    private Integer chunkCount;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 分块策略
     */
    private String chunkStrategy;

    /**
     * 分块参数配置（JSON）
     */
    private String chunkConfig;

    /**
     * 状态（如：解析中、已解析、解析失败等）
     */
    private String status;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 更新人
     */
    private Long updatedBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
