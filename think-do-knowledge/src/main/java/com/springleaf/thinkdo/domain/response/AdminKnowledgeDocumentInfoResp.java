package com.springleaf.thinkdo.domain.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员-文档列表项响应
 */
@Data
public class AdminKnowledgeDocumentInfoResp {

    private String id;

    /**
     * 知识库ID
     */
    private String kbId;

    /**
     * 知识库名称
     */
    private String kbName;

    /**
     * 文档名称
     */
    private String docName;

    /**
     * 来源类型
     */
    private String sourceType;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 分块数
     */
    private Integer chunkCount;

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
     * 状态
     */
    private String status;

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
