package com.springleaf.thinkdo.controller;

import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.domain.request.IntentNodeCreateReq;
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
     * 创建意图节点
     */
    @PostMapping("/intent-node")
    public Result<Void> createIntentNode(@RequestBody IntentNodeCreateReq requestParam) {
        intentNodeService.create(requestParam);
        return Result.success();
    }
}
