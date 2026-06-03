# JChatMind 开发环境排障记录

## 1. 前端导入角色卡报 NetworkError

**现象**：新建智能体 → 导入角色卡 → `NetworkError when attempting to fetch resource`

**根因**：`ui/src/api/http.ts` 中 `BASE_URL` 硬编码为 `http://127.0.0.1:8080/api`，绕过 Vite 代理直接发跨域请求。`multipart/form-data` 文件上传时 CORS 预检失败。

**解决**：
- `BASE_URL` 改为 `/api`（相对路径），走 Vite 代理（`vite.config.ts` 已配置 `proxy: { '/api': 'http://127.0.0.1:8080' }`）
- FormData 请求不再强制传 `headers: {}`

---

## 2. 导入角色卡报 HTTP 500

**现象**：前端收到 `HTTP error! status: 500`

**根因**：Controller 中 `catch` 块抛出 `BizException` → 依赖 `GlobalExceptionHandler` 拦截 → 但 `@ExceptionHandler` 方法缺少 `@ResponseStatus(HttpStatus.OK)` → Spring Boot 默认错误处理返回 HTTP 500。

**解决**：
- `AgentController.importCard()` 的 `catch` 直接 `return ApiResponse.error(...)`，不再抛异常
- `GlobalExceptionHandler` 所有返回 `ApiResponse` 的方法加 `@ResponseStatus(HttpStatus.OK)`

---

## 3. 后端启动失败 — schema.sql 执行失败

**现象**：Spring Boot 启动日志 `Failed to execute database script`，`HikariPool-1 - Starting...` 后崩溃

**根因**：`schema.sql` 中 `CREATE EXTENSION IF NOT EXISTS vector;` 在远程 PostgreSQL 上执行失败（未安装 pgvector 扩展 / 无超管权限）

**解决**：注释掉 `schema.sql` 中的 `CREATE EXTENSION` 语句，扩展由 DBA 预装

---

## 4. 数据库密码认证失败 (password authentication failed)

**现象**：`PSQLException: FATAL: password authentication failed for user "mindharness"`

密码配置确认正确（`mindharness123`），但认证被拒。

**根因**：`pg_hba.conf` 配置了 `host all all all scram-sha-256`，要求 `scram-sha-256` 认证方式，但数据库中用户的密码哈希是旧的 `md5` 格式（容器重建 / 数据 volume 复用时常出现）。

**诊断步骤**：
```powershell
# 1. 检查 TCP 连通性
Test-NetConnection -ComputerName 100.99.85.73 -Port 15432

# 2. 检查 pg_hba.conf（远程服务器）
docker exec mindharness-postgres cat /var/lib/postgresql/data/pg_hba.conf | Select-String -NotMatch "^#|^$"

# 3. 如果最后一行是 `host all all all scram-sha-256`，说明强制 scram-sha-256
```

**解决**（远程服务器上执行）：
```sql
docker exec -it mindharness-postgres psql -U mindharness -d mindharness
ALTER USER mindharness WITH PASSWORD 'mindharness123';
\q
```
这会用 `scram-sha-256` 重新哈希密码。

---

## 5. dev.ps1 JDBC 编译失败 — BOM 问题

**现象**：`javac` 报 `需要 class、interface、enum 或 record`

**根因**：PowerShell 5.1 的 `Set-Content -Encoding UTF8` 会在文件头写入 BOM（`EF BB BF`），`javac` 无法识别

**解决**：用 `[System.IO.File]::WriteAllText($path, $content, [System.Text.UTF8Encoding]::new($false))` 写无 BOM 的 UTF-8

---

## 快速检查清单

| 检查项 | 命令/位置 |
|---|---|
| Vite 代理是否生效 | `ui/vite.config.ts` → `proxy: { '/api': ... }` |
| 前端 BASE_URL | `ui/src/api/http.ts` → `BASE_URL = "/api"` |
| 后端 profile | `mindharness/src/main/resources/application.yaml` → `spring.profiles.active` |
| 数据库配置 | `application-{profile}.yaml` → `spring.datasource.url` |
| pg_hba.conf | 远程服务器 `docker exec mindharness-postgres cat /var/lib/postgresql/data/pg_hba.conf` |
| 密码认证方式 | `ALTER USER mindharness WITH PASSWORD 'xxx'` 重设密码 |
