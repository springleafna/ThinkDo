CREATE TABLE `tb_knowledge_base`
(
    `id`              bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `name`            varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '知识库名称',
    `embedding_model` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '嵌入模型标识',
    `collection_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Milvus Collection',
    `created_by`      BIGINT UNSIGNED COLLATE utf8mb4_unicode_ci  NOT NULL COMMENT '创建人',
    `updated_by`      BIGINT UNSIGNED COLLATE utf8mb4_unicode_ci  DEFAULT NULL COMMENT '修改人',
    `created_at`      datetime                                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      datetime                                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记(0:正常 1:删除)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_collection_name` (`collection_name`) COMMENT 'Collection 唯一约束',
    KEY               `idx_kb_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG知识库表';

CREATE TABLE `tb_knowledge_chunk`
(
    `id`           bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `kb_id`        bigint(20) NOT NULL COMMENT '知识库ID',
    `doc_id`       bigint(20) NOT NULL COMMENT '文档ID',
    `chunk_index`  int(11) NOT NULL COMMENT '分块序号（从0开始）',
    `content`      longtext COLLATE utf8mb4_unicode_ci    NOT NULL COMMENT '分块正文内容',
    `content_hash` varchar(64) COLLATE utf8mb4_unicode_ci          DEFAULT NULL COMMENT '内容哈希（用于幂等/去重）',
    `char_count`   int(11) DEFAULT NULL COMMENT '字符数（可用于统计/调参）',
    `token_count`  int(11) DEFAULT NULL COMMENT 'Token数（可选）',
    `enabled`      tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用 0：禁用 1：启用',
    `created_by`      BIGINT UNSIGNED COLLATE utf8mb4_unicode_ci  NOT NULL COMMENT '创建人',
    `updated_by`      BIGINT UNSIGNED COLLATE utf8mb4_unicode_ci  DEFAULT NULL COMMENT '修改人',
    `created_at`  datetime                               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  datetime                               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记(0:正常 1:删除)',
    PRIMARY KEY (`id`),
    KEY            `idx_doc_id` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG知识库文档分块表';

CREATE TABLE `tb_knowledge_document`
(
    `id`               bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `kb_id`            bigint(20) NOT NULL COMMENT '知识库ID',
    `doc_name`         varchar(256) COLLATE utf8mb4_unicode_ci  NOT NULL COMMENT '文档名称',
    `enabled`          tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用 1：启用 0：禁用',
    `chunk_count`      int(11) DEFAULT '0' COMMENT '分块数（chunk 数量）',
    `file_url`         varchar(1024) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件地址',
    `file_type`        varchar(32) COLLATE utf8mb4_unicode_ci   NOT NULL COMMENT '文件类型',
    `file_size`        bigint(20) DEFAULT NULL COMMENT '文件大小（单位字节）',
    `process_mode`     varchar(32) COLLATE utf8mb4_unicode_ci            DEFAULT 'chunk' COMMENT '处理模式',
    `status`           varchar(32) COLLATE utf8mb4_unicode_ci   NOT NULL DEFAULT 'pending' COMMENT '状态',
    `source_type`      varchar(32) COLLATE utf8mb4_unicode_ci            DEFAULT NULL COMMENT '来源类型：file/url',
    `source_location`  varchar(1024) COLLATE utf8mb4_unicode_ci          DEFAULT NULL COMMENT '来源位置（URL）',
    `schedule_enabled` tinyint(1) DEFAULT NULL COMMENT '定时拉取 0：否 1：是',
    `schedule_cron`    varchar(128) COLLATE utf8mb4_unicode_ci           DEFAULT NULL COMMENT '定时拉取cron表达式',
    `chunk_strategy`   varchar(32) COLLATE utf8mb4_unicode_ci            DEFAULT NULL COMMENT '分块策略',
    `chunk_config`     json                                              DEFAULT NULL COMMENT '分块参数JSON',
    `pipeline_id`      bigint(20) DEFAULT NULL COMMENT '数据通道ID',
    `created_by`       varchar(64) COLLATE utf8mb4_unicode_ci   NOT NULL COMMENT '创建人',
    `updated_by`       varchar(64) COLLATE utf8mb4_unicode_ci            DEFAULT NULL COMMENT '修改人',
    `created_at`      datetime                                 NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      datetime                                 NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记(0:正常 1:删除)',
    PRIMARY KEY (`id`),
    KEY                `idx_kb_id` (`kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG知识库文档表';

CREATE TABLE `tb_knowledge_document_chunk_log`
(
    `id`                 bigint(20) NOT NULL COMMENT '主键ID',
    `doc_id`             bigint(20) NOT NULL COMMENT '文档ID',
    `status`             varchar(20) NOT NULL COMMENT '执行状态',
    `process_mode`       varchar(20) DEFAULT NULL COMMENT '处理模式',
    `chunk_strategy`     varchar(50) DEFAULT NULL COMMENT '分块策略（仅chunk模式）',
    `pipeline_id`        bigint(20) DEFAULT NULL COMMENT 'Pipeline ID（仅pipeline模式）',
    `extract_duration`   bigint(20) DEFAULT NULL COMMENT '文本提取耗时（毫秒）',
    `chunk_duration`     bigint(20) DEFAULT NULL COMMENT '分块耗时（毫秒）',
    `embedding_duration` bigint(20) DEFAULT NULL COMMENT '向量化耗时（毫秒）',
    `total_duration`     bigint(20) DEFAULT NULL COMMENT '总耗时（毫秒）',
    `chunk_count`        int(11) DEFAULT NULL COMMENT '生成的分块数量',
    `error_message`      text COMMENT '错误信息',
    `start_time`         datetime    DEFAULT NULL COMMENT '开始时间',
    `end_time`           datetime    DEFAULT NULL COMMENT '结束时间',
    `created_at`        datetime    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        datetime    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY                  `idx_doc_id` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档分块日志表';

CREATE TABLE `tb_knowledge_document_schedule`
(
    `id`                bigint(20) NOT NULL COMMENT '主键ID',
    `doc_id`            bigint(20) NOT NULL COMMENT '文档ID',
    `kb_id`             bigint(20) NOT NULL COMMENT '知识库ID',
    `cron_expr`         varchar(128)      DEFAULT NULL COMMENT '定时表达式',
    `enabled`           tinyint(4) DEFAULT '0' COMMENT '是否启用定时',
    `next_run_time`     datetime          DEFAULT NULL COMMENT '下一次执行时间',
    `last_run_time`     datetime          DEFAULT NULL COMMENT '上一次执行时间',
    `last_success_time` datetime          DEFAULT NULL COMMENT '上一次成功时间',
    `last_status`       varchar(32)       DEFAULT NULL COMMENT '上一次执行状态',
    `last_error`        varchar(512)      DEFAULT NULL COMMENT '上一次执行错误',
    `last_etag`         varchar(256)      DEFAULT NULL COMMENT '上一次ETag',
    `last_modified`     varchar(256)      DEFAULT NULL COMMENT '上一次Last-Modified',
    `last_content_hash` varchar(128)      DEFAULT NULL COMMENT '上一次内容哈希',
    `lock_owner`        varchar(128)      DEFAULT NULL COMMENT '锁持有者',
    `lock_until`        datetime          DEFAULT NULL COMMENT '锁到期时间',
    `created_at`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doc_id` (`doc_id`),
    KEY                 `idx_next_run` (`next_run_time`),
    KEY                 `idx_lock_until` (`lock_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档定时刷新任务表';

CREATE TABLE `tb_knowledge_document_schedule_exec`
(
    `id`            bigint(20) NOT NULL COMMENT '主键ID',
    `schedule_id`   bigint(20) NOT NULL COMMENT '定时任务ID',
    `doc_id`        bigint(20) NOT NULL COMMENT '文档ID',
    `kb_id`         bigint(20) NOT NULL COMMENT '知识库ID',
    `status`        varchar(32) NOT NULL COMMENT '执行状态',
    `message`       varchar(512)         DEFAULT NULL COMMENT '执行信息',
    `start_time`    datetime             DEFAULT NULL COMMENT '开始时间',
    `end_time`      datetime             DEFAULT NULL COMMENT '结束时间',
    `file_name`     varchar(512)         DEFAULT NULL COMMENT '文件名',
    `file_size`     bigint(20) DEFAULT NULL COMMENT '文件大小',
    `content_hash`  varchar(128)         DEFAULT NULL COMMENT '内容哈希',
    `etag`          varchar(256)         DEFAULT NULL COMMENT 'ETag',
    `last_modified` varchar(256)         DEFAULT NULL COMMENT 'Last-Modified',
    `created_at`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY             `idx_schedule_time` (`schedule_id`,`start_time`),
    KEY             `idx_doc_id` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档定时刷新执行记录';