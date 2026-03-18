package com.springleaf.thinkdo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.springleaf.thinkdo.domain.request.KnowledgeDocumentUpdateReq;
import com.springleaf.thinkdo.domain.request.KnowledgeDocumentUploadReq;
import com.springleaf.thinkdo.domain.response.KnowledgeDocumentResp;
import com.springleaf.thinkdo.domain.response.KnowledgeDocumentSearchResp;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库文档服务接口
 */
public interface KnowledgeDocumentService {

    /**
     * 上传文档
     *
     * @param kbId         知识库 ID
     * @param requestParam 请求对象参数
     * @param file         待上传的文件
     * @return 知识库文档视图对象
     */
    KnowledgeDocumentResp upload(String kbId, KnowledgeDocumentUploadReq requestParam, MultipartFile file);

    /**
     * 开始文档分片处理
     *
     * @param docId 文档 ID
     */
    void startChunk(String docId);

    /**
     * 删除文档
     *
     * @param docId 文档 ID
     */
    void delete(String docId);

    /**
     * 获取文档详情
     *
     * @param docId 文档 ID
     * @return 知识库文档视图对象
     */
    KnowledgeDocumentResp get(String docId);

    /**
     * 更新文档信息
     *
     * @param docId        文档 ID
     * @param requestParam 更新请求参数
     */
    void update(String docId, KnowledgeDocumentUpdateReq requestParam);

    /**
     * 分页查询文档
     *
     * @param kbId    知识库 ID
     * @param page    分页参数
     * @param status  状态筛选
     * @param keyword 关键词搜索
     * @return 文档分页结果
     */
    IPage<KnowledgeDocumentResp> page(String kbId, Page<KnowledgeDocumentResp> page, String status, String keyword);

    /**
     * 启用或禁用文档
     *
     * @param docId   文档 ID
     * @param enabled 是否启用
     */
    void enable(String docId, boolean enabled);

    /**
     * 搜索文档（用于全局检索建议）
     *
     * @param keyword 关键词
     * @param limit   最大返回数量
     * @return 文档列表
     */
    List<KnowledgeDocumentSearchResp> search(String keyword, int limit);
}
