# MindHarness 开发模式启动脚本 (PowerShell)
# 本地运行前后端，远程连接台式机数据库（Tailscale 组网）
$ErrorActionPreference = "Stop"
$Host.UI.RawUI.WindowTitle = "MindHarness 开发模式"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  MindHarness 开发模式（远程数据库）" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path

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
    Write-Host "[警告] 未找到 .env 文件，请复制 .env.example 并填入 API Key" -ForegroundColor Yellow
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
Write-Host "  [1/5] 环境依赖检查" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Cyan

$allChecksPassed = $true

# ---- 检查 Java ----
Write-Host "  [1.1] Java..." -ForegroundColor Gray
try {
    $javaOutput = cmd /c "java -version 2>&1"
    if ($LASTEXITCODE -ne 0 -or -not $javaOutput) {
        throw "Java 未安装或无法执行"
    }
    $javaVersionLine = ($javaOutput | Select-String "version" | ForEach-Object { $_.ToString() })
    Write-Host "        版本: $javaVersionLine"

    if ($javaVersionLine -match 'version "(\d+)\.') {
        $majorVersion = [int]$Matches[1]
    } elseif ($javaVersionLine -match 'version "1\.(\d+)') {
        $majorVersion = [int]$Matches[1]
    } else {
        $majorVersion = 0
    }

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

    # 解析主版本号
    if ($nodeVersion -match 'v(\d+)\.') {
        $nodeMajor = [int]$Matches[1]
        if ($nodeMajor -lt 18) {
            Write-Host "   ✗ Node.js 版本过低（当前: v$nodeMajor），需要 Node.js 18+" -ForegroundColor Red
            $allChecksPassed = $false
        } else {
            Write-Host "   ✓ Node.js 版本检查通过" -ForegroundColor Green
        }
    } else {
        Write-Host "   ✓ Node.js: $nodeVersion" -ForegroundColor Green
    }
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

# ---- 检查 .env 文件及关键配置 ----
Write-Host "  [1.4] .env 配置..." -ForegroundColor Gray
if ($envLoaded) {
    $missingKeys = @()
    $requiredKeys = @("DEEPSEEK_API_KEY")
    foreach ($key in $requiredKeys) {
        $val = [Environment]::GetEnvironmentVariable($key, "Process")
        if (-not $val) {
            $missingKeys += $key
        }
    }
    if ($missingKeys.Count -gt 0) {
        Write-Host "   ✗ 以下必需环境变量未设置: $($missingKeys -join ', ')" -ForegroundColor Red
        Write-Host "     请在 .env 文件中填入对应的 API Key" -ForegroundColor Yellow
        $allChecksPassed = $false
    } else {
        Write-Host "   ✓ 必需环境变量均已配置" -ForegroundColor Green
    }
} else {
    Write-Host "   ✗ .env 文件缺失" -ForegroundColor Red
    Write-Host "     请复制 .env.example 为 .env 并填入 API Key" -ForegroundColor Yellow
    $allChecksPassed = $false
}

Write-Host ""

# ========== 数据库连通性检查 ==========
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  [2/5] 数据库连通性检查" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Cyan

$dbHost = "100.99.85.73"
$dbPort = 15432
$dbName = "mindharness"
$dbUser = "mindharness"
$dbPassword = "mindharness123"

Write-Host "  目标: ${dbHost}:${dbPort} (PostgreSQL, Tailscale 组网)" -ForegroundColor Gray

# ---- 检查 Tailscale 是否在运行 ----
Write-Host "  [2.1] Tailscale 状态..." -ForegroundColor Gray
try {
    $tsStatus = tailscale status 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   ✓ Tailscale 正在运行" -ForegroundColor Green
        # 尝试提取本机 Tailscale IP
        if ($tsStatus -match '(\d+\.\d+\.\d+\.\d+)') {
            Write-Host "        本机 Tailscale IP: $($Matches[1])" -ForegroundColor Gray
        }
    } else {
        Write-Host "   ✗ Tailscale 未运行或未安装，请先启动 Tailscale" -ForegroundColor Red
        $allChecksPassed = $false
    }
} catch {
    Write-Host "   ✗ Tailscale 未安装或不在 PATH 中" -ForegroundColor Red
    $allChecksPassed = $false
}

# ---- TCP 端口连通性检查 ----
Write-Host "  [2.2] TCP 端口 ${dbHost}:${dbPort}..." -ForegroundColor Gray
try {
    $tcpTest = Test-NetConnection -ComputerName $dbHost -Port $dbPort -WarningAction SilentlyContinue -ErrorAction SilentlyContinue
    if ($tcpTest.TcpTestSucceeded) {
        Write-Host "   ✓ TCP 端口 ${dbHost}:${dbPort} 可达" -ForegroundColor Green
    } else {
        Write-Host "   ✗ 无法连接到 ${dbHost}:${dbPort}" -ForegroundColor Red
        Write-Host "     请确认: 1) Tailscale 组网正常  2) 台式机 PostgreSQL 已启动" -ForegroundColor Yellow
        $allChecksPassed = $false
    }
} catch {
    Write-Host "   ✗ TCP 连接测试失败: $_" -ForegroundColor Red
    $allChecksPassed = $false
}

# ---- PostgreSQL 用户名密码认证验证 ----
Write-Host "  [2.3] 用户名密码认证 (${dbUser}@${dbHost}:${dbPort})..." -ForegroundColor Gray

function Test-PostgresAuth {
    param($hostName, $port, $dbName, $user, $password)
    
    # 尝试多个常见的 psql 安装路径
    $psqlPaths = @(
        "C:\Program Files\PostgreSQL\17\bin\psql.exe",
        "C:\Program Files\PostgreSQL\16\bin\psql.exe",
        "C:\Program Files\PostgreSQL\15\bin\psql.exe",
        "C:\Program Files\PostgreSQL\14\bin\psql.exe"
    )
    
    $psqlExe = $null
    foreach ($p in $psqlPaths) {
        if (Test-Path $p) { $psqlExe = $p; break }
    }
    
    # 如果没找到 psql，尝试从 PATH 中找
    if (-not $psqlExe) {
        $fromPath = (Get-Command psql -ErrorAction SilentlyContinue).Source
        if ($fromPath) { $psqlExe = $fromPath }
    }
    
    if ($psqlExe) {
        Write-Host "        使用: $psqlExe" -ForegroundColor Gray
        
        try {
            $env:PGPASSWORD = $password
            $result = & $psqlExe -h $hostName -p $port -U $user -d $dbName `
                -c "SELECT 1 AS connected;" 2>&1
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "   ✓ 数据库认证成功 — 用户名和密码正确" -ForegroundColor Green
                return $true
            } else {
                $errorText = ($result | Out-String).Trim()
                Write-Host "   ✗ 数据库认证失败！" -ForegroundColor Red
                
                if ($errorText -match "password authentication failed") {
                    Write-Host "     密码错误！请确认 ${user} 用户的密码" -ForegroundColor Red
                    Write-Host "     当前配置密码: ${password}" -ForegroundColor Gray
                } elseif ($errorText -match "no pg_hba.conf entry") {
                    Write-Host "     pg_hba.conf 拒绝连接 — 服务器未允许此 IP 的连接" -ForegroundColor Red
                } elseif ($errorText -match "Connection refused|timeout|timed out") {
                    Write-Host "     连接被拒绝或超时 — 请确认 PostgreSQL 正在运行" -ForegroundColor Red
                } else {
                    Write-Host "     错误详情: $errorText" -ForegroundColor Red
                }
                return $false
            }
        } catch {
            Write-Host "   ✗ 认证测试执行异常: $_" -ForegroundColor Red
            return $false
        } finally {
            $env:PGPASSWORD = $null
        }
    }
    
    # ---- 回退方案：使用 Maven 本地仓库中的 PostgreSQL JDBC 驱动 ----
    $m2Repo = "$env:USERPROFILE\.m2\repository"
    $pgJarPattern = "$m2Repo\org\postgresql\postgresql\*\postgresql-*.jar"
    $pgJars = Get-ChildItem -Path $pgJarPattern -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending
    
    if (-not $pgJars) {
        Write-Host "   ⚠ 未找到 psql.exe 或 JDBC 驱动，跳过认证测试" -ForegroundColor Yellow
        Write-Host "     安装 PostgreSQL 或运行过一次 mvn 后可自动验证" -ForegroundColor Gray
        return $true
    }
    
    $pgJar = $pgJars[0].FullName
    Write-Host "        使用 JDBC: $($pgJars[0].Name)" -ForegroundColor Gray
    
    # 检查 javac / java 是否可用
    $javacCmd = (Get-Command javac -ErrorAction SilentlyContinue).Source
    $javaCmd  = (Get-Command java  -ErrorAction SilentlyContinue).Source
    if (-not $javacCmd -or -not $javaCmd) {
        Write-Host "   ⚠ javac/java 不可用，跳过认证测试" -ForegroundColor Yellow
        return $true
    }
    
    # 生成临时 Java 测试类（catch Exception 而非仅 SQLException）
    $javaCode = @"
import java.sql.*;
public class _PgAuthTest {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://${hostName}:${port}/${dbName}?connectTimeout=5&socketTimeout=5";
        try (Connection conn = DriverManager.getConnection(url, "${user}", "${password}")) {
            System.out.println("OK");
        } catch (Exception e) {
            System.out.println("FAIL:" + e.getMessage().replace('\n',' ').replace('\r',' '));
            e.printStackTrace();
        }
    }
}
"@
    $tmpDir = Join-Path $rootDir "mindharness\target\tmp"
    New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null
    $javaFile = Join-Path $tmpDir "_PgAuthTest.java"
    # 使用 .NET 写入无 BOM 的 UTF-8（PowerShell 5.1 的 Set-Content UTF8 带 BOM，javac 不认）
    [System.IO.File]::WriteAllText($javaFile, $javaCode, [System.Text.UTF8Encoding]::new($false))
    try {
        # 编译
        $javacOutput = & javac -cp "$pgJar" -d "$tmpDir" "$javaFile" 2>&1
        if ($LASTEXITCODE -ne 0) {
            $compileError = ($javacOutput | Out-String).Trim()
            Write-Host "   ⚠ JDBC 编译失败:" -ForegroundColor Yellow
            if ($compileError) { Write-Host "     $compileError" -ForegroundColor Gray }
            return $true
        }
        
        # 运行
        $javaOutput = & java -cp "$pgJar;$tmpDir" _PgAuthTest 2>&1
        $outputStr = ($javaOutput | Out-String).Trim()
        
        if ($outputStr -eq "OK") {
            Write-Host "   ✓ 数据库认证成功 — 用户名和密码正确" -ForegroundColor Green
            return $true
        } elseif ($outputStr -match "^FAIL:") {
            $errMsg = $outputStr -replace "^FAIL:", "" 
            $errMsg = ($errMsg -split "`n")[0].Trim()  # 只取第一行（异常消息）
            Write-Host "   ✗ 数据库认证失败！" -ForegroundColor Red
            
            if ($errMsg -match "password authentication failed") {
                Write-Host "     密码错误！请确认 ${user} 用户的密码" -ForegroundColor Red
                Write-Host "     当前配置密码: ${password}" -ForegroundColor Gray
            } elseif ($errMsg -match "no pg_hba.conf entry") {
                Write-Host "     pg_hba.conf 拒绝连接 — 服务器未允许此 IP 的连接" -ForegroundColor Red
            } elseif ($errMsg -match "Connection refused|timeout|timed out|connect") {
                Write-Host "     连接被拒绝或超时 — 请确认 PostgreSQL 正在运行" -ForegroundColor Red
            } else {
                Write-Host "     错误详情: $errMsg" -ForegroundColor Red
            }
            return $false
        } else {
            # 未预期的输出，打印出来帮助排查
            Write-Host "   ⚠ 认证测试返回意外结果:" -ForegroundColor Yellow
            if ($outputStr) {
                $outputStr -split "`n" | Select-Object -First 5 | ForEach-Object {
                    Write-Host "     $_" -ForegroundColor Gray
                }
            }
            return $true
        }
    } catch {
        Write-Host "   ⚠ JDBC 认证测试异常: $_" -ForegroundColor Yellow
        return $true
    } finally {
        # 清理临时文件
        Remove-Item -Recurse -Force "$tmpDir\_PgAuthTest*" -ErrorAction SilentlyContinue
    }
}

$authResult = Test-PostgresAuth -hostName $dbHost -port $dbPort -dbName $dbName -user $dbUser -password $dbPassword
if (-not $authResult) {
    $allChecksPassed = $false
}

Write-Host ""

# ========== 端口可用性检查 & 旧进程清理 ==========
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  [3/5] 端口检查 & 旧进程清理" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Cyan

$backendPort = 8080
$frontendPort = 15173

# 辅助函数：根据端口号终止占用进程
function Stop-ProcessOnPort($port, $label) {
    $netstatLine = netstat -ano | Select-String "LISTENING" | Select-String ":${port}\s"
    if (-not $netstatLine) {
        Write-Host "   ✓ ${label}端口 ${port} 可用" -ForegroundColor Green
        return $true
    }

    # 取第一行提取 PID（IPv4/IPv6 可能返回多行，取首个匹配）
    $firstLine = if ($netstatLine -is [array]) { $netstatLine[0].ToString() } else { $netstatLine.ToString() }
    # 匹配最后一列数字（PID）
    $pidMatch = [regex]::Match($firstLine, '(\d+)\s*$')
    if (-not $pidMatch.Success) {
        Write-Host "   ✗ ${label}端口 ${port} 被占用，但无法解析 PID" -ForegroundColor Red
        Write-Host "     $firstLine" -ForegroundColor Gray
        return $false
    }

    $procId = $pidMatch.Groups[1].Value
    Write-Host "   ⚡ ${label}端口 ${port} 被进程占用 (PID: ${procId})，正在终止..." -ForegroundColor Yellow

    try {
        Stop-Process -Id $procId -Force -ErrorAction Stop
        Write-Host "   ✓ 旧进程 (PID: ${procId}) 已终止" -ForegroundColor Green

        # 等待端口释放
        Start-Sleep -Milliseconds 500
        $stillInUse = netstat -ano | Select-String "LISTENING" | Select-String ":${port}\s"
        if ($stillInUse) {
            Write-Host "   ✗ 终止后端口 ${port} 仍被占用" -ForegroundColor Red
            return $false
        }
        return $true
    } catch {
        Write-Host "   ✗ 无法终止进程 (PID: ${procId}): $_" -ForegroundColor Red
        return $false
    }
}

# 检查后端端口
Write-Host "  [3.1] 后端端口 ${backendPort}..." -ForegroundColor Gray
if (-not (Stop-ProcessOnPort $backendPort "后端")) {
    $allChecksPassed = $false
}

# 检查前端端口
Write-Host "  [3.2] 前端端口 ${frontendPort}..." -ForegroundColor Gray
if (-not (Stop-ProcessOnPort $frontendPort "前端")) {
    $allChecksPassed = $false
}

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
Write-Host "  ✓ 所有环境检查通过，准备启动服务" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""

# ========== 安装前端依赖 ==========
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  [4/5] 前端依赖" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Cyan
if (-not (Test-Path "$rootDir\ui\node_modules")) {
    Write-Host "   首次运行，正在安装前端依赖..."
    Push-Location "$rootDir\ui"
    npm install
    Pop-Location
    Write-Host "   ✓ 前端依赖安装完成" -ForegroundColor Green
} else {
    Write-Host "   ✓ 前端依赖已存在，跳过安装" -ForegroundColor Green
}
Write-Host ""

# ========== 启动服务 ==========
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  [5/5] 启动服务" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# 启动后端（tailscale profile）
Write-Host "   ▶ 启动后端服务（Spring Boot，端口 8080，tailscale 配置）..." -ForegroundColor Cyan
$backendCmd = 'cd /d "' + $rootDir + '\mindharness" && title MindHarness 后端 && .\mvnw.cmd spring-boot:run "-DskipTests" "-Dspring-boot.run.profiles=tailscale"'
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
Write-Host "   ▶ 打开浏览器..." -ForegroundColor Cyan
Start-Process "http://127.0.0.1:${frontendPort}/"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  后端地址: http://127.0.0.1:8080" -ForegroundColor White
Write-Host "  前端地址: http://127.0.0.1:15173" -ForegroundColor White
Write-Host "  数据库:   100.99.85.73:15432（Tailscale）" -ForegroundColor DarkGray
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  按任意键关闭此窗口（服务不受影响）"
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
