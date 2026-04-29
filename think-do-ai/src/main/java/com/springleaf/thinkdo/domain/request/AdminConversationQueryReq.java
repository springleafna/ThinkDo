package com.springleaf.thinkdo.domain.request;

import com.springleaf.thinkdo.common.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 管理员-会话查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminConversationQueryReq extends PageReq {

    /**
     * 关键词搜索（标题模糊匹配）
     */
    private String keyword;

    /**
     * 用户ID筛选
     */
    private Long userId;

    /**
     * 用户名模糊搜索
     */
    private String username;

    /**
     * 创建时间范围-起始
     */
    private LocalDateTime startTime;

    /**
     * 创建时间范围-结束
     */
    private LocalDateTime endTime;
}
