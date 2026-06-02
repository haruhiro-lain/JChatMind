# JChatMind 项目启动脚本 (PowerShell)
# 优先使用 Docker Compose 部署；Docker 不可用时回退到本地环境
$ErrorActionPreference = "Stop"
$Host.UI.RawUI.WindowTitle = "JChatMind 启动脚本"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  JChatMind 项目启动脚本" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$composeFile = "$rootDir\docker\jchatmind\docker-compose.yml"

# ========== Docker 检测 ==========
$dockerAvailable = $false
try {
    $dockerVersion = docker --version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[Docker] $dockerVersion" -ForegroundColor Green
        # 检查 Docker 守护进程是否在运行
        docker info 2>$null | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Host ""
            Write-Host "============================================" -ForegroundColor Red
            Write-Host "  Docker Desktop 未运行！" -ForegroundColor Red
            Write-Host "  请先启动 Docker Desktop，然后重新运行此脚本。" -ForegroundColor Yellow
            Write-Host "============================================" -ForegroundColor Red
            Write-Host ""
            Read-Host "按 Enter 退出"
            exit 1
        }
        $dockerAvailable = $true
    }
} catch { }

$composeAvailable = $false
if ($dockerAvailable) {
    try {
        $composeOutput = docker compose version 2>$null
        if ($LASTEXITCODE -eq 0) {
            $composeAvailable = $true
            Write-Host "[Compose] $composeOutput" -ForegroundColor Green
        }
    } catch { }
}

# ====================================================================
#  方案 A：Docker Compose 部署（优先）
# ====================================================================
if ($dockerAvailable -and $composeAvailable -and (Test-Path $composeFile)) {
    Write-Host ""
    Write-Host ">>> 使用 Docker Compose 部署（推荐） <<<" -ForegroundColor Green
    Write-Host ""

    # 询问是否重建镜像
    $rebuild = Read-Host "是否重新构建镜像？[y/N]"
    $doRebuild = ($rebuild -eq 'y' -or $rebuild -eq 'Y')

    Write-Host ""
    Write-Host "[1/3] 启动容器服务..." -ForegroundColor Yellow
    if ($doRebuild) {
        Write-Host "   docker compose -f docker/jchatmind/docker-compose.yml up -d --build"
    } else {
        Write-Host "   docker compose -f docker/jchatmind/docker-compose.yml up -d"
    }
    Write-Host ""

    Push-Location $rootDir
    try {
        # 先停止并清理可能残留的旧容器（避免端口冲突）
        Write-Host "   清理旧容器..." -ForegroundColor DarkGray
        cmd /c "docker compose -f docker/jchatmind/docker-compose.yml down --remove-orphans 2>&1" | Out-Null

        # 捕获 docker compose 输出以便在失败时显示错误
        $composeFileArg = "docker/jchatmind/docker-compose.yml"
        if ($doRebuild) {
            $composeOutput = cmd /c "docker compose -f $composeFileArg up -d --build 2>&1"
        } else {
            $composeOutput = cmd /c "docker compose -f $composeFileArg up -d 2>&1"
        }
        Write-Host $composeOutput

        if ($LASTEXITCODE -ne 0) {
            Write-Host ""
            Write-Host "============================================" -ForegroundColor Red
            Write-Host "  docker compose up 失败！" -ForegroundColor Red
            Write-Host "  常见原因：" -ForegroundColor Yellow
            Write-Host "  1. 端口冲突：检查 15432/8080/15173 是否被占用" -ForegroundColor DarkGray
            Write-Host "  2. 镜像拉取失败：检查 docker.1ms.run 代理是否可达" -ForegroundColor DarkGray
            Write-Host "  3. Maven/npm 构建失败：检查网络和依赖" -ForegroundColor DarkGray
            Write-Host "============================================" -ForegroundColor Red
            throw "docker compose up 失败，请查看上方错误信息"
        }
    } finally {
        Pop-Location
    }

    Write-Host "[2/3] 等待服务就绪..." -ForegroundColor Yellow
    Write-Host "   （PostgreSQL + 后端 + 前端 约需 30-60 秒）"
    $maxWait = 60
    $waited = 0
    while ($waited -lt $maxWait) {
        Start-Sleep -Seconds 5
        $waited += 5
        try {
            $health = docker inspect --format='{{.State.Health.Status}}' jchatmind-postgres 2>$null
            if ($health -eq "healthy") { break }
        } catch { }
    }
    Write-Host "   等待完成（${waited}秒）" -ForegroundColor Green

    Write-Host "[3/3] 打开浏览器..." -ForegroundColor Yellow
    # 检查前端是否已经可访问，避免重复打开浏览器
    try {
        $alreadyRunning = (Test-NetConnection -ComputerName "127.0.0.1" -Port 15173 -WarningAction SilentlyContinue -ErrorAction SilentlyContinue).TcpTestSucceeded
    } catch { $alreadyRunning = $false }
    if ($alreadyRunning) {
        Write-Host "   ⚠ 前端已在运行，跳过打开浏览器" -ForegroundColor Yellow
    } else {
        Start-Process "http://127.0.0.1:15173/"
    }

    Write-Host ""
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "  前端地址: http://127.0.0.1:15173" -ForegroundColor White
    Write-Host "  后端地址: http://127.0.0.1:8080" -ForegroundColor White
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "常用命令:" -ForegroundColor Yellow
    Write-Host "  查看日志:  docker compose -f docker/jchatmind/docker-compose.yml logs -f"
    Write-Host "  停止服务:  docker compose -f docker/jchatmind/docker-compose.yml down"
    Write-Host ""
    Write-Host "  按任意键关闭此窗口（服务不受影响）"
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    exit 0
}

# ====================================================================
#  方案 B：本地环境启动（回退）
# ====================================================================
if ($dockerAvailable) {
    Write-Host ""
    Write-Host "[警告] Docker 可用但 compose 命令或 docker-compose.yml 缺失，回退到本地启动" -ForegroundColor Yellow
} else {
    Write-Host ""
    Write-Host "[提示] 未检测到 Docker，使用本地环境启动" -ForegroundColor Yellow
    Write-Host "       推荐安装 Docker 以获得更稳定的环境（www.docker.com）"
}
Write-Host ""

# ========== 配置 JDK 21 ==========
$jdk21Path = "D:\Environment\Java\jdk21"
if (Test-Path "$jdk21Path\bin\java.exe") {
    $env:JAVA_HOME = $jdk21Path
    $env:Path = "$jdk21Path\bin;$env:Path"
    Write-Host "[JDK] 已切换到 JDK 21: $jdk21Path" -ForegroundColor Green
} else {
    Write-Host "[警告] JDK 21 未在 $jdk21Path 找到，将使用系统默认 Java" -ForegroundColor Yellow
}
Write-Host ""

# ========== 环境检查 ==========
Write-Host "[1/4] 检查环境依赖..." -ForegroundColor Yellow

# 检查 Java
try {
    $javaOutput = cmd /c "java -version 2>&1"
    if ($LASTEXITCODE -ne 0 -or -not $javaOutput) {
        throw "Java 未安装或无法执行"
    }
    $javaVersionLine = ($javaOutput | Select-String "version" | ForEach-Object { $_.ToString() })
    Write-Host "   Java: $javaVersionLine"

    if ($javaVersionLine -match 'version "(\d+)\.') {
        $majorVersion = [int]$Matches[1]
    } elseif ($javaVersionLine -match 'version "1\.(\d+)') {
        $majorVersion = [int]$Matches[1]
    } else {
        $majorVersion = 0
    }

    if ($majorVersion -lt 21) {
        Write-Host "[错误] Java 版本过低（当前: $majorVersion），请安装 JDK 21+" -ForegroundColor Red
        Read-Host "按 Enter 退出"
        exit 1
    }
    Write-Host "   ✓ Java 版本检查通过" -ForegroundColor Green
} catch {
    Write-Host "[错误] 未找到 Java，请安装 JDK 21+" -ForegroundColor Red
    Read-Host "按 Enter 退出"
    exit 1
}

# 检查 Node.js
try {
    $nodeVersion = node -v
    Write-Host "   Node.js: $nodeVersion"
} catch {
    Write-Host "[错误] 未找到 Node.js，请安装 Node.js 18+" -ForegroundColor Red
    Read-Host "按 Enter 退出"
    exit 1
}

Write-Host "   ✓ 环境检查通过" -ForegroundColor Green
Write-Host ""

# ========== 数据库提示 ==========
Write-Host "[2/4] 数据库检查..." -ForegroundColor Yellow
Write-Host "   请确保 PostgreSQL 已启动（端口 15432）"
Write-Host "   数据库: jchatmind / 用户: jchatmind / 密码: jchatmind123"
Write-Host ""

# ========== 安装前端依赖 ==========
Write-Host "[3/4] 检查前端依赖..." -ForegroundColor Yellow
if (-not (Test-Path "$rootDir\ui\node_modules")) {
    Write-Host "   首次运行，正在安装前端依赖..."
    Set-Location "$rootDir\ui"
    npm install
    Set-Location $rootDir
    Write-Host "   ✓ 前端依赖安装完成" -ForegroundColor Green
} else {
    Write-Host "   ✓ 前端依赖已存在，跳过安装" -ForegroundColor Green
}
Write-Host ""

# ========== 启动服务 ==========
Write-Host "[4/4] 启动服务..." -ForegroundColor Yellow
Write-Host ""

# 启动后端
Write-Host "   ▶ 启动后端服务（Spring Boot，端口 8080）..." -ForegroundColor Cyan
$backendCmd = 'cd /d "' + $rootDir + '\jchatmind" && title JChatMind 后端 && .\mvnw.cmd spring-boot:run -DskipTests'
$backendProc = Start-Process -FilePath "cmd.exe" -ArgumentList "/c", $backendCmd -PassThru

Write-Host "   等待后端初始化（15秒）..."
Start-Sleep -Seconds 15

# 启动前端
Write-Host "   ▶ 启动前端服务（Vite，端口 15173）..." -ForegroundColor Cyan
$frontendCmd = 'cd /d "' + $rootDir + '\ui" && title JChatMind 前端 && npm run dev'
$frontendProc = Start-Process -FilePath "cmd.exe" -ArgumentList "/c", $frontendCmd -PassThru

Write-Host "   等待前端启动（5秒）..."
Start-Sleep -Seconds 5

# 打开浏览器
Write-Host "   ▶ 检查前端状态..." -ForegroundColor Cyan
# 检查前端是否已经可访问，避免重复打开浏览器
try {
    $alreadyRunning = (Test-NetConnection -ComputerName "127.0.0.1" -Port 15173 -WarningAction SilentlyContinue -ErrorAction SilentlyContinue).TcpTestSucceeded
} catch { $alreadyRunning = $false }
if ($alreadyRunning) {
    Write-Host "   ⚠ 前端已在运行，跳过打开浏览器" -ForegroundColor Yellow
} else {
    Write-Host "   ▶ 打开浏览器..." -ForegroundColor Cyan
    Start-Process "http://127.0.0.1:15173/"
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  后端地址: http://127.0.0.1:8080" -ForegroundColor White
Write-Host "  前端地址: http://127.0.0.1:15173" -ForegroundColor White
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  按任意键关闭此窗口（服务不受影响）"
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")