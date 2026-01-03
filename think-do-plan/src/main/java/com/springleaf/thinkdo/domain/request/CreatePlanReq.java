package com.springleaf.thinkdo.domain.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建计划Request
 */
@Data
public class CreatePlanReq {

    /**
     * 分类ID（可为空，表示未分类）
     */
    private Long categoryId;

    /**
     * 计划标题
     */
    @NotBlank(message = "计划标题不能为空")
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
     * 开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDate startDate;

    /**
     * 截止日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDate dueDate;

    /**
     * 重复类型：0-不重复, 1-每天, 2-每周, 3-每月, 4-每年, 5-工作日
     */
    @Min(value = 0, message = "重复类型只能为0、1、2、3、4或5")
    @Max(value = 5, message = "重复类型只能为0、1、2、3、4或5")
    private Integer repeatType;

    /**
     * 重复配置细节(JSON格式)
     * 示例：
     * - 每天：{"interval": 3} (每隔3天)
     * - 每周：{"days": [1, 3]} (每周一和周三)
     * - 每月：{"day": 15} (每月15号) 或 {"day": -1} (每月最后一天)
     * - 每年：{"month": 10, "day": 1} (每年10月1日)
     */
    private String repeatConf;

    /**
     * 重复截止日期(空代表无限重复)
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate repeatUntil;
}
