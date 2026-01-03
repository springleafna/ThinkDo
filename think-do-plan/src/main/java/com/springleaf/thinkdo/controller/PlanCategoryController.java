package com.springleaf.thinkdo.controller;

import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.domain.request.CreatePlanCategoryReq;
import com.springleaf.thinkdo.domain.request.UpdatePlanCategoryReq;
import com.springleaf.thinkdo.domain.response.PlanCategoryInfoResp;
import com.springleaf.thinkdo.service.PlanCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 计划分类Controller
 */
@RestController
@RequestMapping("/plan/category")
@RequiredArgsConstructor
public class PlanCategoryController {

    private final PlanCategoryService planCategoryService;

    /**
     * 创建计划分类
     */
    @PostMapping("/create")
    public Result<Long> createCategory(@RequestBody @Valid CreatePlanCategoryReq createPlanCategoryReq) {
        Long categoryId = planCategoryService.createCategory(createPlanCategoryReq);
        return Result.success(categoryId);
    }

    /**
     * 更新计划分类
     */
    @PutMapping("/update")
    public Result<Void> updateCategory(@RequestBody @Valid UpdatePlanCategoryReq updatePlanCategoryReq) {
        planCategoryService.updateCategory(updatePlanCategoryReq);
        return Result.success();
    }

    /**
     * 删除计划分类
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        planCategoryService.deleteCategory(id);
        return Result.success();
    }

    /**
     * 获取计划分类详情
     */
    @GetMapping("/{id}")
    public Result<PlanCategoryInfoResp> getCategoryById(@PathVariable Long id) {
        return Result.success(planCategoryService.getCategoryById(id));
    }

    /**
     * 获取计划分类列表
     */
    @GetMapping("/list")
    public Result<List<PlanCategoryInfoResp>> getCategoryList() {
        return Result.success(planCategoryService.getCategoryList());
    }
}
