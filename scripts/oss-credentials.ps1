function Import-SkyOssCredentials {
    param([string]$ProjectRoot)
    $credentialPath=Join-Path $ProjectRoot '.local\oss-credentials.json'
    if(-not(Test-Path -LiteralPath $credentialPath)){return}
    $savedCredentials=Get-Content -LiteralPath $credentialPath -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach($item in @(@('AccessKeyId','ALI_OSS_ACCESS_KEY_ID'),@('AccessKeySecret','ALI_OSS_ACCESS_KEY_SECRET'))){
        $secureValue=ConvertTo-SecureString $savedCredentials.($item[0])
        $valuePointer=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureValue)
        try {[Environment]::SetEnvironmentVariable($item[1],[Runtime.InteropServices.Marshal]::PtrToStringBSTR($valuePointer),'Process')}
        finally {[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($valuePointer);$secureValue.Dispose()}
    }
}
