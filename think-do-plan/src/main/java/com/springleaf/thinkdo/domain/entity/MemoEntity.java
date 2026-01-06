package com.springleaf.thinkdo.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 便签实体类
 */
@Data
@TableName("tb_memo")
public class MemoEntity {

    /**
     * 便签ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 便签标题
     */
    private String title;

    /**
     * 便签内容
     */
    private String content;

    /**
     * 便签标签
     */
    private String tag;

    /**
     * 背景颜色
     * #fef9e3 - 黄色
     * #f4f8fe - 蓝色
     * #f9f3ff - 紫色
     * #fff1f2 - 粉色
     * #e8fcf2 - 绿色
     */
    private String backgroundColor;

    /**
     * 是否置顶(0:否 1:是)
     */
    private Integer pinned;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 删除标记(0:正常 1:删除)
     */
    @TableLogic
    private Integer deleted;
}
