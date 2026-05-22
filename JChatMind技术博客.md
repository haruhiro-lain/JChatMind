# JChatMind：从零搭建一个面试级 AI Agent 智能体平台

> 基于 Spring AI + React 的 AI Agent 个人项目，涵盖 Agent Loop 自主决策、工具调用、RAG 知识库检索、SSE 实时推送。
> 本文从**最小可运行原型**出发，逐步叠加功能模块，完整呈现一个 AI Agent 项目的技术全貌。


## 一、项目简介

JChatMind 是一个智能 AI Agent 系统，基于 **Spring AI** 框架构建，实现了自主决策、工具调用和知识库检索等核心能力。

**它不是"聊天机器人"，而是 Agent**：能规划、能调用工具、能检索知识库、还能把执行过程实时推给前端。

做完它之后，面试官再问 AI 项目，你能讲的不是"我接了个接口"，而是：

- 我实现了 **Think-Execute 循环**（ReAct 模式自主决策）
- 我设计了 **可扩展工具调用框架**（固定工具 + 可选工具 + 手动接管 Spring AI）
- 我实现了 **RAG 全链路**（Markdown 解析 → 分块 → Embedding → pgvector 检索）
- 我构建了 **多模型注册表模式**（DeepSeek / 智谱 GLM-4.6 动态切换）
- 我实现了 **SSE 实时推送**（Agent 执行状态可视化）
- 我完成了 **事件驱动的异步执行架构**（ChatEvent → @Async → Agent Loop）

**技术栈**：

| 层级 | 技术 | 说明 |
|------|------|------|
| 后端框架 | Spring Boot 3.5 | 应用框架 |
| AI 框架 | Spring AI 1.1 | LLM 集成、ChatClient、Tool Calling、ChatMemory |
| 数据库 | PostgreSQL 16 + pgvector | 业务数据 + 向量检索（RAG） |
| ORM | MyBatis 3.0 | 持久层，含自定义 TypeHandler |
| 嵌入模型 | Ollama + bge-m3 | 本地 Embedding（RAG 用） |
| LLM | DeepSeek / 智谱 GLM-4.6 | 大语言模型（API Key 配置） |
| 实时通信 | SSE | 服务端推送 Agent 状态 |
| 前端 | React 19 + Vite 7 + Ant Design 6 | 前端界面 |
| 容器化 | Docker + Compose | PostgreSQL + Ollama 一键启动 |

**项目地址**：[https://github.com/youngyangyang04/JChatMind](https://github.com/youngyangyang04/JChatMind)


## 二、开发路线：从最小原型到完整项目

这个项目适合**渐进式开发**，建议分四个里程碑完成：

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Milestone 1  │───▶│  Milestone 2  │───▶│  Milestone 3  │───▶│  Milestone 4  │
│  基础聊天     │    │  Agent Loop   │    │  RAG 知识库    │    │  完善与面试   │
│  (V1)        │    │  + 工具调用    │    │  (V3)         │    │              │
│  1-2 天       │    │  2-3 天       │    │  2-3 天       │    │  1-2 天       │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
```

### Milestone 1：基础聊天（ChatClient + ChatMemory）

**目标**：打通 LLM 调用链路，实现有记忆的对话。

**技术点**：
- Spring AI `ChatClient` 集成 DeepSeek / 智谱
- `MessageWindowChatMemory` 滑动窗口记忆
- `SystemMessage` 系统提示词注入
- `ChatClientRegistry` 注册表模式管理多模型

**关键代码**：

```java
// 多模型 Bean 注册
@Bean("deepseek-chat")
public ChatClient deepSeekChatClient(DeepSeekChatModel model) {
    return ChatClient.create(model);
}

// 注册表模式
@Component
public class ChatClientRegistry {
    private final Map<String, ChatClient> chatClients;
    public ChatClient get(String key) { return chatClients.get(key); }
}
```

### Milestone 2：Agent Loop + 工具调用（当前 beta 分支核心）

**目标**：让 Agent 能自主决策、调用工具完成任务。

**技术点**：
- Think-Execute 循环（ReAct 模式），最多 20 步
- 状态机管理：`IDLE → THINKING → EXECUTING → FINISHED`
- 固定工具（TerminateTool）+ 可选工具（DataBaseTools）
- **手动接管** Spring AI 的 `internalToolExecutionEnabled = false`
- `ToolCallingManager` 显式执行工具调用
- 工具结果注入 `ChatMemory` + 持久化到 PostgreSQL
- SSE 实时推送 Agent 每一步状态

**核心循环**：

```java
// JChatMind.java
public void run() {
    for (int i = 0; i < MAX_STEPS && agentState != FINISHED; i++) {
        step();  // think() → execute()
    }
}

private void step() {
    if (think()) {       // LLM 决策：直接回答 or 调用工具？
        execute();       // 执行工具，结果注入上下文，循环继续
    } else {
        agentState = FINISHED;  // 无工具调用 = 任务结束
    }
}
```

**工具接口设计**：

```java
public interface Tool {
    String getName();          // 工具唯一标识
    String getDescription();   // 给 LLM 看的描述
    ToolType getType();        // FIXED（强制）| OPTIONAL（可选）
}
```

**事件驱动异步架构**：

```
用户 POST /api/chat-messages
  → ChatMessageFacadeService 保存消息
  → 发布 ChatEvent
  → HTTP 200 立即返回

异步线程:
  → ChatEventListener(@Async) 接收事件
  → JChatMindFactory.create() 构建 Agent
  → agent.run() 执行循环
  → 每步通过 SSE 推送到前端
```

### Milestone 3：RAG 知识库检索

**目标**：Agent 能从私有知识库中检索信息，结合 LLM 回答。

这是从"能聊天"到"企业级应用"的关键一步。

**技术栈**：

| 组件 | 用途 |
|------|------|
| PostgreSQL + pgvector | 向量存储与相似度检索 |
| Ollama + bge-m3 | 本地 Embedding 模型（1.2GB） |
| flexmark | Markdown 文档解析与分块 |
| ivfflat 索引 | 加速向量检索，支持 10 万+ 向量 |

**全链路流程**：

```
上传 Markdown 文档
  → MarkdownParserService 解析 + 分块
    → Ollama bge-m3 生成 Embedding
      → 存入 PostgreSQL pgvector 列
        ┌────────────────────────────┐
        │  用户提问                   │
        │  → 生成 Query Embedding     │
        │  → pgvector 相似度检索 (<->)│
        │  → Top-K 相关片段           │
        │  → 拼入 Prompt 发给 LLM     │
        └────────────────────────────┘
```

**为什么选 PostgreSQL + pgvector？** 一套数据库管理结构化和向量数据，部署简单，事务一致性，ivfflat 索引优化后 10 万级向量检索毫秒级响应。

### Milestone 4：完善与面试准备

**目标**：打磨项目细节，准备面试话术。

**可扩展的工具生态**：

| 工具 | 类型 | 功能 |
|------|------|------|
| `TerminateTool` | FIXED | 结束 Agent 任务 |
| `DataBaseTools` | OPTIONAL | 执行 PostgreSQL SELECT 查询 |
| `KnowledgeTools` | FIXED | 触发 RAG 知识库检索 |
| `EmailTools` | OPTIONAL | 异步发送邮件 |
| `FileSystemTools` | OPTIONAL | 读写文件系统 |
| `WeatherTool` | — | 天气查询（演示用） |

**性能指标（可写进简历）**：响应 < 2s、并发 100+、RAG 准确率 85%+


## 三、项目架构图

```
┌──────────────────────────────────────────────────────────┐
│                     展现层（Web 端）                       │
│  React 19 + Ant Design 6 + Tailwind CSS                  │
│  智能体管理 / 聊天会话 / 知识库管理 / SSE 状态可视化         │
└──────────────────────┬───────────────────────────────────┘
                       │ REST API + SSE
┌──────────────────────▼───────────────────────────────────┐
│                      API 层                               │
│  AgentController / ChatSessionController                 │
│  ChatMessageController / KnowledgeBaseController          │
│  SseController / DocumentController / ToolController      │
└──────────────────────┬───────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────┐
│                   系统服务层（核心）                         │
│                                                           │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────┐   │
│  │ Agent 管理   │  │ 会话管理      │  │ 知识库管理      │   │
│  │ Think-Exec   │  │ 生命周期控制   │  │ Markdown 解析  │   │
│  │ 状态机       │  │ 上下文维护     │  │ 内容分块       │   │
│  │ 生命周期     │  │ 异步事件发布   │  │ 文档导入       │   │
│  └─────────────┘  └──────────────┘  └────────────────┘   │
│                                                           │
│  ┌──────────────────────────────────────────────────┐    │
│  │               SSE 服务（实时推送）                  │    │
│  │  连接建立/保持 → 消息推送 → THINKING/EXECUTING/DONE │    │
│  └──────────────────────────────────────────────────┘    │
└──────────────────────┬───────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────┐
│                      服务层                               │
│                                                           │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐    │
│  │ AI 模型   │  │ RAG 引擎  │  │ 工具调用              │    │
│  │ DeepSeek  │  │ bge-m3   │  │ 数据库读取 / 文件读写  │    │
│  │ 智谱 AI   │  │ 向量检索  │  │ 邮件发送 / 天气查询   │    │
│  └──────────┘  └──────────┘  └──────────────────────┘    │
│                                                           │
│  ┌──────────────────────────────────────────────────┐    │
│  │              记忆存储（ChatMemory）                 │    │
│  │  内存临时存储 → PostgreSQL 持久化 → 上下文恢复      │    │
│  └──────────────────────────────────────────────────┘    │
└──────────────────────┬───────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────┐
│                     基础层                                │
│  ┌─────────────────────┐  ┌────────────────────────┐    │
│  │ PostgreSQL 16        │  │ Ollama 本地模型服务      │    │
│  │ + pgvector 向量扩展   │  │ bge-m3 Embedding 模型   │    │
│  └─────────────────────┘  └────────────────────────┘    │
└──────────────────────────────────────────────────────────┘
```

**分层职责**：

| 层 | 职责 | 关键技术 |
|----|------|---------|
| **展现层** | 用户交互、SSE 状态可视化 | React + Ant Design + EventSource |
| **API 层** | RESTful 接口，屏蔽内部复杂度 | Spring MVC + 统一 ApiResponse |
| **系统服务层** | Agent 生命周期、会话管理、知识库、SSE | 事件驱动 + 异步执行 |
| **服务层** | AI 模型调用、RAG 检索、工具执行、记忆存储 | Spring AI + pgvector + MyBatis |
| **基础层** | 数据持久化、向量存储、本地模型 | Docker + PostgreSQL + Ollama |


## 四、数据模型

```sql
-- 智能体配置表
CREATE TABLE agent (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    system_prompt TEXT,              -- 系统提示词
    model VARCHAR(100) DEFAULT 'deepseek-chat',
    allowed_tools JSONB DEFAULT '[]', -- 可选工具列表
    allowed_kbs JSONB DEFAULT '[]',   -- 可访问的知识库
    chat_options JSONB DEFAULT '{"temperature":0.7,"topP":1.0,"messageLength":10}',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 聊天会话表
CREATE TABLE chat_session (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    agent_id UUID REFERENCES agent(id) ON DELETE CASCADE,
    title VARCHAR(255),
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 聊天消息表（核心 — Milestone 1-2 即可用）
CREATE TABLE chat_message (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID REFERENCES chat_session(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,       -- user / assistant / system / tool
    content TEXT,
    metadata JSONB DEFAULT '{}',     -- toolCalls / toolResponse
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 以下为 Milestone 3（RAG）需要的扩展表

-- 知识库表
CREATE TABLE knowledge_base (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 文档表
CREATE TABLE document (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    kb_id UUID REFERENCES knowledge_base(id) ON DELETE CASCADE,
    filename VARCHAR(255),
    filetype VARCHAR(50),
    size BIGINT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 文档分块 + 向量表
CREATE TABLE chunk_bge_m3 (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    doc_id UUID REFERENCES document(id) ON DELETE CASCADE,
    content TEXT,
    embedding vector(1024),          -- bge-m3 输出 1024 维向量
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX ON chunk_bge_m3 USING ivfflat (embedding vector_cosine_ops);
```


## 五、学习路线与时间规划

| 人群 | 学习目标 | 建议时间 | 重点 |
|------|---------|---------|------|
| **有 Java 项目基础** | 吃透 Agent 思路 + 工程实现 | 2-3 周 | M1-M4 全部完成，理解每个设计决策 |
| **只做过 Java 小项目** | 建立 AI 工程体系认知 | 3-4 周 | M1-M2 为主，M3 理解概念即可 |
| **突击面试用** | 跑通 Demo，背面试题 | 1 周 | 跑通 M1-M2，重点看「面试怎么说」章节 |

**建议节奏**（每天 4-5 小时）：

- **第 1 周**：环境搭建 + M1 基础聊天（ChatClient、ChatMemory、多模型）
- **第 2 周**：M2 Agent Loop + 工具调用（Think-Execute 循环、工具框架）
- **第 3 周**：M3 RAG 知识库（Markdown 解析、Embedding、pgvector 检索）
- **第 4 周**：M4 完善 + 面试准备（性能优化、简历写法、模拟面试）


## 六、快速开始

### 前置条件

| 工具 | 版本 | 
|------|------|
| JDK | 17+ |
| Maven | 3.6+（IDEA 自带） |
| Node.js | 22+ |
| Docker Desktop | 最新版 |

### 环境搭建（5 步）

**第 1 步 — 拉取代码**

```bash
git clone https://github.com/youngyangyang04/JChatMind.git
cd JChatMind
```

**第 2 步 — 启动 Docker 基础设施**

```bash
cd TEMP/docker/jchatmind

# 最小原型（仅 PostgreSQL）
docker compose up -d postgres

# 完整项目（PostgreSQL + Ollama，Milestone 3 需要）
docker compose up -d
docker exec -it jchatmind-ollama ollama pull bge-m3
```

> PostgreSQL 映射 `localhost:15432`，数据库 `jchatmind`，用户/密码 `jchatmind/jchatmind123`

**第 3 步 — 配置 API Key**

编辑 `jchatmind/src/main/resources/application.yaml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:15432/jchatmind
    username: jchatmind
    password: jchatmind123
  ai:
    deepseek:
      api-key: sk-your-key-here     # ← 替换
```

**第 4 步 — 启动后端**

```bash
cd jchatmind
# Windows
set JAVA_HOME=D:\Environment\Java\jdk17
mvnw.cmd spring-boot:run

# macOS / Linux
export JAVA_HOME=/path/to/jdk17
./mvnw spring-boot:run
```

验证：`curl http://localhost:8080/api/agents` → 返回 `{"code":200,...}`

**第 5 步 — 启动前端**

```bash
cd ui
npm install
npm run dev
```

访问 `http://127.0.0.1:15173/`。


## 七、面试怎么说

### 1 分钟自我介绍模板

> "我做过一个 AI Agent 智能体项目 JChatMind，基于 Spring AI + React 构建。核心是 Think-Execute 循环的自主决策引擎，大模型在每一步自主决定是直接回答还是调用工具。工具系统采用接口化设计，分固定工具和可选工具，新增工具不碰核心代码。系统通过 SSE 实时推送 Agent 的思考和执行状态到前端，采用事件驱动架构在异步线程中运行，不阻塞 HTTP 响应。在多模型支持上，用注册表模式实现 DeepSeek 和智谱的动态切换。项目还集成了 RAG 知识库检索，基于 PostgreSQL pgvector 做向量相似度搜索。"

### 常见追问速答

**Q: Agent Loop 怎么防止无限循环？**

> 三个机制：(1) 硬限制 MAX_STEPS = 20；(2) TerminateTool 让 LLM 主动终止；(3) 每步检查 agentState 状态机。

**Q: 工具调用怎么扩展？**

> 实现 Tool 接口 → @Component 自动注册 → ToolFacadeService 按 FIXED/OPTIONAL 分类 → Agent 配置中勾选即可。新增工具不修改 JChatMind.java 核心代码。

**Q: 为什么不用 Spring AI 的自动工具执行？**

> 我需要手控三个关键步骤：工具结果注入 ChatMemory（影响后续对话）、SSE 推送到前端（用户体验）、持久化到 PostgreSQL（审计和恢复）。自动执行会跳过这些。

**Q: RAG 为什么选 pgvector 而不是专用向量数据库？**

> 部署简单（一套 PG 搞定）、事务一致（知识和向量在同一个事务）、成本低。ivfflat 索引优化后 10 万级向量响应毫秒级。

**Q: SSE 和 WebSocket 怎么选？**

> Agent 执行是服务端单向推送状态，客户端只需接收。SSE 基于 HTTP，实现简单，EventSource API 浏览器原生支持。


## 八、总结

| 层面 | 你学到了什么 |
|------|-------------|
| **理论** | ReAct 模式、状态机、滑动窗口记忆、向量相似度检索 |
| **设计** | 注册表模式、事件驱动、分层解耦、工具接口化 |
| **工程** | Spring AI、MyBatis、SSE、pgvector、Docker Compose |
| **面试** | Agent Loop 终止条件、工具扩展性、RAG 技术选型理由 |

当面试官问「你的 Agent 和普通聊天机器人有什么区别」时，你能从架构图一路讲到代码细节——这就是项目经验的核心价值。
