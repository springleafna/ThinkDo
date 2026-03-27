package com.springleaf.thinkdo.domain.request;

import lombok.Data;

import java.util.List;

@Data
public class IntentNodeCreateReq {

    /**
     * 知识库 ID
     */
    private Long kbId;

    /**
     * 业务唯一标识，如 group-hr / biz-oa-intro
     */
    private String intentCode;

    /**
     * 知识库范围，仅当kind=0时有效
     */
    private String scope;

    /**
     * 展示名称
     */
    private String name;

    /**
     * 层级：0=DOMAIN,1=CATEGORY,2=TOPIC
     */
    private Integer level;

    /**
     * 父节点的 intent_code
     */
    private String parentCode;

    /**
     * 描述
     */
    private String description;

    /**
     * 示例问题列表
     */
    private List<String> examples;

    /**
     * Milvus Collection 名称（仅对 kind=0 有意义）
     */
    private String collectionName;

    /**
     * MCP 工具 ID（仅对 kind=2 有意义）
     */
    private String mcpToolId;

    /**
     * 节点级检索 TopK（可选）
     */
    private Integer topK;

    /**
     * 类型：0=KB(RAG)，1=SYSTEM，2=MCP
     */
    private Integer kind;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 短规则片段（可选）
     */
    private String promptSnippet;

    /**
     * 场景用的完整 Prompt 模板（可选）
     */
    private String promptTemplate;

    /**
     * 参数提取提示词模板（MCP模式专属）
     */
    private String paramPromptTemplate;
}
