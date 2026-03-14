CREATE TABLE `tb_conversation`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `conversation_id` varchar(64)  NOT NULL COMMENT '会话ID',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `title`           varchar(128) NOT NULL COMMENT '会话名称',
    `last_time`       datetime DEFAULT NULL COMMENT '最近消息时间',
    `deleted`         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记(0:正常 1:删除)',
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_conversation_user` (`conversation_id`,`user_id`),
    KEY               `idx_user_time` (`user_id`,`last_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话列表';

CREATE TABLE `tb_conversation_summary`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `conversation_id` varchar(64) NOT NULL COMMENT '会话ID',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `last_message_id` varchar(64) NOT NULL COMMENT '摘要最后消息ID',
    `content`         text        NOT NULL COMMENT '会话摘要内容',
    `deleted`         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记(0:正常 1:删除)',
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY               `idx_conv_user` (`conversation_id`,`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话摘要表（与消息表分离存储）';

CREATE TABLE `tb_message`
(
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `conversation_id` varchar(64) NOT NULL COMMENT '会话ID',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `role`            varchar(32) NOT NULL COMMENT '角色：system/user/assistant',
    `content`         text        NOT NULL COMMENT '消息内容',
    `deleted`         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记(0:正常 1:删除)',
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY               `idx_conversation_user_time` (`conversation_id`,`user_id`,`created_at`),
    KEY               `idx_conversation_summary` (`conversation_id`,`user_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话消息记录表';