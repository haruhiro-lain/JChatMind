# MindHarness 开发 TODO

> 基于 `beta` 分支从 `main` 拉出后的精简框架阶段提交记录整理。  
> 时间线：2026-05-22 ~ 2026-05-29

---

## 一、架构精简 ✅ 已完成

### 1.1 后端精简
- [x] 删除 `KnowledgeBase` / `Document` / `ChunkBgeM3` 相关的 Controller、Service、Mapper、Converter、Model
- [x] 删除 `MarkdownParser`、`RagService`、`EmailService` 及对应实现
- [x] 删除非核心工具：`KnowledgeTools`、`EmailTools`、`FileSystemTools`
- [x] 删除 `MindHarnessV1` / `MindHarnessV2` 示例文件
- [x] 修改 `MindHarnessFactory`：移除 KnowledgeBase 依赖，简化知识库解析逻辑
- [x] 修改 `MindHarness`：移除 `availableKbs` 字段，简化 `think()` 提示词
- [x] 修改 `AgentConverter`：`allowedKbs` 改为可选字段
- [x] 修改 `pom.xml`：移除 `flexmark`、`spring-boot-starter-mail` 依赖
- [x] 修改 `application.yaml`：移除 mail 和 document-storage 配置
- [x] 保留核心架构：Agent Loop + Tool Calling + ChatMemory + SSE 实时推送

### 1.2 前端精简
- [x] 删除 `useKnowledgeBases` / `useDocuments` hooks
- [x] 删除 `KnowledgeBaseView`、`KnowledgeBaseTabContent`、`AddKnowledgeBaseModal` 组件
- [x] 精简 `api.ts`：移除知识库/文档相关 API 函数和类型定义
- [x] 精简 `types/index.ts`：移除 KnowledgeBase 相关类型
- [x] 精简 `MindHarnessLayout`：移除知识库路由
- [x] 精简 `SideMenu`：移除知识库标签页，清理未使用的 import
- [x] 精简 `AddAgentModal`：移除知识库设置面板
- [x] 保留核心 UI：智能体管理 + 聊天会话 + Agent 对话视图

### 1.3 清理残留
- [x] 删除 `mindharness/output.txt`（测试输出文件）
- [x] 删除 `examples/` 测试页面和重复技术博客
- [x] 删除 Demo/禁用工具：`CityTool`、`DateTool`、`WeatherTool`、`DirectAnswerTool`
- [x] 删除 RAG 遗留代码和空目录（`PgVectorTypeHandler`、`contexts/`、`hooks/`）
- [x] 删除 `.DS_Store` 并加入 `.gitignore`
- [x] 删除空测试类 `MindHarnessTests.java`
- [x] 删除 `TEMP/` 临时目录
- [x] 移除 `allowedKbs` 遗留字段
- [x] 修复 `MindharnessApplication.java` 重复 import
- [x] 确认 `.gitignore` 正确排除 `TEMP/` 和 `target/` 目录

---

## 二、Bug 修复 & 端到端验证 ✅ 已完成

- [x] 修复空状态页"知识问答"卡片文案 → 改为"工具调用"（KB 已移除）
- [x] 修复 SSE URL 硬编码问题 → 改为从 `http.ts` 动态获取 `BASE_URL`
- [x] 清理 `AgentChatView` 未使用的 `BASE_URL` import
- [x] 修复前端 UI Bug
- [x] 端到端验证通过：创建 Agent → 发送消息 → Agent Loop → AI 回复渲染正常

---

## 三、文档建设 ✅ 已完成

### 3.1 README
- [x] 架构总览图（前端 + 后端 + 基础设施）
- [x] 核心模块说明（Agent Loop / 多模型 / ChatMemory / 工具调用 / SSE）
- [x] 数据模型（agent / chat_session / chat_message 三表）
- [x] 5 步快速启动（Docker → 建表 → API Key → 后端 → 前端）
- [x] 项目结构树
- [x] API 接口清单
- [x] 面试要点（Agent Loop 流程 / 工具调用机制 / 技术亮点）
- [x] Docker 常用命令

### 3.2 技术博客
- [x] 从"调 API"到"真 Agent"的叙事角度
- [x] 详细剖析 Agent Loop / Tool Calling / 多模型注册表 / SSE / 事件驱动五大核心设计
- [x] 四里程碑渐进式开发路线（M1 基础聊天 → M2 Agent Loop → M3 RAG → M4 面试）
- [x] 完整分层架构图（展现层 → API 层 → 系统服务层 → 服务层 → 基础层）
- [x] 六表完整数据模型（含 M3 需要的 knowledge_base / document / chunk_bge_m3）
- [x] 学习路线与时间规划

---

## 四、功能增强 ✅ 已完成

### 4.1 深色主题 — VA-11 Hall-A 赛博朋克风格
- [x] 全局 CSS 变量系统，支持深/浅主题切换（默认深色）
- [x] CRT 扫描线效果、霓虹青光晕边框、暗蓝渐变背景
- [x] Inter + JetBrains Mono 字体
- [x] 右下角浮动按钮：侧边栏切换 / 回到顶部 / 滚到底部 / 主题切换
- [x] 全组件适配深色模式（侧边栏、卡片、弹窗、输入框、气泡）
- [x] 强制覆盖 Ant Design CSS 变量
- [x] `index.html` 内联脚本预加载主题防闪烁

### 4.2 API Key 自定义
- [x] Agent 实体/DTO/VO 增加 `apiKey` 字段（数据库 `api_key` 列迁移）
- [x] `DynamicChatClientFactory`：支持用户自定义 Key 动态创建 ChatModel
- [x] Agent 配置 Key 优先级高于消息携带 Key，为空回退系统预设
- [x] `AddAgentModal` 模型设置区增加 API Key 输入
- [x] 聊天界面移除 API Key 输入（统一由 Agent 设置管理）

### 4.3 DeepSeek V4 模型支持
- [x] `ModelType` 枚举更新：`DEEPSEEK_PRO` + `DEEPSEEK_FLASH`
- [x] `MultiChatClientConfig` 注册 `deepseek-pro` / `deepseek-flash` Bean
- [x] `DynamicChatClientFactory` 适配新模型名
- [x] 向后兼容旧数据 `deepseek-chat` → `deepseek-pro`
- [x] 前端下拉选项：DeepSeek V4 Pro / Flash + GLM-4.6

### 4.4 角色卡导入
- [x] 支持导入 SillyTavern / Chub 角色卡（PNG / JSON）作为 Agent
- [x] 角色卡导入后自动保存为智能体头像

### 4.5 聊天 UI 改造
- [x] 聊天 UI 改造为 SillyTavern 风格（角色头像 + 名称标签 + 赛博配色）
- [x] 头像加载失败时回退 emoji
- [x] 头像 img/emoji 叠加显示修复
- [x] nginx `avatars` 代理 + Docker Compose 头像持久化

### 4.6 安全增强
- [x] 移除硬编码 API Key，改用 `.env` 环境变量

---

## 五、补回计划：分步实施方案

> 将精简掉的模块按依赖关系和复杂度拆分为 5 步，每步独立可验证。  
> 前置条件：`beta` 分支当前代码（Agent Loop + Tool Calling + SSE 已就绪）。

---

### 第一步：RAG 知识库系统 ⭐⭐⭐

> **目标**：让 Agent 能够检索用户上传的私有知识库，基于文档内容回答。  
> 这是单体最大模块，涉及数据库、后端、前端三个层面。

#### 环境依赖（新增）

| 依赖 | 版本 | 用途 |
|---|---|---|
| `spring-ai-transformers` | 1.1.0 | Spring AI 的 Embedding 模型支持（BGE-M3） |
| `org.apache.tika:tika-core` | 2.9.2 | 文档文本提取（PDF/Word/HTML） |
| `org.apache.tika:tika-parsers-standard-package` | 2.9.2 | Tika 完整解析器包 |
| `com.vladsch.flexmark:flexmark-all` | 0.64.8 | Markdown 渲染（Agent 回复中展示） |
| `pgvector` 扩展 | pg16 已装 | 向量存储与余弦相似度检索 |
| BGE-M3 模型文件 | `bge-m3.onnx` ~2GB | 本地 Embedding（或使用 SiliconFlow API） |

#### 实现方式

**1. 数据库（3 张新表 + 1 字段）**

- `knowledge_base` — 知识库元数据（名称、Embedding 模型）
- `document` — 文档记录（文件名、类型、解析文本、处理状态）
- `chunk_bge_m3` — 文本块向量（chunk_text + embedding(1024) + ivfflat 索引）
- `agent.kb_ids` — UUID 数组，Agent 关联哪些知识库

**2. 后端新增类（~15 个文件）**

```
agent/tools/KnowledgeTools.java        ← Agent 调用的 search() 方法
service/RagService.java                ← 核心：分块→向量化→检索
service/impl/RagServiceImpl.java
controller/KnowledgeBaseController.java  ← CRUD API
controller/DocumentController.java       ← 上传/删除 API
converter/KnowledgeBaseConverter.java
converter/DocumentConverter.java
mapper/KnowledgeBaseMapper.java + XML
mapper/DocumentMapper.java + XML
mapper/ChunkBgeM3Mapper.java + XML
model/entity/KnowledgeBase.java
model/entity/Document.java
model/entity/ChunkBgeM3.java
... 对应的 DTO / Request / Response / VO ...
```

**3. RAG 核心流程**

```
用户上传 PDF → Tika 提取文本 → 500字符分块(重叠50)
→ BGE-M3 向量化(1024维) → INSERT INTO chunk_bge_m3
→ Agent 提问时调用 knowledgeTool.search()
→ 查询向量化 → pgvector 余弦相似度检索 → TOP-K 结果
→ 拼接为 Context 注入 System Prompt → LLM 生成回答
```

**4. 前端恢复（~5 个组件）**

- `KnowledgeBaseTabContent.vue` — 侧边栏知识库列表
- `AddKnowledgeBaseModal.vue` — 创建/编辑弹窗
- `UploadDocumentModal.vue` — 文档上传（拖拽 + 进度条）
- `KnowledgeBaseView.vue` — 知识库详情页（文档列表 + 检索测试）
- `useKnowledgeBases.ts` / `useDocuments.ts` — 两个 composable
- `api.ts` / `types/index.ts` — 追加类型和接口函数
- `SideMenu.vue` — 恢复知识库标签页
- `AddAgentModal.vue` — 恢复知识库关联面板（Checkbox 列表）

**5. 配置 (`application.yaml`)**

```yaml
spring.ai.embedding.openai:
  api-key: ${EMBEDDING_API_KEY:}
  base-url: https://api.siliconflow.cn/v1
  options.model: BAAI/bge-m3
document.storage.path: uploads/documents
```

**6. 验证方式**

```bash
# 1. 创建知识库 → 上传 PDF → 查看 document.status 变为 done
# 2. 在聊天中问 "根据知识库，xxx 是什么？"
# 3. SSE 日志中看到 knowledgeTool 被调用 → 返回检索结果
```

---

### 第二步：工具生态扩展 ⭐⭐

> **目标**：恢复和扩展 Agent 可用的工具集，让 Agent 能发邮件、读写文件、发 HTTP 请求。  
> 每个工具都是独立的 `@Component`，实现 `Tool` 接口即可挂载到 Agent Loop。

#### 环境依赖（新增）

| 依赖 | 版本 | 用途 |
|---|---|---|
| `spring-boot-starter-mail` | (Spring Boot 管理) | JavaMailSender 发邮件 |

#### 实现方式

**1. FileSystemTools** — 文件读写

- 实现 `Tool` 接口，注册为 `@Component`
- `readFile(path)` / `writeFile(path, content)` / `listDirectory(path)`
- 工作目录通过 `application.yaml` 的 `workspace.root` 配置
- Agent 可用于代码生成后写入文件、读取配置文件

**2. EmailTools** — 邮件发送

- 注入 `JavaMailSender`，调用 `sendEmail(to, subject, body)`
- 配置在 `application.yaml` 的 `spring.mail.*`
- 环境变量 `MAIL_USERNAME` / `MAIL_PASSWORD`
- Agent 可用于发送日报、告警通知

**3. HttpRequestTool** — HTTP 请求

- 使用 `java.net.http.HttpClient`（Java 11+ 内置，零依赖）
- `request(url, method, body)` → 返回响应体
- Agent 可用于调用外部 API、Webhook

**4. 工具注册（ToolFacadeService）**

- 新增 `getFileSystemTools()` / `getEmailTools()` / `getHttpRequestTool()`
- `MindHarnessFactory.build()` 中根据 `agent.allowed_tools` JSON 数组动态挂载
- 例如 `["dataBaseTool","knowledgeTool","emailTool"]` → 生成对应的 `ToolCallback` 列表

**5. 验证方式**

```bash
# 对 Agent 说："帮我发送一封邮件到 test@example.com，主题 Hello，内容 Test"
# SSE 日志：emailTool called → 邮件发送成功
```

---

### 第三步：ChatMode 后端集成 + 对话记忆优化 ⭐⭐

> **目标**：让前端已有的 Ask / Agent / Plan 模式在后端真正生效；优化长对话的记忆管理。

#### 环境依赖

无新增外部依赖，纯代码改动。

#### 实现方式

**1. ChatMode 传递链路**

```
前端 AgentChatInput (mode prop)
  → emit('send', { text, mode })
  → AgentChatView.handleSendMessage()
  → POST /api/chat-messages { ..., chatMode: "plan" }
  → CreateChatMessageRequest.chatMode 字段（新增）
  → ChatMessage 实体存入 DB（chat_mode 列）
  → MindHarness.think() 读取 chatMode 分支
```

**2. 三种模式行为**

| 模式 | `think()` 行为 |
|---|---|
| `ask` | 不加载任何 Tool，纯 ChatClient 问答 |
| `agent` | 默认模式，加载 `allowed_tools` 中所有工具 |
| `plan` | 先调用 LLM 生成执行计划 → SSE 推送 → 等待确认 → 逐步执行 |

**3. Plan 模式状态机**

```
IDLE → PLANNING (LLM 生成计划)
     → PLAN_READY (SSE 推送计划, 等待用户 confirm)
     → EXECUTING (逐步执行)
     → DONE
```

- `AgentState` 枚举新增 `PLANNING`、`PLAN_READY`
- `SseService` 新增 `sendPlanMessage(plan)` 方法
- 新增 `POST /api/chat-sessions/{id}/confirm-plan` 确认接口

**4. 摘要记忆 (Summary Memory)**

- 当对话轮次 > 20 时，取最早的 10 轮调用 ChatModel 生成摘要
- 摘要存入 `ChatMemory` 的 System Message，替换旧消息
- 实现为 `SummaryMemoryAugmenter` 类，注入 `MindHarness.think()`

**5. 向量记忆 (Vector Memory)**

- 复用第一步的 `RagService` 和 `chunk_bge_m3` 表
- 每次对话结束，将关键信息向量化存为 memory chunk（`kb_id` 用特殊 UUID）
- 新对话开始时检索相关历史记忆注入 Prompt

**6. 验证方式**

```bash
# Ask 模式：发送消息 → 观察 AI 回复无 tool call
# Agent 模式：发送 "查询用户表" → 观察 dataBaseTool 被调用
# Plan 模式：发送 "帮我搭建一个 Spring Boot 项目" → 观察先生成计划
```

---

### 第四步：前端 UX 增强 ⭐⭐

> **目标**：消息编辑/重新生成、会话搜索、移动端适配、国际化。

#### 环境依赖（新增）

| 依赖 | 版本 | 用途 |
|---|---|---|
| `vue-i18n` | ^10.x | 国际化 |
| `vitest` + `@vue/test-utils` + `jsdom` | latest | 前端组件测试 |

#### 实现方式

**1. 消息编辑 & 重新生成**

- 每条 AI 消息 hover 时显示操作按钮（`EditOutlined` / `ReloadOutlined`）
- 重新生成：`DELETE /api/chat-messages?since={messageId}` → 重新发送最后一条用户消息
- 编辑：将消息内容回填到输入框（`AgentChatInput` 新增 `editText` prop）

**2. 会话搜索**

- `ChatTabContent.vue` 顶部增加 `a-input` 搜索框
- 前端过滤：按 `title` + `agentName` 模糊匹配
- 防抖 300ms

**3. 移动端响应式**

- `Sidebar.vue`：< 768px 变为固定浮层，点击遮罩关闭
- 输入框 `font-size: 16px` 防止 iOS 自动缩放
- Agent 卡片单列布局

**4. 国际化 (i18n)**

- `src/i18n/index.ts`：`createI18n()` 注册 zh-CN / en-US / ja-JP
- `main.ts`：`app.use(i18n)`
- 组件中使用 `$t('chat.placeholder')` 替换硬编码中文

**5. 组件测试**

- `vitest.config.ts`：jsdom 环境
- `AgentChatInput.spec.ts`：验证 emit 事件、trim 行为
- `AgentTabContent.spec.ts`：验证卡片渲染、点击事件

---

### 第五步：DevOps 基础设施 ⭐

> **目标**：CI/CD 自动构建测试、日志监控、数据库备份、性能压测。

#### 环境依赖（新增）

| 依赖 | 用途 |
|---|---|
| GitHub Actions | 免费 CI/CD（已内置） |
| Loki + Grafana (Docker) | 日志聚合 + 可视化 |
| k6 | API 性能压测 |

#### 实现方式

**1. CI/CD Pipeline**

`.github/workflows/build.yml`：
- **backend job**：PostgreSQL service container → `mvnw test` → 上传测试报告
- **frontend job**：`npm ci` → `npm run build` → `npm run lint`
- **docker job**：`docker compose build` → `up -d` → `curl` 健康检查

**2. 日志监控**

`docker-compose.yml` 新增两个服务：
- `loki:3100` — 日志聚合
- `grafana:3000` — 可视化面板（预配 Loki 数据源 + 仪表板 JSON）

**3. 数据库备份**

- `docker/mindharness/backup.sh`：`pg_dump` → 保留 7 天
- `docker-compose.yml` 新增 `backup` 服务（cron 每日执行）

**4. 性能压测**

- `k6-test.js`：模拟 10 并发用户，持续 2 分钟
- 监控指标：P95 延迟、错误率、吞吐量

---

### 执行顺序建议

```
第一步(RAG) → 第二步(工具) → 第三步(ChatMode+记忆)
                    ↘ 第四步(前端UX) ← 可与第三步并行
                    ↘ 第五步(DevOps) ← 最后执行，不依赖前三步
```

每步完成后验证标准：

| 步骤 | 验证方式 |
|---|---|
| 第一步 | 上传 PDF → 知识库问答 → 检索结果正确 |
| 第二步 | Agent 调用 emailTool → 邮件发送成功 |
| 第三步 | 切换 Ask/Agent/Plan → 行为差异可见 |
| 第四步 | 移动端 Chrome DevTools → 布局正常；切换语言 → UI 变化 |
| 第五步 | Push 代码 → GitHub Actions 通过；k6 压测 → P95 < 2s |

**目标**：让 Agent 能够检索用户上传的私有知识库，基于文档内容回答。

**技术栈**：
| 层级 | 技术 |
|---|---|
| 向量数据库 | pgvector (已安装 PostgreSQL pg16) |
| Embedding 模型 | BGE-M3 (BAAI/bge-m3, 1024维) 或 text-embedding-3-small |
| 文档解析 | Apache Tika (PDF/Word/HTML/Markdown) |
| Markdown 渲染 | flexmark 0.64.8 |

#### 5.1.1 数据库 Schema

```sql
-- 知识库表
CREATE TABLE IF NOT EXISTS knowledge_base (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    embedding_model VARCHAR(100) DEFAULT 'bge-m3',
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

-- 文档表
CREATE TABLE IF NOT EXISTS document (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    kb_id           UUID NOT NULL REFERENCES knowledge_base(id) ON DELETE CASCADE,
    file_name       VARCHAR(500) NOT NULL,
    file_type       VARCHAR(50),
    file_size       BIGINT,
    original_text   TEXT,
    status          VARCHAR(50) DEFAULT 'pending',  -- pending|processing|done|error
    created_at      TIMESTAMP DEFAULT now()
);

-- 文本块表（向量存储）
CREATE TABLE IF NOT EXISTS chunk_bge_m3 (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    kb_id           UUID NOT NULL REFERENCES knowledge_base(id) ON DELETE CASCADE,
    doc_id          UUID NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    chunk_text      TEXT NOT NULL,
    chunk_index     INT NOT NULL,
    embedding       vector(1024),  -- BGE-M3 维度
    metadata        JSONB,
    created_at      TIMESTAMP DEFAULT now()
);

-- 向量索引（加速检索）
CREATE INDEX IF NOT EXISTS idx_chunk_embedding 
    ON chunk_bge_m3 USING ivfflat (embedding vector_cosine_ops);

-- Agent 与知识库关联
ALTER TABLE agent 
    ADD COLUMN IF NOT EXISTS kb_ids UUID[] DEFAULT '{}';
```

#### 5.1.2 Maven 依赖 (`pom.xml`)

```xml
<!-- Embedding 模型 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-transformers</artifactId>
</dependency>
<!-- 文档解析 -->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
    <version>2.9.2</version>
</dependency>
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-parsers-standard-package</artifactId>
    <version>2.9.2</version>
</dependency>
<!-- Markdown 渲染 -->
<dependency>
    <groupId>com.vladsch.flexmark</groupId>
    <artifactId>flexmark-all</artifactId>
    <version>0.64.8</version>
</dependency>
```

#### 5.1.3 后端类结构

```
agent/tools/
├── KnowledgeTools.java        # Agent 可调用的知识库检索工具
service/
├── KnowledgeBaseFacadeService.java
├── DocumentFacadeService.java
├── RagService.java             # RAG 核心（分块+向量化+检索）
└── impl/
    ├── KnowledgeBaseFacadeServiceImpl.java
    ├── DocumentFacadeServiceImpl.java
    └── RagServiceImpl.java
controller/
├── KnowledgeBaseController.java
└── DocumentController.java
converter/
├── KnowledgeBaseConverter.java
└── DocumentConverter.java
mapper/
├── KnowledgeBaseMapper.java
├── DocumentMapper.java
└── ChunkBgeM3Mapper.java
model/entity/
├── KnowledgeBase.java
├── Document.java
└── ChunkBgeM3.java
model/dto/
├── KnowledgeBaseDTO.java
├── DocumentDTO.java
└── ChunkBgeM3DTO.java
model/request/
├── CreateKnowledgeBaseRequest.java
├── CreateDocumentRequest.java
└── SearchChunksRequest.java
model/response/
├── CreateKnowledgeBaseResponse.java
├── GetKnowledgeBasesResponse.java
└── SearchChunksResponse.java
model/vo/
├── KnowledgeBaseVO.java
└── DocumentVO.java
```

#### 5.1.4 核心代码 — KnowledgeTools

```java
package com.kama.mindharness.agent.tools;

import com.kama.mindharness.service.RagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class KnowledgeTools implements Tool {

    private final RagService ragService;
    private final List<UUID> kbIds;  // 当前 Agent 关联的知识库

    public KnowledgeTools(RagService ragService) {
        this.ragService = ragService;
        this.kbIds = List.of();
    }

    public void setKbIds(List<UUID> kbIds) {
        // 通过反射或 setter 注入当前 Agent 的知识库 ID
    }

    @Override
    public String getName() {
        return "knowledgeTool";
    }

    @Override
    public String getDescription() {
        return "从知识库中检索相关文档内容。参数：query(查询字符串), topK(返回条数,默认3)";
    }

    // Spring AI ToolCallingManager 会根据方法签名自动生成 ToolCallback
    public String search(String query, int topK) {
        if (kbIds.isEmpty()) return "未关联任何知识库";
        List<Map<String, Object>> results = ragService.search(
            kbIds.stream().map(UUID::toString).toList(), query, topK);
        if (results.isEmpty()) return "未找到相关内容";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            sb.append("【").append(i + 1).append("】")
              .append(results.get(i).get("chunk_text")).append("\n\n");
        }
        return sb.toString();
    }

    @Override
    public ToolType getType() {
        return ToolType.RETRIEVAL;
    }
}
```

#### 5.1.5 核心代码 — RagService

```java
package com.kama.mindharness.service;

import java.util.List;
import java.util.Map;

public interface RagService {
    /** 处理文档：分块 → 向量化 → 存入 pgvector */
    void processDocument(String kbId, String docId, String text);

    /** 向量检索 */
    List<Map<String, Object>> search(List<String> kbIds, String query, int topK);

    /** 计算 Embedding */
    float[] embed(String text);
}
```

```java
package com.kama.mindharness.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagServiceImpl implements RagService {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingModel embeddingModel;  // Spring AI 自动注入

    @Override
    @Transactional
    public void processDocument(String kbId, String docId, String text) {
        // 1. 分块 (每块 ~500 字符，重叠 50 字符)
        List<String> chunks = splitText(text, 500, 50);

        // 2. 批量向量化
        List<float[]> embeddings = embeddingModel.embed(
            chunks.stream().map(c -> (org.springframework.ai.document.Document)
                new org.springframework.ai.document.Document(c)).toList()
        ).stream().map(e -> (float[]) e.getEmbedding()).toList();

        // 3. 批量写入 pgvector
        String sql = "INSERT INTO chunk_bge_m3 (kb_id, doc_id, chunk_text, chunk_index, embedding) "
                   + "VALUES (?, ?, ?, ?, ?::vector)";
        for (int i = 0; i < chunks.size(); i++) {
            jdbcTemplate.update(sql, kbId, docId, chunks.get(i), i,
                vectorToString(embeddings.get(i)));
        }
    }

    @Override
    public List<Map<String, Object>> search(List<String> kbIds, String query, int topK) {
        // 1. 向量化查询
        float[] queryEmbedding = embed(query);

        // 2. 余弦相似度检索
        String sql = """
            SELECT chunk_text, doc_id,
                   1 - (embedding <=> ?::vector) AS similarity
            FROM chunk_bge_m3
            WHERE kb_id = ANY(?)
            ORDER BY embedding <=> ?::vector
            LIMIT ?
            """;
        return jdbcTemplate.queryForList(sql,
            vectorToString(queryEmbedding),
            kbIds.toArray(new String[0]),
            vectorToString(queryEmbedding),
            topK);
    }

    @Override
    public float[] embed(String text) {
        // Spring AI embedding 调用
        float[] emb = embeddingModel.embed(text);
        return emb;  // BGE-M3: 1024维
    }

    private List<String> splitText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start += (chunkSize - overlap);
        }
        return chunks;
    }

    private String vectorToString(float[] vec) {
        return "[" + String.join(",", Arrays.stream(vec)
            .mapToObj(String::valueOf).toArray(String[]::new)) + "]";
    }
}
```

#### 5.1.6 MindHarnessFactory 集成

```java
// 在 build() 方法中注入知识库 ID
public MindHarness build() {
    // ... 现有代码 ...
    
    // 获取 Agent 关联的知识库
    if (agentConfig.getAllowedKbs() != null && !agentConfig.getAllowedKbs().isEmpty()) {
        KnowledgeTools kt = toolFacadeService.getKnowledgeTools();
        kt.setKbIds(agentConfig.getAllowedKbs());
    }
    
    // ... 现有代码 ...
}
```

#### 5.1.7 MindHarness.think() 提示词增强

```java
// think() 方法中已有 availableKbs，将其注入 System Prompt
private String buildSystemPrompt() {
    StringBuilder prompt = new StringBuilder(systemPrompt);
    prompt.append("\n\n## 可用知识库\n");
    prompt.append("你可以使用 knowledgeTool 检索以下知识库中的内容：\n");
    // 从 Agent 配置中读取 kb_ids，查询名称后列出
    return prompt.toString();
}
```

#### 5.1.8 application.yaml 配置

```yaml
spring:
  ai:
    embedding:
      # 本地 BGE-M3 (通过 ONNX Runtime)
      bge-m3:
        enabled: true
        model-path: classpath:models/bge-m3.onnx
      # 或者使用 OpenAI 兼容的 Embedding API
      openai:
        api-key: ${EMBEDDING_API_KEY:}
        base-url: https://api.siliconflow.cn/v1
        options:
          model: BAAI/bge-m3
    vectorstore:
      pgvector:
        initialize-schema: false  # 已手动管理
```

#### 5.1.9 前端组件恢复

```
src/components/
├── tabs/
│   └── KnowledgeBaseTabContent.vue    # 知识库列表（新建/编辑/删除）
├── modals/
│   └── AddKnowledgeBaseModal.vue      # 知识库创建/编辑弹窗
│   └── UploadDocumentModal.vue        # 文档上传弹窗
└── views/
    └── KnowledgeBaseView.vue          # 知识库详情（文档列表+搜索测试）

src/composables/
├── useKnowledgeBases.ts               # 知识库 CRUD hook
└── useDocuments.ts                    # 文档上传/删除 hook

src/api/api.ts  # 追加 API 函数
src/types/index.ts  # 追加类型定义
src/router/index.ts  # 追加路由
```

#### 5.1.10 AddAgentModal 补充

```vue
<!-- 在 AddAgentModal.vue 的 Tab 中增加"知识库"面板 -->
<a-tab-pane key="knowledge" tab="知识库">
  <a-checkbox-group v-model:value="selectedKbIds">
    <a-checkbox v-for="kb in knowledgeBases" :key="kb.id" :value="kb.id">
      {{ kb.name }} — {{ kb.description }}
    </a-checkbox>
  </a-checkbox-group>
</a-tab-pane>
```

---

### 5.2 文档管理系统 ⭐⭐

**目标**：支持上传 PDF/Word/Markdown/TXT 文档到知识库，自动解析和向量化。

**技术栈**：Apache Tika 2.x + Spring MultipartFile

#### 5.2.1 后端 — DocumentController

```java
@RestController
@RequestMapping("/api/knowledge-bases/{kbId}/documents")
public class DocumentController {

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreateDocumentResponse> upload(
            @PathVariable String kbId,
            @RequestParam("file") MultipartFile file) {
        // 1. 保存文件到 uploads/documents/{kbId}/
        // 2. 创建 Document 实体 (status=pending)
        // 3. 异步调用 RagService.processDocument()
        // 4. 返回 documentId
    }

    @GetMapping
    public ResponseEntity<List<DocumentVO>> list(@PathVariable String kbId) { ... }

    @DeleteMapping("/{docId}")
    public ResponseEntity<Void> delete(@PathVariable String kbId,
                                       @PathVariable String docId) { ... }
}
```

#### 5.2.2 文本解析

```java
// 使用 Apache Tika 提取文本
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;

public String parseDocument(byte[] fileBytes, String fileName) {
    try (InputStream is = new ByteArrayInputStream(fileBytes)) {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);  // 无大小限制
        Metadata metadata = new Metadata();
        metadata.set(Metadata.RESOURCE_NAME_KEY, fileName);
        parser.parse(is, handler, metadata);
        return handler.toString();
    }
}
```

#### 5.2.3 application.yaml 补充

```yaml
document:
  storage:
    path: uploads/documents     # 文档存储路径
  max-size: 50MB                # 单文件大小限制
  supported-types:
    - application/pdf
    - application/msword
    - application/vnd.openxmlformats-officedocument.wordprocessingml.document
    - text/plain
    - text/markdown
    - text/html
```

---

### 5.3 邮件工具 ⭐

**目标**：让 Agent 能够发送邮件通知（如日报总结、告警通知）。

#### 5.3.1 Maven 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

#### 5.3.2 application.yaml 配置

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

#### 5.3.3 EmailTools 实现

```java
package com.kama.mindharness.agent.tools;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailTools implements Tool {

    private final JavaMailSender mailSender;

    @Override public String getName() { return "emailTool"; }
    @Override public String getDescription() { 
        return "发送邮件。参数：to(收件人), subject(主题), body(正文)"; 
    }
    @Override public ToolType getType() { return ToolType.ACTION; }

    public String sendEmail(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
        return "邮件已发送至 " + to;
    }
}
```

---

### 5.4 文件系统工具 ⭐

**目标**：Agent 可读写项目文件（代码生成、配置修改）。

```java
@Component
public class FileSystemTools implements Tool {

    @Override public String getName() { return "fileSystemTool"; }

    @Override public String getDescription() {
        return """
        文件系统操作工具，支持以下操作：
        - read: 读取文件内容，参数 path(文件路径)
        - write: 写入文件内容，参数 path(文件路径), content(内容)
        - list: 列出目录内容，参数 path(目录路径)
        """;
    }

    public String readFile(String path) {
        String fullPath = Paths.get(workspaceRoot, path).toString();
        return Files.readString(Path.of(fullPath));
    }

    public String writeFile(String path, String content) {
        String fullPath = Paths.get(workspaceRoot, path).toString();
        Files.createDirectories(Path.of(fullPath).getParent());
        Files.writeString(Path.of(fullPath), content);
        return "文件已写入：" + path;
    }

    public String listDirectory(String path) {
        String fullPath = Paths.get(workspaceRoot, path).toString();
        return Files.list(Path.of(fullPath))
            .map(p -> p.getFileName().toString())
            .collect(Collectors.joining("\n"));
    }

    @Override public ToolType getType() { return ToolType.FILE; }
}
```

---

### 5.5 ChatMode 后端集成 ⭐⭐

**当前状态**：前端已有 Ask / Agent / Plan 模式 UI，但未传递到后端。

#### 5.5.1 后端改动

```java
// CreateChatMessageRequest.java 增加字段
public class CreateChatMessageRequest {
    // ... 现有字段 ...
    private String chatMode;  // "ask" | "agent" | "plan"
}
```

```java
// MindHarness.think() 根据模式调整行为
public void think() {
    switch (chatMode) {
        case "ask" -> {
            // 不加载工具，纯问答模式
            chatClient = chatClient.mutate().tools(List.of()).build();
            doChat();
        }
        case "agent" -> {
            // 默认 Agent 模式，加载所有允许的工具
            doChat();
        }
        case "plan" -> {
            // 先让 LLM 生成计划，确认后再执行
            String plan = generatePlan(userMessage);
            sseService.sendPlanMessage(plan);
            // 等待用户确认后再执行...
            doChat();
        }
    }
}
```

#### 5.5.2 ChatMessage 表增强

```sql
ALTER TABLE chat_message ADD COLUMN IF NOT EXISTS chat_mode VARCHAR(20) DEFAULT 'agent';
```

#### 5.5.3 stateDiagram 状态机

```java
public enum AgentState {
    IDLE,       // 空闲
    PLANNING,   // 规划中 (Plan 模式)
    PLAN_READY, // 计划已生成，等待确认
    EXECUTING,  // Agent 执行中
    DONE        // 完成
}
```

---

### 5.6 多轮对话记忆优化 ⭐⭐

#### 5.6.1 摘要记忆 (Summary Memory)

```java
// 当对话超过 20 轮时，生成摘要替代旧消息
public class SummaryMemoryAugmenter {
    
    private final ChatModel summaryModel;
    
    public String summarize(List<Message> oldMessages) {
        String prompt = "请总结以下对话的关键信息：\n" 
            + oldMessages.stream().map(Message::getText).collect(Collectors.joining("\n"));
        return summaryModel.call(prompt);
    }
}
```

#### 5.6.2 向量记忆 (Vector Memory)

```java
// 将历史消息向量化存储，检索相关记忆
// 复用 RagService 的 pgvector 基础设施
public class VectorMemoryAugmenter {
    // 每次对话结束时，将关键信息向量化存入 chunk_bge_m3
    // 新对话开始时，检索最相关的历史记忆注入 System Prompt
}
```

---

## 六、完整技术架构快照（含扩展模块）

```
┌──────────────────────────────────────────────────────────┐
│                    前端 (Vue 3 + Vite)                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────────┐ │
│  │AgentChat │ │ ChatHist │ │ChatInput │ │ AddAgentModal│ │
│  │  View    │ │   ory    │ │ (w/Mode) │ │ (w/KB panel) │ │
│  └──────────┘ └──────────┘ └──────────┘ └─────────────┘ │
│  ┌──────────┐ ┌──────────────┐ ┌──────────────────────┐  │
│  │ AgentTab │ │KnowledgeBase │ │KnowledgeBaseTab      │  │
│  │          │ │    View      │ │Content + UploadModal │  │
│  └──────────┘ └──────────────┘ └──────────────────────┘  │
│              API Layer (Fetch + SSE + i18n)               │
├──────────────────────────────────────────────────────────┤
│                 后端 (Spring Boot 3.5)                     │
│  ┌─────────────────── Controller ────────────────────┐   │
│  │ Agent ChatMessage ChatSession KB Document Sse     │   │
│  └───────────────────────────────────────────────────┘   │
│  ┌────────────────── FacadeService ──────────────────┐   │
│  │ Agent ChatMessage ChatSession KB Document Tool    │   │
│  └───────────────────────────────────────────────────┘   │
│  ┌─────────────── Agent Loop Engine ─────────────────┐   │
│  │  MindHarness ──→ ToolCallingManager                 │   │
│  │     │           ┌──────────────────────────┐      │   │
│  │     ├──────────→│ DataBaseTools (SQL查询)   │      │   │
│  │     ├──────────→│ KnowledgeTools (RAG检索)  │      │   │
│  │     ├──────────→│ LspTool (代码分析)        │      │   │
│  │     ├──────────→│ EmailTools (邮件发送)     │      │   │
│  │     ├──────────→│ FileSystemTools (文件IO)  │      │   │
│  │     ├──────────→│ HttpRequestTool (HTTP)    │      │   │
│  │     └──────────→│ TerminateTool (终止)      │      │   │
│  │                  └──────────────────────────┘      │   │
│  └────────────────────────────────────────────────────┘   │
│  ┌──────────────── Core Services ────────────────────┐   │
│  │  RagService      ← EmbeddingModel + pgvector      │   │
│  │  SseService      ← EventSource 实时推送            │   │
│  │  ChatMemory      ← MessageWindow + SummaryMemory  │   │
│  │  DynamicChatClientFactory ← DeepSeek/GLM 多模型    │   │
│  └───────────────────────────────────────────────────┘   │
│  ┌──────────────── MyBatis Mapper ───────────────────┐   │
│  │  Agent ChatMessage ChatSession KB Document Chunk  │   │
│  └───────────────────────────────────────────────────┘   │
├──────────────────────────────────────────────────────────┤
│              基础设施 (Docker Compose)                     │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐  │
│  │PostgreSQL│ │  Nginx   │ │  Spring  │ │  Loki +    │  │
│  │(pgvector)│ │ (SPA+API)│ │   Boot   │ │  Grafana   │  │
│  │  :15432  │ │  :15173  │ │  :8080   │ │  :3000     │  │
│  └──────────┘ └──────────┘ └──────────┘ └────────────┘  │
├──────────────────────────────────────────────────────────┤
│                 DevOps (GitHub Actions)                    │
│   Build → Test → Lint → Docker Build → Deploy            │
└──────────────────────────────────────────────────────────┘
```

### 数据模型

| 表 | 用途 | 关键字段 |
|---|---|---|
| `agent` | 智能体配置 | id, name, system_prompt, model, api_key, allowed_tools(JSONB), chat_options(JSONB), kb_ids(UUID[]), avatar |
| `chat_session` | 聊天会话 | id, agent_id, title, metadata(JSONB) |
| `chat_message` | 聊天消息 | id, session_id, role, content, chat_mode, metadata(JSONB) |
| `knowledge_base` | 知识库 | id, name, description, embedding_model |
| `document` | 文档 | id, kb_id, file_name, file_type, original_text, status |
| `chunk_bge_m3` | 文本块向量 | id, kb_id, doc_id, chunk_text, embedding(vector 1024) |

### 开发路线图

```
M1 ✅ 基础聊天 ──→ M2 ✅ Agent Loop ──→ M3 ⬜ 第一步 RAG 知识库
  (已完成)           (已完成)               (详见第五章第一步)

M4 ⬜ 第二步 工具生态 ──→ M5 ⬜ 第三步 ChatMode集成 ──→ M6 ⬜ 第五步 DevOps
  (详见第五章第二步)        (详见第五章第三步)            (详见第五章第五步)
```

---

## 附：临时想法 & 备忘录 💡

> 随手记录，不定期整理。想到什么写什么，格式不限。

### 灵感

- [ ] 

### 待调研

- [ ] 

### 问题 & 备忘

- [ ] 

### 碎碎念



