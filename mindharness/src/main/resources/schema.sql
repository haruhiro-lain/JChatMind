-- MindHarness 数据库初始化脚本
-- PostgreSQL + pgvector
-- 注意：uuid-ossp 和 vector 扩展需由 DBA 预先安装

-- 启用 uuid 扩展（需超管权限，通常已预装）
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- CREATE EXTENSION IF NOT EXISTS vector;

-- ==================== agent 表 ====================
CREATE TABLE IF NOT EXISTS agent (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    system_prompt   TEXT,
    model           VARCHAR(100) NOT NULL,
    allowed_tools   JSONB,
    chat_options    JSONB,
    api_key         VARCHAR(512),
    avatar          VARCHAR(255),
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

-- ==================== chat_session 表 ====================
CREATE TABLE IF NOT EXISTS chat_session (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    agent_id        UUID NOT NULL REFERENCES agent(id) ON DELETE CASCADE,
    title           VARCHAR(255),
    metadata        JSONB,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

-- ==================== chat_message 表 ====================
CREATE TABLE IF NOT EXISTS chat_message (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    session_id      UUID NOT NULL REFERENCES chat_session(id) ON DELETE CASCADE,
    role            VARCHAR(50) NOT NULL,
    content         TEXT,
    metadata        JSONB,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_chat_session_agent_id ON chat_session(agent_id);
CREATE INDEX IF NOT EXISTS idx_chat_session_updated_at ON chat_session(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_message_session_id ON chat_message(session_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_created_at ON chat_message(created_at ASC);
