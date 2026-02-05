package com.springleaf.thinkdo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.springleaf.thinkdo.domain.entity.NoteCategoryEntity;
import com.springleaf.thinkdo.domain.request.CreateNoteCategoryReq;
import com.springleaf.thinkdo.domain.request.UpdateNoteCategoryReq;
import com.springleaf.thinkdo.domain.response.NoteCategoryInfoResp;

import java.util.List;

/**
 * 笔记分类Service
 */
public interface NoteCategoryService extends IService<NoteCategoryEntity> {

    /**
     * 创建笔记分类
     *
     * @param createNoteCategoryReq 创建分类请求
     * @return 分类ID
     */
    Long createCategory(CreateNoteCategoryReq createNoteCategoryReq);

    /**
     * 更新笔记分类
     *
     * @param updateNoteCategoryReq 更新分类请求
     */
    void updateCategory(UpdateNoteCategoryReq updateNoteCategoryReq);

    /**
     * 删除笔记分类
     *
     * @param id 分类ID
     */
    void deleteCategory(Long id);

    /**
     * 获取当前用户的笔记分类列表
     *
     * @return 分类列表
     */
    List<NoteCategoryInfoResp> getCategoryList();
}
