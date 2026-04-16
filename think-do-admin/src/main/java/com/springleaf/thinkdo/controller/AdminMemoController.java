package com.springleaf.thinkdo.controller;

import com.springleaf.thinkdo.common.PageResp;
import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.context.UserContext;
import com.springleaf.thinkdo.domain.request.AdminMemoQueryReq;
import com.springleaf.thinkdo.domain.response.AdminMemoInfoResp;
import com.springleaf.thinkdo.enums.ResultCodeEnum;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员-便签管理接口
 */
@RestController
@RequestMapping("/admin/memo")
@RequiredArgsConstructor
public class AdminMemoController {

    private final MemoService memoService;

    @GetMapping("/list")
    public Result<PageResp<AdminMemoInfoResp>> listMemos(AdminMemoQueryReq queryReq) {
        checkAdmin();
        return Result.success(memoService.adminListMemos(queryReq));
    }

    @GetMapping("/detail/{id}")
    public Result<AdminMemoInfoResp> getMemoDetail(@PathVariable Long id) {
        checkAdmin();
        return Result.success(memoService.adminGetMemoDetail(id));
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteMemo(@PathVariable Long id) {
        checkAdmin();
        memoService.adminDeleteMemo(id);
        return Result.success();
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "无管理员权限");
        }
    }
}
