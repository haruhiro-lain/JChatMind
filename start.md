# JChatMind 项目启动指南

## 项目概述

JChatMind 是一个 AI Agent 聊天应用，采用前后端分离架构：

| 模块 | 技术栈 | 端口 |
|------|--------|------|
| 后端 (`jchatmind/`) | Spring Boot 3.5.8 + Java 17 + MyBatis + PostgreSQL | `8080` |
| 前端 (`ui/`) | React 19 + TypeScript + Vite + Ant Design + Tailwind CSS | `15173` |

## 前置依赖

- **JDK 17+**
- **Maven**（项目自带 Maven Wrapper，无需额外安装）
- **Node.js**（推荐 18+）
- **PostgreSQL**（需运行在 `localhost:15432`）

### 数据库配置

确保 PostgreSQL 已启动，数据库 `jchatmind` 已创建。默认连接信息：

```
Host: localhost
Port: 15432
Database: jchatmind
Username: jchatmind
Password: jchatmind123
```

> 初始化 SQL 脚本见 `TEMP/init.sql`

### AI API 配置

在 `jchatmind/src/main/resources/application.yaml` 中配置：

```yaml
spring:
  ai:
    deepseek:
      api-key: your-deepseek-api-key
    zhipuai:
      api-key: your-zhipuai-api-key
```

## 启动方式

### 方式一：分别启动（开发推荐）

**1. 启动后端**

```powershell
cd jchatmind
.\mvnw.cmd spring-boot:run -DskipTests
```

后端启动后访问：`http://localhost:8080`

**2. 启动前端**

```powershell
cd ui
npm install    # 首次运行需安装依赖
npm run dev
```

前端启动后访问：`http://127.0.0.1:15173/`

### 方式二：IDE 一键启动

在 IntelliJ IDEA / VS Code 中直接运行：
- 后端入口类：`com.kama.jchatmind.JchatmindApplication`
- 前端：终端执行 `npm run dev`

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

## 项目结构

```
JChatMind/
├── jchatmind/              # Spring Boot 后端
│   ├── src/main/java/      # Java 源码
│   ├── src/main/resources/ # 配置文件 & MyBatis XML
│   └── pom.xml             # Maven 依赖
├── ui/                     # React 前端
│   ├── src/                # TypeScript 源码
│   │   ├── api/            # API 接口层
│   │   ├── components/     # UI 组件
│   │   ├── contexts/       # React Context
│   │   ├── hooks/          # 自定义 Hooks
│   │   └── types/          # 类型定义
│   └── package.json        # Node 依赖
├── examples/               # HTML 示例页面
├── TEMP/                   # 临时文件（SQL、文档等）
└── start.md                # 本文件
```

## 常见问题

### 后端启动报数据库连接失败

检查 PostgreSQL 是否运行在 `localhost:15432`，数据库 `jchatmind` 是否已创建。

### 前端启动报端口占用

修改 `ui/vite.config.ts` 中的 `server.port` 配置。

### 前端请求后端接口跨域

开发环境下 Vite 已配置代理，接口请求会自动转发到后端 `8080` 端口。
