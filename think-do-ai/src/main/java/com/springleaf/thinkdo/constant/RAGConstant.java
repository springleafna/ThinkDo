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

}
