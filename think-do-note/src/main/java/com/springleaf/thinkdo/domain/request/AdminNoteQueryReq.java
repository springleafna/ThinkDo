package com.springleaf.thinkdo.domain.request;

import com.springleaf.thinkdo.common.PageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理员-笔记查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminNoteQueryReq extends PageReq {

    /**
     * 用户ID筛选
     */
    private Long userId;

    /**
     * 用户名模糊搜索
     */
    private String username;

    /**
     * 关键词搜索（标题/内容）
     */
    private String keyword;

    /**
     * 收藏状态筛选（0-否，1-是）
     */
    private Integer favorited;
}
