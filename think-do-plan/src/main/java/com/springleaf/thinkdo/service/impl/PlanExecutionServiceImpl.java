package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springleaf.thinkdo.domain.entity.PlanEntity;
import com.springleaf.thinkdo.domain.entity.PlanExecutionEntity;
import com.springleaf.thinkdo.domain.request.CreatePlanExecutionReq;
import com.springleaf.thinkdo.domain.request.UpdatePlanExecutionReq;
import com.springleaf.thinkdo.domain.response.PlanExecutionInfoResp;
import com.springleaf.thinkdo.enums.*;
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
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
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
    private final ObjectMapper objectMapper;

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
    public void updatePlanExecution(UpdatePlanExecutionReq updatePlanExecutionReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        PlanExecutionEntity planExecution = planExecutionMapper.selectById(updatePlanExecutionReq.getId());
        if (planExecution == null) {
            throw new BusinessException("每日清单不存在");
        }

        // 获取关联的计划
        PlanEntity plan = planMapper.selectById(planExecution.getPlanId());
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此每日计划");
        }

        // 如果更新开始时间和截止时间，需要验证
        LocalDateTime newStartTime = updatePlanExecutionReq.getStartTime();
        LocalDateTime newDueTime = updatePlanExecutionReq.getDueTime();
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

        // 更新计划表字段
        if (updatePlanExecutionReq.getTitle() != null) {
            plan.setTitle(updatePlanExecutionReq.getTitle());
        }
        if (updatePlanExecutionReq.getPriority() != null) {
            if (!PlanPriorityEnum.isValid(updatePlanExecutionReq.getPriority())) {
                throw new BusinessException("无效的优先级");
            }
            plan.setPriority(updatePlanExecutionReq.getPriority());
        }
        if (newStartTime != null) {
            plan.setStartTime(newStartTime);
        }
        if (newDueTime != null) {
            plan.setDueTime(newDueTime);
        }
        if (updatePlanExecutionReq.getTags() != null) {
            plan.setTags(updatePlanExecutionReq.getTags());
        }

        planMapper.updateById(plan);
        log.info("更新每日计划成功, userId={}, planExecutionId={}, planId={}", userId, planExecution.getId(), plan.getId());
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

        // 1. 获取每日计划列表
        List<PlanEntity> dailyPlanList = getDailyPlans(userId, executeDate);
        List<PlanEntity> repeatPlanList = getRepeatPlans(userId, executeDate);

        // 2. 处理execution表中存在的计划
        List<PlanExecutionInfoResp> planExecutionInfoRespList = new ArrayList<>();

        // 处理每日计划
        processPlans(dailyPlanList, executeDate, planExecutionInfoRespList);
        // 处理重复计划
        processPlans(repeatPlanList, executeDate, planExecutionInfoRespList);

        return planExecutionInfoRespList;
    }

    /**
     * 获取每日计划列表
     */
    private List<PlanEntity> getDailyPlans(Long userId, LocalDate executeDate) {
        LocalDateTime start = executeDate.atStartOfDay();
        LocalDateTime end = executeDate.plusDays(1).atStartOfDay();

        LambdaQueryWrapper<PlanEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlanEntity::getUserId, userId)
                .eq(PlanEntity::getType, PlanTypeEnum.DAILY.getCode())
                .ge(PlanEntity::getCreatedAt, start)
                .lt(PlanEntity::getCreatedAt, end)
                .select(PlanEntity::getId, PlanEntity::getTitle, PlanEntity::getDescription,
                        PlanEntity::getType, PlanEntity::getPriority, PlanEntity::getStartTime,
                        PlanEntity::getDueTime, PlanEntity::getTags, PlanEntity::getStatus,
                        PlanEntity::getCompletedAt, PlanEntity::getCreatedAt, PlanEntity::getUpdatedAt);

        return planMapper.selectList(wrapper);
    }

    /**
     * 获取重复计划列表
     */
    private List<PlanEntity> getRepeatPlans(Long userId, LocalDate executeDate) {
        LambdaQueryWrapper<PlanEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlanEntity::getUserId, userId)
                .eq(PlanEntity::getType, PlanTypeEnum.NORMAL.getCode())
                .ne(PlanEntity::getRepeatType, PlanRepeatTypeEnum.NONE.getCode())
                .select(PlanEntity::getId, PlanEntity::getTitle, PlanEntity::getDescription, PlanEntity::getType, PlanEntity::getStatus,
                        PlanEntity::getCompletedAt, PlanEntity::getCreatedAt, PlanEntity::getUpdatedAt, PlanEntity::getPriority, PlanEntity::getStartTime,
                        PlanEntity::getDueTime, PlanEntity::getTags, PlanEntity::getRepeatType, PlanEntity::getRepeatConf, PlanEntity::getRepeatUntil);

        List<PlanEntity> repeatPlanList = planMapper.selectList(wrapper);

        repeatPlanList.removeIf(plan -> !isPlanActiveToday(plan, executeDate));

        return repeatPlanList;
    }

    /**
     * 处理计划列表，转换为响应对象
     */
    private void processPlans(List<PlanEntity> planList, LocalDate executeDate,
                              List<PlanExecutionInfoResp> resultList) {
        for (PlanEntity planEntity : planList) {
            PlanExecutionInfoResp resp = buildPlanExecutionResp(planEntity, executeDate);
            if (resp != null) {
                resultList.add(resp);
            }
        }
    }

    /**
     * 构建计划执行响应对象
     */
    private PlanExecutionInfoResp buildPlanExecutionResp(PlanEntity planEntity, LocalDate executeDate) {
        // 检查执行记录
        LambdaQueryWrapper<PlanExecutionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlanExecutionEntity::getPlanId, planEntity.getId())
                .eq(PlanExecutionEntity::getExecuteDate, executeDate);
        PlanExecutionEntity planExecutionEntity = planExecutionMapper.selectOne(wrapper);

        // 如果存在且已删除，则跳过
        if (planExecutionEntity != null && planExecutionEntity.getDeleted() == 1) {
            return null;
        }

        // 创建响应对象
        PlanExecutionInfoResp resp = new PlanExecutionInfoResp();

        // 设置状态：优先使用执行记录的状态，否则使用计划状态
        Integer status = (planExecutionEntity != null)
                ? planExecutionEntity.getStatus()
                : planEntity.getStatus();
        resp.setStatus(status);

        // 设置公共字段
        resp.setPlanId(planEntity.getId());
        resp.setPlanTitle(planEntity.getTitle());
        resp.setPlanType(planEntity.getType());
        resp.setPriority(planEntity.getPriority());
        resp.setStartTime(planEntity.getStartTime());
        resp.setDueTime(planEntity.getDueTime());
        resp.setTags(planEntity.getTags());
        resp.setExecuteDate(executeDate);
        resp.setCompletedAt(planEntity.getCompletedAt());
        resp.setCreatedAt(planEntity.getCreatedAt());
        resp.setUpdatedAt(planEntity.getUpdatedAt());

        return resp;
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

    /**
     * 判断某个配置了重复规则的计划在指定日期是否存在
     */
    public boolean isPlanActiveToday(PlanEntity plan, LocalDate targetDate) {
        // 0. 确定“有效开始日期”,如果有 start_time，以 start_time 为准，如果没有 start_time，以 created_at (创建时间) 为准
        LocalDate startDate;
        if (plan.getStartTime() != null) {
            startDate = plan.getStartTime().toLocalDate();
        } else {
            startDate = plan.getCreatedAt().toLocalDate();
        }

        // 1. 如果目标日期在开始日期之前，肯定不显示
        if (targetDate.isBefore(startDate)) {
            return false;
        }

        /*// 2. 如果是不重复任务 (repeat_type = 0)
        if (plan.getRepeatType() == 0) {
            LocalDate dueDate = plan.getDueTime() != null ? plan.getDueTime().toLocalDate() : null;
            // 如果有截止时间，targetDate 不能晚于 dueDate
            return dueDate == null || !targetDate.isAfter(dueDate);
        }*/

        // 3. 解析 JSON 配置 (Jackson)
        // 如果是重复任务但没有配置，默认不显示或根据业务逻辑容错
        if (plan.getRepeatConf() == null || plan.getRepeatConf().isEmpty()) {
            // 如果是每天重复(type=1)且没有conf，可以默认为 interval=1，否则返回false
            if (plan.getRepeatType() != 1) return false;
        }

        JsonNode conf;
        try {
            // 对于 type=1 且 conf 为空的情况，给一个空对象节点防止报错，方便后续取默认值
            String jsonStr = (plan.getRepeatConf() == null || plan.getRepeatConf().isEmpty()) ? "{}" : plan.getRepeatConf();
            conf = objectMapper.readTree(jsonStr);
        } catch (JsonProcessingException e) {
            // 记录日志，这里简单抛出或返回 false
            e.printStackTrace();
            return false;
        }

        // 4. 根据类型判断
        switch (plan.getRepeatType()) {
            case 1: // 每天
                // Jackson: path() 方法安全，不存在时不会报空指针，asInt(1) 提供默认值
                int interval = conf.path("interval").asInt(1);
                long daysDiff = ChronoUnit.DAYS.between(startDate, targetDate);
                return daysDiff >= 0 && (daysDiff % interval == 0);

            case 2: // 每周
                // JSON: {"days": [1, 3]}
                int todayOfWeek = targetDate.getDayOfWeek().getValue();
                JsonNode daysNode = conf.get("days");

                if (daysNode != null && daysNode.isArray()) {
                    // Jackson 没有直接转 List 的简便方法，建议直接遍历 ArrayNode
                    for (JsonNode day : daysNode) {
                        if (day.asInt() == todayOfWeek) {
                            return true;
                        }
                    }
                }
                return false;

            case 3: // 每月
                // JSON: {"day": 15} 或 {"day": -1}
                int targetDayOfMonth = conf.path("day").asInt(0); // 默认为0表示未配置
                if (targetDayOfMonth == -1) {
                    // 判断今天是不是当月最后一天
                    return targetDate.equals(targetDate.with(TemporalAdjusters.lastDayOfMonth()));
                } else {
                    return targetDate.getDayOfMonth() == targetDayOfMonth;
                }

            case 4: // 每年
                // JSON: {"month": 10, "day": 1}
                int targetMonth = conf.path("month").asInt(0);
                int targetDay = conf.path("day").asInt(0);
                return targetDate.getMonthValue() == targetMonth && targetDate.getDayOfMonth() == targetDay;

            case 5: // 工作日
                int dayOfWeek = targetDate.getDayOfWeek().getValue();
                // 周一(1) 到 周五(5)
                return dayOfWeek >= 1 && dayOfWeek <= 5;

            default:
                return false;
        }
    }
}
