package com.springleaf.thinkdo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.springleaf.thinkdo.common.PageResp;
import com.springleaf.thinkdo.domain.request.AdminKnowledgeBaseQueryReq;
import com.springleaf.thinkdo.domain.request.KnowledgeBaseCreateReq;
import com.springleaf.thinkdo.domain.request.KnowledgeBasePageReq;
import com.springleaf.thinkdo.domain.request.KnowledgeBaseUpdateReq;
import com.springleaf.thinkdo.domain.response.AdminKnowledgeBaseInfoResp;
import com.springleaf.thinkdo.domain.response.KnowledgeBaseResp;
import com.springleaf.thinkdo.domain.response.KnowledgeStatisticsResp;

/**
 * 知识库服务接口
 */
public interface KnowledgeBaseService {

    /**
     * 创建知识库
     *
     * @param requestParam 创建知识库请求参数
     * @return 知识库ID
     */
    String create(KnowledgeBaseCreateReq requestParam);

    /**
     * 更新知识库
     *
     * @param requestParam 更新知识库请求参数
     */
    void update(KnowledgeBaseUpdateReq requestParam);

    /**
     * 重命名知识库
     *
     * @param kbId         知识库ID
     * @param requestParam 重命名请求参数
     */
    void rename(String kbId, KnowledgeBaseUpdateReq requestParam);

    /**
     * 删除知识库
     *
     * @param kbId 知识库ID
     */
    void delete(String kbId);

    /**
     * 根据ID查询知识库详情
     *
     * @param kbId 知识库ID
     * @return 知识库详细信息
     */
    KnowledgeBaseResp queryById(String kbId);

    /**
     * 分页查询知识库
     *
     * @param requestParam 分页查询请求参数
     * @return 知识库分页结果
     */
    IPage<KnowledgeBaseResp> pageQuery(KnowledgeBasePageReq requestParam);

    /**
     * 获取当前用户的知识库统计信息
     *
     * @return 统计信息（知识库数量和文档数量）
     */
    KnowledgeStatisticsResp getStatistics();

    /**
     * 统计所有知识库文档总数
     * @return 文档总数
     */
    Long countDocumentTotal();

    /**
     * 统计指定日期上传的文档数
     * @param date 日期
     * @return 文档数
     */
    Long countDocumentByDate(java.time.LocalDate date);

    // ==================== 管理员接口 ====================

    /**
     * 管理员-分页查询所有知识库
     */
    PageResp<AdminKnowledgeBaseInfoResp> adminListKnowledgeBases(AdminKnowledgeBaseQueryReq queryReq);

    /**
     * 管理员-获取知识库详情
     */
    AdminKnowledgeBaseInfoResp adminGetKnowledgeBaseDetail(String kbId);

    /**
     * 管理员-删除知识库（级联删除文档、分块、意图节点）
     */
    void adminDeleteKnowledgeBase(String kbId);
}
