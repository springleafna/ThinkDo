package com.springleaf.thinkdo.controller;

import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.domain.request.CreatePlanStepReq;
import com.springleaf.thinkdo.domain.request.UpdatePlanStepReq;
import com.springleaf.thinkdo.domain.response.PlanStepInfoResp;
import com.springleaf.thinkdo.service.PlanStepService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 计划步骤Controller
 */
@RestController
@RequestMapping("/plan/step")
@RequiredArgsConstructor
public class PlanStepController {

    private final PlanStepService planStepService;

    /**
     * 创建计划步骤
     */
    @PostMapping("/create")
    public Result<Long> createStep(@RequestBody @Valid CreatePlanStepReq createPlanStepReq) {
        Long stepId = planStepService.createStep(createPlanStepReq);
        return Result.success(stepId);
    }

    /**
     * 更新计划步骤
     */
    @PutMapping("/update")
    public Result<Void> updateStep(@RequestBody @Valid UpdatePlanStepReq updatePlanStepReq) {
        planStepService.updateStep(updatePlanStepReq);
        return Result.success();
    }

    /**
     * 删除计划步骤
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteStep(@PathVariable Long id) {
        planStepService.deleteStep(id);
        return Result.success();
    }

    /**
     * 获取计划步骤详情
     */
    @GetMapping("/{id}")
    public Result<PlanStepInfoResp> getStepById(@PathVariable Long id) {
        return Result.success(planStepService.getStepById(id));
    }

    /**
     * 获取指定计划的所有步骤列表
     */
    @GetMapping("/list/{planId}")
    public Result<List<PlanStepInfoResp>> getStepListByPlanId(@PathVariable Long planId) {
        return Result.success(planStepService.getStepListByPlanId(planId));
    }

    /**
     * 切换步骤完成状态
     */
    @PutMapping("/toggleStatus/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        planStepService.toggleStatus(id);
        return Result.success();
    }
}
