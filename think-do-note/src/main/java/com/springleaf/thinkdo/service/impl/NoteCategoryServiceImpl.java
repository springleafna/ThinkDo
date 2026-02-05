package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.thinkdo.domain.entity.NoteCategoryEntity;
import com.springleaf.thinkdo.domain.entity.NoteEntity;
import com.springleaf.thinkdo.domain.request.CreateNoteCategoryReq;
import com.springleaf.thinkdo.domain.request.UpdateNoteCategoryReq;
import com.springleaf.thinkdo.domain.response.NoteCategoryInfoResp;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.NoteCategoryMapper;
import com.springleaf.thinkdo.mapper.NoteMapper;
import com.springleaf.thinkdo.service.NoteCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 笔记分类Service实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NoteCategoryServiceImpl extends ServiceImpl<NoteCategoryMapper, NoteCategoryEntity> implements NoteCategoryService {

    private final NoteCategoryMapper noteCategoryMapper;
    private final NoteMapper noteMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(CreateNoteCategoryReq createNoteCategoryReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 检查分类名称是否已存在
        LambdaQueryWrapper<NoteCategoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteCategoryEntity::getUserId, userId);
        wrapper.eq(NoteCategoryEntity::getName, createNoteCategoryReq.getName());
        Long count = noteCategoryMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("分类名称已存在");
        }

        NoteCategoryEntity category = new NoteCategoryEntity();
        category.setUserId(userId);
        category.setName(createNoteCategoryReq.getName());

        noteCategoryMapper.insert(category);
        log.info("创建笔记分类成功, userId={}, categoryId={}", userId, category.getId());

        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(UpdateNoteCategoryReq updateNoteCategoryReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        NoteCategoryEntity category = noteCategoryMapper.selectById(updateNoteCategoryReq.getId());
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        // 验证是否为当前用户的分类
        if (!category.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此分类");
        }

        // 如果要修改名称，检查新名称是否已存在
        if (StringUtils.hasText(updateNoteCategoryReq.getName())) {
            LambdaQueryWrapper<NoteCategoryEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NoteCategoryEntity::getUserId, userId);
            wrapper.eq(NoteCategoryEntity::getName, updateNoteCategoryReq.getName());
            wrapper.ne(NoteCategoryEntity::getId, updateNoteCategoryReq.getId());
            Long count = noteCategoryMapper.selectCount(wrapper);
            if (count > 0) {
                throw new BusinessException("分类名称已存在");
            }
            category.setName(updateNoteCategoryReq.getName());
        }

        noteCategoryMapper.updateById(category);
        log.info("更新笔记分类成功, userId={}, categoryId={}", userId, category.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        NoteCategoryEntity category = noteCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        // 验证是否为当前用户的分类
        if (!category.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此分类");
        }

        // 检查分类下是否有笔记
        LambdaQueryWrapper<NoteEntity> noteWrapper = new LambdaQueryWrapper<>();
        noteWrapper.eq(NoteEntity::getCategoryId, id);
        Long noteCount = noteMapper.selectCount(noteWrapper);
        if (noteCount > 0) {
            throw new BusinessException("分类下存在笔记，无法删除");
        }

        noteCategoryMapper.deleteById(id);
        log.info("删除笔记分类成功, userId={}, categoryId={}", userId, id);
    }

    @Override
    public List<NoteCategoryInfoResp> getCategoryList() {
        Long userId = StpUtil.getLoginIdAsLong();

        LambdaQueryWrapper<NoteCategoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteCategoryEntity::getUserId, userId);
        wrapper.orderByDesc(NoteCategoryEntity::getUpdatedAt);

        List<NoteCategoryEntity> categoryList = noteCategoryMapper.selectList(wrapper);
        return categoryList.stream()
                .map(this::convertToResp)
                .collect(Collectors.toList());
    }

    /**
     * 转换为响应对象
     */
    private NoteCategoryInfoResp convertToResp(NoteCategoryEntity category) {
        NoteCategoryInfoResp resp = new NoteCategoryInfoResp();
        BeanUtils.copyProperties(category, resp);
        return resp;
    }
}
