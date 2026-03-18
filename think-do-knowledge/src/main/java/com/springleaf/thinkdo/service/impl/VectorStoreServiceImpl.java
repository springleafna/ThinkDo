package com.springleaf.thinkdo.service.impl;

import cn.hutool.core.lang.Assert;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.springleaf.thinkdo.config.RAGDefaultProperties;
import com.springleaf.thinkdo.document.chunk.VectorChunk;
import com.springleaf.thinkdo.domain.dto.VectorSpaceId;
import com.springleaf.thinkdo.domain.dto.VectorSpaceSpec;
import com.springleaf.thinkdo.domain.entity.KnowledgeBaseEntity;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.KnowledgeBaseMapper;
import com.springleaf.thinkdo.service.VectorStoreService;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.response.DeleteResp;
import io.milvus.v2.service.vector.response.InsertResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreServiceImpl implements VectorStoreService {

    private final MilvusClientV2 milvusClient;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final RAGDefaultProperties ragDefaultProperties;

    @Override
    public void indexDocumentChunks(String kbId, String docId, List<VectorChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            throw new BusinessException("文档分块不允许为空");
        }

        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new BusinessException("知识库不存在");
        }

        // 维度校验（你的 schema dim=4096）
        final int dim = 4096;
        List<float[]> vectors = extractVectors(chunks, dim);

        List<JsonObject> rows = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            VectorChunk chunk = chunks.get(i);

            String content = chunk.getContent() == null ? "" : chunk.getContent();
            if (content.length() > 65535) {
                content = content.substring(0, 65535);
            }

            JsonObject metadata = new JsonObject();
            metadata.addProperty("kb_id", kbId);
            metadata.addProperty("doc_id", docId);
            metadata.addProperty("chunk_index", chunk.getIndex());

            JsonObject row = new JsonObject();
            row.addProperty("doc_id", chunk.getChunkId());
            row.addProperty("content", content);
            row.add("metadata", metadata);
            row.add("embedding", toJsonArray(vectors.get(i)));

            rows.add(row);
        }

        String collection = kb.getCollectionName();
        InsertReq req = InsertReq.builder()
                .collectionName(collection)
                .data(rows)
                .build();

        InsertResp resp = milvusClient.insert(req);
        log.info("Milvus chunk 建立/写入向量索引成功, collection={}, rows={}", collection, resp.getInsertCnt());
    }

    private JsonArray toJsonArray(float[] v) {
        JsonArray arr = new JsonArray(v.length);
        for (float x : v) {
            arr.add(x);
        }
        return arr;
    }

    private List<float[]> extractVectors(List<VectorChunk> chunks, int expectedDim) {
        List<float[]> vectors = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            VectorChunk chunk = chunks.get(i);
            float[] vector = extractVector(chunk, expectedDim);
            vectors.add(vector);
        }
        return vectors;
    }

    private float[] extractVector(VectorChunk chunk, int expectedDim) {
        float[] vector = chunk.getEmbedding();
        if (vector == null || vector.length == 0) {
            throw new BusinessException("向量不能为空");
        }
        if (vector.length != expectedDim) {
            throw new BusinessException("向量维度不匹配，期望维度为 " + expectedDim);
        }
        return vector;
    }

    @Override
    public void updateChunk(String kbId, String docId, VectorChunk chunk) {

    }

    @Override
    public void deleteDocumentVectors(String kbId, String docId) {
        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new BusinessException("知识库不存在");
        }

        String collection = kb.getCollectionName();

        // 按 JSON 过滤：删除该 kbId 下、该文档ID 的所有 chunk
        String filter = "metadata[\"kb_id\"] == \"" + kbId + "\" && " +
                "metadata[\"doc_id\"] == \"" + docId + "\"";

        DeleteReq deleteReq = DeleteReq.builder()
                .collectionName(collection)
                .filter(filter)
                .build();

        DeleteResp resp = milvusClient.delete(deleteReq);
        log.info("Milvus 删除指定文档的所有 chunk 向量索引成功, collection={}, kbId={}, docId={}, deleteCnt={}",
                collection, kbId, docId, resp.getDeleteCnt());
    }

    @Override
    public void deleteChunkById(String kbId, String chunkId) {

    }

    @Override
    public void ensureVectorSpace(VectorSpaceSpec spec) {
        String logicalName = spec.getSpaceId().getLogicalName();
        boolean exists = Boolean.TRUE.equals(milvusClient.hasCollection(
                HasCollectionReq.builder().collectionName(logicalName).build()
        ));
        if (exists) {
            throw new BusinessException("向量集合已存在，禁止重复创建：" + logicalName);
        }

        // 定义集合的字段 schema
        List<CreateCollectionReq.FieldSchema> fieldSchemaList = new ArrayList<>();

        // 主键字段：文档 ID，变长字符串类型
        fieldSchemaList.add(
                CreateCollectionReq.FieldSchema.builder()
                        .name("doc_id")
                        .dataType(DataType.VarChar)
                        .maxLength(36)
                        .isPrimaryKey(true)
                        .autoID(false)
                        .build()
        );

        // 内容字段：存储分块文本内容
        fieldSchemaList.add(
                CreateCollectionReq.FieldSchema.builder()
                        .name("content")
                        .dataType(DataType.VarChar)
                        .maxLength(65535)
                        .build()
        );

        // 元数据字段：JSON 格式存储额外信息
        fieldSchemaList.add(
                CreateCollectionReq.FieldSchema.builder()
                        .name("metadata")
                        .dataType(DataType.JSON)
                        .build()
        );

        // 向量字段：浮点型向量，维度由配置决定
        fieldSchemaList.add(
                CreateCollectionReq.FieldSchema.builder()
                        .name("embedding")
                        .dataType(DataType.FloatVector)
                        .dimension(ragDefaultProperties.getDimension())
                        .build()
        );

        // 构建集合 schema
        CreateCollectionReq.CollectionSchema collectionSchema = CreateCollectionReq.CollectionSchema
                .builder()
                .fieldSchemaList(fieldSchemaList)
                .build();

        // 配置 HNSW 索引参数，用于向量相似度检索
        IndexParam hnswIndex = IndexParam.builder()
                .fieldName("embedding")
                .indexType(IndexParam.IndexType.HNSW)
                .metricType(IndexParam.MetricType.COSINE)
                .indexName("embedding")
                .extraParams(Map.of(
                        "M", "48",
                        "efConstruction", "200",
                        "mmap.enabled", "false"
                ))
                .build();

        // 构建创建集合的请求
        CreateCollectionReq createReq = CreateCollectionReq.builder()
                .collectionName(logicalName)
                .collectionSchema(collectionSchema)
                .primaryFieldName("doc_id")
                .vectorFieldName("embedding")
                .metricType(ragDefaultProperties.getMetricType())
                .consistencyLevel(ConsistencyLevel.BOUNDED)
                .indexParams(List.of(hnswIndex))
                .description(spec.getRemark())
                .build();

        milvusClient.createCollection(createReq);
    }

    @Override
    public boolean vectorSpaceExists(VectorSpaceId spaceId) {
        String logicalName = spaceId.getLogicalName();
        return milvusClient.hasCollection(
                HasCollectionReq.builder().collectionName(logicalName).build()
        );
    }
}
