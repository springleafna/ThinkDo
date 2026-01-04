package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.thinkdo.domain.entity.PlanEntity;
import com.springleaf.thinkdo.domain.entity.PlanStepEntity;
import com.springleaf.thinkdo.domain.request.CreatePlanStepReq;
import com.springleaf.thinkdo.domain.request.UpdatePlanStepReq;
import com.springleaf.thinkdo.domain.response.PlanStepInfoResp;
import com.springleaf.thinkdo.enums.PlanStatusEnum;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.mapper.PlanMapper;
import com.springleaf.thinkdo.mapper.PlanStepMapper;
import com.springleaf.thinkdo.service.PlanStepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 计划步骤Service实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlanStepServiceImpl extends ServiceImpl<PlanStepMapper, PlanStepEntity> implements PlanStepService {

    private final PlanStepMapper planStepMapper;
    private final PlanMapper planMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStep(CreatePlanStepReq createPlanStepReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 验证计划是否存在且属于当前用户
        PlanEntity plan = planMapper.selectById(createPlanStepReq.getPlanId());
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }
        if (!plan.getUserId().equals(userId)) {
            throw new BusinessException("无权为此计划添加步骤");
        }

        PlanStepEntity step = new PlanStepEntity();
        step.setPlanId(createPlanStepReq.getPlanId());
        step.setTitle(createPlanStepReq.getTitle());
        step.setStatus(PlanStatusEnum.NOT_STARTED.getCode());

        planStepMapper.insert(step);
        log.info("创建计划步骤成功, userId={}, planId={}, stepId={}", userId, plan.getId(), step.getId());

        return step.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStep(UpdatePlanStepReq updatePlanStepReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        PlanStepEntity step = planStepMapper.selectById(updatePlanStepReq.getId());
        if (step == null) {
            throw new BusinessException("步骤不存在");
        }

        // 验证步骤所属的计划是否属于当前用户
        PlanEntity plan = planMapper.selectById(step.getPlanId());
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此步骤");
        }

        // 更新字段
        if (StringUtils.hasText(updatePlanStepReq.getTitle())) {
            step.setTitle(updatePlanStepReq.getTitle());
        }
        if (updatePlanStepReq.getStatus() != null) {
            if (!PlanStatusEnum.isValid(updatePlanStepReq.getStatus())) {
                throw new BusinessException("无效的状态");
            }
            step.setStatus(updatePlanStepReq.getStatus());
            // 如果步骤状态变为完成，检查是否所有步骤都完成，如果是则完成计划
            if (PlanStatusEnum.COMPLETED.getCode().equals(updatePlanStepReq.getStatus())) {
                checkAndCompletePlan(step.getPlanId());
            }
        }

        planStepMapper.updateById(step);
        log.info("更新计划步骤成功, userId={}, stepId={}", userId, step.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStep(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        PlanStepEntity step = planStepMapper.selectById(id);
        if (step == null) {
            throw new BusinessException("步骤不存在");
        }

        // 验证步骤所属的计划是否属于当前用户
        PlanEntity plan = planMapper.selectById(step.getPlanId());
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此步骤");
        }

        planStepMapper.deleteById(id);
        log.info("删除计划步骤成功, userId={}, stepId={}", userId, id);
    }

    @Override
    public PlanStepInfoResp getStepById(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        PlanStepEntity step = planStepMapper.selectById(id);
        if (step == null) {
            throw new BusinessException("步骤不存在");
        }

        // 验证步骤所属的计划是否属于当前用户
        PlanEntity plan = planMapper.selectById(step.getPlanId());
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BusinessException("无权查看此步骤");
        }

        return convertToResp(step);
    }

    @Override
    public List<PlanStepInfoResp> getStepListByPlanId(Long planId) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 验证计划是否存在且属于当前用户
        PlanEntity plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }
        if (!plan.getUserId().equals(userId)) {
            throw new BusinessException("无权查看此计划的步骤");
        }

        LambdaQueryWrapper<PlanStepEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlanStepEntity::getPlanId, planId)
                .orderByAsc(PlanStepEntity::getCreatedAt);

        List<PlanStepEntity> stepList = planStepMapper.selectList(wrapper);
        return stepList.stream()
                .map(this::convertToResp)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        PlanStepEntity step = planStepMapper.selectById(id);
        if (step == null) {
            throw new BusinessException("步骤不存在");
        }

        // 验证步骤所属的计划是否属于当前用户
        PlanEntity plan = planMapper.selectById(step.getPlanId());
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此步骤");
        }

        // 切换状态
        boolean isCompleting = PlanStatusEnum.NOT_STARTED.getCode().equals(step.getStatus());
        if (isCompleting) {
            step.setStatus(PlanStatusEnum.COMPLETED.getCode());
        } else {
            step.setStatus(PlanStatusEnum.NOT_STARTED.getCode());
        }

        planStepMapper.updateById(step);
        String action = isCompleting ? "完成" : "未完成";
        log.info("步骤状态切换成功, userId={}, stepId={}, status={}", userId, id, action);

        // 如果步骤状态变为完成，检查是否所有步骤都完成，如果是则完成计划
        if (isCompleting) {
            checkAndCompletePlan(step.getPlanId());
        }
    }

    /**
     * 检查计划的所有步骤是否都已完成，如果是则将计划状态设置为已完成
     * @param planId 计划ID
     */
    private void checkAndCompletePlan(Long planId) {
        // 查询该计划下的所有步骤
        LambdaQueryWrapper<PlanStepEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlanStepEntity::getPlanId, planId);
        List<PlanStepEntity> steps = planStepMapper.selectList(wrapper);

        // 如果没有步骤，不做处理
        if (steps.isEmpty()) {
            return;
        }

        // 检查是否所有步骤都已完成
        boolean allCompleted = steps.stream()
                .allMatch(s -> PlanStatusEnum.COMPLETED.getCode().equals(s.getStatus()));

        if (allCompleted) {
            // 所有步骤都完成，将计划状态设置为已完成
            PlanEntity plan = planMapper.selectById(planId);
            if (plan != null && PlanStatusEnum.NOT_STARTED.getCode().equals(plan.getStatus())) {
                plan.setStatus(PlanStatusEnum.COMPLETED.getCode());
                plan.setCompletedAt(LocalDateTime.now());
                planMapper.updateById(plan);
                log.info("计划所有步骤已完成，自动完成计划, planId={}", planId);
            }
        }
    }

    /**
     * 转换为响应对象
     */
    private PlanStepInfoResp convertToResp(PlanStepEntity step) {
        PlanStepInfoResp resp = new PlanStepInfoResp();
        BeanUtils.copyProperties(step, resp);
        return resp;
    }
}
