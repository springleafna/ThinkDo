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
import java.util.*;
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

        // 创建计划（type=2 每日计划）
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

        return plan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlanExecution(UpdatePlanExecutionReq req) {
        Long userId = StpUtil.getLoginIdAsLong();

        PlanEntity plan = planMapper.selectById(req.getId());
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此每日计划");
        }

        // 如果更新开始时间和截止时间，需要验证
        LocalDateTime targetStartTime = req.getStartTime() != null ? req.getStartTime() : plan.getStartTime();
        LocalDateTime targetDueTime = req.getDueTime() != null ? req.getDueTime() : plan.getDueTime();

        if (targetStartTime != null && targetDueTime != null && targetStartTime.isAfter(targetDueTime)) {
            throw new BusinessException("开始时间不能晚于截止时间");
        }

        // 如果是每日计划直接更新plan表的该计划即可
        if (plan.getType().equals(PlanTypeEnum.DAILY.getCode())) {
            if (req.getTitle() != null) plan.setTitle(req.getTitle());
            if (req.getPriority() != null) {
                if (!PlanPriorityEnum.isValid(req.getPriority())) {
                    throw new BusinessException("无效的优先级");
                }
                plan.setPriority(req.getPriority());
            }
            if (req.getTags() != null) plan.setTags(req.getTags());
            plan.setStartTime(targetStartTime);
            plan.setDueTime(targetDueTime);
            planMapper.updateById(plan);

            log.info("更新Type-2每日计划成功, planId={}", plan.getId());
        } else if (plan.getType().equals(PlanTypeEnum.NORMAL.getCode()) && !Objects.equals(plan.getRepeatType(), PlanRepeatTypeEnum.NONE.getCode())) {
            LocalDate executeDate = LocalDate.now();
            // 屏蔽原计划在当天的显示
            // 查找是否已有 execution 记录
            PlanExecutionEntity existingExe = planExecutionMapper.selectIgnoreLogicDelete(plan.getId(), executeDate);

            if (existingExe == null) {
                // 如果没有，插入一条 deleted=1 的记录
                PlanExecutionEntity blockRecord = new PlanExecutionEntity();
                blockRecord.setPlanId(plan.getId());
                blockRecord.setExecuteDate(executeDate);
                blockRecord.setDeleted(1);
                planExecutionMapper.insert(blockRecord);
            } else {
                // 如果有，直接更新为 deleted=1
                planExecutionMapper.deleteById(existingExe);
            }

            // 创建新的 Type=2 分身计划
            PlanEntity forkPlan = new PlanEntity();
            // 复制原计划基础信息
            forkPlan.setUserId(userId);
            forkPlan.setCategoryId(plan.getCategoryId());
            forkPlan.setDescription(plan.getDescription());
            forkPlan.setQuadrant(plan.getQuadrant());
            forkPlan.setType(PlanTypeEnum.DAILY.getCode()); // 变更为每日计划
            forkPlan.setStatus(PlanStatusEnum.NOT_STARTED.getCode());

            // 应用修改后的属性 (如果req没传，就用原计划的)
            forkPlan.setTitle(req.getTitle() != null ? req.getTitle() : plan.getTitle());
            forkPlan.setPriority(req.getPriority() != null ? req.getPriority() : plan.getPriority());
            forkPlan.setTags(req.getTags() != null ? req.getTags() : plan.getTags());
            forkPlan.setStartTime(targetStartTime);
            forkPlan.setDueTime(targetDueTime);
            planMapper.insert(forkPlan);

            log.info("分身策略更新成功: 原PlanId={} 被屏蔽, 新PlanId={} 创建", plan.getId(), forkPlan.getId());

        } else {
            throw new BusinessException("该计划类型不支持此修改操作");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlanExecution(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 验证计划是否属于当前用户
        PlanEntity plan = planMapper.selectById(id);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此每日清单");
        }

        // 查找是否已有 execution 记录
        LocalDate executeDate = LocalDate.now();
        PlanExecutionEntity existingExe = planExecutionMapper.selectIgnoreLogicDelete(plan.getId(), executeDate);

        if (plan.getType().equals(PlanTypeEnum.DAILY.getCode())) {
            // 如果是每日计划 (Type 2)直接软删除 Plan表即可
            planMapper.deleteById(plan);

            if (existingExe != null) {
                planExecutionMapper.deleteById(existingExe);
            }
        } else if (plan.getType().equals(PlanTypeEnum.NORMAL.getCode()) && !Objects.equals(plan.getRepeatType(), PlanRepeatTypeEnum.NONE.getCode())) {
            // 如果是配置了重复规则的普通计划，则不能删 Plan，只能在 Execution 表里“屏蔽”这一天

            if (existingExe != null) {
                // 如果本来就有记录，直接标记删除
                planExecutionMapper.deleteById(existingExe);
            } else {
                // 如果没有，插入一条 deleted=1 的记录
                PlanExecutionEntity blockRecord = new PlanExecutionEntity();
                blockRecord.setPlanId(id);
                blockRecord.setExecuteDate(executeDate);
                blockRecord.setDeleted(1);
                planExecutionMapper.insert(blockRecord);
            }
        } else {
            throw new BusinessException("该计划类型不支持此修改操作");
        }

        log.info("删除计划成功 userId={}, planId={}, date={}", userId, id, executeDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long planId) {
        Long userId = StpUtil.getLoginIdAsLong();

        PlanEntity plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }
        if (!plan.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此计划");
        }

        // 查找当天的 Execution 记录
        LocalDate executeDate = LocalDate.now();
        PlanExecutionEntity execution = planExecutionMapper.selectIgnoreLogicDelete(plan.getId(), executeDate);

        // 计算新状态:如果没有记录，默认为未完成(0)，新状态就是完成(1);如果有记录，取反
        Integer currentStatus = (execution != null) ? execution.getStatus() : PlanExecutionStatusEnum.NOT_COMPLETED.getCode();
        Integer newStatus = (currentStatus == 1) ? 0 : 1;

        // 更新或插入 Execution 记录
        if (execution == null) {
            // 懒加载：插入新记录
            execution = new PlanExecutionEntity();
            execution.setPlanId(plan.getId());
            execution.setExecuteDate(executeDate);
            execution.setStatus(newStatus);
            execution.setCompletedAt(newStatus == 1 ? LocalDateTime.now() : null);
            planExecutionMapper.insert(execution);
        } else {
            // 更新已有记录
            execution.setStatus(newStatus);
            execution.setCompletedAt(newStatus == 1 ? LocalDateTime.now() : null);
            planExecutionMapper.updateById(execution);
        }

        // 同步 Plan 表状态 (仅限 Type=2 每日计划)
        if (PlanTypeEnum.DAILY.getCode().equals(plan.getType())) {
            // Type=2 的任务，Execution 状态就是 Plan 状态
            plan.setStatus(newStatus == 1 ? PlanStatusEnum.COMPLETED.getCode() : PlanStatusEnum.NOT_STARTED.getCode());
            plan.setCompletedAt(newStatus == 1 ? LocalDateTime.now() : null);
            planMapper.updateById(plan);
        }

        // Type=0 的普通计划，这里什么都不做。Plan 状态保持 "进行中(0)"。

        log.info("状态切换成功 userId={}, planId={}, date={}, newStatus={}", userId, planId, executeDate, newStatus);
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

        return planExecutionInfoRespList.stream()
                .sorted(Comparator.comparing(PlanExecutionInfoResp::getStatus))
                .collect(Collectors.toList());    }

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
        PlanExecutionEntity planExecutionEntity = planExecutionMapper.selectIgnoreLogicDelete(planEntity.getId(), executeDate);

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
        resp.setId(planEntity.getId());
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
