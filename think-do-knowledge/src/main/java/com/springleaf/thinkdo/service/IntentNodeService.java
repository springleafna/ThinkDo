package com.springleaf.thinkdo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.springleaf.thinkdo.domain.request.IntentNodeCreateReq;
import com.springleaf.thinkdo.domain.request.IntentNodePageReq;
import com.springleaf.thinkdo.domain.request.IntentNodeUpdateReq;
import com.springleaf.thinkdo.domain.response.IntentNodeTreeResp;

import java.util.List;

public interface IntentNodeService {

    /**
     * 查询整棵意图树（包含 RAG + SYSTEM）
     */
    List<IntentNodeTreeResp> getFullTree();

    /**
     * 创建意图节点
     */
    void create(IntentNodeCreateReq requestParam);

    /**
     * 编辑意图节点
     */
    void update(IntentNodeUpdateReq requestParam);

    /**
     * 删除意图节点（级联删除子节点）
     */
    void delete(Long id);

    /**
     * 启用/禁用意图节点
     */
    void toggleEnabled(Long id);

    /**
     * 分页查询意图节点列表
     */
    IPage<IntentNodeTreeResp> pageQuery(IntentNodePageReq requestParam);
}
