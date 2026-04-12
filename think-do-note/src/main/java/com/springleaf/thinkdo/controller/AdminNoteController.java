package com.springleaf.thinkdo.controller;

import com.springleaf.thinkdo.common.PageResp;
import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.context.UserContext;
import com.springleaf.thinkdo.domain.request.AdminNoteQueryReq;
import com.springleaf.thinkdo.domain.response.AdminNoteDetailResp;
import com.springleaf.thinkdo.domain.response.AdminNoteInfoResp;
import com.springleaf.thinkdo.enums.ResultCodeEnum;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员-笔记管理接口
 */
@RestController
@RequestMapping("/admin/note")
@RequiredArgsConstructor
public class AdminNoteController {

    private final NoteService noteService;

    @GetMapping("/list")
    public Result<PageResp<AdminNoteInfoResp>> listNotes(AdminNoteQueryReq queryReq) {
        checkAdmin();
        return Result.success(noteService.adminListNotes(queryReq));
    }

    @GetMapping("/detail/{id}")
    public Result<AdminNoteDetailResp> getNoteDetail(@PathVariable Long id) {
        checkAdmin();
        return Result.success(noteService.adminGetNoteDetail(id));
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteNote(@PathVariable Long id) {
        checkAdmin();
        noteService.adminDeleteNote(id);
        return Result.success();
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "无管理员权限");
        }
    }
}
