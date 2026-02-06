package com.springleaf.thinkdo.domain.response;

import lombok.Data;

import java.util.List;

/**
 * 笔记统计Response
 */
@Data
public class NoteStatisticsResp {

    /**
     * 全部笔记数量
     */
    private Integer totalCount;

    /**
     * 收藏笔记数量
     */
    private Integer favoritedCount;

    /**
     * 未分类笔记数量
     */
    private Integer unclassifiedCount;

    /**
     * 各分类笔记数量
     */
    private List<CategoryCount> categoryCounts;

    @Data
    public static class CategoryCount {
        /**
         * 分类ID
         */
        private Long categoryId;

        /**
         * 分类名称
         */
        private String categoryName;

        /**
         * 笔记数量
         */
        private Integer count;
    }
}
