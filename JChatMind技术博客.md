# 从调 API 到真 Agent：手写一个面试级 AI 智能体聊天平台

> 一个能自主决策、调用工具、实时推送的 Java AI Agent 系统是如何从零搭建的？  
> 本文基于 **Spring AI + React** 技术栈，带你深入 Agent Loop 核心引擎的设计与实现。

---

## 一、为什么要做这个项目？

面试官看到简历上写着「接入大模型、实现聊天」，往往一句话就能让你哑口无言：

> **"你到底做了什么？不就是调 API 吗？"**

一个聊天对话框和一个真正意义上的 **AI Agent**，差的不只是代码量，而是整个系统的设计思路。做完这个项目后，你能讲清楚的不再是「我接了一个接口」，而是：

- 我实现了 **Think-Execute 循环**（自主决策引擎）
- 我设计了 **可扩展工具调用框架**（固定工具 + 可选工具）
- 我构建了 **多模型注册表模式**（DeepSeek / 智谱动态切换）
- 我实现了 **SSE 实时推送**（Agent 状态可视化）
- 我完成了 **事件驱动的异步执行**（不阻塞 HTTP 线程）

---

## 二、架构总览

```
┌──────────────────────────────────────────────────┐
│              前端 React + Vite + Ant Design        │
│            SSE EventSource 接收实时推送             │
└────────────────────┬─────────────────────────────┘
                     │ REST + SSE
┌────────────────────▼─────────────────────────────┐
│           后端 Spring Boot 3.5 + Spring AI 1.1     │
│                                                    │
│  Controller  →  FacadeService  →  MyBatis Mapper   │
│       ↓                              ↓             │
│  ┌─────────────────────┐     ┌──────────────┐     │
│  │   Agent 核心引擎      │────▶│ PostgreSQL 16 │     │
│  │                      │     │ + pgvector    │     │
│  │  JChatMind           │     └──────────────┘     │
│  │  ├─ think()  决策     │                         │
│  │  ├─ execute() 执行    │     ┌──────────────┐     │
│  │  └─ run()    循环     │     │ DeepSeek API  │     │
│  │                      │     │ (外部 LLM)    │     │
│  │  ChatClientRegistry   │────▶│               │     │
│  │  ToolCallingManager   │     └──────────────┘     │
│  └─────────────────────┘                           │
└────────────────────────────────────────────────────┘
```

项目采用经典的四层架构：**Controller → FacadeService → Mapper → Database**。核心是中间的 **Agent 引擎模块**，它独立于 CRUD 层，通过 Spring 事件驱动异步运行。

---

## 三、核心设计一：Agent Loop（Think-Execute 循环）

这不是「调用一次大模型就结束」的简单流程，而是一个完整的 **ReAct（Reasoning + Acting）模式**：

```java
// JChatMind.java — 核心循环
public void run() {
    for (int i = 0; i < MAX_STEPS && agentState != FINISHED; i++) {
        step();  // 单步：think → execute
    }
}

private void step() {
    if (think()) {    // LLM 决策：直接回答 or 调用工具？
        execute();    // 执行工具，结果注入上下文
    } else {
        agentState = FINISHED;  // 无工具调用 = 任务结束
    }
}
```

### 状态机设计

```
IDLE → THINKING → EXECUTING → FINISHED
                    ↑              │
                    └──── 循环 ────┘
                    最多 20 步
```

关键约束：
- **最大步数控制**（`MAX_STEPS = 20`）防止无限循环
- **TerminateTool** 作为固定工具，大模型可主动终止任务
- 每次 `think()` 前注入「你拥有的工具列表」提示词，引导模型决策

### Think 阶段的提示词设计

```java
private boolean think() {
    String thinkPrompt = """
        现在你是一个智能的具体「决策模块」
        请根据当前对话上下文，决定下一步的动作。
        如果有可用的工具，优先考虑调用工具来完成任务。
        """;
    
    this.lastChatResponse = chatClient
        .prompt(prompt)
        .system(thinkPrompt)
        .toolCallbacks(availableTools.toArray(...))
        .call()
        .chatResponse();
    
    // 如果 LLM 返回了工具调用 → 进入 execute 阶段
    return !toolCalls.isEmpty();
}
```

这里有一个巧妙的设计：**通过 `.system(thinkPrompt)` 注入决策指令，而不是 `.user()`**。这样 think 提示词不会污染聊天历史，每次决策都是「干净」的上下文。

---

## 四、核心设计二：工具调用框架

很多人做工具调用只是「写几个 if-else」。JChatMind 的工具系统是**框架化**的：

### 工具分类

```java
public enum ToolType {
    FIXED,     // 固定工具 — 所有 Agent 强制拥有（如 TerminateTool）
    OPTIONAL   // 可选工具 — 按 Agent 配置自由选择（如 DataBaseTools）
}
```

### 工具注册与调度

```java
// JChatMindFactory.java — 运行时工具解析
private List<Tool> resolveRuntimeTools(AgentDTO agentConfig) {
    List<Tool> runtimeTools = new ArrayList<>(getFixedTools());   // 固定工具
    
    Map<String, Tool> optionals = getOptionalTools();             // 可选工具
    for (String toolName : agentConfig.getAllowedTools()) {
        Tool tool = optionals.get(toolName);
        if (tool != null) runtimeTools.add(tool);
    }
    return runtimeTools;
}
```

**关键决策：手动接管 Spring AI 的自动工具执行**

```java
this.chatOptions = DefaultToolCallingChatOptions.builder()
    .internalToolExecutionEnabled(false)  // 关闭自动执行
    .build();
```

为什么要关闭自动执行？因为我们需要**完整控制**工具调用的结果处理流程——包括：
- 将工具返回结果注入 ChatMemory
- 通过 SSE 实时推送工具调用状态到前端
- 持久化工具调用记录到数据库

### DataBaseTools 示例

```java
@Component
public class DataBaseTools implements Tool {
    @Tool(name = "databaseQuery", 
          description = "在 PostgreSQL 中执行只读查询（SELECT）")
    public String query(String sql) {
        // 安全检查：仅允许 SELECT
        if (!trimmedSql.startsWith("SELECT")) {
            return "错误：仅支持 SELECT 查询";
        }
        return jdbcTemplate.queryForList(sql).toString();
    }
}
```

Agent 在运行时可以**自主判断**需要查询数据库，**自主构造** SQL 语句，然后**自主解读**查询结果。整个过程无需人工干预。

---

## 五、核心设计三：多模型注册表

项目不是「绑定一个模型」，而是用**注册表模式**实现解耦：

```java
@Component
public class ChatClientRegistry {
    private final Map<String, ChatClient> chatClients;
    
    public ChatClient get(String key) {
        return chatClients.get(key);  // "deepseek-chat" 或 "glm-4.6"
    }
}
```

模型实例通过 Spring Bean 管理：

```java
@Bean("deepseek-chat")
public ChatClient deepSeekChatClient(DeepSeekChatModel model) {
    return ChatClient.create(model);
}

@Bean("glm-4.6")
public ChatClient zhiPuAiChatClient(ZhiPuAiChatModel model) {
    return ChatClient.create(model);
}
```

**添加新模型的成本**：只需新增一个 `@Bean` + 添加对应依赖，不碰核心 Agent 代码。面试时可以讲：「用注册表模式实现模型热插拔，新增模型零侵入」。

---

## 六、核心设计四：SSE 实时推送

Agent 执行过程对用户来说像一个**黑盒**——你不知道它在思考还是卡住了。

我们的解决方案是 **SSE（Server-Sent Events）**：

```
Agent 执行中:
  THINKING   → 前端显示 "思考中..."
  EXECUTING  → 前端显示 "正在调用 databaseQuery..."
  DONE       → 前端显示 "完成"
  AI_GENERATED_CONTENT → 前端追加 AI 回复内容
```

```java
// SseServiceImpl.java
public void send(String chatSessionId, SseMessage message) {
    SseEmitter emitter = clients.get(chatSessionId);
    if (emitter != null) {
        emitter.send(SseEmitter.event().data(message));
    }
}
```

**为什么选 SSE 而不是 WebSocket？**
- SSE 是 HTTP 标准，浏览器原生支持 `EventSource` API
- Agent 场景是**服务端单向推送**，不需要客户端频繁回传
- 实现简单，连接管理成本低

前端侧：

```typescript
const es = new EventSource(`${sseBaseUrl}/sse/connect/${chatSessionId}`);
es.addEventListener("message", (event) => {
    const message = JSON.parse(event.data);
    if (message.type === "AI_GENERATED_CONTENT") {
        addMessage(message.payload.message);  // 实时追加 AI 回复
    }
});
```

---

## 七、核心设计五：事件驱动的异步执行

用户发送消息后，如果同步等待 Agent 执行完再返回，HTTP 线程会被阻塞数秒。

我们的设计：

```java
// ChatEventListener.java
@Async
@EventListener
public void handle(ChatEvent event) {
    JChatMind agent = factory.create(event.getAgentId(), event.getSessionId());
    agent.run();  // 在异步线程中执行 Agent Loop
}
```

流程：

```
用户 POST /api/chat-messages
  → ChatMessageFacadeService 保存用户消息
  → 发布 ChatEvent
  → Controller 立即返回 200（HTTP 线程释放）
  
异步线程:
  → ChatEventListener 接收事件
  → JChatMindFactory.create() 构建 Agent 实例
  → agent.run() 执行 Think-Execute 循环
  → 每一步通过 SSE 推送结果到前端
```

---

## 八、项目结构速览

```
jchatmind/src/main/java/com/kama/jchatmind/
├── agent/              # 🔥 核心引擎
│   ├── JChatMind.java          # Agent Loop 主循环
│   ├── JChatMindFactory.java   # Agent 工厂 + 工具解析
│   ├── AgentState.java         # 状态枚举
│   └── tools/                 # 工具集（TerminateTool/DataBaseTools）
├── config/             # 多模型/CORS/异步配置
├── controller/         # REST API 层
├── service/            # 业务逻辑层（Facade 模式）
├── mapper/             # MyBatis 数据访问
├── model/              # Entity/DTO/VO/Request/Response
├── event/              # Spring 事件驱动（ChatEvent + Listener）
├── exception/          # 全局异常处理
└── message/            # SSE 消息模型
```

---

## 九、面试怎么说？

### 自我介绍模板

> "我做过一个 AI Agent 智能体项目，核心技术栈是 Spring AI + React。我负责设计了 Agent 的核心引擎，实现了 Think-Execute 循环的自主决策机制，以及可扩展的工具调用框架。系统支持 DeepSeek 和智谱多模型动态切换，通过 SSE 实现 Agent 执行状态的实时推送。后端采用事件驱动架构，Agent 在异步线程中执行，不阻塞 HTTP 响应。"

### 常见追问准备

**Q: Agent Loop 如何防止无限循环？**
> 设置了最大步数上限（20 步），同时提供 TerminateTool 让大模型可以主动终止任务。每次循环检查 agentState，当状态变为 FINISHED 或超过最大步数时自动退出。

**Q: 工具调用怎么做扩展？**
> 工具实现统一的 Tool 接口，分为固定工具（FIXED）和可选工具（OPTIONAL）。新增工具只需实现接口 + @Component 注解，Agent 配置中选择即可，核心引擎无需修改。

**Q: 为什么关闭 Spring AI 的自动工具执行？**
> 因为需要手动控制工具结果的处理流程——包括结果注入 ChatMemory、SSE 推送到前端、持久化到数据库。自动执行会跳过这些关键步骤。

**Q: SSE 和 WebSocket 的区别？**
> SSE 基于 HTTP，单向推送，实现简单；WebSocket 是全双工协议。Agent 场景中服务端主动推送状态，客户端只需接收，SSE 更合适。

---

## 十、快速启动

```bash
# 1. 启动 PostgreSQL
cd TEMP/docker/jchatmind && docker compose up -d postgres

# 2. 建表（在 psql 中执行）
# agent / chat_session / chat_message 三张表

# 3. 配置 DeepSeek API Key
# 编辑 application.yaml → spring.ai.deepseek.api-key

# 4. 启动后端（JDK 17）
cd jchatmind && mvnw.cmd spring-boot:run

# 5. 启动前端
cd ui && npm install && npm run dev
# → http://127.0.0.1:15173
```

---

## 十一、总结

这个项目最大的价值不在于「调了一个 API」，而在于：

| 层面 | 能力 |
|------|------|
| **架构设计** | 分层解耦、事件驱动、注册表模式 |
| **核心算法** | ReAct Loop、状态机、滑动窗口记忆 |
| **系统工程** | SSE 实时通信、异步执行、异常处理 |
| **可扩展性** | 工具框架、多模型支持、前后端分离 |

当你把这些点讲清楚，面试官不会问「你是不是只调了 API」，而是会追问「你的工具调用怎么做的」「循环怎么终止的」——这些都是你真正理解系统的证明。
