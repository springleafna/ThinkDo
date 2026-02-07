package com.springleaf.thinkdo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.springleaf.thinkdo.domain.entity.PlanEntity;
import com.springleaf.thinkdo.domain.request.AiCreatePlanReq;
import com.springleaf.thinkdo.domain.request.CreatePlanReq;
import com.springleaf.thinkdo.domain.request.CreateQuadrantPlanReq;
import com.springleaf.thinkdo.domain.request.PlanQueryReq;
import com.springleaf.thinkdo.domain.request.UpdatePlanReq;
import com.springleaf.thinkdo.domain.response.PlanInfoResp;
import com.springleaf.thinkdo.domain.response.PlanQuadrantResp;

import java.util.List;

/**
 * 计划Service
 */
public interface PlanService extends IService<PlanEntity> {

    /**
     * 创建计划
     * @param createPlanReq 创建计划请求
     * @return 计划ID
     */
    Long createPlan(CreatePlanReq createPlanReq);

    /**
     * AI创建计划
     * @param aiCreatePlanReq AI创建计划请求
     * @return 计划ID
     */
    Long aiCreatePlan(AiCreatePlanReq aiCreatePlanReq);

    /**
     * 创建四象限计划
     * @param createQuadrantPlanReq 创建四象限计划请求
     * @return 计划ID
     */
    Long createQuadrantPlan(CreateQuadrantPlanReq createQuadrantPlanReq);

    /**
     * 更新计划
     * @param updatePlanReq 更新计划请求
     */
    void updatePlan(UpdatePlanReq updatePlanReq);

    /**
     * 删除计划
     * @param id 计划ID
     */
    void deletePlan(Long id);

    /**
     * 获取计划详情
     * @param id 计划ID
     * @return 计划信息
     */
    PlanInfoResp getPlanById(Long id);

    /**
     * 获取当前用户的计划列表
     * @param queryReq 查询条件
     * @return 计划列表
     */
    List<PlanInfoResp> getPlanList(PlanQueryReq queryReq);

    /**
     * 根据分类ID获取计划列表
     * @param categoryId 分类ID
     * @return 计划列表
     */
    List<PlanInfoResp> getPlanListByCategoryId(Long categoryId);

    /**
     * 切换计划完成状态
     * @param id 计划ID
     */
    void toggleStatus(Long id);

    /**
     * 获取四象限计划列表
     * @return 四象限计划
     */
    PlanQuadrantResp getQuadrantPlans();
}
