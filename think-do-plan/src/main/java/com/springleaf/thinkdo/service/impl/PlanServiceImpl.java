package com.springleaf.thinkdo.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springleaf.thinkdo.domain.entity.PlanCategoryEntity;
import com.springleaf.thinkdo.domain.entity.PlanEntity;
import com.springleaf.thinkdo.domain.request.*;
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
import com.springleaf.thinkdo.service.PlanCategoryService;
import com.springleaf.thinkdo.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 计划Service实现
 */
@Service
@Slf4j
public class PlanServiceImpl extends ServiceImpl<PlanMapper, PlanEntity> implements PlanService {

    private final PlanMapper planMapper;
    private final PlanCategoryMapper planCategoryMapper;
    private final PlanCategoryService planCategoryService;
    private final ChatClient chatClient;
    private final ResourceLoader resourceLoader;

    public PlanServiceImpl(PlanMapper planMapper, PlanCategoryMapper planCategoryMapper,
                           PlanCategoryService planCategoryService, ChatClient.Builder builder,
                           ResourceLoader resourceLoader) {
        this.planMapper = planMapper;
        this.planCategoryMapper = planCategoryMapper;
        this.planCategoryService = planCategoryService;
        this.chatClient = builder.build();
        this.resourceLoader = resourceLoader;
    }

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
    public Long aiCreatePlan(AiCreatePlanReq aiCreatePlanReq) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 获取用户现有分类列表
        List<String> userCategories = planCategoryService.getCategoryList().stream()
                .map(com.springleaf.thinkdo.domain.response.PlanCategoryInfoResp::getName)
                .toList();

        // 加载提示词模板
        Resource resource = resourceLoader.getResource("classpath:prompts/create-plan.st");
        PromptTemplate promptTemplate = new PromptTemplate(resource);

        // 构建模板参数
        Map<String, Object> promptParameters = new HashMap<>();
        promptParameters.put("description", aiCreatePlanReq.getDescription());
        promptParameters.put("userHasCategories", !userCategories.isEmpty());
        promptParameters.put("categories", String.join("、", userCategories));
        boolean hasType = aiCreatePlanReq.getType() != null;
        promptParameters.put("hasType", hasType);

        // 无论 hasType 是 true 还是 false，都必须初始化 typeDesc
        String typeDesc = "";
        if (hasType) {
            typeDesc = switch (aiCreatePlanReq.getType()) {
                case 0 -> "普通计划";
                case 1 -> "四象限计划（重要紧急矩阵）";
                case 2 -> "每日计划";
                default -> "普通计划";
            };
        }
        // 必须执行 put，哪怕是空字符串
        promptParameters.put("typeDesc", typeDesc);

        // 生成提示词
        Prompt prompt = promptTemplate.create(promptParameters);

        // 打印构建完成的提示词
        String promptContent = prompt.getContents();
        log.info("AI构建的计划创建提示词：\n{}", promptContent);

        // 调用AI生成计划
        String aiResponse = chatClient.prompt(prompt)
                .call()
                .content();
        log.info("AI生成的计划创建结果：\n{}", aiResponse);

        // 解析AI响应并创建计划
        return parseAiResponseAndCreatePlan(aiResponse, aiCreatePlanReq, userId, userCategories);
    }

    /**
     * 解析AI响应并创建计划
     */
    private Long parseAiResponseAndCreatePlan(String aiResponse, AiCreatePlanReq aiCreatePlanReq,
                                               Long userId, List<String> userCategories) {
        try {
            // 提取JSON（处理可能的markdown代码块）
            String jsonContent = extractJson(aiResponse);

            // 使用正则表达式解析各字段（避免引入JSON依赖）
            String title = extractField(jsonContent, "title");
            String description = extractField(jsonContent, "description");
            String categoryName = extractField(jsonContent, "categoryName");
            Integer type = null;
            if (aiCreatePlanReq.getType() == null) {
                type = parseIntegerField(jsonContent, "type", PlanTypeEnum.NORMAL.getCode());
            }
            Integer priority = parseIntegerField(jsonContent, "priority", PlanPriorityEnum.MEDIUM.getCode());
            Integer quadrant = parseIntegerField(jsonContent, "quadrant", PlanQuadrantEnum.NONE.getCode());
            String tags = extractField(jsonContent, "tags");
            String startTimeStr = extractField(jsonContent, "startTime");
            String dueTimeStr = extractField(jsonContent, "dueTime");

            // 验证必填字段
            if (!StringUtils.hasText(title)) {
                title = truncateDescription(aiCreatePlanReq.getDescription(), 50);
            }
            if (!StringUtils.hasText(description)) {
                description = aiCreatePlanReq.getDescription();
            }

            // 处理分类：查找或创建
            Long categoryId = null;
            if (StringUtils.hasText(categoryName)) {
                categoryId = findOrCreateCategory(categoryName, userCategories);
            }

            // 解析时间
            LocalDateTime startTime = null;
            LocalDateTime dueTime = null;
            if (StringUtils.hasText(startTimeStr) && StringUtils.hasText(dueTimeStr)) {
                try {
                    startTime = LocalDateTime.parse(startTimeStr.replace(" ", "T"));
                    dueTime = LocalDateTime.parse(dueTimeStr.replace(" ", "T"));
                } catch (Exception e) {
                    log.warn("解析时间失败，startTime: {}, dueTime: {}", startTimeStr, dueTimeStr);
                }
            }

            // 创建计划
            PlanEntity plan = new PlanEntity();
            plan.setUserId(userId);
            plan.setType(type != null ? type : (aiCreatePlanReq.getType() != null ? aiCreatePlanReq.getType() : PlanTypeEnum.NORMAL.getCode()));
            plan.setCategoryId(categoryId);
            plan.setTitle(title);
            plan.setDescription(description);
            plan.setPriority(priority);
            plan.setQuadrant(quadrant);
            plan.setTags(tags);
            plan.setStartTime(startTime);
            plan.setDueTime(dueTime);
            plan.setRepeatType(PlanRepeatTypeEnum.NONE.getCode());
            plan.setStatus(PlanStatusEnum.NOT_STARTED.getCode());

            planMapper.insert(plan);
            log.info("AI创建计划成功, userId={}, planId={}, categoryId={}", userId, plan.getId(), categoryId);

            return plan.getId();

        } catch (Exception e) {
            log.error("解析AI响应失败: {}", aiResponse, e);
            throw new BusinessException("AI生成计划失败，请重试");
        }
    }

    /**
     * 查找或创建分类
     */
    private Long findOrCreateCategory(String categoryName, List<String> userCategories) {
        // 精确匹配现有分类
        if (userCategories.contains(categoryName)) {
            LambdaQueryWrapper<PlanCategoryEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PlanCategoryEntity::getUserId, StpUtil.getLoginIdAsLong())
                    .eq(PlanCategoryEntity::getName, categoryName);
            PlanCategoryEntity category = planCategoryMapper.selectOne(wrapper);
            if (category != null) {
                return category.getId();
            }
        }

        // 模糊匹配（处理可能的别名）
        for (String existingCategory : userCategories) {
            if (existingCategory.contains(categoryName) || categoryName.contains(existingCategory)) {
                LambdaQueryWrapper<PlanCategoryEntity> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(PlanCategoryEntity::getUserId, StpUtil.getLoginIdAsLong())
                        .eq(PlanCategoryEntity::getName, existingCategory);
                PlanCategoryEntity category = planCategoryMapper.selectOne(wrapper);
                if (category != null) {
                    return category.getId();
                }
            }
        }

        // 创建新分类
        try {
            CreatePlanCategoryReq createReq = new CreatePlanCategoryReq();
            createReq.setName(categoryName);
            return planCategoryService.createCategory(createReq);
        } catch (Exception e) {
            log.warn("创建分类失败: {}", categoryName, e);
            return null;
        }
    }

    /**
     * 从响应中提取JSON内容
     */
    private String extractJson(String response) {
        String content = response.trim();

        // 处理markdown代码块
        if (content.startsWith("```")) {
            int firstNewline = content.indexOf('\n');
            int lastBackticks = content.lastIndexOf("```");
            if (firstNewline > 0 && lastBackticks > firstNewline) {
                content = content.substring(firstNewline + 1, lastBackticks).trim();
            }
        }

        // 查找第一个{和最后一个}
        int firstBrace = content.indexOf('{');
        int lastBrace = content.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            content = content.substring(firstBrace, lastBrace + 1);
        }

        return content;
    }

    /**
     * 从JSON中提取字符串字段
     */
    private String extractField(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return null;
    }

    /**
     * 从JSON中提取整数字段
     */
    private Integer parseIntegerField(String json, String fieldName, Integer defaultValue) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 截断描述作为标题
     */
    private String truncateDescription(String description, int maxLength) {
        if (description == null) {
            return "未命名计划";
        }
        String truncated = description.length() > maxLength
                ? description.substring(0, maxLength)
                : description;
        // 去除可能的换行符
        return truncated.replaceAll("\\r?\\n", " ").trim();
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
    public List<PlanInfoResp> getPlanListByCategoryId(Long categoryId) {
        Long userId = StpUtil.getLoginIdAsLong();

        LambdaQueryWrapper<PlanEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlanEntity::getUserId, userId)
                .eq(PlanEntity::getCategoryId, categoryId)
                .orderByAsc(PlanEntity::getStatus)
                .orderByDesc(PlanEntity::getPriority)
                .orderByDesc(PlanEntity::getCreatedAt);

        List<PlanEntity> planList = planMapper.selectList(wrapper);

        return planList.stream()
                .map(this::convertToResp)
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
