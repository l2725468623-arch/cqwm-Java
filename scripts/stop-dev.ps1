$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$processFile = Join-Path $projectRoot '.local\dev-server.pid'
$jarPath = Join-Path $projectRoot 'sky-server\target\sky-server-1.0-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $processFile)) { Write-Host '项目当前没有运行记录。'; exit 0 }
$serverProcessId = [int](Get-Content -LiteralPath $processFile -Raw)
$serverProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$serverProcessId" -ErrorAction SilentlyContinue
if ($serverProcess -and $serverProcess.CommandLine -like "*$jarPath*") {
    Stop-Process -Id $serverProcessId
    Write-Host '苍穹外卖服务已停止。'
} else { Write-Host '原服务已退出。' }
Remove-Item -LiteralPath $processFile
