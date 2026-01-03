package com.springleaf.thinkdo.controller;

import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.domain.request.CreatePlanReq;
import com.springleaf.thinkdo.domain.request.PlanQueryReq;
import com.springleaf.thinkdo.domain.request.UpdatePlanReq;
import com.springleaf.thinkdo.domain.response.PlanInfoResp;
import com.springleaf.thinkdo.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 计划Controller
 */
@RestController
@RequestMapping("/plan/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    /**
     * 创建计划
     */
    @PostMapping("/create")
    public Result<Long> createPlan(@RequestBody @Valid CreatePlanReq createPlanReq) {
        Long planId = planService.createPlan(createPlanReq);
        return Result.success(planId);
    }

    /**
     * 更新计划
     */
    @PutMapping("/update")
    public Result<Void> updatePlan(@RequestBody @Valid UpdatePlanReq updatePlanReq) {
        planService.updatePlan(updatePlanReq);
        return Result.success();
    }

    /**
     * 删除计划
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return Result.success();
    }

    /**
     * 获取计划详情
     */
    @GetMapping("/{id}")
    public Result<PlanInfoResp> getPlanById(@PathVariable Long id) {
        return Result.success(planService.getPlanById(id));
    }

    /**
     * 获取计划列表
     */
    @GetMapping("/list")
    public Result<List<PlanInfoResp>> getPlanList(PlanQueryReq queryReq) {
        return Result.success(planService.getPlanList(queryReq));
    }

    /**
     * 切换计划完成状态
     */
    @PutMapping("/toggleStatus/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        planService.toggleStatus(id);
        return Result.success();
    }
}
