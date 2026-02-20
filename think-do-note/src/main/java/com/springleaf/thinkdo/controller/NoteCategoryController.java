package com.springleaf.thinkdo.controller;

import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.domain.request.CreateNoteCategoryReq;
import com.springleaf.thinkdo.domain.request.UpdateNoteCategoryReq;
import com.springleaf.thinkdo.domain.response.NoteCategoryInfoResp;
import com.springleaf.thinkdo.service.NoteCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 笔记分类Controller
 */
@RestController
@RequestMapping("/note/category")
@RequiredArgsConstructor
public class NoteCategoryController {

    private final NoteCategoryService noteCategoryService;

    /**
     * 创建笔记分类
     */
    @PostMapping("/create")
    public Result<Long> createCategory(@RequestBody @Valid CreateNoteCategoryReq createNoteCategoryReq) {
        Long categoryId = noteCategoryService.createCategory(createNoteCategoryReq);
        return Result.success(categoryId);
    }

    /**
     * 更新笔记分类
     */
    @PutMapping("/update")
    public Result<Void> updateCategory(@RequestBody @Valid UpdateNoteCategoryReq updateNoteCategoryReq) {
        noteCategoryService.updateCategory(updateNoteCategoryReq);
        return Result.success();
    }

    /**
     * 删除笔记分类
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        noteCategoryService.deleteCategory(id);
        return Result.success();
    }

    /**
     * 获取笔记分类列表
     */
    @GetMapping("/list")
    public Result<List<NoteCategoryInfoResp>> getCategoryList() {
        return Result.success(noteCategoryService.getCategoryList());
    }
}
