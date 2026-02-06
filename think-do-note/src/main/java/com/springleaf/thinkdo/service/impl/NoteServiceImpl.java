package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.thinkdo.domain.entity.NoteCategoryEntity;
import com.springleaf.thinkdo.domain.entity.NoteEntity;
import com.springleaf.thinkdo.domain.request.CreateNoteReq;
import com.springleaf.thinkdo.domain.request.NoteQueryReq;
import com.springleaf.thinkdo.domain.request.UpdateNoteReq;
import com.springleaf.thinkdo.domain.response.NoteInfoResp;
import com.springleaf.thinkdo.domain.response.NoteStatisticsResp;
import com.springleaf.thinkdo.enums.NoteFavoritedEnum;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.NoteCategoryMapper;
import com.springleaf.thinkdo.mapper.NoteMapper;
import com.springleaf.thinkdo.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 笔记Service实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NoteServiceImpl extends ServiceImpl<NoteMapper, NoteEntity> implements NoteService {

    private final NoteMapper noteMapper;
    private final NoteCategoryMapper noteCategoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNote(CreateNoteReq createNoteReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 如果指定了分类ID，验证分类是否存在且属于当前用户
        if (createNoteReq.getCategoryId() != null) {
            validateCategoryOwnership(createNoteReq.getCategoryId(), userId);
        }

        NoteEntity note = new NoteEntity();
        note.setUserId(userId);
        note.setTitle(createNoteReq.getTitle());
        note.setContent(createNoteReq.getContent());
        note.setCategoryId(createNoteReq.getCategoryId());
        note.setTags(createNoteReq.getTags());

        noteMapper.insert(note);
        log.info("创建笔记成功, userId={}, noteId={}", userId, note.getId());

        return note.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNote(UpdateNoteReq updateNoteReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        NoteEntity note = noteMapper.selectById(updateNoteReq.getId());
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }

        // 验证是否为当前用户的笔记
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此笔记");
        }

        // 如果指定了分类ID，验证分类是否存在且属于当前用户
        if (updateNoteReq.getCategoryId() != null) {
            validateCategoryOwnership(updateNoteReq.getCategoryId(), userId);
        }

        // 更新字段
        if (StringUtils.hasText(updateNoteReq.getTitle())) {
            note.setTitle(updateNoteReq.getTitle());
        }
        if (StringUtils.hasText(updateNoteReq.getContent())) {
            note.setContent(updateNoteReq.getContent());
        }
        if (updateNoteReq.getCategoryId() != null) {
            note.setCategoryId(updateNoteReq.getCategoryId());
        }
        if (updateNoteReq.getTags() != null) {
            note.setTags(updateNoteReq.getTags());
        }

        noteMapper.updateById(note);
        log.info("更新笔记成功, userId={}, noteId={}", userId, note.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNote(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        NoteEntity note = noteMapper.selectById(id);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }

        // 验证是否为当前用户的笔记
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此笔记");
        }

        noteMapper.deleteById(id);
        log.info("删除笔记成功, userId={}, noteId={}", userId, id);
    }

    @Override
    public NoteInfoResp getNoteById(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        NoteEntity note = noteMapper.selectById(id);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }

        // 验证是否为当前用户的笔记
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException("无权查看此笔记");
        }

        return convertToResp(note);
    }

    @Override
    public List<NoteInfoResp> getNoteList(NoteQueryReq queryReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        LambdaQueryWrapper<NoteEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteEntity::getUserId, userId);

        // 按分类筛选（包括未分类的笔记）
        if (queryReq.getCategoryId() != null) {
            wrapper.eq(NoteEntity::getCategoryId, queryReq.getCategoryId());
        }

        // 按收藏状态筛选
        if (queryReq.getFavorited() != null) {
            wrapper.eq(NoteEntity::getFavorited, queryReq.getFavorited());
        }

        // 关键词搜索
        if (StringUtils.hasText(queryReq.getKeyword())) {
            String keyword = queryReq.getKeyword();
            wrapper.and(w -> w.like(NoteEntity::getTitle, keyword)
                    .or()
                    .like(NoteEntity::getContent, keyword));
        }

        // 排序：收藏的在前，然后按更新时间倒序
        wrapper.orderByDesc(NoteEntity::getFavorited)
                .orderByDesc(NoteEntity::getUpdatedAt);

        List<NoteEntity> noteList = noteMapper.selectList(wrapper);

        // 批量获取分类名称
        List<Long> categoryIds = noteList.stream()
                .map(NoteEntity::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        Map<Long, String> categoryNameMap = Map.of();
        if (!categoryIds.isEmpty()) {
            LambdaQueryWrapper<NoteCategoryEntity> categoryWrapper = new LambdaQueryWrapper<>();
            categoryWrapper.in(NoteCategoryEntity::getId, categoryIds);
            categoryWrapper.eq(NoteCategoryEntity::getUserId, userId);
            categoryWrapper.select(NoteCategoryEntity::getId, NoteCategoryEntity::getName);
            List<NoteCategoryEntity> categories = noteCategoryMapper.selectList(categoryWrapper);
            categoryNameMap = categories.stream()
                    .collect(Collectors.toMap(NoteCategoryEntity::getId, NoteCategoryEntity::getName));
        }

        Map<Long, String> finalCategoryNameMap = categoryNameMap;
        return noteList.stream()
                .map(note -> {
                    NoteInfoResp resp = convertToResp(note);
                    if (note.getCategoryId() != null) {
                        resp.setCategoryName(finalCategoryNameMap.get(note.getCategoryId()));
                    }
                    return resp;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<NoteInfoResp> searchNotes(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }

        Long userId = StpUtil.getLoginIdAsLong();

        LambdaQueryWrapper<NoteEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteEntity::getUserId, userId);
        wrapper.and(w -> w.like(NoteEntity::getTitle, keyword)
                .or()
                .like(NoteEntity::getContent, keyword));
        wrapper.orderByDesc(NoteEntity::getUpdatedAt);

        List<NoteEntity> noteList = noteMapper.selectList(wrapper);
        return noteList.stream()
                .map(this::convertToResp)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleFavorited(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        NoteEntity note = noteMapper.selectById(id);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }

        // 验证是否为当前用户的笔记
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此笔记");
        }

        // 切换收藏状态
        NoteFavoritedEnum currentStatus = NoteFavoritedEnum.getByCode(note.getFavorited());
        NoteFavoritedEnum newStatus = currentStatus.toggle();
        note.setFavorited(newStatus.getCode());
        noteMapper.updateById(note);
        log.info("笔记{}成功, userId={}, noteId={}", newStatus.getDesc(), userId, id);
    }

    @Override
    public NoteStatisticsResp getStatistics() {
        Long userId = StpUtil.getLoginIdAsLong();

        NoteStatisticsResp resp = new NoteStatisticsResp();

        // 全部笔记数量
        LambdaQueryWrapper<NoteEntity> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(NoteEntity::getUserId, userId);
        Long totalCount = noteMapper.selectCount(totalWrapper);
        resp.setTotalCount(totalCount.intValue());

        // 收藏笔记数量
        LambdaQueryWrapper<NoteEntity> favoritedWrapper = new LambdaQueryWrapper<>();
        favoritedWrapper.eq(NoteEntity::getUserId, userId);
        favoritedWrapper.eq(NoteEntity::getFavorited, NoteFavoritedEnum.FAVORITED.getCode());
        Long favoritedCount = noteMapper.selectCount(favoritedWrapper);
        resp.setFavoritedCount(favoritedCount.intValue());

        // 未分类笔记数量
        LambdaQueryWrapper<NoteEntity> unclassifiedWrapper = new LambdaQueryWrapper<>();
        unclassifiedWrapper.eq(NoteEntity::getUserId, userId);
        unclassifiedWrapper.isNull(NoteEntity::getCategoryId);
        Long unclassifiedCount = noteMapper.selectCount(unclassifiedWrapper);
        resp.setUnclassifiedCount(unclassifiedCount.intValue());

        // 获取所有分类
        LambdaQueryWrapper<NoteCategoryEntity> categoryWrapper = new LambdaQueryWrapper<>();
        categoryWrapper.eq(NoteCategoryEntity::getUserId, userId);
        List<NoteCategoryEntity> categories = noteCategoryMapper.selectList(categoryWrapper);

        // 各分类笔记数量
        List<NoteStatisticsResp.CategoryCount> categoryCounts = categories.stream()
                .map(category -> {
                    LambdaQueryWrapper<NoteEntity> noteWrapper = new LambdaQueryWrapper<>();
                    noteWrapper.eq(NoteEntity::getUserId, userId);
                    noteWrapper.eq(NoteEntity::getCategoryId, category.getId());
                    Long count = noteMapper.selectCount(noteWrapper);

                    NoteStatisticsResp.CategoryCount categoryCount = new NoteStatisticsResp.CategoryCount();
                    categoryCount.setCategoryId(category.getId());
                    categoryCount.setCategoryName(category.getName());
                    categoryCount.setCount(count.intValue());
                    return categoryCount;
                })
                .collect(Collectors.toList());

        resp.setCategoryCounts(categoryCounts);
        return resp;
    }

    /**
     * 验证分类所有权
     */
    private void validateCategoryOwnership(Long categoryId, Long userId) {
        LambdaQueryWrapper<NoteCategoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteCategoryEntity::getId, categoryId);
        wrapper.eq(NoteCategoryEntity::getUserId, userId);
        Long count = noteCategoryMapper.selectCount(wrapper);
        if (count == 0) {
            throw new BusinessException("分类不存在或无权访问");
        }
    }

    /**
     * 转换为响应对象
     */
    private NoteInfoResp convertToResp(NoteEntity note) {
        NoteInfoResp resp = new NoteInfoResp();
        BeanUtils.copyProperties(note, resp);
        return resp;
    }
}
