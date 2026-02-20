package com.springleaf.thinkdo.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记实体类
 */
@Data
@TableName("tb_note")
public class NoteEntity {

    /**
     * 笔记ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 笔记标题
     */
    private String title;

    /**
     * 笔记内容（Markdown格式）
     */
    private String content;

    /**
     * 笔记预览（纯文本前100字符）
     */
    private String preview;

    /**
     * 分类ID（关联tb_note_category表），NULL表示未分类
     */
    private Long categoryId;

    /**
     * 笔记标签（逗号分隔）：important,idea,todo
     */
    private String tags;

    /**
     * 是否收藏：0-否, 1-是
     */
    private Integer favorited;

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
