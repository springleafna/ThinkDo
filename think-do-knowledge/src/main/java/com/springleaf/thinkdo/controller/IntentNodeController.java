package com.springleaf.thinkdo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.domain.request.IntentNodeCreateReq;
import com.springleaf.thinkdo.domain.request.IntentNodePageReq;
import com.springleaf.thinkdo.domain.request.IntentNodeUpdateReq;
import com.springleaf.thinkdo.domain.response.IntentNodeTreeResp;
import com.springleaf.thinkdo.service.IntentNodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 意图树控制器
 * 提供意图节点的创建和意图树查询接口
 */
@RestController
@RequiredArgsConstructor
public class IntentNodeController {

    private final IntentNodeService intentNodeService;

    /**
     * 获取整棵意图树
     */
    @GetMapping("/intent-tree")
    public Result<List<IntentNodeTreeResp>> getFullTree() {
        return Result.success(intentNodeService.getFullTree());
    }

    /**
     * 分页查询意图节点列表
     */
    @GetMapping("/intent-node")
    public Result<IPage<IntentNodeTreeResp>> pageQuery(IntentNodePageReq requestParam) {
        return Result.success(intentNodeService.pageQuery(requestParam));
    }

    /**
     * 创建意图节点
     */
    @PostMapping("/intent-node")
    public Result<Void> createIntentNode(@RequestBody IntentNodeCreateReq requestParam) {
        intentNodeService.create(requestParam);
        return Result.success();
    }

    /**
     * 编辑意图节点
     */
    @PutMapping("/intent-node")
    public Result<Void> updateIntentNode(@RequestBody IntentNodeUpdateReq requestParam) {
        intentNodeService.update(requestParam);
        return Result.success();
    }

    /**
     * 删除意图节点（级联删除子节点）
     */
    @DeleteMapping("/intent-node/{id}")
    public Result<Void> deleteIntentNode(@PathVariable Long id) {
        intentNodeService.delete(id);
        return Result.success();
    }

    /**
     * 启用/禁用意图节点
     */
    @PutMapping("/intent-node/{id}/toggle-enabled")
    public Result<Void> toggleEnabled(@PathVariable Long id) {
        intentNodeService.toggleEnabled(id);
        return Result.success();
    }
}
