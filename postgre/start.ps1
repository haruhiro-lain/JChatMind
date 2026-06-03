# MindHarness 数据库启动脚本（台式机用）
# 仅启动 PostgreSQL 容器，供远程开发机连接

$ErrorActionPreference = "Stop"
$Host.UI.RawUI.WindowTitle = "MindHarness 数据库"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  MindHarness PostgreSQL 数据库" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# 检查 Docker
try {
    docker info 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[错误] Docker Desktop 未运行，请先启动 Docker" -ForegroundColor Red
        Read-Host "按 Enter 退出"
        exit 1
    }
    Write-Host "[Docker] 已就绪" -ForegroundColor Green
} catch {
    Write-Host "[错误] 未检测到 Docker" -ForegroundColor Red
    Read-Host "按 Enter 退出"
    exit 1
}

Write-Host ""
Write-Host "操作选项:" -ForegroundColor Yellow
Write-Host "  1. 启动数据库" -ForegroundColor White
Write-Host "  2. 停止数据库" -ForegroundColor White
Write-Host "  3. 查看日志" -ForegroundColor White
Write-Host "  4. 进入 psql" -ForegroundColor White
Write-Host ""
$choice = Read-Host "请选择 [1-4]"

Push-Location $scriptDir
try {
    switch ($choice) {
        "1" {
            Write-Host ""
            Write-Host "▶ 启动 PostgreSQL..." -ForegroundColor Cyan
            $rebuild = Read-Host "是否重新构建镜像并启动？ (y/n)"
            if ($rebuild -match '^[Yy]') {
                docker compose up -d --build
            } else {
                docker compose up -d
            }
            Write-Host ""
            Write-Host "✓ 数据库已启动" -ForegroundColor Green
            Write-Host "  地址: localhost:15432" -ForegroundColor White
            Write-Host "  数据库: mindharness  用户: mindharness  密码: mindharness123" -ForegroundColor DarkGray
        }
        "2" {
            Write-Host ""
            Write-Host "▶ 停止 PostgreSQL..." -ForegroundColor Cyan
            docker compose down
            Write-Host "✓ 数据库已停止" -ForegroundColor Green
        }
        "3" {
            Write-Host ""
            Write-Host "▶ 查看日志（Ctrl+C 退出）..." -ForegroundColor Cyan
            docker compose logs -f
        }
        "4" {
            Write-Host ""
            Write-Host "▶ 进入 psql..." -ForegroundColor Cyan
            docker exec -it mindharness-postgres psql -U mindharness -d mindharness
        }
        default {
            Write-Host "无效选项" -ForegroundColor Red
        }
    }
} finally {
    Pop-Location
}

Write-Host ""
Read-Host "按 Enter 退出"
