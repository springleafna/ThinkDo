package com.springleaf.thinkdo.domain.request;

import lombok.Data;

/**
 * 便签查询Request
 */
@Data
public class MemoQueryReq {

    /**
     * 便签标签(筛选条件)
     */
    private String tag;

    /**
     * 背景颜色(筛选条件)
     */
    private String backgroundColor;

    /**
     * 是否置顶(筛选条件, 0:否 1:是 null:全部)
     */
    private Integer pinned;

    /**
     * 关键词搜索(标题或内容)
     */
    private String keyword;
}
