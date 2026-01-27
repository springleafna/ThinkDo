package com.springleaf.thinkdo.controller;

import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.domain.request.CreatePlanExecutionReq;
import com.springleaf.thinkdo.domain.request.UpdatePlanExecutionReq;
import com.springleaf.thinkdo.domain.response.PlanExecutionInfoResp;
import com.springleaf.thinkdo.service.PlanExecutionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日清单Controller
 */
@RestController
@RequestMapping("/plan/execution")
@RequiredArgsConstructor
public class PlanExecutionController {

    private final PlanExecutionService planExecutionService;

    /**
     * 创建每日清单
     * 直接在plan表中创建type=2的计划即可
     */
    @PostMapping("/create")
    public Result<Long> createPlanExecution(@RequestBody @Valid CreatePlanExecutionReq createPlanExecutionReq) {
        Long id = planExecutionService.createPlanExecution(createPlanExecutionReq);
        return Result.success(id);
    }

    /**
     * 更新每日清单
     * 如果是每日计划直接更新即可
     * 如果是普通计划则需要重新创建一条一样内容的每日计划，并创建一条原先记录的execution记录并标记为删除
     */
    @PutMapping("/update")
    public Result<Void> updatePlanExecution(@RequestBody @Valid UpdatePlanExecutionReq updatePlanExecutionReq) {
        planExecutionService.updatePlanExecution(updatePlanExecutionReq);
        return Result.success();
    }

    /**
     * 删除每日清单
     * 如果是每日计划需要将plan表和execution表中的该记录标记为删除
     * 如果是普通计划则插入（或更新）一条标记为 deleted=1 的 Execution 记录
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deletePlanExecution(@PathVariable @NotNull(message = "任务id不能为空") Long id) {
        planExecutionService.deletePlanExecution(id);
        return Result.success();
    }

    /**
     * 切换每日清单状态
     * 如果是每日计划需要同步切换plan表和execution表中的该记录的状态
     * 如果是普通计划则仅切换execution表中的该记录的状态
     */
    @PutMapping("/toggleStatus/{id}")
    public Result<Void> toggleStatus(@PathVariable @NotNull(message = "计划id不能为空") Long id) {
        planExecutionService.toggleStatus(id);
        return Result.success();
    }

    /**
     * 获取每日清单详情
     */
    @GetMapping("/{id}")
    public Result<PlanExecutionInfoResp> getPlanExecutionById(@PathVariable Long id) {
        return Result.success(planExecutionService.getPlanExecutionById(id));
    }

    /**
     * 获取指定日期的每日清单列表
     */
    @GetMapping("/list/date/{executeDate}")
    public Result<List<PlanExecutionInfoResp>> getPlanExecutionListByDate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate executeDate) {
        return Result.success(planExecutionService.getPlanExecutionListByDate(executeDate));
    }

    /**
     * 获取今日每日清单列表
     * 1. 获取plan表中的今天的每日计划并删除execution表中存在的已删除的该记录
     * 2. 获取plan表中的设置了重复规则的普通计划并且包含了今天的记录，并且删除execution表中存在的已删除的该记录
     */
    @GetMapping("/list/today")
    public Result<List<PlanExecutionInfoResp>> getTodayPlanExecutionList() {
        return Result.success(planExecutionService.getPlanExecutionListByDate(LocalDate.now()));
    }

    /**
     * 获取每日清单列表
     * @param executeDate 执行日期（可选）
     * @param status 执行状态（可选）
     */
    @GetMapping("/list")
    public Result<List<PlanExecutionInfoResp>> getPlanExecutionList(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate executeDate,
            @RequestParam(required = false) Integer status) {
        return Result.success(planExecutionService.getPlanExecutionList(executeDate, status));
    }
}
