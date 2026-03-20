package com.springleaf.thinkdo.service;

import com.springleaf.thinkdo.domain.response.IntentNodeTreeResp;

import java.util.List;

public interface IntentNodeService {

    /**
     * 查询整棵意图树（包含 RAG + SYSTEM）
     */
    List<IntentNodeTreeResp> getFullTree();

}
