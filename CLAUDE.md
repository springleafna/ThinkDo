# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

ThinkDo 是一个基于 Spring Boot 3.5 + JDK 21 的多模块后端项目，聚焦于「AI 对话 + 知识库(RAG) + 笔记 + 计划管理」一体化场景。 groupId 为 `com.springleaf`，所有 Java 代码位于 `com.springleaf.thinkdo` 包下。

## 构建与运行

```bash
# 编译主应用（跳过测试）
mvn clean package -DskipTests -pl think-do-start -am

# 编译 MCP Server
mvn clean package -DskipTests -pl think-do-mcp -am

# 运行主应用（默认端口 8091）
java -jar think-do-start/target/think-do-start-1.0-SNAPSHOT.jar

# 运行 MCP Server（默认端口 9099）
java -jar think-do-mcp/target/think-do-mcp-1.0-SNAPSHOT.jar
```

项目在根 pom 中全局配置了 `skipTests=true`，目前没有活跃的单元测试。

## 环境配置

- 配置文件入口：`think-do-start/src/main/resources/application.yml`
- 通过 `spring.config.import: optional:file:.env[.properties]` 加载 `.env` 文件
- `.env` 已在 `.gitignore` 中，包含 `API_KEY`、`AI_BAILIAN_API_KEY`、`AI_SILICONFLOW_API_KEY` 等敏感配置
- 多环境支持：`application-dev.yml` / `application-prod.yml`，通过 `SPRING_PROFILES_ACTIVE` 切换

## 模块架构

项目采用分层模块化架构，依赖关系自上而下：

```
think-do-start (启动器，聚合所有业务模块)
├── think-do-common (通用工具：Result, Exception, 枚举, 配置)
├── think-do-system (用户、权限、认证)
│   └── think-do-common
├── think-do-ai (LLM 对话、模型路由、SSE 流式输出、MCP 客户端)
│   └── think-do-system
├── think-do-knowledge (知识库、文档管理、RAG 检索、向量存储、意图节点)
│   ├── think-do-ai
│   └── think-do-system
├── think-do-note (笔记 CRUD、AI 润色/扩写/纠错)
│   ├── think-do-ai
│   └── think-do-system
├── think-do-plan (计划、步骤、每日清单、便签)
│   ├── think-do-ai
│   └── think-do-system
└── think-do-admin (管理员后台：用户管理、会话管理、知识库管理、数据看板)
    ├── think-do-knowledge
    ├── think-do-note
    ├── think-do-plan
    ├── think-do-ai
    └── think-do-system

think-do-mcp (独立 MCP Server 应用，端口 9099)
```

### 关键架构设计

**多模型路由与熔断** (`think-do-ai/model/`)：
- `ModelSelector`：根据能力(chat/embedding/rerank)和模式(普通/深度思考)从配置中选择候选模型列表
- `ModelRoutingExecutor`：遍历候选列表执行调用，失败自动切换到下一个模型(Fallback)
- `ModelHealthStore`：基于失败次数和熔断时间窗口的模型健康状态管理
- 模型提供商配置在 `application.yml` 的 `ai.providers` 下，当前支持百炼(BaiLian)和硅基流动(SiliconFlow)

**LLM 调用抽象** (`think-do-ai/chat/`)：
- `LLMService` 接口：统一的 LLM 访问入口，支持同步和流式调用
- `RoutingLLMService`：基于路由机制的实现，结合 ModelSelector 和 ModelRoutingExecutor
- `ChatClient` 接口 + `BaiLianChatClient` / `SiliconFlowChatClient`：按提供商适配的 HTTP SSE 客户端
- `OpenAIStyleSseParser`：解析 OpenAI 兼容格式的 SSE 流
- `FirstPacketAwaiter`：首包超时检测，超时自动触发 Fallback

**RAG 检索增强** (`think-do-knowledge/`)：
- `RagChatService`：RAG 流式问答的核心服务，整合意图识别、向量检索、Rerank、LLM 生成
- `VectorStoreService`：Milvus 向量数据库操作（创建 Collection、插入/检索向量）
- 双通道检索策略：意图导向检索(`intent-directed`) + 全局向量检索(`vector-global`)，通过 `confidence-threshold` 决定是否启用全局兜底
- 查询改写：`rag.query-rewrite.enabled` 控制是否启用基于历史消息的查询改写
- 文档处理链路：上传 → Tika 解析 → 分块 → Embedding 向量化 → Milvus 存储

**MCP 工具协议** (`think-do-mcp/`)：
- 独立 Spring Boot 应用，实现了 MCP (Model Context Protocol) 服务端
- `MCPDispatcher`：基于 JSON-RPC 协议分发工具调用请求
- `MCPToolRegistry`：工具注册中心，`MCPToolExecutor` 为执行器接口
- 已实现的工具：`DuePlanQueryMCPExecutor`(待办查询)、`CreateMemoMCPExecutor`(创建便签)、`WeatherMCPExecutor`(天气查询)
- 主应用通过 `think-do-ai/mcp/client/` 中的 `HttpMCPClient` 远程调用 MCP Server

**SSE 流式通信**：
- 前端通过 `SseEmitter` 接收流式响应
- `StreamCallback` 定义 onContent/onComplete/onError 回调
- `StreamTaskManager` 管理流式任务的生命周期和取消

## 代码约定

- 统一响应格式：`Result<T>`（code/message/data），错误码定义在 `ResultCodeEnum`
- 异常体系：`BusinessException` 用于业务异常，`GlobalExceptionHandler` 统一捕获
- ORM 层：MyBatis-Plus，Entity 放在各模块 `domain/entity/`，Mapper 在 `mapper/`
- 领域对象分层：`domain/entity/`(数据库实体)、`domain/dto/`(传输对象)、`enums/`(枚举)
- 配置类统一放在各模块的 `config/` 包下，以 `Properties` 后缀的类对应 YAML 配置映射
- 使用 Lombok（@Data, @RequiredArgsConstructor 等）减少样板代码
- MyBatis-Plus 自动填充：`MybatisPlusMetaObjectHandler` 处理公共字段

## CI/CD

- GitHub Actions 工作流：`.github/workflows/main.yml`
- 触发条件：push 到 main/master 分支
- 流程：Maven 构建 → Docker 镜像构建推送(阿里云 ACR) → SSH 部署到服务器
- Docker 镜像基于 `eclipse-temurin:21-jre`，暴露端口 8091

## 向量数据库

- 使用 Milvus 存储 Embedding 向量
- 默认 Collection：`rag_default_store`，维度 4096，距离度量 COSINE
- `CollectionInitializer` 在应用启动时自动初始化 Collection
- Embedding 模型默认使用 `Qwen3-Embedding-8B`（4096 维）
