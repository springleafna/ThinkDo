package com.springleaf.thinkdo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tb_plan")
public class Plan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long categoryId;

    private String title;

    private Integer type;

    private String description;

    private Integer priority;

    private Integer quadrant;

    private String tags;

    private LocalDateTime startTime;

    private LocalDateTime dueTime;

    private Integer repeatType;

    private String repeatConf;

    private LocalDate repeatUntil;

    private Integer status;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
