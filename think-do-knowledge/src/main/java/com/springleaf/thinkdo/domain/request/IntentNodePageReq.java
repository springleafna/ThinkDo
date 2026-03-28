package com.springleaf.thinkdo.domain.request;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

/**
 * 意图节点分页查询请求
 */
@Data
public class IntentNodePageReq extends Page {

    /**
     * 意图名称/编码（支持模糊匹配）
     */
    private String keyword;

    /**
     * 层级：0=DOMAIN,1=CATEGORY,2=TOPIC
     */
    private Integer level;

    /**
     * 类型：0=KB(RAG)，1=SYSTEM，2=MCP
     */
    private Integer kind;

    /**
     * 启用状态：0=禁用，1=启用
     */
    private Integer enabled;
}
