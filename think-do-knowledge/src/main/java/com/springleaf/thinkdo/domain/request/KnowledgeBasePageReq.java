package com.springleaf.thinkdo.domain.request;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.springleaf.thinkdo.enums.KnowledgeScopeEnum;
import lombok.Data;

/**
 * 知识库分页查询请求
 */
@Data
public class KnowledgeBasePageReq extends Page {

    /**
     * 知识库名称（支持模糊匹配）
     */
    private String name;

    /**
     * 知识库作用域筛选
     */
    private KnowledgeScopeEnum scope;
}
