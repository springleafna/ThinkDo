package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.thinkdo.domain.entity.PlanCategoryEntity;
import com.springleaf.thinkdo.domain.entity.PlanEntity;
import com.springleaf.thinkdo.domain.request.CreatePlanCategoryReq;
import com.springleaf.thinkdo.domain.request.UpdatePlanCategoryReq;
import com.springleaf.thinkdo.domain.response.PlanCategoryInfoResp;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.PlanCategoryMapper;
import com.springleaf.thinkdo.mapper.PlanMapper;
import com.springleaf.thinkdo.service.PlanCategoryService;
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
 * 计划分类Service实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlanCategoryServiceImpl extends ServiceImpl<PlanCategoryMapper, PlanCategoryEntity> implements PlanCategoryService {

    private final PlanCategoryMapper planCategoryMapper;
    private final PlanMapper planMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(CreatePlanCategoryReq createPlanCategoryReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 检查分类名称是否已存在
        LambdaQueryWrapper<PlanCategoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlanCategoryEntity::getUserId, userId)
                .eq(PlanCategoryEntity::getName, createPlanCategoryReq.getName());
        if (planCategoryMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("分类名称已存在");
        }

        PlanCategoryEntity category = new PlanCategoryEntity();
        category.setUserId(userId);
        category.setName(createPlanCategoryReq.getName());

        planCategoryMapper.insert(category);
        log.info("创建计划分类成功, userId={}, categoryId={}", userId, category.getId());

        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(UpdatePlanCategoryReq updatePlanCategoryReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        PlanCategoryEntity category = planCategoryMapper.selectById(updatePlanCategoryReq.getId());
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        // 验证是否为当前用户的分类
        if (!category.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此分类");
        }

        // 检查分类名称是否已存在（排除自己）
        if (StringUtils.hasText(updatePlanCategoryReq.getName())) {
            LambdaQueryWrapper<PlanCategoryEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PlanCategoryEntity::getUserId, userId)
                    .eq(PlanCategoryEntity::getName, updatePlanCategoryReq.getName())
                    .ne(PlanCategoryEntity::getId, updatePlanCategoryReq.getId());
            if (planCategoryMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("分类名称已存在");
            }
            category.setName(updatePlanCategoryReq.getName());
        }

        planCategoryMapper.updateById(category);
        log.info("更新计划分类成功, userId={}, categoryId={}", userId, category.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        PlanCategoryEntity category = planCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        // 验证是否为当前用户的分类
        if (!category.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此分类");
        }

        // 检查该分类下是否还有计划
        LambdaQueryWrapper<PlanEntity> planWrapper = new LambdaQueryWrapper<>();
        planWrapper.eq(PlanEntity::getCategoryId, id);
        long planCount = planMapper.selectCount(planWrapper);
        if (planCount > 0) {
            throw new BusinessException("该分类下还有" + planCount + "个计划，无法删除");
        }

        planCategoryMapper.deleteById(id);
        log.info("删除计划分类成功, userId={}, categoryId={}", userId, id);
    }

    @Override
    public PlanCategoryInfoResp getCategoryById(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        PlanCategoryEntity category = planCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        // 验证是否为当前用户的分类
        if (!category.getUserId().equals(userId)) {
            throw new BusinessException("无权查看此分类");
        }

        // 查询该分类下的计划数量
        LambdaQueryWrapper<PlanEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlanEntity::getCategoryId, id);
        long planCount = planMapper.selectCount(wrapper);

        PlanCategoryInfoResp resp = new PlanCategoryInfoResp();
        BeanUtils.copyProperties(category, resp);
        resp.setPlanCount((int) planCount);
        return resp;
    }

    @Override
    public List<PlanCategoryInfoResp> getCategoryList() {
        Long userId = StpUtil.getLoginIdAsLong();

        LambdaQueryWrapper<PlanCategoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlanCategoryEntity::getUserId, userId)
                .orderByAsc(PlanCategoryEntity::getCreatedAt);

        List<PlanCategoryEntity> categoryList = planCategoryMapper.selectList(wrapper);

        // 查询每个分类下的计划数量
        List<Long> categoryIds = categoryList.stream()
                .map(PlanCategoryEntity::getId)
                .collect(Collectors.toList());

        Map<Long, Long> planCountMap = Map.of();
        if (!categoryIds.isEmpty()) {
            LambdaQueryWrapper<PlanEntity> planWrapper = new LambdaQueryWrapper<>();
            planWrapper.in(PlanEntity::getCategoryId, categoryIds)
                    .select(PlanEntity::getCategoryId);
            List<PlanEntity> plans = planMapper.selectList(planWrapper);
            planCountMap = plans.stream()
                    .collect(Collectors.groupingBy(PlanEntity::getCategoryId, Collectors.counting()));
        }

        Map<Long, Long> finalPlanCountMap = planCountMap;
        return categoryList.stream()
                .map(category -> {
                    PlanCategoryInfoResp resp = new PlanCategoryInfoResp();
                    BeanUtils.copyProperties(category, resp);
                    resp.setPlanCount(finalPlanCountMap.getOrDefault(category.getId(), 0L).intValue());
                    return resp;
                })
                .collect(Collectors.toList());
    }
}
