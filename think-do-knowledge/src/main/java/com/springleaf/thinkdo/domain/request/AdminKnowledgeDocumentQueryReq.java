package com.springleaf.thinkdo.domain.request;

import com.springleaf.thinkdo.common.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理员-文档查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminKnowledgeDocumentQueryReq extends PageReq {

    /**
     * 知识库ID筛选
     */
    private Long kbId;

    /**
     * 关键词搜索（文档名称模糊匹配）
     */
    private String keyword;

    /**
     * 状态筛选（pending/running/failed/success）
     */
    private String status;
}
