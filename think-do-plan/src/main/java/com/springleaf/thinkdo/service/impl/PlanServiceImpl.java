package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.thinkdo.domain.entity.PlanCategoryEntity;
import com.springleaf.thinkdo.domain.entity.PlanEntity;
import com.springleaf.thinkdo.domain.request.CreatePlanReq;
import com.springleaf.thinkdo.domain.request.CreateQuadrantPlanReq;
import com.springleaf.thinkdo.domain.request.PlanQueryReq;
import com.springleaf.thinkdo.domain.request.UpdatePlanReq;
import com.springleaf.thinkdo.domain.response.PlanInfoResp;
import com.springleaf.thinkdo.domain.response.PlanQuadrantResp;
import com.springleaf.thinkdo.domain.response.PlanQuadrantResp.PlanQuadrantInfoResp;
import com.springleaf.thinkdo.enums.PlanPriorityEnum;
import com.springleaf.thinkdo.enums.PlanQuadrantEnum;
import com.springleaf.thinkdo.enums.PlanRepeatTypeEnum;
import com.springleaf.thinkdo.enums.PlanStatusEnum;
import com.springleaf.thinkdo.enums.PlanTypeEnum;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.PlanCategoryMapper;
import com.springleaf.thinkdo.mapper.PlanMapper;
import com.springleaf.thinkdo.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 计划Service实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlanServiceImpl extends ServiceImpl<PlanMapper, PlanEntity> implements PlanService {

    private final PlanMapper planMapper;
    private final PlanCategoryMapper planCategoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPlan(CreatePlanReq createPlanReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 验证分类ID
        if (createPlanReq.getCategoryId() != null) {
            PlanCategoryEntity category = planCategoryMapper.selectById(createPlanReq.getCategoryId());
            if (category == null) {
                throw new BusinessException("分类不存在");
            }
            if (!category.getUserId().equals(userId)) {
                throw new BusinessException("无权使用此分类");
            }
        }

        // 验证开始时间和截止时间：要么都填要么都不填
        if ((createPlanReq.getStartTime() == null && createPlanReq.getDueTime() != null) ||
            (createPlanReq.getStartTime() != null && createPlanReq.getDueTime() == null)) {
            throw new BusinessException("开始时间和截止时间必须同时填写或同时为空");
        }

        // 验证时间范围：开始时间不能晚于截止时间
        if (createPlanReq.getStartTime() != null && createPlanReq.getDueTime() != null &&
            createPlanReq.getStartTime().isAfter(createPlanReq.getDueTime())) {
            throw new BusinessException("开始时间不能晚于截止时间");
        }

        // 验证重复类型和重复截止日期
        if (createPlanReq.getRepeatType() != null && createPlanReq.getRepeatType() > 0) {
            if (!PlanRepeatTypeEnum.isValid(createPlanReq.getRepeatType())) {
                throw new BusinessException("无效的重复类型");
            }
            // 如果设置了重复截止日期，验证日期格式
            if (createPlanReq.getRepeatUntil() != null && createPlanReq.getStartTime() != null &&
                createPlanReq.getRepeatUntil().isBefore(createPlanReq.getStartTime().toLocalDate())) {
                throw new BusinessException("重复截止日期不能早于开始时间");
            }
        }

        PlanEntity plan = new PlanEntity();
        plan.setUserId(userId);
        plan.setType(createPlanReq.getType() != null ? createPlanReq.getType() : PlanTypeEnum.NORMAL.getCode());
        plan.setCategoryId(createPlanReq.getCategoryId());
        plan.setTitle(createPlanReq.getTitle());
        plan.setDescription(createPlanReq.getDescription());
        plan.setPriority(createPlanReq.getPriority() != null ? createPlanReq.getPriority() : PlanPriorityEnum.MEDIUM.getCode());
        plan.setQuadrant(createPlanReq.getQuadrant() != null ? createPlanReq.getQuadrant() : PlanQuadrantEnum.NONE.getCode());
        plan.setTags(createPlanReq.getTags());
        plan.setStartTime(createPlanReq.getStartTime());
        plan.setDueTime(createPlanReq.getDueTime());
        plan.setRepeatType(createPlanReq.getRepeatType() != null ? createPlanReq.getRepeatType() : PlanRepeatTypeEnum.NONE.getCode());
        plan.setRepeatConf(createPlanReq.getRepeatConf());
        plan.setRepeatUntil(createPlanReq.getRepeatUntil());
        plan.setStatus(PlanStatusEnum.NOT_STARTED.getCode());

        planMapper.insert(plan);
        log.info("创建计划成功, userId={}, planId={}", userId, plan.getId());

        return plan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createQuadrantPlan(CreateQuadrantPlanReq createQuadrantPlanReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 验证四象限状态
        if (!PlanQuadrantEnum.isValid(createQuadrantPlanReq.getQuadrant())) {
            throw new BusinessException("无效的四象限状态");
        }

        PlanEntity plan = new PlanEntity();
        plan.setUserId(userId);
        plan.setType(PlanTypeEnum.QUADRANT.getCode());
        plan.setTitle(createQuadrantPlanReq.getTitle());
        plan.setQuadrant(createQuadrantPlanReq.getQuadrant());
        plan.setPriority(PlanPriorityEnum.MEDIUM.getCode());
        plan.setStatus(PlanStatusEnum.NOT_STARTED.getCode());

        planMapper.insert(plan);
        log.info("创建四象限计划成功, userId={}, planId={}, quadrant={}", userId, plan.getId(), createQuadrantPlanReq.getQuadrant());

        return plan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlan(UpdatePlanReq updatePlanReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        PlanEntity plan = planMapper.selectById(updatePlanReq.getId());
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }

        // 验证是否为当前用户的计划
        if (!plan.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此计划");
        }

        // 验证分类ID
        if (updatePlanReq.getCategoryId() != null) {
            PlanCategoryEntity category = planCategoryMapper.selectById(updatePlanReq.getCategoryId());
            if (category == null) {
                throw new BusinessException("分类不存在");
            }
            if (!category.getUserId().equals(userId)) {
                throw new BusinessException("无权使用此分类");
            }
        }

        // 如果更新开始时间和截止时间，需要验证
        LocalDateTime newStartTime = updatePlanReq.getStartTime();
        LocalDateTime newDueTime = updatePlanReq.getDueTime();
        LocalDateTime currentStartTime = plan.getStartTime();
        LocalDateTime currentDueTime = plan.getDueTime();

        // 确定实际的开始时间和截止时间
        LocalDateTime finalStartTime = (newStartTime != null) ? newStartTime : currentStartTime;
        LocalDateTime finalDueTime = (newDueTime != null) ? newDueTime : currentDueTime;

        // 如果只提供了一个时间，检查另一个是否存在
        if ((newStartTime != null && newDueTime == null && currentDueTime == null) ||
            (newDueTime != null && newStartTime == null && currentStartTime == null)) {
            throw new BusinessException("开始时间和截止时间必须同时填写");
        }

        // 验证时间范围
        if (finalStartTime != null && finalDueTime != null && finalStartTime.isAfter(finalDueTime)) {
            throw new BusinessException("开始时间不能晚于截止时间");
        }

        // 更新字段
        if (updatePlanReq.getCategoryId() != null) {
            plan.setCategoryId(updatePlanReq.getCategoryId());
        }
        if (StringUtils.hasText(updatePlanReq.getTitle())) {
            plan.setTitle(updatePlanReq.getTitle());
        }
        if (updatePlanReq.getDescription() != null) {
            plan.setDescription(updatePlanReq.getDescription());
        }
        if (updatePlanReq.getPriority() != null) {
            if (!PlanPriorityEnum.isValid(updatePlanReq.getPriority())) {
                throw new BusinessException("无效的优先级");
            }
            plan.setPriority(updatePlanReq.getPriority());
        }
        if (updatePlanReq.getQuadrant() != null) {
            if (!PlanQuadrantEnum.isValid(updatePlanReq.getQuadrant())) {
                throw new BusinessException("无效的四象限状态");
            }
            plan.setQuadrant(updatePlanReq.getQuadrant());
        }
        if (updatePlanReq.getTags() != null) {
            plan.setTags(updatePlanReq.getTags());
        }
        if (newStartTime != null) {
            plan.setStartTime(newStartTime);
        }
        if (newDueTime != null) {
            plan.setDueTime(newDueTime);
        }
        if (updatePlanReq.getRepeatType() != null) {
            if (!PlanRepeatTypeEnum.isValid(updatePlanReq.getRepeatType())) {
                throw new BusinessException("无效的重复类型");
            }
            plan.setRepeatType(updatePlanReq.getRepeatType());
        }
        if (updatePlanReq.getRepeatConf() != null) {
            plan.setRepeatConf(updatePlanReq.getRepeatConf());
        }
        if (updatePlanReq.getRepeatUntil() != null) {
            plan.setRepeatUntil(updatePlanReq.getRepeatUntil());
        }
        if (updatePlanReq.getStatus() != null) {
            if (!PlanStatusEnum.isValid(updatePlanReq.getStatus())) {
                throw new BusinessException("无效的状态");
            }
            Integer oldStatus = plan.getStatus();
            plan.setStatus(updatePlanReq.getStatus());
            // 如果状态从未完成变为完成，记录完成时间
            if (PlanStatusEnum.NOT_STARTED.getCode().equals(oldStatus) &&
                PlanStatusEnum.COMPLETED.getCode().equals(updatePlanReq.getStatus())) {
                plan.setCompletedAt(LocalDateTime.now());
            }
            // 如果状态从完成变为未完成，清空完成时间
            else if (PlanStatusEnum.COMPLETED.getCode().equals(oldStatus) &&
                PlanStatusEnum.NOT_STARTED.getCode().equals(updatePlanReq.getStatus())) {
                plan.setCompletedAt(null);
            }
        }

        planMapper.updateById(plan);
        log.info("更新计划成功, userId={}, planId={}", userId, plan.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        PlanEntity plan = planMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }

        // 验证是否为当前用户的计划
        if (!plan.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此计划");
        }

        planMapper.deleteById(id);
        log.info("删除计划成功, userId={}, planId={}", userId, id);
    }

    @Override
    public PlanInfoResp getPlanById(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        PlanEntity plan = planMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }

        // 验证是否为当前用户的计划
        if (!plan.getUserId().equals(userId)) {
            throw new BusinessException("无权查看此计划");
        }

        return convertToResp(plan);
    }

    @Override
    public List<PlanInfoResp> getPlanList(PlanQueryReq queryReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        LambdaQueryWrapper<PlanEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlanEntity::getUserId, userId);

        // 按分类筛选
        if (queryReq.getCategoryId() != null) {
            wrapper.eq(PlanEntity::getCategoryId, queryReq.getCategoryId());
        }

        // 按计划类型筛选
        if (queryReq.getType() != null) {
            wrapper.eq(PlanEntity::getType, queryReq.getType());
        }

        // 按优先级筛选
        if (queryReq.getPriority() != null) {
            wrapper.eq(PlanEntity::getPriority, queryReq.getPriority());
        }

        // 按四象限筛选
        if (queryReq.getQuadrant() != null) {
            wrapper.eq(PlanEntity::getQuadrant, queryReq.getQuadrant());
        }

        // 按状态筛选
        if (queryReq.getStatus() != null) {
            wrapper.eq(PlanEntity::getStatus, queryReq.getStatus());
        }

        // 按重复类型筛选
        if (queryReq.getRepeatType() != null) {
            wrapper.eq(PlanEntity::getRepeatType, queryReq.getRepeatType());
        }

        // 按标签筛选
        if (StringUtils.hasText(queryReq.getTags())) {
            wrapper.like(PlanEntity::getTags, queryReq.getTags());
        }

        // 按开始时间范围筛选
        if (queryReq.getStartTimeFrom() != null) {
            wrapper.ge(PlanEntity::getStartTime, queryReq.getStartTimeFrom());
        }
        if (queryReq.getStartTimeTo() != null) {
            wrapper.le(PlanEntity::getStartTime, queryReq.getStartTimeTo());
        }

        // 按截止时间范围筛选
        if (queryReq.getDueTimeFrom() != null) {
            wrapper.ge(PlanEntity::getDueTime, queryReq.getDueTimeFrom());
        }
        if (queryReq.getDueTimeTo() != null) {
            wrapper.le(PlanEntity::getDueTime, queryReq.getDueTimeTo());
        }

        // 关键词搜索
        if (StringUtils.hasText(queryReq.getKeyword())) {
            String keyword = queryReq.getKeyword();
            wrapper.and(w -> w.like(PlanEntity::getTitle, keyword)
                    .or()
                    .like(PlanEntity::getDescription, keyword));
        }

        // 排序：未完成的在前，然后按优先级降序，最后按创建时间倒序
        wrapper.orderByAsc(PlanEntity::getStatus)
                .orderByDesc(PlanEntity::getPriority)
                .orderByDesc(PlanEntity::getCreatedAt);

        List<PlanEntity> planList = planMapper.selectList(wrapper);

        // 获取所有分类ID，用于查询分类名称
        List<Long> categoryIds = planList.stream()
                .map(PlanEntity::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> categoryNameMap = Map.of();
        if (!categoryIds.isEmpty()) {
            LambdaQueryWrapper<PlanCategoryEntity> categoryWrapper = new LambdaQueryWrapper<>();
            categoryWrapper.in(PlanCategoryEntity::getId, categoryIds)
                    .select(PlanCategoryEntity::getId, PlanCategoryEntity::getName);
            List<PlanCategoryEntity> categories = planCategoryMapper.selectList(categoryWrapper);
            categoryNameMap = categories.stream()
                    .collect(Collectors.toMap(PlanCategoryEntity::getId, PlanCategoryEntity::getName));
        }

        Map<Long, String> finalCategoryNameMap = categoryNameMap;
        return planList.stream()
                .map(plan -> {
                    PlanInfoResp resp = convertToResp(plan);
                    if (plan.getCategoryId() != null) {
                        resp.setCategoryName(finalCategoryNameMap.get(plan.getCategoryId()));
                    }
                    return resp;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        PlanEntity plan = planMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }

        // 验证是否为当前用户的计划
        if (!plan.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此计划");
        }

        // 切换状态
        if (PlanStatusEnum.NOT_STARTED.getCode().equals(plan.getStatus())) {
            plan.setStatus(PlanStatusEnum.COMPLETED.getCode());
            plan.setCompletedAt(LocalDateTime.now());
        } else {
            plan.setStatus(PlanStatusEnum.NOT_STARTED.getCode());
            plan.setCompletedAt(null);
        }

        planMapper.updateById(plan);
        String action = PlanStatusEnum.COMPLETED.getCode().equals(plan.getStatus()) ? "完成" : "未完成";
        log.info("计划状态切换成功, userId={}, planId={}, status={}", userId, id, action);
    }

    /**
     * 转换为响应对象
     */
    private PlanInfoResp convertToResp(PlanEntity plan) {
        PlanInfoResp resp = new PlanInfoResp();
        BeanUtils.copyProperties(plan, resp);
        return resp;
    }

    @Override
    public PlanQuadrantResp getQuadrantPlans() {
        Long userId = StpUtil.getLoginIdAsLong();

        // 查询所有未完成的计划
        LambdaQueryWrapper<PlanEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlanEntity::getUserId, userId)
                .eq(PlanEntity::getStatus, PlanStatusEnum.NOT_STARTED.getCode())
                .orderByDesc(PlanEntity::getPriority)
                .orderByDesc(PlanEntity::getCreatedAt);

        List<PlanEntity> planList = planMapper.selectList(wrapper);

        // 筛选四象限计划：quadrant不为null 或 type=1（四象限计划），并按id去重
        List<PlanQuadrantInfoResp> quadrantPlanList = planList.stream()
                .filter(plan -> plan.getQuadrant() != null || PlanTypeEnum.QUADRANT.getCode().equals(plan.getType()))
                .collect(Collectors.toMap(
                        PlanEntity::getId,
                        this::convertToQuadrantInfoResp,
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .toList();

        // 按四象限分组
        Map<Integer, List<PlanQuadrantInfoResp>> quadrantMap = quadrantPlanList.stream()
                .collect(Collectors.groupingBy(plan -> plan.getQuadrant() != null ? plan.getQuadrant() : PlanQuadrantEnum.NONE.getCode()));

        // 构建响应对象
        PlanQuadrantResp resp = new PlanQuadrantResp();
        resp.setImportantUrgent(quadrantMap.getOrDefault(PlanQuadrantEnum.IMPORTANT_URGENT.getCode(), Collections.emptyList()));
        resp.setImportantNotUrgent(quadrantMap.getOrDefault(PlanQuadrantEnum.IMPORTANT_NOT_URGENT.getCode(), Collections.emptyList()));
        resp.setUrgentNotImportant(quadrantMap.getOrDefault(PlanQuadrantEnum.URGENT_NOT_IMPORTANT.getCode(), Collections.emptyList()));
        resp.setNotImportantNotUrgent(quadrantMap.getOrDefault(PlanQuadrantEnum.NOT_IMPORTANT_NOT_URGENT.getCode(), Collections.emptyList()));

        return resp;
    }

    /**
     * 转换为四象限信息响应对象
     */
    private PlanQuadrantInfoResp convertToQuadrantInfoResp(PlanEntity plan) {
        PlanQuadrantInfoResp resp = new PlanQuadrantInfoResp();
        resp.setId(plan.getId());
        resp.setType(plan.getType());
        resp.setTitle(plan.getTitle());
        resp.setDescription(plan.getDescription());
        resp.setQuadrant(plan.getQuadrant());
        resp.setStartTime(plan.getStartTime());
        resp.setDueTime(plan.getDueTime());
        resp.setRepeatType(plan.getRepeatType());
        resp.setRepeatConf(plan.getRepeatConf());
        resp.setRepeatUntil(plan.getRepeatUntil());
        resp.setStatus(plan.getStatus());
        return resp;
    }
}
