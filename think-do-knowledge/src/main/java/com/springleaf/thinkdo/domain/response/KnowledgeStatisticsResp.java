package com.springleaf.thinkdo.domain.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库统计信息响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeStatisticsResp {

    /**
     * 知识库数量
     */
    private Integer baseCount;

    /**
     * 文档数量
     */
    private Integer documentCount;
}
