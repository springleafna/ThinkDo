package com.springleaf.thinkdo.domain.request;

import lombok.Data;

@Data
public class IntentNodeUpdateReq {

    /**
     * 节点 ID
     */
    private Long id;

    /**
     * 展示名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 示例问题：JSON 数组字符串
     */
    private String examples;

    /**
     * MCP 工具 ID（仅对 kind=2 有意义）
     */
    private String mcpToolId;

    /**
     * 节点级检索 TopK（可选）
     * 为空时使用全局默认 TopK
     */
    private Integer topK;

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
