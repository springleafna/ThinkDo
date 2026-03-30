package com.springleaf.thinkdo.constant;

/**
 * RAG 系统常量类
 */
public class RAGConstant {


    /**
     * 默认返回的 TopK
     */
    public static final int DEFAULT_TOP_K = 10;

    /**
     * 意图识别最低分数阈值
     * <p>
     * 低于这个分数就当成"聊偏了"，不参与 RAG 检索流程
     * </p>
     */
    public static final double INTENT_MIN_SCORE = 0.35;

    /**
     * 单次查询最多参与的意图数量上限
     * 防止拉取过多 Collection 导致性能问题
     */
    public static final int MAX_INTENT_COUNT = 3;

    /**
     * 多通道检索占位符键
     * <p>
     * 当没有意图识别结果时，使用此键作为 intentChunks Map 的占位符
     * 实际处理时只使用 Map 的 values，不关心具体的 key 值
     * </p>
     */
    public static final String MULTI_CHANNEL_KEY = "multi_channel";

    /**
     * 系统对话提示词模板路径
     * 定义企业知识助手「小知」的角色设定和对话规则，包括打招呼、自我介绍、问题分类处理等场景。模板通过 {@code {question}} 占位符接收用户问题。
     */
    public static final String CHAT_SYSTEM_PROMPT_PATH = "prompt/answer-chat-system.st";

    /**
     * 默认 RAG 问答提示词模板路径
     * 用于指导大模型基于检索到的文档内容进行准确回答，包含严格的事实性约束和链接处理规则
     */
    public static final String RAG_ENTERPRISE_PROMPT_PATH = "prompt/answer-chat-kb.st";

    /**
     * MCP 工具参数提取提示词模板路径
     * 用于从用户问题中提取工具调用参数
     */
    public static final String MCP_PARAMETER_EXTRACT_PROMPT_PATH = "prompt/mcp-parameter-extract.st";

    /**
     * MCP-only 场景提示词模板路径
     * 仅动态数据片段时使用
     */
    public static final String MCP_ONLY_PROMPT_PATH = "prompt/answer-chat-mcp.st";

    /**
     * MCP + KB 混合场景提示词模板路径
     * 兼顾动态数据片段与知识库内容的综合回答
     */
    public static final String MCP_KB_MIXED_PROMPT_PATH = "prompt/answer-chat-mcp-kb-mixed.st";

    /**
     * 对话记忆压缩提示词模板路径
     * 通过 {@code {summary_max_chars}} 控制摘要长度上限
     */
    public static final String CONVERSATION_SUMMARY_PROMPT_PATH = "prompt/conversation-summary.st";

}
