package com.springleaf.thinkdo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.springleaf.thinkdo.domain.request.KnowledgeBaseCreateReq;
import com.springleaf.thinkdo.domain.request.KnowledgeBasePageReq;
import com.springleaf.thinkdo.domain.request.KnowledgeBaseUpdateReq;
import com.springleaf.thinkdo.domain.response.KnowledgeBaseResp;

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
}
