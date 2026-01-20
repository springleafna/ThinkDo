package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.thinkdo.domain.entity.PlanEntity;
import com.springleaf.thinkdo.domain.entity.PlanExecutionEntity;
import com.springleaf.thinkdo.domain.request.CreatePlanExecutionReq;
import com.springleaf.thinkdo.domain.response.PlanExecutionInfoResp;
import com.springleaf.thinkdo.enums.PlanExecutionStatusEnum;
import com.springleaf.thinkdo.enums.PlanPriorityEnum;
import com.springleaf.thinkdo.enums.PlanStatusEnum;
import com.springleaf.thinkdo.enums.PlanTypeEnum;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.PlanExecutionMapper;
import com.springleaf.thinkdo.mapper.PlanMapper;
import com.springleaf.thinkdo.service.PlanExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 每日清单Service实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlanExecutionServiceImpl extends ServiceImpl<PlanExecutionMapper, PlanExecutionEntity> implements PlanExecutionService {

    private final PlanExecutionMapper planExecutionMapper;
    private final PlanMapper planMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPlanExecution(CreatePlanExecutionReq createPlanExecutionReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 验证开始时间和截止时间：要么都填要么都不填
        if ((createPlanExecutionReq.getStartTime() == null && createPlanExecutionReq.getDueTime() != null) ||
            (createPlanExecutionReq.getStartTime() != null && createPlanExecutionReq.getDueTime() == null)) {
            throw new BusinessException("开始时间和截止时间必须同时填写或同时为空");
        }

        // 验证时间范围：开始时间不能晚于截止时间
        if (createPlanExecutionReq.getStartTime() != null && createPlanExecutionReq.getDueTime() != null &&
            createPlanExecutionReq.getStartTime().isAfter(createPlanExecutionReq.getDueTime())) {
            throw new BusinessException("开始时间不能晚于截止时间");
        }

        // 确定执行日期，如果未指定则使用当天
        LocalDate executeDate = createPlanExecutionReq.getExecuteDate() != null ? createPlanExecutionReq.getExecuteDate() : LocalDate.now();

        // 先创建计划（type=2 每日计划）
        PlanEntity plan = new PlanEntity();
        plan.setUserId(userId);
        plan.setType(PlanTypeEnum.DAILY.getCode());
        plan.setTitle(createPlanExecutionReq.getTitle());
        plan.setPriority(createPlanExecutionReq.getPriority() != null ? createPlanExecutionReq.getPriority() : PlanPriorityEnum.MEDIUM.getCode());
        plan.setStartTime(createPlanExecutionReq.getStartTime());
        plan.setDueTime(createPlanExecutionReq.getDueTime());
        plan.setTags(createPlanExecutionReq.getTags());
        plan.setStatus(PlanStatusEnum.NOT_STARTED.getCode());

        planMapper.insert(plan);
        log.info("创建每日计划成功, userId={}, planId={}", userId, plan.getId());

        // 再创建执行记录
        PlanExecutionEntity planExecution = new PlanExecutionEntity();
        planExecution.setPlanId(plan.getId());
        planExecution.setExecuteDate(executeDate);
        planExecution.setStatus(PlanExecutionStatusEnum.NOT_COMPLETED.getCode());

        planExecutionMapper.insert(planExecution);
        log.info("创建每日清单执行记录成功, userId={}, planId={}, executeDate={}", userId, plan.getId(), executeDate);

        return planExecution.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlanExecution(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        PlanExecutionEntity planExecution = planExecutionMapper.selectById(id);
        if (planExecution == null) {
            throw new BusinessException("每日清单不存在");
        }

        // 验证计划是否属于当前用户
        PlanEntity plan = planMapper.selectById(planExecution.getPlanId());
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此每日清单");
        }

        // 删除执行记录
        planExecutionMapper.deleteById(id);

        // 同步删除计划表中的数据
        planMapper.deleteById(planExecution.getPlanId());

        log.info("删除每日计划成功, userId={}, planExecutionId={}, planId={}", userId, id, planExecution.getPlanId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        PlanExecutionEntity planExecution = planExecutionMapper.selectById(id);
        if (planExecution == null) {
            throw new BusinessException("每日清单不存在");
        }

        // 验证计划是否属于当前用户
        PlanEntity plan = planMapper.selectById(planExecution.getPlanId());
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此每日清单");
        }

        // 切换执行记录状态
        if (PlanExecutionStatusEnum.NOT_COMPLETED.getCode().equals(planExecution.getStatus())) {
            planExecution.setStatus(PlanExecutionStatusEnum.COMPLETED.getCode());
            planExecution.setCompletedAt(LocalDateTime.now());
        } else {
            planExecution.setStatus(PlanExecutionStatusEnum.NOT_COMPLETED.getCode());
            planExecution.setCompletedAt(null);
        }
        planExecutionMapper.updateById(planExecution);

        // 同步更新计划表状态
        if (PlanExecutionStatusEnum.COMPLETED.getCode().equals(planExecution.getStatus())) {
            plan.setStatus(PlanStatusEnum.COMPLETED.getCode());
            plan.setCompletedAt(LocalDateTime.now());
        } else {
            plan.setStatus(PlanStatusEnum.NOT_STARTED.getCode());
            plan.setCompletedAt(null);
        }
        planMapper.updateById(plan);

        String action = PlanExecutionStatusEnum.COMPLETED.getCode().equals(planExecution.getStatus()) ? "完成" : "未完成";
        log.info("每日计划状态切换成功, userId={}, planExecutionId={}, planId={}, status={}", userId, id, plan.getId(), action);
    }

    @Override
    public List<PlanExecutionInfoResp> getPlanExecutionListByDate(LocalDate executeDate) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 查询当前用户的所有计划ID
        LambdaQueryWrapper<PlanEntity> planWrapper = new LambdaQueryWrapper<>();
        planWrapper.eq(PlanEntity::getUserId, userId)
                .select(PlanEntity::getId, PlanEntity::getTitle, PlanEntity::getDescription, PlanEntity::getType,
                        PlanEntity::getPriority, PlanEntity::getStartTime, PlanEntity::getDueTime, PlanEntity::getTags);
        List<PlanEntity> planList = planMapper.selectList(planWrapper);
        List<Long> planIds = planList.stream().map(PlanEntity::getId).collect(Collectors.toList());

        if (planIds.isEmpty()) {
            return List.of();
        }

        // 查询指定日期的执行记录
        LambdaQueryWrapper<PlanExecutionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PlanExecutionEntity::getPlanId, planIds)
                .eq(PlanExecutionEntity::getExecuteDate, executeDate)
                .orderByDesc(PlanExecutionEntity::getCreatedAt);

        List<PlanExecutionEntity> executionList = planExecutionMapper.selectList(wrapper);

        // 构建计划信息映射
        Map<Long, PlanEntity> planMap = planList.stream()
                .collect(Collectors.toMap(PlanEntity::getId, p -> p));

        return executionList.stream()
                .map(execution -> convertToResp(execution, planMap.get(execution.getPlanId())))
                .collect(Collectors.toList());
    }

    @Override
    public PlanExecutionInfoResp getPlanExecutionById(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        PlanExecutionEntity planExecution = planExecutionMapper.selectById(id);
        if (planExecution == null) {
            throw new BusinessException("每日清单不存在");
        }

        // 验证计划是否属于当前用户
        PlanEntity plan = planMapper.selectById(planExecution.getPlanId());
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BusinessException("无权查看此每日清单");
        }

        return convertToResp(planExecution, plan);
    }

    @Override
    public List<PlanExecutionInfoResp> getPlanExecutionList(LocalDate executeDate, Integer status) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 查询当前用户的所有计划ID
        LambdaQueryWrapper<PlanEntity> planWrapper = new LambdaQueryWrapper<>();
        planWrapper.eq(PlanEntity::getUserId, userId)
                .select(PlanEntity::getId, PlanEntity::getTitle, PlanEntity::getDescription, PlanEntity::getType,
                        PlanEntity::getPriority, PlanEntity::getStartTime, PlanEntity::getDueTime, PlanEntity::getTags);
        List<PlanEntity> planList = planMapper.selectList(planWrapper);
        List<Long> planIds = planList.stream().map(PlanEntity::getId).collect(Collectors.toList());

        if (planIds.isEmpty()) {
            return List.of();
        }

        // 构建查询条件
        LambdaQueryWrapper<PlanExecutionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PlanExecutionEntity::getPlanId, planIds);

        if (executeDate != null) {
            wrapper.eq(PlanExecutionEntity::getExecuteDate, executeDate);
        }
        if (status != null) {
            wrapper.eq(PlanExecutionEntity::getStatus, status);
        }

        wrapper.orderByDesc(PlanExecutionEntity::getExecuteDate)
                .orderByDesc(PlanExecutionEntity::getCreatedAt);

        List<PlanExecutionEntity> executionList = planExecutionMapper.selectList(wrapper);

        // 构建计划信息映射
        Map<Long, PlanEntity> planMap = planList.stream()
                .collect(Collectors.toMap(PlanEntity::getId, p -> p));

        return executionList.stream()
                .map(execution -> convertToResp(execution, planMap.get(execution.getPlanId())))
                .collect(Collectors.toList());
    }

    /**
     * 转换为响应对象
     */
    private PlanExecutionInfoResp convertToResp(PlanExecutionEntity execution, PlanEntity plan) {
        PlanExecutionInfoResp resp = new PlanExecutionInfoResp();
        BeanUtils.copyProperties(execution, resp);
        if (plan != null) {
            resp.setPlanTitle(plan.getTitle());
            resp.setPlanType(plan.getType());
            resp.setPriority(plan.getPriority());
            resp.setStartTime(plan.getStartTime());
            resp.setDueTime(plan.getDueTime());
            resp.setTags(plan.getTags());
        }
        return resp;
    }
}
