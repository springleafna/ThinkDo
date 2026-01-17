package com.springleaf.thinkdo.controller;

import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.domain.request.CreateMemoReq;
import com.springleaf.thinkdo.domain.request.MemoQueryReq;
import com.springleaf.thinkdo.domain.request.UpdateMemoReq;
import com.springleaf.thinkdo.domain.response.MemoInfoResp;
import com.springleaf.thinkdo.service.MemoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 便签Controller
 */
@RestController
@RequestMapping("/plan/memo")
@RequiredArgsConstructor
public class MemoController {

    private final MemoService memoService;

    /**
     * 创建便签
     */
    @PostMapping("/create")
    public Result<Long> createMemo(@RequestBody @Valid CreateMemoReq createMemoReq) {
        Long memoId = memoService.createMemo(createMemoReq);
        return Result.success(memoId);
    }

    /**
     * 更新便签
     */
    @PutMapping("/update")
    public Result<Void> updateMemo(@RequestBody @Valid UpdateMemoReq updateMemoReq) {
        memoService.updateMemo(updateMemoReq);
        return Result.success();
    }

    /**
     * 删除便签
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteMemo(@PathVariable Long id) {
        memoService.deleteMemo(id);
        return Result.success();
    }

    /**
     * 获取便签详情
     */
    @GetMapping("/{id}")
    public Result<MemoInfoResp> getMemoById(@PathVariable Long id) {
        return Result.success(memoService.getMemoById(id));
    }

    /**
     * 获取便签列表
     */
    @GetMapping("/list")
    public Result<List<MemoInfoResp>> getMemoList(MemoQueryReq queryReq) {
        return Result.success(memoService.getMemoList(queryReq));
    }

    /**
     * 切换便签置顶状态
     */
    @PutMapping("/togglePinned/{id}")
    public Result<Void> togglePinned(@PathVariable Long id) {
        memoService.togglePinned(id);
        return Result.success();
    }

    /**
     * 获取最新修改的两个便签
     */
    @GetMapping("/latest")
    public Result<List<MemoInfoResp>> getLatestMemos() {
        return Result.success(memoService.getLatestMemos());
    }
}
