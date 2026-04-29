package com.springleaf.thinkdo.domain.request;

import com.springleaf.thinkdo.common.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理员-知识库查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminKnowledgeBaseQueryReq extends PageReq {

    /**
     * 关键词搜索（知识库名称模糊匹配）
     */
    private String keyword;

    /**
     * 知识库作用域筛选（SYSTEM/USER）
     */
    private String scope;

    /**
     * 创建人用户名模糊搜索
     */
    private String username;
}
