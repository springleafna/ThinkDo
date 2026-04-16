package com.springleaf.thinkdo.controller;

import com.springleaf.thinkdo.common.Result;
import com.springleaf.thinkdo.context.UserContext;
import com.springleaf.thinkdo.domain.response.AdminDashboardStatsResp;
import com.springleaf.thinkdo.domain.response.AdminDashboardTrendResp;
import com.springleaf.thinkdo.enums.ResultCodeEnum;
import com.springleaf.thinkdo.exception.BusinessException;
import com.springleaf.thinkdo.service.ConversationService;
import com.springleaf.thinkdo.service.KnowledgeBaseService;
import com.springleaf.thinkdo.service.MemoService;
import com.springleaf.thinkdo.service.NoteService;
import com.springleaf.thinkdo.service.PlanService;
import com.springleaf.thinkdo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 管理员-运营总览接口
 */
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UserService userService;
    private final ConversationService conversationService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final NoteService noteService;
    private final PlanService planService;
    private final MemoService memoService;

    /**
     * 获取运营总览统计数据
     */
    @GetMapping("/stats")
    public Result<AdminDashboardStatsResp> getStats() {
        checkAdmin();
        AdminDashboardStatsResp resp = new AdminDashboardStatsResp();
        resp.setUserTotal(userService.countTotal());
        resp.setConversationTotal(conversationService.countTotal());
        resp.setDocumentTotal(knowledgeBaseService.countDocumentTotal());
        resp.setNoteTotal(noteService.countTotal());
        resp.setPlanTotal(planService.countTotal());
        resp.setMemoTotal(memoService.countTotal());
        return Result.success(resp);
    }

    /**
     * 获取今日趋势数据
     */
    @GetMapping("/trend")
    public Result<AdminDashboardTrendResp> getTrend() {
        checkAdmin();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        AdminDashboardTrendResp resp = new AdminDashboardTrendResp();

        // 用户注册趋势
        Long todayUserCount = userService.countByDate(today);
        Long yesterdayUserCount = userService.countByDate(yesterday);
        resp.setUserRegisterCount(todayUserCount);
        resp.setUserRegisterCompare(calculateCompare(todayUserCount, yesterdayUserCount));

        // 会话创建趋势
        Long todayConversationCount = conversationService.countByDate(today);
        Long yesterdayConversationCount = conversationService.countByDate(yesterday);
        resp.setConversationCreateCount(todayConversationCount);
        resp.setConversationCreateCompare(calculateCompare(todayConversationCount, yesterdayConversationCount));

        // 文档上传趋势
        Long todayDocumentCount = knowledgeBaseService.countDocumentByDate(today);
        Long yesterdayDocumentCount = knowledgeBaseService.countDocumentByDate(yesterday);
        resp.setDocumentUploadCount(todayDocumentCount);
        resp.setDocumentUploadCompare(calculateCompare(todayDocumentCount, yesterdayDocumentCount));

        // 内容创建趋势（笔记+计划）
        Long todayNoteCount = noteService.countByDate(today);
        Long yesterdayNoteCount = noteService.countByDate(yesterday);
        Long todayPlanCount = planService.countByDate(today);
        Long yesterdayPlanCount = planService.countByDate(yesterday);
        Long todayContentCount = todayNoteCount + todayPlanCount;
        Long yesterdayContentCount = yesterdayNoteCount + yesterdayPlanCount;
        resp.setContentCreateCount(todayContentCount);
        resp.setContentCreateCompare(calculateCompare(todayContentCount, yesterdayContentCount));

        return Result.success(resp);
    }

    private void checkAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "无管理员权限");
        }
    }

    /**
     * 计算对比变化百分比
     * @return 格式化的百分比字符串，如 "+12%" 或 "-5%"
     */
    private String calculateCompare(Long todayCount, Long yesterdayCount) {
        if (yesterdayCount == null || yesterdayCount == 0) {
            return todayCount == null || todayCount == 0 ? "0%" : "+100%";
        }
        if (todayCount == null) {
            todayCount = 0L;
        }
        long diff = todayCount - yesterdayCount;
        long percent = (diff * 100) / yesterdayCount;
        return percent >= 0 ? "+" + percent + "%" : percent + "%";
    }
}
