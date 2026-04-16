package com.springleaf.thinkdo.domain.response;

import lombok.Data;

/**
 * 管理员-运营总览统计响应
 */
@Data
public class AdminDashboardStatsResp {

    /**
     * 用户总数
     */
    private Long userTotal;

    /**
     * 活跃会话数
     */
    private Long conversationTotal;

    /**
     * 知识库文档总数
     */
    private Long documentTotal;

    /**
     * 笔记总数
     */
    private Long noteTotal;

    /**
     * 计划总数
     */
    private Long planTotal;

    /**
     * 便签总数
     */
    private Long memoTotal;
}
