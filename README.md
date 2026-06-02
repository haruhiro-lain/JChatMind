# JChatMind — AI Agent 智能体聊天平台

> 基于 **Spring AI + React** 的多模型 AI Agent 聊天系统，支持 Agent Loop（ReAct 模式）与工具调用。
> 面向面试准备的精简版，保留核心架构：**Think-Execute 循环 + Tool Calling + ChatMemory + SSE 实时推送**。

---

## 项目概述

| 模块 | 技术栈 | 端口 |
|------|--------|------|
| 后端 (`jchatmind/`) | Spring Boot 3.5.8 + Java 17 + MyBatis + PostgreSQL | `8080` |
| 前端 (`ui/`) | React 19 + TypeScript + Vite + Ant Design + Tailwind CSS | `15173` |

---

## 快速启动

### 前置条件

- **JDK 17+**
- **Maven**（项目自带 Maven Wrapper，无需额外安装）
- **Node.js**（推荐 18+）
- **Docker Desktop**（用于运行 PostgreSQL）

### 1. 启动 PostgreSQL

```bash
cd TEMP/docker/jchatmind
docker compose up -d postgres
```

> Windows 若端口 5432 被保留，已配置为 15432:5432 映射。

### 2. 初始化数据库

在 `psql` 中执行（或用 DataGrip 连接 `localhost:15432`，用户 `jchatmind` / 密码 `jchatmind123`）：

```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE agent (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL, description TEXT, system_prompt TEXT,
    model VARCHAR(100) DEFAULT 'deepseek-chat',
    allowed_tools JSONB DEFAULT '[]', allowed_kbs JSONB DEFAULT '[]',
    chat_options JSONB DEFAULT '{"temperature":0.7,"topP":1.0,"messageLength":10}',
    created_at TIMESTAMP DEFAULT NOW(), updated_at TIMESTAMP DEFAULT NOW()
);
CREATE TABLE chat_session (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    agent_id UUID REFERENCES agent(id) ON DELETE CASCADE,
    title VARCHAR(255), metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT NOW(), updated_at TIMESTAMP DEFAULT NOW()
);
CREATE TABLE chat_message (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID REFERENCES chat_session(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL, content TEXT, metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT NOW(), updated_at TIMESTAMP DEFAULT NOW()
);
```

> 初始化 SQL 脚本也可在 `TEMP/init.sql` 中找到。

### 3. 配置 API Key

**方式一：通过 `.env` 文件（推荐，Docker Compose 自动加载）**

```bash
# 复制模板
cp .env.example .env

# 编辑 .env 填入你的 API Key
DEEPSEEK_API_KEY=sk-your-key-here
ZHIPUAI_API_KEY=your-zhipuai-key    # 可选
```

> `.env` 已加入 `.gitignore`，不会被提交到 Git。

**方式二：直接编辑 `application.yaml`**

```yaml
spring:
  ai:
    deepseek:
      api-key: sk-your-key-here    # ← 替换为你的 Key
```

### 4. 启动后端

**命令行启动：**

```bash
cd jchatmind
set JAVA_HOME=D:\Environment\Java\jdk17   # Windows, 指向你的 JDK 17 路径
mvnw.cmd spring-boot:run                   # macOS/Linux: ./mvnw spring-boot:run
```

**IDE 一键启动：**

在 IntelliJ IDEA / VS Code 中直接运行入口类：`com.kama.jchatmind.JchatmindApplication`

验证：`curl http://localhost:8080/api/agents` → 返回 JSON

### 5. 启动前端

```bash
cd ui
npm install    # 首次运行需安装依赖
npm run dev
```

访问 `http://127.0.0.1:15173/`，开始使用。

> 若端口 5173 被 Windows 保留，`vite.config.ts` 已配置为 `127.0.0.1:15173`。

---

## 🔥 热修改支持

| 端 | 方案 | 说明 |
|----|------|------|
| **后端** | spring-boot-devtools | 修改 Java 代码后重新编译，应用自动重启（LiveReload 端口 `35729`） |
| **前端** | Vite HMR | 修改 React 组件/样式后浏览器自动局部更新，无需刷新 |

### 后端热重载触发方式

1. 在 IDE 中修改 `.java` 文件
2. 重新编译（IDE 自动编译 或 执行 `mvn compile`）
3. DevTools 检测到 `target/classes` 变更后自动重启应用

### 前端热重载触发方式

1. 修改 `.tsx` / `.ts` / `.css` 文件
2. 保存文件（`Ctrl+S`）
3. 浏览器自动更新，组件状态保留

---

## 架构总览

```
┌─────────────────────────────────────────────────────────┐
│                    前端 (React + Vite)                    │
│  Ant Design 6 + Tailwind CSS + SSE EventSource          │
│  Port: 5173                                              │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP REST + SSE
┌──────────────────────▼──────────────────────────────────┐
│                后端 (Spring Boot 3.5 + Spring AI 1.1)     │
│                                                          │
│  ┌──────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │ Controller│─▶│ FacadeService │─▶│  Mapper (MyBatis) │   │
│  │  Layer   │  │    Layer      │  │     Layer        │   │
│  └──────────┘  └──────────────┘  └────────┬─────────┘   │
│                                           │              │
│  ┌───────────────────────────────────────┐│              │
│  │          Agent 核心引擎                ││              │
│  │  ┌─────────────────────────────────┐  ││              │
│  │  │  JChatMind (Agent Loop)         │  ││              │
│  │  │  IDLE → THINKING → EXECUTING    │  ││              │
│  │  │  → FINISHED / ERROR             │  ││              │
│  │  │  think() → execute() → 循环     │  ││              │
│  │  └─────────────────────────────────┘  ││              │
│  │  JChatMindFactory (Agent 工厂)        ││              │
│  │  ChatClientRegistry (多模型注册表)     ││              │
│  │  ToolCallingManager (工具调用管理)     ││              │
│  └───────────────────────────────────────┘│              │
│                                           │              │
│  Port: 8080                               │              │
└───────────────────────────────────────────┼──────────────┘
                                            │
┌───────────────────────────────────────────┼──────────────┐
│                    基础设施 (Docker)        │              │
│  ┌─────────────────────┐                                │
│  │ PostgreSQL 16        │                                │
│  │ + pgvector 向量扩展   │                                │
│  │ Port: 15432          │                                │
│  └─────────────────────┘                                │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │  外部 LLM API (DeepSeek / 智谱 GLM-4.6)           │   │
│  └──────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────┘
```

### 核心模块

| 模块 | 技术 | 说明 |
|------|------|------|
| **Agent Loop** | 自实现 ReAct 模式 | `think()` 决策 → `execute()` 工具执行 → 循环，最多 20 步 |
| **多模型支持** | Spring AI + ChatClientRegistry | DeepSeek / 智谱 GLM-4.6 动态切换 |
| **聊天记忆** | ChatMemory (MessageWindow) | 滑动窗口记忆，窗口长度可配置 |
| **工具调用** | Spring AI Tool Calling | TerminateTool (固定) + DataBaseTools (可选) |
| **实时推送** | SSE (Server-Sent Events) | Agent 状态实时推送前端 |
| **持久化** | MyBatis + PostgreSQL + pgvector | Agent / ChatSession / ChatMessage 三表 |
| **异步驱动** | Spring Event + @Async | 聊天请求事件驱动 Agent 执行 |

### 数据模型

```
agent (智能体)
├── id (UUID)
├── name / description / system_prompt
├── model (deepseek-chat / glm-4.6)
├── allowed_tools (JSONB)    — 可选工具列表
├── allowed_kbs (JSONB)      — 知识库列表（精简版暂不使用）
└── chat_options (JSONB)     — temperature / topP / messageLength

chat_session (聊天会话)
├── id (UUID)
├── agent_id → agent.id
└── title

chat_message (聊天消息)
├── id (UUID)
├── session_id → chat_session.id
├── role (user / assistant / system / tool)
├── content
└── metadata (JSONB) — toolCalls / toolResponse
```

---

## 项目结构

```
JChatMind/
├── jchatmind/                    # 后端 (Spring Boot 3.5)
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd           # Maven Wrapper
│   └── src/main/
│       ├── java/com/kama/jchatmind/
│       │   ├── JchatmindApplication.java
│       │   ├── agent/            # Agent 核心引擎
│       │   │   ├── JChatMind.java           # Think-Execute 循环
│       │   │   ├── JChatMindFactory.java    # Agent 工厂
│       │   │   ├── AgentState.java          # 状态枚举
│       │   │   └── tools/                  # 工具集
│       │   ├── config/           # 配置 (多模型/CORS/异步)
│       │   ├── controller/       # REST 控制器
│       │   ├── service/          # 业务服务层 (Facade)
│       │   ├── mapper/           # MyBatis 数据访问
│       │   ├── converter/        # 实体<->DTO<->VO 转换
│       │   ├── model/            # 数据模型
│       │   │   ├── entity/       # 数据库实体
│       │   │   ├── dto/          # 数据传输对象
│       │   │   ├── vo/           # 视图对象
│       │   │   ├── request/      # 请求对象
│       │   │   └── response/     # 响应对象
│       │   ├── event/            # Spring 事件驱动
│       │   ├── exception/        # 全局异常处理
│       │   └── message/          # SSE 消息模型
│       └── resources/
│           ├── application.yaml
│           └── mapper/           # MyBatis XML 映射
├── ui/                           # 前端 (React 19 + Vite 7)
│   ├── package.json
│   └── src/
│       ├── api/                  # API 请求层
│       ├── components/           # UI 组件
│       │   ├── views/AgentChatView.tsx       # 对话视图
│       │   ├── modals/AddAgentModal.tsx      # Agent 配置弹窗
│       │   └── tabs/                        # 侧边栏标签页
│       ├── hooks/                # 自定义 Hooks
│       ├── contexts/             # React Context
│       ├── layout/               # 布局组件
│       └── types/                # TypeScript 类型
├── TEMP/docker/jchatmind/        # Docker 编排
│   └── docker-compose.yml
└── README.md
```

---

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/agents` | 获取所有智能体 |
| `POST` | `/api/agents` | 创建智能体 |
| `PATCH` | `/api/agents/{id}` | 更新智能体 |
| `DELETE` | `/api/agents/{id}` | 删除智能体 |
| `GET` | `/api/chat-sessions` | 获取所有聊天会话 |
| `POST` | `/api/chat-sessions` | 创建聊天会话 |
| `GET` | `/api/chat-sessions/agent/{agentId}` | 按 Agent 获取会话 |
| `DELETE` | `/api/chat-sessions/{id}` | 删除聊天会话 |
| `GET` | `/api/chat-messages/session/{sessionId}` | 获取会话消息 |
| `POST` | `/api/chat-messages` | 创建消息（触发 Agent） |
| `GET` | `/api/tools` | 获取可选工具列表 |
| `GET` | `/sse/connect/{chatSessionId}` | SSE 连接 |

---

## 面试要点

### Agent Loop (ReAct 模式)

```
用户发送消息
  → ChatEventListener 异步触发
    → JChatMindFactory.create(agentId, sessionId)
      → JChatMind.run()
        → for i = 1..20:
          → think()     # LLM 决策：直接回答 or 调用工具？
          → execute()   # 执行工具，结果注入上下文
          → 检查 terminate → 结束
```

### 工具调用机制

- **固定工具** (FIXED)：`TerminateTool` — 所有 Agent 都有，用于结束任务
- **可选工具** (OPTIONAL)：`DataBaseTools` — 允许 Agent 执行 SELECT 查询
- 手动接管 Spring AI 的 `internalToolExecutionEnabled = false`，由 `ToolCallingManager` 显式执行

### 技术亮点

1. **多模型注册表**：`ChatClientRegistry` 基于 Bean 名称动态切换 LLM
2. **SSE 实时推送**：Agent 思考/执行/完成状态实时送达前端
3. **事件驱动**：`ChatEvent` → `@Async` → Agent Loop，不阻塞 HTTP 线程
4. **滑动窗口记忆**：`MessageWindowChatMemory` + PostgreSQL 持久化
5. **分层架构**：Controller → FacadeService → Mapper，职责清晰

---

## Docker 常用命令

```bash
cd TEMP/docker/jchatmind

# 启动 PostgreSQL（beta 精简版无需 Ollama）
docker compose up -d postgres

# 查看状态
docker compose ps

# 查看日志
docker compose logs -f postgres

# 进入 psql
docker exec -it jchatmind-postgres psql -U jchatmind -d jchatmind

# 停止
docker compose down
```

> PostgreSQL 映射端口: `15432`（Windows 避免与系统端口冲突）
> 数据库: `jchatmind` | 用户/密码: `jchatmind` / `jchatmind123`

---

## 常见问题

### 后端启动报数据库连接失败

检查 PostgreSQL 是否运行在 `localhost:15432`，数据库 `jchatmind` 是否已创建。

### 前端启动报端口占用

修改 `ui/vite.config.ts` 中的 `server.port` 配置。

### 前端请求后端接口跨域

开发环境下 Vite 已配置代理，接口请求会自动转发到后端 `8080` 端口。

---

## 版本说明

| 分支 | 说明 |
|------|------|
| `main` | 完整版（含 RAG 知识库 + Ollama 嵌入模型） |
| `beta` | **精简版**（当前）— 核心 Agent + Tool Calling，无需 Ollama |
