-- ====================================================================
-- RAG意图树SQL构建脚本
-- 说明：包含系统知识库意图节点和用户知识库意图节点模板
-- ====================================================================

-- ====================================================================
-- 第一部分：系统知识库意图节点（SYSTEM scope）
-- ====================================================================

-- ------------------------------------------------------------
-- SYSTEM DOMAIN 层级节点（level=0）
-- ------------------------------------------------------------
-- 智能助手根节点 - 所有系统知识的根
INSERT INTO tb_intent_node (kb_id, intent_code, name, scope, level, parent_code, description, examples, collection_name, top_k, kind, prompt_snippet, sort_order, enabled, created_by, deleted)
VALUES (
           NULL,
           'root_system',
           '智能助手',
           'SYSTEM',
           0,
           NULL,
           '智能助手官方知识库根节点，包含所有产品功能指南、技术文档和常见问题',
           '智能助手能帮我做什么？|有什么功能？',
           NULL,
           NULL,
           0,
           '请基于以下官方知识库内容回答用户问题，优先使用系统提供的知识。',
           1,
           1,
           0,
           0
       );

-- ------------------------------------------------------------
-- SYSTEM CATEGORY 层级节点（level=1）
-- ------------------------------------------------------------
-- 产品使用指南
INSERT INTO tb_intent_node (kb_id, intent_code, name, scope, level, parent_code, description, examples, collection_name, top_k, kind, prompt_snippet, sort_order, enabled, created_by, deleted)
VALUES (
           NULL,
           'category_product_guide',
           '产品使用指南',
           'SYSTEM',
           1,
           'root_system',
           '智能助手产品的功能使用指南，包括计划管理、笔记编辑、团队协作等核心功能的操作说明',
           '怎么使用计划功能？|如何创建笔记？|怎么邀请协作者？|产品功能介绍',
           'collection_product_guide',
           5,
           0,
           '请详细介绍产品功能的使用方法，包括具体操作步骤和注意事项。',
           1,
           1,
           0,
           0
       );

-- 技术文档
INSERT INTO tb_intent_node (kb_id, intent_code, name, scope, level, parent_code, description, examples, collection_name, top_k, kind, prompt_snippet, sort_order, enabled, created_by, deleted)
VALUES (
           NULL,
           'category_tech_doc',
           '技术文档',
           'SYSTEM',
           1,
           'root_system',
           '智能助手技术相关文档，包括系统架构、API接口、配置说明、开发者指南等',
           '系统架构是什么？|API接口怎么调用？|Spring Boot怎么配置？|数据库设计',
           'collection_tech_doc',
           5,
           0,
           '请基于技术文档提供准确的技术信息，包括配置示例和代码片段。',
           2,
           1,
           0,
           0
       );

-- 常见问题
INSERT INTO tb_intent_node (kb_id, intent_code, name, scope, level, parent_code, description, examples, collection_name, top_k, kind, prompt_snippet, sort_order, enabled, created_by, deleted)
VALUES (
           NULL,
           'category_faq',
           '常见问题',
           'SYSTEM',
           1,
           'root_system',
           '用户使用智能助手过程中的常见问题解答，包括账号、权限、限制、故障处理等',
           '忘记密码怎么办？|账号被封了怎么解封？|有存储限制吗？|怎么联系客服？',
           'collection_faq',
           3,
           0,
           '请简洁明了地解答用户疑问，如问题复杂建议联系人工客服。',
           3,
           1,
           0,
           0
       );

-- ------------------------------------------------------------
-- SYSTEM TOPIC 层级节点（level=2）- 产品使用指南分类下
-- ------------------------------------------------------------
-- 计划功能
INSERT INTO tb_intent_node (kb_id, intent_code, name, scope, level, parent_code, description, examples, collection_name, top_k, kind, prompt_snippet, sort_order, enabled, created_by, deleted)
VALUES (
           NULL,
           'topic_plan_feature',
           '计划功能',
           'SYSTEM',
           2,
           'category_product_guide',
           '智能助手的计划管理功能，包括创建计划、设置目标、进度跟踪、计划模板等',
           '怎么创建计划？|如何设置计划目标？|计划模板怎么用？|怎么查看计划进度？',
           'collection_plan_feature',
           5,
           0,
           '请详细说明计划功能的操作步骤，可举例说明最佳实践。',
           1,
           1,
           0,
           0
       );

-- 笔记功能
INSERT INTO tb_intent_node (kb_id, intent_code, name, scope, level, parent_code, description, examples, collection_name, top_k, kind, prompt_snippet, sort_order, enabled, created_by, deleted)
VALUES (
           NULL,
           'topic_note_feature',
           '笔记功能',
           'SYSTEM',
           2,
           'category_product_guide',
           '智能助手的笔记编辑功能，包括富文本编辑、Markdown支持、标签分类、搜索等',
           '怎么创建笔记？|支持Markdown吗？|怎么给笔记打标签？|如何搜索笔记？',
           'collection_note_feature',
           5,
           0,
           '请详细说明笔记功能的使用方法，包括编辑技巧和组织建议。',
           2,
           1,
           0,
           0
       );

-- 协作功能
INSERT INTO tb_intent_node (kb_id, intent_code, name, scope, level, parent_code, description, examples, collection_name, top_k, kind, prompt_snippet, sort_order, enabled, created_by, deleted)
VALUES (
           NULL,
           'topic_collaboration',
           '协作功能',
           'SYSTEM',
           2,
           'category_product_guide',
           '智能助手的团队协作功能，包括分享、评论、权限管理、多人编辑等',
           '怎么分享笔记？|如何设置协作权限？|支持多人同时编辑吗？|怎么添加协作者？',
           'collection_collaboration',
           5,
           0,
           '请详细说明协作功能的使用方法和权限控制逻辑。',
           3,
           1,
           0,
           0
       );

-- ------------------------------------------------------------
-- SYSTEM TOPIC 层级节点（level=2）- 技术文档分类下
-- ------------------------------------------------------------
-- Spring Boot配置
INSERT INTO tb_intent_node (kb_id, intent_code, name, scope, level, parent_code, description, examples, collection_name, top_k, kind, prompt_snippet, sort_order, enabled, created_by, deleted)
VALUES (
           NULL,
           'topic_springboot_config',
           'Spring Boot配置',
           'SYSTEM',
           2,
           'category_tech_doc',
           '智能助手后端使用的Spring Boot框架配置说明，包括application配置、日志配置、数据库连接等',
           'Spring Boot怎么配置数据库？|日志级别怎么设置？|如何配置端口？|怎么添加拦截器？',
           'collection_springboot_config',
           5,
           0,
           '请提供准确的配置示例和代码片段，说明配置的作用。',
           1,
           1,
           0,
           0
       );

-- 数据库设计
INSERT INTO tb_intent_node (kb_id, intent_code, name, scope, level, parent_code, description, examples, collection_name, top_k, kind, prompt_snippet, sort_order, enabled, created_by, deleted)
VALUES (
           NULL,
           'topic_database_design',
           '数据库设计',
           'SYSTEM',
           2,
           'category_tech_doc',
           '智能助手的数据库设计文档，包括表结构、索引设计、数据字典、ER图等',
           '用户表结构是什么？|怎么设计索引？|数据字典在哪里？|表关系是怎样的？',
           'collection_database_design',
           5,
           0,
           '请提供准确的表结构信息和设计说明，必要时展示SQL示例。',
           2,
           1,
           0,
           0
       );

-- API接口
INSERT INTO tb_intent_node (kb_id, intent_code, name, scope, level, parent_code, description, examples, collection_name, top_k, kind, prompt_snippet, sort_order, enabled, created_by, deleted)
VALUES (
           NULL,
           'topic_api',
           'API接口',
           'SYSTEM',
           2,
           'category_tech_doc',
           '智能助手提供的API接口文档，包括RESTful接口、请求参数、响应格式、错误码等',
           'API怎么调用？|接口地址是什么？|参数怎么传？|错误码什么意思？',
           'collection_api',
           5,
           0,
           '请提供准确的接口信息，包括请求示例和响应示例。',
           3,
           1,
           0,
           0
       );

-- ------------------------------------------------------------
-- SYSTEM TOPIC 层级节点（level=2）- 常见问题分类下
-- ------------------------------------------------------------
-- 账号相关
INSERT INTO tb_intent_node (kb_id, intent_code, name, scope, level, parent_code, description, examples, collection_name, top_k, kind, prompt_snippet, sort_order, enabled, created_by, deleted)
VALUES (
           NULL,
           'topic_account',
           '账号相关',
           'SYSTEM',
           2,
           'category_faq',
           '智能助手账号相关常见问题，包括注册、登录、密码重置、账号绑定、注销等',
           '怎么注册账号？|忘记密码怎么办？|怎么绑定手机号？|如何注销账号？',
           'collection_account_faq',
           3,
           0,
           '请提供清晰的步骤指导，必要时提供截图说明或跳转链接。',
           1,
           1,
           0,
           0
       );

-- 功能限制
INSERT INTO tb_intent_node (kb_id, intent_code, name, scope, level, parent_code, description, examples, collection_name, top_k, kind, prompt_snippet, sort_order, enabled, created_by, deleted)
VALUES (
           NULL,
           'topic_limits',
           '功能限制',
           'SYSTEM',
           2,
           'category_faq',
           '智能助手的功能限制说明，包括存储空间限制、成员数量限制、文件大小限制等',
           '有存储限制吗？|最多可以添加多少协作者？|单个文件多大？|免费版有什么限制？',
           'collection_limits_faq',
           3,
           0,
           '请明确说明限制规则和升级方案。',
           2,
           1,
           0,
           0
       );


-- ====================================================================
-- 第二部分：用户知识库意图节点模板（USER scope）
-- ====================================================================

-- ------------------------------------------------------------
-- 说明：用户知识库意图节点分为两类：
-- 1. 用户根节点和分类节点 - 用户注册时自动创建
-- 2. 用户知识库主题节点 - 用户创建知识库时自动生成
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- USER DOMAIN 层级节点（level=0）
-- 用户注册时自动创建，每个用户一条
-- ------------------------------------------------------------
INSERT INTO tb_intent_node (kb_id, intent_code, name, scope, level, parent_code, description, examples, collection_name, top_k, kind, prompt_snippet, sort_order, enabled, created_by, deleted)
VALUES (
           NULL,
           'root_user_{userId}',
           '用户私人知识库',
           'USER',
           0,
           NULL,
           '用户私有知识库根节点，包含用户上传的所有文件内容',
           '',
           NULL,
           NULL,
           0,
           '请基于用户私有知识库内容回答，这些是用户自己上传的文件。',
           1,
           1,
           {userId},
           0
       );

-- ------------------------------------------------------------
-- USER CATEGORY 层级节点（level=1）
-- 用户注册时自动创建，每个用户一条
-- ------------------------------------------------------------
-- 模板SQL（实际使用时替换 3）
INSERT INTO tb_intent_node (kb_id, intent_code, name, scope, level, parent_code, description, examples, collection_name, top_k, kind, prompt_snippet, sort_order, enabled, created_by, deleted)
VALUES (
           NULL,
           'category_user_kb_{userId}',
           '我的知识库',
           'USER',
           1,
           'root_user_{userId}',
           '用户创建的私有知识库集合，包含用户上传的所有文件',
           '',
           NULL,
           NULL,
           0,
           '请基于用户知识库中的私有内容回答问题。',
           1,
           1,
           {userId},
           0
       );

-- ------------------------------------------------------------
-- USER TOPIC 层级节点（level=2）
-- 用户创建知识库时自动生成，每个知识库一条
-- ------------------------------------------------------------
-- 模板SQL（实际使用时替换以下变量）：
-- 3        - 用户ID
-- {kb_id}          - 知识库ID（主键）
-- {kb_name}        - 知识库名称（用户输入）
-- {kb_description} - 知识库描述（用户输入）
-- {collection_name} - Collection名称（系统生成，如 kb_user_3_{kb_id}）
-- {intent_code}    - 意图代码（系统生成，如 user_kb_3_{kb_id}）

INSERT INTO tb_intent_node (kb_id, intent_code, name, scope, level, parent_code, description, examples, collection_name, top_k, kind, prompt_snippet, sort_order, enabled, created_by, deleted)
VALUES (
           {kb_id},
           'user_kb_{userId}_{kb_id}',
           '{kb_name}',
           'USER',
           2,
           'category_user_kb_{userId}',
           '{kb_description}',
           '我的{kb_name}|我的笔记关于{kb_name}|我上传的{kb_name}',
           '{collection_name}',
           5,
           0,
           '请基于用户知识库"{kb_name}"中的私有内容回答，这些是用户上传的个人文档。',
           1,
           1,
           {userId},
           0
       );

-- ------------------------------------------------------------
-- 实际示例：假设用户ID=10001创建了一个知识库
-- ------------------------------------------------------------
-- 示例1：用户10001的根节点和分类节点
INSERT INTO tb_intent_node (kb_id, intent_code, name, scope, level, parent_code, description, examples, collection_name, top_k, kind, prompt_snippet, sort_order, enabled, created_by, deleted)
VALUES (
           NULL,
           'root_user_10001',
           '我的知识',
           'USER',
           0,
           NULL,
           '用户私有知识库根节点，包含用户上传的所有文档、笔记、项目资料等私有内容',
           '',
           NULL,
           NULL,
           0,
           '请基于用户私有知识库内容回答，这些是用户自己上传的文件。',
           1,
           1,
           10001,
           0
       );

INSERT INTO tb_intent_node (kb_id, intent_code, name, scope, level, parent_code, description, examples, collection_name, top_k, kind, prompt_snippet, sort_order, enabled, created_by, deleted)
VALUES (
           NULL,
           'category_user_kb_10001',
           '我的知识库',
           'USER',
           1,
           'root_user_10001',
           '用户创建的私有知识库集合，包含用户个人文档、学习笔记、项目资料等',
           '',
           NULL,
           NULL,
           0,
           '请基于用户知识库中的私有内容回答问题。',
           1,
           1,
           10001,
           0
       );

-- 示例2：用户10001创建了一个"Spring Boot学习笔记"知识库（假设kb_id=20001）
INSERT INTO tb_intent_node (kb_id, intent_code, name, scope, level, parent_code, description, examples, collection_name, top_k, kind, prompt_snippet, sort_order, enabled, created_by, deleted)
VALUES (
           20001,
           'user_kb_10001_20001',
           'Spring Boot学习笔记',
           'USER',
           2,
           'category_user_kb_10001',
           'Spring Boot学习笔记，包含配置、注解、最佳实践等个人学习资料',
           '',
           'kb_user_10001_20001',
           5,
           0,
           '请基于用户知识库"Spring Boot学习笔记"中的私有内容回答，这些是用户上传的个人文档。',
           1,
           1,
           10001,
           0
       );

-- 示例3：用户10001创建了一个"项目管理文档"知识库（假设kb_id=20002）
INSERT INTO tb_intent_node (kb_id, intent_code, name, scope, level, parent_code, description, examples, collection_name, top_k, kind, prompt_snippet, sort_order, enabled, created_by, deleted)
VALUES (
           20002,
           'user_kb_10001_20002',
           '项目管理文档',
           'USER',
           2,
           'category_user_kb_10001',
           '项目管理相关文档，包含需求文档、设计文档、会议记录等项目资料',
           '',
           'kb_user_10001_20002',
           5,
           0,
           '请基于用户知识库"项目管理文档"中的私有内容回答，这些是用户上传的个人文档。',
           2,
           1,
           10001,
           0
       );

-- 示例4：用户10001创建了一个"读书笔记"知识库（假设kb_id=20003）
INSERT INTO tb_intent_node (kb_id, intent_code, name, scope, level, parent_code, description, examples, collection_name, top_k, kind, prompt_snippet, sort_order, enabled, created_by, deleted)
VALUES (
           20003,
           'user_kb_10001_20003',
           '读书笔记',
           'USER',
           2,
           'category_user_kb_10001',
           '读书笔记，包含各类书籍的阅读心得、重点摘录、思维导图等',
           '',
           'kb_user_10001_20003',
           5,
           0,
           '请基于用户知识库"读书笔记"中的私有内容回答，这些是用户上传的个人文档。',
           3,
           1,
           10001,
           0
       );


-- ====================================================================
-- 第三部分：字段映射说明
-- ====================================================================

/*
用户知识库意图节点字段来源说明：

【用户可设置字段】（来自用户创建知识库时的输入）
- name（展示名称）：用户输入的知识库名称
- description（语义描述）：用户输入的知识库描述

【系统自动生成字段】
- intent_code：系统生成，格式 user_kb_3_{kb_id}
- collection_name：系统生成，格式 kb_user_3_{kb_id}
- parent_code：固定为 'category_user_kb_3'
- kb_id：知识库表的主键

【固定字段】
- scope：固定为 'USER'
- level：固定为 2（TOPIC层级）
- kind：固定为 0（RAG知识库类型）
- top_k：固定为 5
- sort_order：根据知识库创建顺序递增
- enabled：固定为 1（启用）

【自动生成字段（基于description）】
- examples：基于知识库名称和描述自动生成，格式：
  "我的{kb_name}|{kb_name}笔记|关于{kb_name}的内容"

【NULL字段】（不设置）
- mcp_tool_id：NULL（用户知识库不关联MCP工具）
- prompt_template：NULL（使用默认模板）
- param_prompt_template：NULL（用户知识库不需要）


【多租户隔离字段】
- created_by：设置为当前用户ID（用于数据权限过滤）
*/
