param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$runtimeDirectory = Join-Path $projectRoot '.local'
$configurationPath = Join-Path $runtimeDirectory 'idea-dev.properties'
$securePassword = $null
$passwordPointer = [IntPtr]::Zero
$savedMysqlPassword = $env:MYSQL_PWD
$savedOssId = $env:ALI_OSS_ACCESS_KEY_ID
$savedOssSecret = $env:ALI_OSS_ACCESS_KEY_SECRET

function ConvertTo-PropertyValue {
    param([AllowEmptyString()][string]$Value)
    $builder = New-Object System.Text.StringBuilder
    foreach ($character in $Value.ToCharArray()) {
        $number = [int]$character
        if ($number -le 32 -or $number -gt 126 -or $number -in 33,35,58,61,92) {
            [void]$builder.Append(('\u{0:x4}' -f $number))
        } else { [void]$builder.Append($character) }
    }
    return $builder.ToString()
}

try {
    Write-Host '配置 IDEA 运行与断点调试' -ForegroundColor Cyan
    Write-Host '数据库密码仅在本地输入，不要发到聊天里。'
    Write-Host '配置将保存在 .local\idea-dev.properties，仅当前 Windows 账号和 SYSTEM 可读，且不会上传 Git。'
    Write-Host ''
    $securePassword = Read-Host '请输入 MySQL 的 root 密码（输入不显示）' -AsSecureString
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $databasePassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    $mysqlExecutable = 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe'
    if (-not (Test-Path -LiteralPath $mysqlExecutable)) { $mysqlExecutable = (Get-Command mysql -ErrorAction Stop).Source }
    $env:MYSQL_PWD = $databasePassword
    & $mysqlExecutable --protocol=TCP --host=127.0.0.1 --port=3306 --user=root --database=sky_take_out --batch --skip-column-names '--execute=SELECT 1;' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw '数据库连接失败，尚未保存配置。请核对密码与 MySQL 服务。' }
    $env:MYSQL_PWD = $savedMysqlPassword

    . (Join-Path $PSScriptRoot 'oss-credentials.ps1')
    Import-SkyOssCredentials -ProjectRoot $projectRoot
    $randomBytes = New-Object byte[] 32
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($randomBytes) } finally { $generator.Dispose() }
    $properties = [ordered]@{
        'server.port' = '8080'
        'server.address' = '127.0.0.1'
        'sky.datasource.host' = '127.0.0.1'
        'sky.datasource.port' = '3306'
        'sky.datasource.database' = 'sky_take_out'
        'sky.datasource.username' = 'root'
        'sky.datasource.password' = $databasePassword
        'sky.jwt.admin-secret-key' = [Convert]::ToBase64String($randomBytes)
        'spring.web.resources.static-locations' = 'file:./sky-server/src/main/resources/static/,classpath:/static/'
        'spring.web.resources.cache.period' = '0'
    }
    if ($env:ALI_OSS_ACCESS_KEY_ID -and $env:ALI_OSS_ACCESS_KEY_SECRET) {
        $properties['sky.alioss.access-key-id'] = $env:ALI_OSS_ACCESS_KEY_ID
        $properties['sky.alioss.access-key-secret'] = $env:ALI_OSS_ACCESS_KEY_SECRET
    }

    New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
    $currentUser = [Security.Principal.WindowsIdentity]::GetCurrent().User
    $systemUser = New-Object Security.Principal.SecurityIdentifier('S-1-5-18')
    $acl = New-Object Security.AccessControl.FileSecurity
    $acl.SetOwner($currentUser)
    $acl.SetAccessRuleProtection($true, $false)
    foreach ($identity in @($currentUser, $systemUser)) {
        $rule = New-Object Security.AccessControl.FileSystemAccessRule($identity, 'FullControl', 'Allow')
        $acl.AddAccessRule($rule)
    }
    # Apply the restricted ACL before writing any credential data.
    if (-not (Test-Path -LiteralPath $configurationPath)) {
        [IO.File]::WriteAllText($configurationPath, '', [Text.Encoding]::ASCII)
    }
    Set-Acl -LiteralPath $configurationPath -AclObject $acl
    $lines = @('# Local IDEA configuration. Contains credentials. Do not commit or share.')
    foreach ($entry in $properties.GetEnumerator()) {
        $lines += $entry.Key + '=' + (ConvertTo-PropertyValue ([string]$entry.Value))
    }
    [IO.File]::WriteAllLines($configurationPath, $lines, [Text.Encoding]::ASCII)
    Write-Host ''
    Write-Host 'IDEA 配置完成。' -ForegroundColor Green
    Write-Host '1. 如果之前用启动项目.cmd启动过，请先双击停止项目.cmd。'
    Write-Host '2. 在 IDEA 右上角选择：苍穹外卖-IDEA断点调试。'
    Write-Host '3. 点击小虫子 Debug，启动后打开 http://localhost:8080/#/login。'
    if ($properties.Contains('sky.alioss.access-key-id')) { Write-Host '已复用本地保存的 OSS 凭证。' }
    else { Write-Host '未找到 OSS 凭证，菜品/员工等接口可用；图片上传前请运行配置云存储.cmd，再运行本工具。' }
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
} finally {
    $env:MYSQL_PWD = $savedMysqlPassword
    $env:ALI_OSS_ACCESS_KEY_ID = $savedOssId
    $env:ALI_OSS_ACCESS_KEY_SECRET = $savedOssSecret
    $databasePassword = $null
    $properties = $null
    $lines = $null
    if ($passwordPointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer) }
    if ($securePassword) { $securePassword.Dispose() }
}
