CREATE TABLE `tb_knowledge_base`
(
    `id`              bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `name`            varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '知识库名称',
    `scope`           VARCHAR(16) NOT NULL DEFAULT 'SYSTEM' COMMENT '知识库类型：SYSTEM、USER',
    `description`     TEXT DEFAULT NULL COMMENT '知识库描述',
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
    `status`           varchar(32) COLLATE utf8mb4_unicode_ci   NOT NULL DEFAULT 'pending' COMMENT '状态',
    `source_type`      varchar(32) COLLATE utf8mb4_unicode_ci            DEFAULT NULL COMMENT '来源类型：file/url',
    `source_location`  varchar(1024) COLLATE utf8mb4_unicode_ci          DEFAULT NULL COMMENT '来源位置（URL）',
    `chunk_strategy`   varchar(32) COLLATE utf8mb4_unicode_ci            DEFAULT NULL COMMENT '分块策略',
    `chunk_config`     json                                              DEFAULT NULL COMMENT '分块参数JSON',
    `created_by`      BIGINT UNSIGNED COLLATE utf8mb4_unicode_ci  NOT NULL COMMENT '创建人',
    `updated_by`      BIGINT UNSIGNED COLLATE utf8mb4_unicode_ci  DEFAULT NULL COMMENT '修改人',
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
    `chunk_strategy`     varchar(50) DEFAULT NULL COMMENT '分块策略（仅chunk模式）',
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

CREATE TABLE `tb_intent_node`
(
    `id`                    bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `kb_id`                 bigint(20) DEFAULT NULL COMMENT '知识库ID',
    `intent_code`           varchar(64) NOT NULL COMMENT '业务唯一标识',
    `name`                  varchar(64) NOT NULL COMMENT '展示名称',
    `scope`                 VARCHAR(16) DEFAULT 'SYSTEM' COMMENT '节点范围：SYSTEM/USER',
    `level`                 tinyint(4) NOT NULL COMMENT '层级 0：DOMAIN 1：CATEGORY 2：TOPIC',
    `parent_code`           varchar(64)          DEFAULT NULL COMMENT '父节点标识',
    `description`           varchar(512)         DEFAULT NULL COMMENT '语义描述',
    `examples`              text COMMENT '示例问题',
    `collection_name`       varchar(128)         DEFAULT NULL COMMENT '关联的Collection名称',
    `top_k`                 int(11) DEFAULT NULL COMMENT '知识库检索TopK',
    `mcp_tool_id`           varchar(128)         DEFAULT NULL COMMENT 'MCP工具ID',
    `kind`                  tinyint(1) NOT NULL DEFAULT '0' COMMENT '类型 0：RAG知识库类 1：SYSTEM系统交互类',
    `prompt_snippet`        text COMMENT '提示词片段',
    `prompt_template`       text COMMENT '提示词模板',
    `param_prompt_template` text COMMENT '参数提取提示词模板（MCP模式专属）',
    `sort_order`            int(11) NOT NULL DEFAULT '0' COMMENT '排序字段',
    `enabled`               tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用 1：启用 0：禁用',
    `created_by`      BIGINT UNSIGNED COLLATE utf8mb4_unicode_ci  NOT NULL COMMENT '创建人',
    `updated_by`      BIGINT UNSIGNED COLLATE utf8mb4_unicode_ci  DEFAULT NULL COMMENT '修改人',
    `created_at`            datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`            datetime           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记(0:正常 1:删除)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2018166720850104321 DEFAULT CHARSET=utf8mb4 COMMENT='RAG意图树节点配置表';
