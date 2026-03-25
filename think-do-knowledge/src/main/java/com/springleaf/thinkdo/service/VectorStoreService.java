package com.springleaf.thinkdo.service;

import com.springleaf.thinkdo.document.chunk.VectorChunk;
import com.springleaf.thinkdo.domain.dto.VectorSpaceId;
import com.springleaf.thinkdo.domain.dto.VectorSpaceSpec;
import com.springleaf.thinkdo.exception.BusinessException;

import java.util.List;

/**
 * 向量存储服务接口
 */
public interface VectorStoreService {

    /**
     * 批量建立文档的向量索引
     *
     * @param kbId   知识库唯一标识
     * @param docId  文档唯一标识
     * @param chunks 文档切片列表，包含文本内容、chunk索引等信息
     * @param userId 用户ID，用于多租户隔离
     * @throws IllegalArgumentException 当参数为空或无效时抛出
     */
    void indexDocumentChunks(String kbId, String docId, List<VectorChunk> chunks, Long userId);

    /**
     * 更新单个 chunk 的向量索引
     *
     * @param kbId  知识库唯一标识
     * @param docId 文档唯一标识
     * @param chunk 待更新的文档切片，包含最新的文本内容
     * @throws IllegalArgumentException 当参数为空或无效时抛出
     */
    void updateChunk(String kbId, String docId, VectorChunk chunk);

    /**
     * 删除文档的所有向量索引
     *
     * @param kbId  知识库唯一标识
     * @param docId 文档唯一标识
     * @throws IllegalArgumentException 当参数为空或无效时抛出
     */
    void deleteDocumentVectors(String kbId, String docId);

    /**
     * 删除指定的单个 chunk 向量索引
     *
     * @param kbId    知识库唯一标识
     * @param chunkId chunk 的唯一标识
     * @throws IllegalArgumentException 当参数为空或无效时抛出
     */
    void deleteChunkById(String kbId, String chunkId);

    /**
     * 确保向量空间存在，若不存在则创建
     * 创建包含文档 ID、内容、元数据和向量字段的集合，并建立 HNSW 索引
     *
     * @param spec 向量空间规格定义，包含空间 ID 和备注信息
     * @throws BusinessException 当向量集合已存在时抛出异常
     */
    void ensureVectorSpace(VectorSpaceSpec spec);

    /**
     * 只判断存在性（不创建）
     */
    boolean vectorSpaceExists(VectorSpaceId spaceId);
}
