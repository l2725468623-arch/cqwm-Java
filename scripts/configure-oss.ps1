$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$keyId=$null;$keySecret=$null
$previousId=$env:ALI_OSS_ACCESS_KEY_ID;$previousSecret=$env:ALI_OSS_ACCESS_KEY_SECRET
try {
    Write-Host '配置阿里云 OSS：lay-java / 广州。' -ForegroundColor Cyan
    Write-Host '请输入已轮换的新密钥，或你确认要使用的有效密钥。输入内容不会回显。'
    $keyId=Read-Host 'AccessKey ID' -AsSecureString
    $keySecret=Read-Host 'AccessKey Secret' -AsSecureString
    if($keyId.Length -eq 0 -or $keySecret.Length -eq 0){throw '两项均不能为空。'}
    $localDirectory=Join-Path $projectRoot '.local'
    New-Item -ItemType Directory -Path $localDirectory -Force | Out-Null
    $protectedCredentials=@{AccessKeyId=($keyId|ConvertFrom-SecureString);AccessKeySecret=($keySecret|ConvertFrom-SecureString)}
    [System.IO.File]::WriteAllText((Join-Path $localDirectory 'oss-credentials.json'),($protectedCredentials|ConvertTo-Json),[System.Text.UTF8Encoding]::new($false))
    . (Join-Path $PSScriptRoot 'oss-credentials.ps1')
    Import-SkyOssCredentials -ProjectRoot $projectRoot
    $jarPath=Join-Path $projectRoot 'sky-server\target\sky-server-1.0-SNAPSHOT.jar'
    if(-not(Test-Path -LiteralPath $jarPath)){throw '密钥已加密保存。请先构建项目，再运行本工具检查连接。'}
    Write-Host '正在上传并核对一个临时测试文件…'
    $result=@(& java '-Dloader.main=com.sky.tools.OssConnectionCheck' -cp $jarPath org.springframework.boot.loader.PropertiesLauncher)
    $exitCode=$LASTEXITCODE
    $safeResult=@($result|Where-Object {$_ -match '^OSS_CHECK_'})
    $safeResult | Set-Content -LiteralPath (Join-Path $localDirectory 'oss-check.log') -Encoding UTF8
    if($exitCode -ne 0){Write-Host ($safeResult -join "`n");throw '密钥已加密保存，但云端连接检查失败，请核对密钥权限与 Bucket。'}
    Write-Host 'OSS 上传连接成功。重新启动项目后生效。' -ForegroundColor Green
    if($safeResult -contains 'OSS_CHECK_OK_TEST_FILE_RETAINED'){Write-Host '测试文件因缺少删除权限而保留在 connection-check 目录。'}
    Write-Host '密钥仅以当前 Windows 账号可解密的形式保存，已排除 Git 上传。'
} catch {Write-Host $_.Exception.Message -ForegroundColor Red;exit 1}
finally {
    $env:ALI_OSS_ACCESS_KEY_ID=$previousId;$env:ALI_OSS_ACCESS_KEY_SECRET=$previousSecret
    if($keyId){$keyId.Dispose()};if($keySecret){$keySecret.Dispose()}
}
