package com.springleaf.thinkdo.domain.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 更新计划Request
 */
@Data
public class UpdatePlanReq {

    /**
     * 计划ID
     */
    @NotNull(message = "计划ID不能为空")
    private Long id;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 计划标题
     */
    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;

    /**
     * 计划描述
     */
    @Size(max = 5000, message = "描述长度不能超过5000个字符")
    private String description;

    /**
     * 计划优先级：1-低 2-中 3-高
     */
    @Min(value = 1, message = "优先级只能为1、2或3")
    @Max(value = 3, message = "优先级只能为1、2或3")
    private Integer priority;

    /**
     * 四象限状态：0-无, 1-重要且紧急, 2-重要不紧急, 3-紧急不重要, 4-不重要不紧急
     */
    @Min(value = 0, message = "四象限状态只能为0、1、2、3或4")
    @Max(value = 4, message = "四象限状态只能为0、1、2、3或4")
    private Integer quadrant;

    /**
     * 计划标签（逗号分隔）
     */
    @Size(max = 255, message = "标签长度不能超过255个字符")
    private String tags;

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
    @Min(value = 0, message = "重复类型只能为0、1、2、3、4或5")
    @Max(value = 5, message = "重复类型只能为0、1、2、3、4或5")
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
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status;
}
