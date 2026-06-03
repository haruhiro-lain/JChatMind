# MindHarness 项目启动脚本 (PowerShell)
# 优先使用 Docker Compose 部署；Docker 不可用时回退到本地环境
$ErrorActionPreference = "Stop"
$Host.UI.RawUI.WindowTitle = "MindHarness 启动脚本"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  MindHarness 项目启动脚本" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$composeFile = "$rootDir\docker\mindharness\docker-compose.yml"

# ========== 加载 .env 文件 ==========
$envFile = Join-Path $rootDir ".env"
$envLoaded = $false
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $name, $value = $line.Split("=", 2)
            $name = $name.Trim()
            $value = $value.Trim()
            if ($name -and $value) {
                [Environment]::SetEnvironmentVariable($name, $value, "Process")
            }
        }
    }
    $envLoaded = $true
    Write-Host "[ENV] 已加载 .env 配置" -ForegroundColor Green
} else {
    Write-Host "[警告] 未找到 .env 文件" -ForegroundColor Yellow
}
Write-Host ""

# ========== 配置 JDK 17 ==========
$jdk17Path = "D:\Environment\Java\jdk17"
if (Test-Path "$jdk17Path\bin\java.exe") {
    $env:JAVA_HOME = $jdk17Path
    $env:Path = "$jdk17Path\bin;$env:Path"
    Write-Host "[JDK] 已切换到 JDK 17: $jdk17Path" -ForegroundColor Green
} else {
    Write-Host "[警告] JDK 17 未在 $jdk17Path 找到，将使用系统默认 Java" -ForegroundColor Yellow
}
Write-Host ""

# ========== 环境检查 ==========
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  [1/4] 环境依赖检查" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Cyan

$allChecksPassed = $true

# ---- 检查 Java ----
Write-Host "  [1.1] Java..." -ForegroundColor Gray
try {
    $javaOutput = cmd /c "java -version 2>&1"
    if ($LASTEXITCODE -ne 0 -or -not $javaOutput) { throw "Java 未安装或无法执行" }
    $javaVersionLine = ($javaOutput | Select-String "version" | ForEach-Object { $_.ToString() })
    Write-Host "        版本: $javaVersionLine"
    # 先匹配 1.x 格式 (Java 8 及更早: "1.8.0_422")
    if ($javaVersionLine -match 'version "1\.(\d+)') { $majorVersion = [int]$Matches[1] }
    # 再匹配 9+ 格式 (Java 17+: "17.0.9")
    elseif ($javaVersionLine -match 'version "(\d+)\.') { $majorVersion = [int]$Matches[1] }
    else { $majorVersion = 0 }
    if ($majorVersion -lt 17) {
        Write-Host "   ✗ Java 版本过低（当前: $majorVersion），需要 JDK 17+" -ForegroundColor Red
        $allChecksPassed = $false
    } else {
        Write-Host "   ✓ Java $majorVersion 版本检查通过" -ForegroundColor Green
    }
} catch {
    Write-Host "   ✗ 未找到 Java，请安装 JDK 17+" -ForegroundColor Red
    $allChecksPassed = $false
}

# ---- 检查 Node.js ----
Write-Host "  [1.2] Node.js..." -ForegroundColor Gray
try {
    $nodeVersion = node -v
    Write-Host "        版本: $nodeVersion"
    if ($nodeVersion -match 'v(\d+)\.') {
        $nodeMajor = [int]$Matches[1]
        if ($nodeMajor -lt 18) {
            Write-Host "   ✗ Node.js 版本过低（当前: v$nodeMajor），需要 Node.js 18+" -ForegroundColor Red
            $allChecksPassed = $false
        } else { Write-Host "   ✓ Node.js 版本检查通过" -ForegroundColor Green }
    } else { Write-Host "   ✓ Node.js: $nodeVersion" -ForegroundColor Green }
} catch {
    Write-Host "   ✗ 未找到 Node.js，请安装 Node.js 18+" -ForegroundColor Red
    $allChecksPassed = $false
}

# ---- 检查 Maven Wrapper ----
Write-Host "  [1.3] Maven Wrapper..." -ForegroundColor Gray
$mvnwPath = Join-Path $rootDir "mindharness\mvnw.cmd"
if (Test-Path $mvnwPath) {
    Write-Host "   ✓ mvnw.cmd 存在" -ForegroundColor Green
} else {
    Write-Host "   ✗ 未找到 mvnw.cmd（路径: $mvnwPath）" -ForegroundColor Red
    $allChecksPassed = $false
}

# ---- 检查 .env 配置 ----
Write-Host "  [1.4] .env 配置..." -ForegroundColor Gray
if ($envLoaded) {
    $missingKeys = @()
    $requiredKeys = @("DEEPSEEK_API_KEY")
    foreach ($key in $requiredKeys) {
        $val = [Environment]::GetEnvironmentVariable($key, "Process")
        if (-not $val) { $missingKeys += $key }
    }
    if ($missingKeys.Count -gt 0) {
        Write-Host "   ✗ 以下必需环境变量未设置: $($missingKeys -join ', ')" -ForegroundColor Red
        $allChecksPassed = $false
    } else { Write-Host "   ✓ 必需环境变量均已配置" -ForegroundColor Green }
} else {
    Write-Host "   ✗ .env 文件缺失" -ForegroundColor Red
    $allChecksPassed = $false
}

Write-Host ""

# ========== 端口检查 & 旧进程清理 ==========
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  [2/4] 端口检查 & 旧进程清理" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Cyan

$backendPort = 8080
$frontendPort = 15173
$dbPort = 15432

function Stop-ProcessOnPort($port, $label) {
    $netstatLine = netstat -ano | Select-String "LISTENING" | Select-String ":${port}\s"
    if (-not $netstatLine) {
        Write-Host "   ✓ ${label}端口 ${port} 可用" -ForegroundColor Green
        return $true
    }
    $firstLine = if ($netstatLine -is [array]) { $netstatLine[0].ToString() } else { $netstatLine.ToString() }
    $pidMatch = [regex]::Match($firstLine, '(\d+)\s*$')
    if (-not $pidMatch.Success) {
        Write-Host "   ✗ ${label}端口 ${port} 被占用，但无法解析 PID" -ForegroundColor Red
        return $false
    }
    $procId = $pidMatch.Groups[1].Value
    Write-Host "   ⚡ ${label}端口 ${port} 被进程占用 (PID: ${procId})，正在处理..." -ForegroundColor Yellow

    # 先尝试停掉使用该端口的 Docker 容器
    try {
        $containerId = docker ps -q --filter "publish=${port}" 2>$null
        if ($containerId) {
            Write-Host "     发现 Docker 容器占用，正在停止..." -ForegroundColor Gray
            docker stop $containerId 2>$null | Out-Null
            docker rm $containerId 2>$null | Out-Null
            Start-Sleep -Milliseconds 500
            Write-Host "   ✓ Docker 容器已停止" -ForegroundColor Green
            return $true
        }
    } catch { }

    # Docker 没有占用，直接杀进程
    try {
        Stop-Process -Id $procId -Force -ErrorAction Stop
        Start-Sleep -Milliseconds 500
        $stillInUse = netstat -ano | Select-String "LISTENING" | Select-String ":${port}\s"
        if ($stillInUse) {
            Write-Host "   ✗ 终止后端口 ${port} 仍被占用" -ForegroundColor Red
            return $false
        }
        Write-Host "   ✓ 旧进程 (PID: ${procId}) 已终止" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "   ✗ 无法终止进程 (PID: ${procId}): $_" -ForegroundColor Red
        return $false
    }
}

Write-Host "  [2.1] 数据库端口 ${dbPort}..." -ForegroundColor Gray
if (-not (Stop-ProcessOnPort $dbPort "数据库")) { $allChecksPassed = $false }
Write-Host "  [2.2] 后端端口 ${backendPort}..." -ForegroundColor Gray
if (-not (Stop-ProcessOnPort $backendPort "后端")) { $allChecksPassed = $false }
Write-Host "  [2.3] 前端端口 ${frontendPort}..." -ForegroundColor Gray
if (-not (Stop-ProcessOnPort $frontendPort "前端")) { $allChecksPassed = $false }

Write-Host ""

# ========== 检查结果汇总 ==========
if (-not $allChecksPassed) {
    Write-Host "============================================" -ForegroundColor Red
    Write-Host "  ✗ 环境检查未通过，请修复以上问题后重试" -ForegroundColor Red
    Write-Host "============================================" -ForegroundColor Red
    Write-Host ""
    Read-Host "按 Enter 退出"
    exit 1
}

Write-Host "============================================" -ForegroundColor Green
Write-Host "  ✓ 所有环境检查通过" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""

# ========== Docker 检测 ==========
$dockerAvailable = $false
try {
    $dockerVersion = docker --version 2>&1
    if ($dockerVersion -match "Docker version") {
        Write-Host "[Docker] $dockerVersion" -ForegroundColor Green
        $dockerAvailable = $true
    }
} catch { }

$composeAvailable = $false
$composeCmd = ""
if ($dockerAvailable) {
    # 检测 compose 命令（v2: docker compose, v1: docker-compose）
    $v2 = cmd /c "docker compose version 2>&1"
    if ($LASTEXITCODE -eq 0 -and $v2 -match "Docker Compose") {
        $composeAvailable = $true
        $composeCmd = "docker compose"
        Write-Host "[Compose] $v2" -ForegroundColor Green
    } else {
        $v1 = cmd /c "docker-compose version 2>&1"
        if ($LASTEXITCODE -eq 0 -and $v1 -match "Docker Compose") {
            $composeAvailable = $true
            $composeCmd = "docker-compose"
            Write-Host "[Compose] $v1" -ForegroundColor Green
        }
    }
}

# ====================================================================
#  方案 A：Docker Compose 部署（优先）
# ====================================================================
if ($dockerAvailable -and $composeAvailable -and (Test-Path $composeFile)) {
    Write-Host ""
    Write-Host ">>> 使用 Docker Compose 部署（推荐） <<<" -ForegroundColor Green
    Write-Host ""

    # 检查是否已有镜像，有则跳过重建（加 --build 参数可强制重建）
    $doRebuild = $false
    if ($args -contains "--build") {
        $doRebuild = $true
        Write-Host "   检测到 --build 参数，将重新构建镜像" -ForegroundColor Yellow
    }

    Write-Host ""
    Write-Host "[3/4] 启动容器服务..." -ForegroundColor Yellow
    if ($doRebuild) {
        Write-Host "   $composeCmd -f docker/mindharness/docker-compose.yml up -d --build"
    } else {
        Write-Host "   $composeCmd -f docker/mindharness/docker-compose.yml up -d"
    }
    Write-Host ""

    Push-Location $rootDir
    try {
        # 先停止并清理可能残留的旧容器（避免端口冲突）
        Write-Host "   清理旧容器..." -ForegroundColor DarkGray
        cmd /c "$composeCmd -f docker/mindharness/docker-compose.yml down --remove-orphans 2>&1" | Out-Null

        # 捕获 docker compose 输出以便在失败时显示错误
        $composeFileArg = "docker/mindharness/docker-compose.yml"
        if ($doRebuild) {
            $composeOutput = cmd /c "$composeCmd -f $composeFileArg up -d --build 2>&1"
        } else {
            $composeOutput = cmd /c "$composeCmd -f $composeFileArg up -d 2>&1"
        }
        Write-Host $composeOutput

        if ($LASTEXITCODE -ne 0) {
            Write-Host ""
            Write-Host "============================================" -ForegroundColor Red
            Write-Host "  docker compose up 失败！" -ForegroundColor Red
            Write-Host "  常见原因：" -ForegroundColor Yellow
            Write-Host "  1. 端口冲突：检查 15432/8080/15173 是否被占用" -ForegroundColor DarkGray
            Write-Host "  2. 镜像拉取失败：检查 Docker Hub 网络连接或代理配置" -ForegroundColor DarkGray
            Write-Host "  3. Maven/npm 构建失败：检查网络和依赖" -ForegroundColor DarkGray
            Write-Host "============================================" -ForegroundColor Red
            throw "docker compose up 失败，请查看上方错误信息"
        }
    } finally {
        Pop-Location
    }

    Write-Host "[4/4] 等待服务就绪..." -ForegroundColor Yellow
    Write-Host "   （PostgreSQL + 后端 + 前端 约需 30-60 秒）"
    $maxWait = 60
    $waited = 0
    while ($waited -lt $maxWait) {
        Start-Sleep -Seconds 5
        $waited += 5
        try {
            $health = docker inspect --format='{{.State.Health.Status}}' mindharness-postgres 2>$null
            if ($health -eq "healthy") { break }
        } catch { }
    }
    Write-Host "   等待完成（${waited}秒）" -ForegroundColor Green

    Write-Host "      打开浏览器..." -ForegroundColor Yellow
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
    Write-Host "  查看日志:  $composeCmd -f docker/mindharness/docker-compose.yml logs -f"
    Write-Host "  停止服务:  $composeCmd -f docker/mindharness/docker-compose.yml down"
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

# ========== 配置 JDK 17 ==========
$jdk17Path = "D:\Environment\Java\jdk17"
if (Test-Path "$jdk17Path\bin\java.exe") {
    $env:JAVA_HOME = $jdk17Path
    $env:Path = "$jdk17Path\bin;$env:Path"
    Write-Host "[JDK] 已切换到 JDK 17: $jdk17Path" -ForegroundColor Green
} else {
    Write-Host "[警告] JDK 17 未在 $jdk17Path 找到，将使用系统默认 Java" -ForegroundColor Yellow
}
Write-Host ""

# ========== 本地启动 ==========
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  [3/4] 本地启动服务" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Cyan

Write-Host ""

# ========== 安装前端依赖 ==========
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
$backendCmd = 'cd /d "' + $rootDir + '\mindharness" && title MindHarness 后端 && .\mvnw.cmd spring-boot:run -DskipTests'
$backendProc = Start-Process -FilePath "cmd.exe" -ArgumentList "/c", $backendCmd -PassThru

Write-Host "   等待后端初始化（15秒）..."
Start-Sleep -Seconds 15

# 启动前端
Write-Host "   ▶ 启动前端服务（Vite，端口 15173）..." -ForegroundColor Cyan
$frontendCmd = 'cd /d "' + $rootDir + '\ui" && title MindHarness 前端 && npm run dev'
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