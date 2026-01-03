package com.springleaf.thinkdo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.springleaf.thinkdo.domain.entity.PlanCategoryEntity;
import com.springleaf.thinkdo.domain.request.CreatePlanCategoryReq;
import com.springleaf.thinkdo.domain.request.UpdatePlanCategoryReq;
import com.springleaf.thinkdo.domain.response.PlanCategoryInfoResp;

import java.util.List;

/**
 * 计划分类Service
 */
public interface PlanCategoryService extends IService<PlanCategoryEntity> {

    /**
     * 创建计划分类
     * @param createPlanCategoryReq 创建计划分类请求
     * @return 分类ID
     */
    Long createCategory(CreatePlanCategoryReq createPlanCategoryReq);

    /**
     * 更新计划分类
     * @param updatePlanCategoryReq 更新计划分类请求
     */
    void updateCategory(UpdatePlanCategoryReq updatePlanCategoryReq);

    /**
     * 删除计划分类
     * @param id 分类ID
     */
    void deleteCategory(Long id);

    /**
     * 获取计划分类详情
     * @param id 分类ID
     * @return 分类信息
     */
    PlanCategoryInfoResp getCategoryById(Long id);

    /**
     * 获取当前用户的计划分类列表
     * @return 分类列表
     */
    List<PlanCategoryInfoResp> getCategoryList();
}
