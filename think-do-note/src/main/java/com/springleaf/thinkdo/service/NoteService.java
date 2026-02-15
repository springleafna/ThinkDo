package com.springleaf.thinkdo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.springleaf.thinkdo.domain.entity.NoteEntity;
import com.springleaf.thinkdo.domain.request.AiTransformReq;
import com.springleaf.thinkdo.domain.request.CreateNoteReq;
import com.springleaf.thinkdo.domain.request.NoteQueryReq;
import com.springleaf.thinkdo.domain.request.UpdateNoteReq;
import com.springleaf.thinkdo.domain.response.NoteInfoResp;
import com.springleaf.thinkdo.domain.response.NoteListItemResp;
import com.springleaf.thinkdo.domain.response.NoteStatisticsResp;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 笔记Service
 */
public interface NoteService extends IService<NoteEntity> {

    /**
     * 创建笔记
     *
     * @param createNoteReq 创建笔记请求
     * @return 笔记ID
     */
    Long createNote(CreateNoteReq createNoteReq);

    /**
     * 更新笔记
     *
     * @param updateNoteReq 更新笔记请求
     */
    void updateNote(UpdateNoteReq updateNoteReq);

    /**
     * 删除笔记
     *
     * @param id 笔记ID
     */
    void deleteNote(Long id);

    /**
     * 获取笔记详情
     *
     * @param id 笔记ID
     * @return 笔记信息
     */
    NoteInfoResp getNoteById(Long id);

    /**
     * 根据分类ID获取笔记列表
     *
     * @param queryReq 查询条件
     * @return 笔记列表
     */
    List<NoteListItemResp> getNoteList(NoteQueryReq queryReq);

    /**
     * 根据笔记内容搜索笔记
     *
     * @param keyword 搜索关键词
     * @return 笔记列表
     */
    List<NoteListItemResp> searchNotes(String keyword);

    /**
     * 切换笔记收藏状态
     *
     * @param id 笔记ID
     */
    void toggleFavorited(Long id);

    /**
     * 获取笔记统计信息
     *
     * @return 统计信息
     */
    NoteStatisticsResp getStatistics();

    /**
     * AI流式转换文本
     *
     * @param req 转换请求
     * @return 流式响应
     */
    Flux<String> aiTransformStream(AiTransformReq req);

    /**
     * 获取最近修改的笔记
     *
     * @return 最近修改的两条笔记
     */
    List<NoteListItemResp> getRecentNotes();
}
