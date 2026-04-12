package com.springleaf.thinkdo.controller;

import com.springleaf.thinkdo.common.PageResp;
import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.context.UserContext;
import com.springleaf.thinkdo.domain.request.AdminPlanQueryReq;
import com.springleaf.thinkdo.domain.response.AdminPlanDetailResp;
import com.springleaf.thinkdo.domain.response.AdminPlanInfoResp;
import com.springleaf.thinkdo.enums.ResultCodeEnum;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员-计划管理接口
 */
@RestController
@RequestMapping("/admin/plan")
@RequiredArgsConstructor
public class AdminPlanController {

    private final PlanService planService;

    @GetMapping("/list")
    public Result<PageResp<AdminPlanInfoResp>> listPlans(AdminPlanQueryReq queryReq) {
        checkAdmin();
        return Result.success(planService.adminListPlans(queryReq));
    }

    @GetMapping("/detail/{id}")
    public Result<AdminPlanDetailResp> getPlanDetail(@PathVariable Long id) {
        checkAdmin();
        return Result.success(planService.adminGetPlanDetail(id));
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deletePlan(@PathVariable Long id) {
        checkAdmin();
        planService.adminDeletePlan(id);
        return Result.success();
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "无管理员权限");
        }
    }
}
