package com.springleaf.thinkdo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.springleaf.thinkdo.domain.entity.PlanExecutionEntity;
import com.springleaf.thinkdo.domain.request.CreatePlanExecutionReq;
import com.springleaf.thinkdo.domain.response.PlanExecutionInfoResp;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日清单Service
 */
public interface PlanExecutionService extends IService<PlanExecutionEntity> {

    /**
     * 创建每日清单
     * @param createPlanExecutionReq 创建每日清单请求
     * @return 每日清单ID
     */
    Long createPlanExecution(CreatePlanExecutionReq createPlanExecutionReq);

    /**
     * 删除每日清单
     * @param id 每日清单ID
     */
    void deletePlanExecution(Long id);

    /**
     * 切换每日清单状态
     * @param id 每日清单ID
     */
    void toggleStatus(Long id);

    /**
     * 获取指定日期的每日清单列表
     * @param executeDate 执行日期
     * @return 每日清单列表
     */
    List<PlanExecutionInfoResp> getPlanExecutionListByDate(LocalDate executeDate);

    /**
     * 获取每日清单详情
     * @param id 每日清单ID
     * @return 每日清单信息
     */
    PlanExecutionInfoResp getPlanExecutionById(Long id);

    /**
     * 获取每日清单列表（分页）
     * @param executeDate 执行日期（可选）
     * @param status 执行状态（可选）
     * @return 每日清单列表
     */
    List<PlanExecutionInfoResp> getPlanExecutionList(LocalDate executeDate, Integer status);
}
