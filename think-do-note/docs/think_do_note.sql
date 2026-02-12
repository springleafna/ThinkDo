CREATE TABLE tb_note (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '笔记ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `title` VARCHAR(200) NOT NULL COMMENT '笔记标题',
    `content` TEXT NOT NULL COMMENT '笔记内容（Markdown格式）',
    `preview` VARCHAR(100) NULL COMMENT '笔记预览（纯文本前100字符）',
    `category_id` BIGINT UNSIGNED NULL COMMENT '分类ID（关联tb_note_category表），NULL表示未分类',
    `tags` VARCHAR(255) NULL COMMENT '计划标签（逗号分隔）：important,idea,todo',
    `favorited` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否收藏：0-否, 1-是',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记(0:正常 1:删除)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记表';



CREATE TABLE tb_note_category (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称：学习笔记, 工作记录, 生活感悟',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_category` (`user_id`, `name`) COMMENT '用户分类唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记分类表';

