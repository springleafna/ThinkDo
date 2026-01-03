package com.springleaf.thinkdo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.springleaf.thinkdo.domain.entity.PlanStepEntity;
import com.springleaf.thinkdo.domain.request.CreatePlanStepReq;
import com.springleaf.thinkdo.domain.request.UpdatePlanStepReq;
import com.springleaf.thinkdo.domain.response.PlanStepInfoResp;

import java.util.List;

/**
 * 计划步骤Service
 */
public interface PlanStepService extends IService<PlanStepEntity> {

    /**
     * 创建计划步骤
     * @param createPlanStepReq 创建计划步骤请求
     * @return 步骤ID
     */
    Long createStep(CreatePlanStepReq createPlanStepReq);

    /**
     * 更新计划步骤
     * @param updatePlanStepReq 更新计划步骤请求
     */
    void updateStep(UpdatePlanStepReq updatePlanStepReq);

    /**
     * 删除计划步骤
     * @param id 步骤ID
     */
    void deleteStep(Long id);

    /**
     * 获取计划步骤详情
     * @param id 步骤ID
     * @return 步骤信息
     */
    PlanStepInfoResp getStepById(Long id);

    /**
     * 获取指定计划的所有步骤列表
     * @param planId 计划ID
     * @return 步骤列表
     */
    List<PlanStepInfoResp> getStepListByPlanId(Long planId);

    /**
     * 切换步骤完成状态
     * @param id 步骤ID
     */
    void toggleStatus(Long id);
}
