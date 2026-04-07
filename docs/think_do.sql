CREATE DATABASE IF NOT EXISTS think_do;

create table tb_conversation
(
    id              bigint unsigned auto_increment comment '主键ID'
        primary key,
    conversation_id varchar(64)                          not null comment '会话ID',
    user_id         bigint unsigned                      not null comment '用户ID',
    title           varchar(128)                         not null comment '会话名称',
    last_time       datetime                             null comment '最近消息时间',
    deleted         tinyint(1) default 0                 not null comment '删除标记(0:正常 1:删除)',
    created_at      datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at      datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_conversation_user
        unique (conversation_id, user_id)
)
    comment '会话列表' charset = utf8mb4;

create index idx_user_time
    on tb_conversation (user_id, last_time);

create table tb_conversation_summary
(
    id              bigint unsigned auto_increment comment '主键ID'
        primary key,
    conversation_id varchar(64)                          not null comment '会话ID',
    user_id         bigint unsigned                      not null comment '用户ID',
    last_message_id varchar(64)                          not null comment '摘要最后消息ID',
    content         text                                 not null comment '会话摘要内容',
    deleted         tinyint(1) default 0                 not null comment '删除标记(0:正常 1:删除)',
    created_at      datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at      datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '会话摘要表（与消息表分离存储）' charset = utf8mb4;

create index idx_conv_user
    on tb_conversation_summary (conversation_id, user_id);

create table tb_intent_node
(
    id                    bigint auto_increment comment '自增主键'
        primary key,
    kb_id                 bigint                                null comment '知识库ID',
    intent_code           varchar(64)                           not null comment '业务唯一标识',
    name                  varchar(64)                           not null comment '展示名称',
    scope                 varchar(16) default 'SYSTEM'          null comment '节点范围：SYSTEM/USER',
    level                 tinyint                               not null comment '层级 0：DOMAIN 1：CATEGORY 2：TOPIC',
    parent_code           varchar(64)                           null comment '父节点标识',
    description           varchar(512)                          null comment '语义描述',
    examples              text                                  null comment '示例问题',
    collection_name       varchar(128)                          null comment '关联的Collection名称',
    top_k                 int                                   null comment '知识库检索TopK',
    mcp_tool_id           varchar(128)                          null comment 'MCP工具ID',
    kind                  tinyint(1)  default 0                 not null comment '类型 0：RAG知识库类 1：SYSTEM系统交互类',
    prompt_snippet        text                                  null comment '提示词片段',
    prompt_template       text                                  null comment '提示词模板',
    param_prompt_template text                                  null comment '参数提取提示词模板（MCP模式专属）',
    sort_order            int         default 0                 not null comment '排序字段',
    enabled               tinyint(1)  default 1                 not null comment '是否启用 1：启用 0：禁用',
    created_by            bigint unsigned                       not null comment '创建人',
    updated_by            bigint unsigned                       null comment '修改人',
    created_at            datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at            datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted               tinyint(1)  default 0                 not null comment '删除标记(0:正常 1:删除)'
)
    comment 'RAG意图树节点配置表' charset = utf8mb4;

create table tb_knowledge_base
(
    id              bigint auto_increment comment '主键 ID'
        primary key,
    name            varchar(128)                          not null comment '知识库名称',
    embedding_model varchar(128)                          not null comment '嵌入模型标识',
    collection_name varchar(128)                          not null comment 'Milvus Collection',
    created_by      bigint unsigned                       not null comment '创建人',
    updated_by      bigint unsigned                       null comment '修改人',
    created_at      datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at      datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted         tinyint(1)  default 0                 not null comment '删除标记(0:正常 1:删除)',
    scope           varchar(16) default 'USER'            null,
    description     text                                  null
)
    comment 'RAG知识库表' collate = utf8mb4_unicode_ci;

create index idx_kb_name
    on tb_knowledge_base (name);

create table tb_knowledge_chunk
(
    id           bigint auto_increment comment 'ID'
        primary key,
    kb_id        bigint                               not null comment '知识库ID',
    doc_id       bigint                               not null comment '文档ID',
    chunk_index  int                                  not null comment '分块序号（从0开始）',
    content      longtext                             not null comment '分块正文内容',
    content_hash varchar(64)                          null comment '内容哈希（用于幂等/去重）',
    char_count   int                                  null comment '字符数（可用于统计/调参）',
    token_count  int                                  null comment 'Token数（可选）',
    enabled      tinyint(1) default 1                 not null comment '是否启用 0：禁用 1：启用',
    created_by   bigint unsigned                      not null comment '创建人',
    updated_by   bigint unsigned                      null comment '修改人',
    created_at   datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at   datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted      tinyint(1) default 0                 not null comment '删除标记(0:正常 1:删除)'
)
    comment 'RAG知识库文档分块表' collate = utf8mb4_unicode_ci;

create index idx_doc_id
    on tb_knowledge_chunk (doc_id);

create table tb_knowledge_document
(
    id              bigint auto_increment comment 'ID'
        primary key,
    kb_id           bigint                                not null comment '知识库ID',
    doc_name        varchar(256)                          not null comment '文档名称',
    enabled         tinyint(1)  default 1                 not null comment '是否启用 1：启用 0：禁用',
    chunk_count     int         default 0                 null comment '分块数（chunk 数量）',
    file_url        varchar(1024)                         not null comment '文件地址',
    file_type       varchar(32)                           not null comment '文件类型',
    file_size       bigint                                null comment '文件大小（单位字节）',
    status          varchar(32) default 'pending'         not null comment '状态',
    source_type     varchar(32)                           null comment '来源类型：file/url',
    source_location varchar(1024)                         null comment '来源位置（URL）',
    chunk_strategy  varchar(32)                           null comment '分块策略',
    chunk_config    json                                  null comment '分块参数JSON',
    created_by      varchar(64)                           not null comment '创建人',
    updated_by      varchar(64)                           null comment '修改人',
    created_at      datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at      datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted         tinyint(1)  default 0                 not null comment '删除标记(0:正常 1:删除)'
)
    comment 'RAG知识库文档表' collate = utf8mb4_unicode_ci;

create index idx_kb_id
    on tb_knowledge_document (kb_id);

create table tb_knowledge_document_chunk_log
(
    id                 bigint                             not null comment '主键ID'
        primary key,
    doc_id             bigint                             not null comment '文档ID',
    status             varchar(20)                        not null comment '执行状态',
    chunk_strategy     varchar(50)                        null comment '分块策略（仅chunk模式）',
    extract_duration   bigint                             null comment '文本提取耗时（毫秒）',
    chunk_duration     bigint                             null comment '分块耗时（毫秒）',
    embedding_duration bigint                             null comment '向量化耗时（毫秒）',
    total_duration     bigint                             null comment '总耗时（毫秒）',
    chunk_count        int                                null comment '生成的分块数量',
    error_message      text                               null comment '错误信息',
    start_time         datetime                           null comment '开始时间',
    end_time           datetime                           null comment '结束时间',
    created_at         datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at         datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '知识库文档分块日志表' charset = utf8mb4;

create index idx_doc_id
    on tb_knowledge_document_chunk_log (doc_id);

create table tb_memo
(
    id               bigint auto_increment comment '便签ID'
        primary key,
    user_id          bigint                                                                                 not null comment '用户ID',
    title            varchar(100)                                                                           null comment '便签标题',
    content          text                                                                                   null comment '便签内容',
    tag              varchar(20)                                                                            null comment '便签标签',
    background_color enum ('#fef9e3', '#f4f8fe', '#f9f3ff', '#fff1f2', '#e8fcf2') default '#fef9e3'         null comment '背景颜色',
    pinned           tinyint(1)                                                   default 0                 null comment '是否置顶(0:否 1:是)',
    created_at       datetime                                                     default CURRENT_TIMESTAMP not null,
    updated_at       datetime                                                     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted          tinyint(1)                                                   default 0                 null comment '删除标记(0:正常 1:删除)'
)
    comment '便签表' charset = utf8mb4;

create table tb_message
(
    id              bigint unsigned auto_increment comment '主键ID'
        primary key,
    conversation_id varchar(64)                          not null comment '会话ID',
    user_id         bigint unsigned                      not null comment '用户ID',
    role            varchar(32)                          not null comment '角色：system/user/assistant',
    content         text                                 not null comment '消息内容',
    deleted         tinyint(1) default 0                 not null comment '删除标记(0:正常 1:删除)',
    created_at      datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at      datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '会话消息记录表' charset = utf8mb4;

create index idx_conversation_summary
    on tb_message (conversation_id, user_id, created_at);

create index idx_conversation_user_time
    on tb_message (conversation_id, user_id, created_at);

create table tb_note
(
    id          bigint unsigned auto_increment comment '笔记ID'
        primary key,
    user_id     bigint unsigned                      not null comment '用户ID',
    title       varchar(200)                         not null comment '笔记标题',
    content     text                                 not null comment '笔记内容（Markdown格式）',
    category_id bigint unsigned                      null comment '分类ID（关联tb_note_category表），NULL表示未分类',
    tags        varchar(255)                         null comment '计划标签（逗号分隔）：important,idea,todo',
    favorited   tinyint(1) default 0                 not null comment '是否收藏：0-否, 1-是',
    deleted     tinyint(1) default 0                 not null comment '删除标记(0:正常 1:删除)',
    created_at  datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at  datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '笔记表' collate = utf8mb4_unicode_ci;

create table tb_note_category
(
    id         bigint unsigned auto_increment comment '分类ID'
        primary key,
    user_id    bigint unsigned                    not null comment '用户ID',
    name       varchar(50)                        not null comment '分类名称：学习笔记, 工作记录, 生活感悟',
    created_at datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_user_category
        unique (user_id, name) comment '用户分类唯一索引'
)
    comment '笔记分类表' collate = utf8mb4_unicode_ci;

create table tb_plan
(
    id           bigint auto_increment
        primary key,
    user_id      bigint                               not null comment '所属用户',
    category_id  bigint                               null comment '分类ID',
    title        varchar(255)                         not null comment '计划标题',
    type         tinyint    default 0                 not null comment '计划类型：0.普通计划，1.四象限计划，2.每日计划',
    description  text                                 null comment '计划描述',
    priority     tinyint    default 2                 not null comment '计划优先级：1-低 2-中 3-高',
    quadrant     tinyint    default 0                 not null comment '四象限状态：0-无, 1-重要且紧急, 2-重要不紧急, 3-紧急不重要, 4-不重要不紧急',
    tags         varchar(255)                         null comment '计划标签（逗号分隔）',
    start_time   datetime                             null comment '开始时间',
    due_time     datetime                             null comment '截止时间',
    repeat_type  tinyint    default 0                 not null comment '重复类型：0-不重复, 1-每天, 2-每周, 3-每月, 4-每年, 5-工作日',
    repeat_conf  varchar(255)                         null comment '重复配置细节(JSON格式)',
    repeat_until date                                 null comment '重复截止日期(空代表无限重复)',
    status       tinyint    default 0                 not null comment '计划状态：0-未完成 1-已完成',
    completed_at datetime                             null comment '计划完成时间',
    created_at   datetime   default CURRENT_TIMESTAMP not null,
    updated_at   datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted      tinyint(1) default 0                 null comment '删除标记(0:正常 1:删除)'
)
    comment '计划表' charset = utf8mb4;

create table tb_plan_category
(
    id         bigint auto_increment
        primary key,
    user_id    bigint                               not null comment '所属用户',
    name       varchar(50)                          not null comment '分类名称',
    created_at datetime   default CURRENT_TIMESTAMP not null,
    updated_at datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted    tinyint(1) default 0                 null comment '删除标记(0:正常 1:删除)'
)
    comment '计划分类表（长期计划）' charset = utf8mb4;

create table tb_plan_execution
(
    id           bigint auto_increment
        primary key,
    plan_id      bigint                               not null comment '计划ID',
    execute_date date                                 not null comment '执行日期',
    status       tinyint    default 0                 not null comment '执行状态：0-未完成 1-已完成',
    completed_at datetime                             null comment '当天完成时间',
    created_at   datetime   default CURRENT_TIMESTAMP not null,
    updated_at   datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted      tinyint(1) default 0                 null comment '删除标记(0:正常 1:删除)'
)
    comment '每日清单表' charset = utf8mb4;

create table tb_plan_step
(
    id         bigint auto_increment
        primary key,
    plan_id    bigint                               not null comment '关联的父计划ID',
    title      varchar(255)                         not null comment '步骤标题',
    status     tinyint    default 0                 not null comment '状态：0-未完成 1-已完成',
    created_at datetime   default CURRENT_TIMESTAMP not null,
    updated_at datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted    tinyint(1) default 0                 null comment '删除标记(0:正常 1:删除)'
)
    comment '计划步骤表' charset = utf8mb4;

create table tb_role
(
    id          bigint auto_increment comment '角色ID'
        primary key,
    name        varchar(50)                         not null comment '角色名称，如 USER、ADMIN',
    description varchar(255)                        null comment '角色描述',
    created_at  timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    constraint name
        unique (name)
)
    comment '角色表' charset = utf8mb4;

create table tb_user
(
    id         bigint auto_increment comment '用户ID'
        primary key,
    username   varchar(255)                         not null comment '用户名，唯一',
    password   varchar(255)                         not null comment '加密后的密码',
    created_at timestamp  default CURRENT_TIMESTAMP null comment '创建时间',
    updated_at timestamp  default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted    tinyint(1) default 0                 not null comment '删除标志（0正常 1删除）',
    constraint username
        unique (username)
)
    comment '用户表' charset = utf8mb4;

create table tb_user_role
(
    id         bigint auto_increment
        primary key,
    user_id    bigint                              not null comment '用户ID',
    role_id    bigint                              not null comment '角色ID',
    created_at timestamp default CURRENT_TIMESTAMP null comment '关联创建时间',
    constraint uk_user_role
        unique (user_id, role_id)
)
    comment '用户角色关联表' charset = utf8mb4;

