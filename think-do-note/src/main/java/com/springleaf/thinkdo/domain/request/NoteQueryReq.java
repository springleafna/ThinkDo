package com.springleaf.thinkdo.domain.request;

import lombok.Data;

/**
 * 笔记查询Request
 */
@Data
public class NoteQueryReq {

    /**
     * 分类ID（筛选条件，null表示所有笔记包括未分类）
     */
    private Long categoryId;

    /**
     * 关键词搜索（标题或内容）
     */
    private String keyword;

    /**
     * 是否收藏（筛选条件，0:否 1:是 null:全部）
     */
    private Integer favorited;
}
