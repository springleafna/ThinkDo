package com.springleaf.thinkdo.controller;

import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.domain.request.CreatePlanExecutionReq;
import com.springleaf.thinkdo.domain.response.PlanExecutionInfoResp;
import com.springleaf.thinkdo.service.PlanExecutionService;
import jakarta.validation.Valid;
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
     * 创建每日计划
     * 内部先创建计划(type=2)，再创建执行记录
     */
    @PostMapping("/create")
    public Result<Long> createPlanExecution(@RequestBody @Valid CreatePlanExecutionReq createPlanExecutionReq) {
        Long id = planExecutionService.createPlanExecution(createPlanExecutionReq);
        return Result.success(id);
    }

    /**
     * 删除每日清单
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deletePlanExecution(@PathVariable Long id) {
        planExecutionService.deletePlanExecution(id);
        return Result.success();
    }

    /**
     * 切换每日清单状态
     */
    @PutMapping("/toggleStatus/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id) {
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
