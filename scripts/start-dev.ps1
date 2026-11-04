param([switch]$Build)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $projectRoot 'sky-server\target\sky-server-1.0-SNAPSHOT.jar'
$runtimeDirectory = Join-Path $projectRoot '.local'
$logDirectory = Join-Path $projectRoot 'logs'
New-Item -ItemType Directory -Path $runtimeDirectory,$logDirectory -Force | Out-Null
Set-Location -LiteralPath $projectRoot

$savedDbPassword = $env:DB_PASSWORD
$savedMysqlPassword = $env:MYSQL_PWD
$savedJwtSecret = $env:SKY_JWT_ADMIN_SECRET
$savedOssId = $env:ALI_OSS_ACCESS_KEY_ID
$savedOssSecret = $env:ALI_OSS_ACCESS_KEY_SECRET
$securePassword = $null
$passwordPointer = [IntPtr]::Zero
try {
    $existingProcessFile = Join-Path $runtimeDirectory 'dev-server.pid'
    if (Test-Path -LiteralPath $existingProcessFile) {
        $existingProcessId = [int](Get-Content -LiteralPath $existingProcessFile -Raw)
        $existingProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$existingProcessId" -ErrorAction SilentlyContinue
        if ($existingProcess -and $existingProcess.CommandLine -like "*$jarPath*") {
            Write-Host '苍穹外卖已经在运行：http://localhost:8080/#/login' -ForegroundColor Green
            exit 0
        }
    }
    $portCheck = New-Object System.Net.Sockets.TcpClient
    try { $portCheck.Connect('127.0.0.1',8080); $portInUse = $true } catch { $portInUse = $false } finally { $portCheck.Dispose() }
    if ($portInUse) { throw '8080 端口已被其他程序占用，请先停止该程序。' }

    $sourceFiles = Get-ChildItem -Path (Join-Path $projectRoot 'sky-common'),(Join-Path $projectRoot 'sky-pojo'),(Join-Path $projectRoot 'sky-server') -Recurse -File |
        Where-Object { $_.FullName -notmatch '\\target\\' -and $_.Extension -in '.java','.xml','.yml','.html','.css','.js','.png','.webp' }
    $jarItem = Get-Item -LiteralPath $jarPath -ErrorAction SilentlyContinue
    $sourceChanged = $jarItem -and @($sourceFiles | Where-Object { $_.LastWriteTimeUtc -gt $jarItem.LastWriteTimeUtc }).Count -gt 0
    if ($Build -or -not $jarItem -or $sourceChanged) {
        Write-Host '正在构建项目，请稍候…'
        & mvn -B -ntp package
        if ($LASTEXITCODE -ne 0) { throw '项目构建失败，请查看上方错误。' }
    }

    if ([string]::IsNullOrEmpty($env:DB_PASSWORD)) {
        $securePassword = Read-Host '请输入本地 MySQL 的 root 密码（输入时不显示字符）' -AsSecureString
        $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
        $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    }
    $env:MYSQL_PWD = $env:DB_PASSWORD
    $mysqlExecutable = 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe'
    if (-not (Test-Path -LiteralPath $mysqlExecutable)) { $mysqlExecutable = (Get-Command mysql -ErrorAction Stop).Source }
    $OutputEncoding = [System.Text.UTF8Encoding]::new($false)
    Write-Host '正在连接数据库并准备课程初始账号…'
    $schemaPath = Join-Path $projectRoot 'sql\sky_take_out.sql'
    $seedPath = Join-Path $projectRoot 'sql\seed-admin.sql'
    Get-Content -LiteralPath $schemaPath,$seedPath -Encoding UTF8 |
        & $mysqlExecutable --protocol=TCP --host=127.0.0.1 --port=3306 --user=root --default-character-set=utf8mb4 --batch --raw |
        Out-Null
    if ($LASTEXITCODE -ne 0) { throw '数据库连接或初始化失败，请核对 root 密码与 MySQL 服务状态。' }
    $env:MYSQL_PWD = $savedMysqlPassword
    if ([string]::IsNullOrWhiteSpace($env:SKY_JWT_ADMIN_SECRET)) {
        $randomBytes = New-Object byte[] 32
        $randomGenerator = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $randomGenerator.GetBytes($randomBytes) } finally { $randomGenerator.Dispose() }
        $env:SKY_JWT_ADMIN_SECRET = [Convert]::ToBase64String($randomBytes)
    }

    . (Join-Path $PSScriptRoot 'oss-credentials.ps1')
    Import-SkyOssCredentials -ProjectRoot $projectRoot
    $javaExecutable = (Get-Command java -ErrorAction Stop).Source
    $staticDirectory = (Join-Path $projectRoot 'sky-server\src\main\resources\static').Replace('\','/')
    $javaProcess = Start-Process -FilePath $javaExecutable -ArgumentList @('-jar',('"' + $jarPath + '"'),'--spring.profiles.active=dev',
        '--server.port=8080','--server.address=127.0.0.1','--sky.datasource.host=127.0.0.1',
        '--sky.datasource.port=3306','--sky.datasource.database=sky_take_out','--sky.datasource.username=root',
        ('"--spring.web.resources.static-locations=file:' + $staticDirectory + '/,classpath:/static/"'),
        '--spring.web.resources.cache.period=0') `
        -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logDirectory 'server-out.log') `
        -RedirectStandardError (Join-Path $logDirectory 'server-error.log')
    [System.IO.File]::WriteAllText($existingProcessFile, [string]$javaProcess.Id)
    Write-Host '正在启动后端与前端页面…'
    $ready = $false
    for ($attempt = 0; $attempt -lt 45; $attempt++) {
        Start-Sleep -Seconds 1
        $javaProcess.Refresh()
        if ($javaProcess.HasExited) { throw '服务启动失败，请查看 logs\server-out.log 与 logs\server-error.log。' }
        try {
            $health = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/health' -TimeoutSec 2
            if ($health.code -eq 1) { $ready = $true; break }
        } catch { }
    }
    if (-not $ready) { throw '启动等待超时，请查看 logs 目录中的日志。' }
    Write-Host ''
    Write-Host '启动成功：http://localhost:8080/#/login' -ForegroundColor Green
    Write-Host '接口文档：http://localhost:8080/doc.html'
    Write-Host '课程初始账号：admin    密码：123456'
    Write-Host '若 admin 已存在，脚本会保留其原密码。'
    Write-Host '关闭此窗口不会停止服务；需要停止时双击“停止项目.cmd”。'
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
} finally {
    $env:DB_PASSWORD = $savedDbPassword
    $env:MYSQL_PWD = $savedMysqlPassword
    $env:SKY_JWT_ADMIN_SECRET = $savedJwtSecret
    $env:ALI_OSS_ACCESS_KEY_ID = $savedOssId
    $env:ALI_OSS_ACCESS_KEY_SECRET = $savedOssSecret
    if ($passwordPointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer) }
    if ($securePassword) { $securePassword.Dispose() }
}
