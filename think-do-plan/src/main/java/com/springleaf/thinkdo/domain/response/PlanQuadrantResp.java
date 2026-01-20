package com.springleaf.thinkdo.domain.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 计划四象限Response
 */
@Data
public class PlanQuadrantResp {

    /**
     * 重要且紧急
     */
    private List<PlanQuadrantInfoResp> importantUrgent;

    /**
     * 重要不紧急
     */
    private List<PlanQuadrantInfoResp> importantNotUrgent;

    /**
     * 紧急不重要
     */
    private List<PlanQuadrantInfoResp> urgentNotImportant;

    /**
     * 不重要不紧急
     */
    private List<PlanQuadrantInfoResp> notImportantNotUrgent;

    /**
     * 四象限计划信息Response
     */
    @Data
    public static class PlanQuadrantInfoResp {

        /**
         * 计划ID
         */
        private Long id;

        /**
         * 计划类型：0-普通计划，1-四象限计划，2-每日计划
         */
        private Integer type;

        /**
         * 计划标题
         */
        private String title;

        /**
         * 计划描述
         */
        private String description;

        /**
         * 四象限状态：0-无, 1-重要且紧急, 2-重要不紧急, 3-紧急不重要, 4-不重要不紧急
         */
        private Integer quadrant;

        /**
         * 开始时间
         */
        private LocalDateTime startTime;

        /**
         * 截止时间
         */
        private LocalDateTime dueTime;

        /**
         * 重复类型：0-不重复, 1-每天, 2-每周, 3-每月, 4-每年, 5-工作日
         */
        private Integer repeatType;

        /**
         * 重复配置细节(JSON格式)
         */
        private String repeatConf;

        /**
         * 重复截止日期
         */
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate repeatUntil;

        /**
         * 计划状态：0-未完成 1-已完成
         */
        private Integer status;
    }
}
