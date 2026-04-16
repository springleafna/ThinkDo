package com.springleaf.thinkdo.domain.response;

import lombok.Data;

/**
 * 管理员-今日趋势响应
 */
@Data
public class AdminDashboardTrendResp {

    /**
     * 今日用户注册数
     */
    private Long userRegisterCount;

    /**
     * 用户注册数对比昨日变化（百分比字符串，如 "+12%" 或 "-5%"）
     */
    private String userRegisterCompare;

    /**
     * 今日新建会话数
     */
    private Long conversationCreateCount;

    /**
     * 新建会话数对比昨日变化
     */
    private String conversationCreateCompare;

    /**
     * 今日文档上传数
     */
    private Long documentUploadCount;

    /**
     * 文档上传数对比昨日变化
     */
    private String documentUploadCompare;

    /**
     * 今日新增笔记和计划数
     */
    private Long contentCreateCount;

    /**
     * 新增笔记和计划数对比昨日变化
     */
    private String contentCreateCompare;
}
