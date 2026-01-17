package com.springleaf.thinkdo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.springleaf.thinkdo.domain.entity.MemoEntity;
import com.springleaf.thinkdo.domain.request.CreateMemoReq;
import com.springleaf.thinkdo.domain.request.MemoQueryReq;
import com.springleaf.thinkdo.domain.request.UpdateMemoReq;
import com.springleaf.thinkdo.domain.response.MemoInfoResp;

import java.util.List;

/**
 * 便签Service
 */
public interface MemoService extends IService<MemoEntity> {

    /**
     * 创建便签
     * @param createMemoReq 创建便签请求
     * @return 便签ID
     */
    Long createMemo(CreateMemoReq createMemoReq);

    /**
     * 更新便签
     * @param updateMemoReq 更新便签请求
     */
    void updateMemo(UpdateMemoReq updateMemoReq);

    /**
     * 删除便签
     * @param id 便签ID
     */
    void deleteMemo(Long id);

    /**
     * 获取便签详情
     * @param id 便签ID
     * @return 便签信息
     */
    MemoInfoResp getMemoById(Long id);

    /**
     * 获取当前用户的便签列表
     * @param queryReq 查询条件
     * @return 便签列表
     */
    List<MemoInfoResp> getMemoList(MemoQueryReq queryReq);

    /**
     * 切换便签置顶状态
     * @param id 便签ID
     */
    void togglePinned(Long id);

    /**
     * 获取最新修改的两个便签
     * @return 便签列表
     */
    List<MemoInfoResp> getLatestMemos();
}
