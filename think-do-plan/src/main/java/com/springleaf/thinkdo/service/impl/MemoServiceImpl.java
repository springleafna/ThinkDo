package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.thinkdo.domain.entity.MemoEntity;
import com.springleaf.thinkdo.domain.request.CreateMemoReq;
import com.springleaf.thinkdo.domain.request.MemoQueryReq;
import com.springleaf.thinkdo.domain.request.UpdateMemoReq;
import com.springleaf.thinkdo.domain.response.MemoInfoResp;
import com.springleaf.thinkdo.enums.BackgroundColorEnum;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.MemoMapper;
import com.springleaf.thinkdo.service.MemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 便签Service实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MemoServiceImpl extends ServiceImpl<MemoMapper, MemoEntity> implements MemoService {

    private final MemoMapper memoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMemo(CreateMemoReq createMemoReq) {
        // 校验标题和内容至少一项不为空
        if (!StringUtils.hasText(createMemoReq.getTitle()) && !StringUtils.hasText(createMemoReq.getContent())) {
            throw new BusinessException("便签标题和内容不能同时为空");
        }

        Long userId = StpUtil.getLoginIdAsLong();

        MemoEntity memo = new MemoEntity();
        memo.setUserId(userId);
        memo.setTitle(createMemoReq.getTitle());
        memo.setContent(createMemoReq.getContent());
        memo.setTag(createMemoReq.getTag());

        // 设置背景颜色，如果未指定则使用默认颜色
        String backgroundColor = createMemoReq.getBackgroundColor();
        if (!StringUtils.hasText(backgroundColor)) {
            backgroundColor = BackgroundColorEnum.getDefaultColorCode();
        } else if (!BackgroundColorEnum.isValidColor(backgroundColor)) {
            throw new BusinessException("无效的背景颜色");
        }
        memo.setBackgroundColor(backgroundColor);

        // 设置置顶状态，默认为0
        memo.setPinned(createMemoReq.getPinned() != null ? createMemoReq.getPinned() : 0);

        memoMapper.insert(memo);
        log.info("创建便签成功, userId={}, memoId={}", userId, memo.getId());

        return memo.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMemo(UpdateMemoReq updateMemoReq) {
        // 校验标题和内容至少一项不为空
        if (!StringUtils.hasText(updateMemoReq.getTitle()) && !StringUtils.hasText(updateMemoReq.getContent())) {
            throw new BusinessException("便签标题和内容不能同时为空");
        }

        Long userId = StpUtil.getLoginIdAsLong();

        MemoEntity memo = memoMapper.selectById(updateMemoReq.getId());
        if (memo == null) {
            throw new BusinessException("便签不存在");
        }

        // 验证是否为当前用户的便签
        if (!memo.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此便签");
        }

        // 更新字段
        if (StringUtils.hasText(updateMemoReq.getTitle())) {
            memo.setTitle(updateMemoReq.getTitle());
        }
        if (StringUtils.hasText(updateMemoReq.getContent())) {
            memo.setContent(updateMemoReq.getContent());
        }
        if (updateMemoReq.getTag() != null) {
            memo.setTag(updateMemoReq.getTag());
        }
        if (StringUtils.hasText(updateMemoReq.getBackgroundColor())) {
            if (!BackgroundColorEnum.isValidColor(updateMemoReq.getBackgroundColor())) {
                throw new BusinessException("无效的背景颜色");
            }
            memo.setBackgroundColor(updateMemoReq.getBackgroundColor());
        }
        if (updateMemoReq.getPinned() != null) {
            memo.setPinned(updateMemoReq.getPinned());
        }

        memoMapper.updateById(memo);
        log.info("更新便签成功, userId={}, memoId={}", userId, memo.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMemo(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        MemoEntity memo = memoMapper.selectById(id);
        if (memo == null) {
            throw new BusinessException("便签不存在");
        }

        // 验证是否为当前用户的便签
        if (!memo.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此便签");
        }

        memoMapper.deleteById(id);
        log.info("删除便签成功, userId={}, memoId={}", userId, id);
    }

    @Override
    public MemoInfoResp getMemoById(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        MemoEntity memo = memoMapper.selectById(id);
        if (memo == null) {
            throw new BusinessException("便签不存在");
        }

        // 验证是否为当前用户的便签
        if (!memo.getUserId().equals(userId)) {
            throw new BusinessException("无权查看此便签");
        }

        return convertToResp(memo);
    }

    @Override
    public List<MemoInfoResp> getMemoList(MemoQueryReq queryReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        LambdaQueryWrapper<MemoEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemoEntity::getUserId, userId);

        // 按标签筛选
        if (StringUtils.hasText(queryReq.getTag())) {
            wrapper.eq(MemoEntity::getTag, queryReq.getTag());
        }

        // 按背景颜色筛选
        if (StringUtils.hasText(queryReq.getBackgroundColor())) {
            wrapper.eq(MemoEntity::getBackgroundColor, queryReq.getBackgroundColor());
        }

        // 按置顶状态筛选
        if (queryReq.getPinned() != null) {
            wrapper.eq(MemoEntity::getPinned, queryReq.getPinned());
        }

        // 关键词搜索
        if (StringUtils.hasText(queryReq.getKeyword())) {
            String keyword = queryReq.getKeyword();
            wrapper.and(w -> w.like(MemoEntity::getTitle, keyword)
                    .or()
                    .like(MemoEntity::getContent, keyword));
        }

        // 排序：置顶的在前，然后按更新时间倒序
        wrapper.orderByDesc(MemoEntity::getPinned)
                .orderByDesc(MemoEntity::getUpdatedAt);

        List<MemoEntity> memoList = memoMapper.selectList(wrapper);
        return memoList.stream()
                .map(this::convertToResp)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void togglePinned(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        MemoEntity memo = memoMapper.selectById(id);
        if (memo == null) {
            throw new BusinessException("便签不存在");
        }

        // 验证是否为当前用户的便签
        if (!memo.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此便签");
        }

        // 切换置顶状态
        memo.setPinned(memo.getPinned() == 0 ? 1 : 0);
        memoMapper.updateById(memo);
        String action = memo.getPinned() == 1 ? "置顶" : "取消置顶";
        log.info("便签{}成功, userId={}, memoId={}", action, userId, id);
    }

    /**
     * 转换为响应对象
     */
    private MemoInfoResp convertToResp(MemoEntity memo) {
        MemoInfoResp resp = new MemoInfoResp();
        BeanUtils.copyProperties(memo, resp);
        return resp;
    }
}
