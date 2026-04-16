package com.springleaf.thinkdo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.springleaf.thinkdo.common.PageResp;
import com.springleaf.thinkdo.domain.entity.NoteEntity;
import com.springleaf.thinkdo.domain.request.AdminNoteQueryReq;
import com.springleaf.thinkdo.domain.request.AiTransformReq;
import com.springleaf.thinkdo.domain.request.CreateNoteReq;
import com.springleaf.thinkdo.domain.request.NoteQueryReq;
import com.springleaf.thinkdo.domain.request.UpdateNoteReq;
import com.springleaf.thinkdo.domain.response.AdminNoteDetailResp;
import com.springleaf.thinkdo.domain.response.AdminNoteInfoResp;
import com.springleaf.thinkdo.domain.response.NoteInfoResp;
import com.springleaf.thinkdo.domain.response.NoteListItemResp;
import com.springleaf.thinkdo.domain.response.NoteStatisticsResp;
import org.springframework.web.multipart.MultipartFile;
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

    /**
     * 上传笔记图片到对象存储
     *
     * @param file 图片文件
     * @return 图片访问URL
     */
    String uploadImage(MultipartFile file);

    /**
     * 管理员-分页查询所有笔记
     * @param queryReq 查询条件
     * @return 分页笔记列表
     */
    PageResp<AdminNoteInfoResp> adminListNotes(AdminNoteQueryReq queryReq);

    /**
     * 管理员-查看笔记详情
     * @param id 笔记ID
     * @return 笔记详情
     */
    AdminNoteDetailResp adminGetNoteDetail(Long id);

    /**
     * 管理员-删除笔记
     * @param id 笔记ID
     */
    void adminDeleteNote(Long id);

    /**
     * 统计笔记总数
     * @return 笔记总数
     */
    Long countTotal();

    /**
     * 统计指定日期创建的笔记数
     * @param date 日期
     * @return 笔记数
     */
    Long countByDate(java.time.LocalDate date);
}
