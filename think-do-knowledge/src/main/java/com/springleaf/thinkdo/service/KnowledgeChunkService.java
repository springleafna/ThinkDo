package com.springleaf.thinkdo.service;

import com.springleaf.thinkdo.domain.request.KnowledgeChunkCreateRequest;

import java.util.List;

/**
 * 知识库分片服务接口
 */
public interface KnowledgeChunkService {

    /**
     * 根据文档 ID 查询是否已存在分片
     *
     * @param docId 文档 ID
     * @return 如果存在分片返回 true，否则返回 false
     */
    Boolean existsByDocId(String docId);

    /**
     * 删除指定文档的所有分片
     *
     * @param docId 文档 ID
     */
    void deleteByDocId(String docId);

    /**
     * 批量新增文档分片（默认不写入向量库）
     *
     * @param docId         文档 ID
     * @param requestParams 批量新增分片请求参数列表
     */
    void batchCreate(String docId, List<KnowledgeChunkCreateRequest> requestParams, Long userId);

    /**
     * 批量新增文档分片（可选同步写入向量库）
     *
     * @param docId         文档 ID
     * @param requestParams 批量新增分片请求参数列表
     * @param writeVector   是否同步写入向量库
     */
    void batchCreate(String docId, List<KnowledgeChunkCreateRequest> requestParams, boolean writeVector, Long userId);

}
