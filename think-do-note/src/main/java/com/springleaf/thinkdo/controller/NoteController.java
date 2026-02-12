package com.springleaf.thinkdo.controller;

import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.domain.request.CreateNoteReq;
import com.springleaf.thinkdo.domain.request.NoteQueryReq;
import com.springleaf.thinkdo.domain.request.UpdateNoteReq;
import com.springleaf.thinkdo.domain.response.NoteInfoResp;
import com.springleaf.thinkdo.domain.response.NoteListItemResp;
import com.springleaf.thinkdo.domain.response.NoteStatisticsResp;
import com.springleaf.thinkdo.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 笔记Controller
 */
@RestController
@RequestMapping("/note")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    /**
     * 创建笔记
     */
    @PostMapping("/create")
    public Result<Long> createNote(@RequestBody @Valid CreateNoteReq createNoteReq) {
        Long noteId = noteService.createNote(createNoteReq);
        return Result.success(noteId);
    }

    /**
     * 更新笔记
     */
    @PutMapping("/update")
    public Result<Void> updateNote(@RequestBody @Valid UpdateNoteReq updateNoteReq) {
        noteService.updateNote(updateNoteReq);
        return Result.success();
    }

    /**
     * 删除笔记
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteNote(@PathVariable Long id) {
        noteService.deleteNote(id);
        return Result.success();
    }

    /**
     * 获取笔记详情
     */
    @GetMapping("/{id}")
    public Result<NoteInfoResp> getNoteById(@PathVariable Long id) {
        return Result.success(noteService.getNoteById(id));
    }

    /**
     * 获取笔记列表
     */
    @GetMapping("/list")
    public Result<List<NoteListItemResp>> getNoteList(NoteQueryReq queryReq) {
        return Result.success(noteService.getNoteList(queryReq));
    }

    /**
     * 根据笔记内容搜索笔记
     */
    @GetMapping("/search")
    public Result<List<NoteListItemResp>> searchNotes(@RequestParam String keyword) {
        return Result.success(noteService.searchNotes(keyword));
    }

    /**
     * 切换笔记收藏状态
     */
    @PutMapping("/toggleFavorited/{id}")
    public Result<Void> toggleFavorited(@PathVariable Long id) {
        noteService.toggleFavorited(id);
        return Result.success();
    }

    /**
     * 获取笔记统计信息
     */
    @GetMapping("/statistics")
    public Result<NoteStatisticsResp> getStatistics() {
        return Result.success(noteService.getStatistics());
    }
}
