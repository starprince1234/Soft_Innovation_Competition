param(
    [string]$ImagePath = "D:\VScodeProjects\JiGuYunYu_3_8_anzhuoliantiao\test.jpeg",
    [string]$ModelBase = "https://u817542-83de-33e3ef6c.bjb2.seetacloud.com:8443/v1",
    [string]$ModelApiKey = "sk-123456",
    [string]$PythonBase = "http://127.0.0.1:8000",
    [string]$InternalToken = "your_internal_token_123456",
    [string]$PythonEnvFile = "D:\VScodeProjects\JiGuYunYu_3_8_anzhuoliantiao\JiGuYunYu_Python\.env",
    [string]$PythonExe = "d:/VScodeProjects/JiGuYunYu_3_8_anzhuoliantiao/.venv/Scripts/python.exe"
)

function Import-DotEnv([string]$Path) {
    if (-not (Test-Path $Path)) {
        return
    }
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) {
            return
        }
        $parts = $line.Split("=", 2)
        if ($parts.Count -ne 2) {
            return
        }
        $name = $parts[0].Trim()
        $value = $parts[1].Trim()
        if ($name) {
            Set-Item -Path "Env:$name" -Value $value
        }
    }
}

Import-DotEnv -Path $PythonEnvFile

if (-not (Test-Path $ImagePath)) {
    Write-Error "Image not found: $ImagePath"
    exit 1
}

$imgBytes = [System.IO.File]::ReadAllBytes($ImagePath)
$imgBase64 = [System.Convert]::ToBase64String($imgBytes)

Write-Host "[0/3] Verify OSS upload/download roundtrip..."
$ossBucket = $env:BOS_BUCKET
if (-not $ossBucket) {
    # Keep compatibility with existing deploy examples when .env misses BOS_BUCKET.
    $ossBucket = "jigu-bucket"
}
$ossRaw = & $PythonExe .\scripts\oss_roundtrip_test.py --image-path "$ImagePath" --endpoint "$env:BOS_ENDPOINT" --access-key "$env:BOS_ACCESS_KEY" --secret-key "$env:BOS_SECRET_KEY" --bucket "$ossBucket" --keep-object
if ($LASTEXITCODE -ne 0) {
    Write-Error "OSS roundtrip failed: $ossRaw"
    exit 2
}
$ossResult = $ossRaw | ConvertFrom-Json
if (-not $ossResult.ok) {
    Write-Error "OSS roundtrip response not ok: $ossRaw"
    exit 2
}
Write-Host "OSS roundtrip ok, image url: $($ossResult.url)"

Write-Host "[1/3] Call remote VLM endpoint via curl-style JSON..."
$modelPayload = @{
    model = "qwen3-vl-lora"
    messages = @(
        @{
            role = "user"
            content = @(
                @{ type = "text"; text = "你是专业考古学家，请识别这张图中的文物，并按JSON输出name/confidence/category/era/description" },
                @{ type = "image_url"; image_url = @{ url = "data:image/jpeg;base64,$imgBase64" } }
            )
        }
    )
    temperature = 0.6
    top_p = 0.8
    max_tokens = 1024
    stop = @("<|im_end|>", "<|endoftext|>", "<|end|>")
} | ConvertTo-Json -Depth 12

$vlmResp = Invoke-RestMethod -Method Post -Uri "$ModelBase/chat/completions" -Headers @{
    Authorization = "Bearer $ModelApiKey"
    "Content-Type" = "application/json"
} -Body $modelPayload

$rawContent = $vlmResp.choices[0].message.content
Write-Host "Model raw response preview:"
Write-Host $rawContent

Write-Host "`n[2/3] Call Python internal detect endpoint with OSS image URL..."
$detectByUrlPayload = @{
    taskId = "local-curl-test-url"
    imageUrl = "$($ossResult.url)"
} | ConvertTo-Json -Depth 4

$detectByUrlResp = Invoke-RestMethod -Method Post -Uri "$PythonBase/internal/detect/process" -Headers @{
    "X-Internal-Token" = $InternalToken
    "Content-Type" = "application/json"
} -Body $detectByUrlPayload

$detectByUrlResp | ConvertTo-Json -Depth 10

Write-Host "`n[3/3] Call Python internal detect endpoint with same image base64 (fallback path)..."
$detectPayload = @{
    taskId = "local-curl-test"
    imageBase64 = $imgBase64
} | ConvertTo-Json -Depth 4

$detectResp = Invoke-RestMethod -Method Post -Uri "$PythonBase/internal/detect/process" -Headers @{
    "X-Internal-Token" = $InternalToken
    "Content-Type" = "application/json"
} -Body $detectPayload

$detectResp | ConvertTo-Json -Depth 10
