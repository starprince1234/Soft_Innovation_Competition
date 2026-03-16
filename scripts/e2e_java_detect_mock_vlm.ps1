param(
    [string]$JavaBase = "http://127.0.0.1:8080",
    [string]$ImagePath = "D:\VScodeProjects\JiGuYunYu_3_8_anzhuoliantiao\test.jpeg",
    [int]$PollMaxAttempts = 20,
    [int]$PollIntervalSeconds = 2,
    [string]$ExpectedBucket = "jiguyunyu1"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $ImagePath)) {
    Write-Error "Image not found: $ImagePath"
    exit 1
}

$imageBytes = [System.IO.File]::ReadAllBytes($ImagePath)
$imageBase64 = [System.Convert]::ToBase64String($imageBytes)
$imageDataUrl = "data:image/jpeg;base64,$imageBase64"

$username = "e2e_" + [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$password = "Passw0rd!123"

Write-Host "[1/5] Register user: $username"
$registerPayload = @{ username = $username; password = $password } | ConvertTo-Json
$null = Invoke-RestMethod -Method Post -Uri "$JavaBase/api/v1/auth/register" -ContentType "application/json" -Body $registerPayload

Write-Host "[2/5] Login user and acquire JWT"
$loginPayload = @{ username = $username; password = $password } | ConvertTo-Json
$loginResp = Invoke-RestMethod -Method Post -Uri "$JavaBase/api/v1/auth/login" -ContentType "application/json" -Body $loginPayload
$token = $loginResp.data.token
if (-not $token) {
    Write-Error "Login succeeded but token is empty"
    exit 2
}

$authHeaders = @{ Authorization = "Bearer $token"; "Content-Type" = "application/json" }

Write-Host "[3/5] Create detect task via Java public API"
$createPayload = @{ imageBase64 = $imageDataUrl } | ConvertTo-Json -Depth 4
$createResp = Invoke-RestMethod -Method Post -Uri "$JavaBase/api/v1/detect/tasks" -Headers $authHeaders -Body $createPayload
$taskId = $createResp.data.taskId
if (-not $taskId) {
    Write-Error "Create task failed: missing taskId"
    exit 3
}

Write-Host "Task created: $taskId"

$finalStatus = ""
for ($i = 1; $i -le $PollMaxAttempts; $i++) {
    Start-Sleep -Seconds $PollIntervalSeconds
    $statusResp = Invoke-RestMethod -Method Get -Uri "$JavaBase/api/v1/detect/tasks/$taskId/status" -Headers @{ Authorization = "Bearer $token" }
    $finalStatus = $statusResp.data.status
    Write-Host "Poll $i/$PollMaxAttempts status=$finalStatus"

    if ($finalStatus -eq "COMPLETED" -or $finalStatus -eq "FAILED") {
        break
    }
}

if ($finalStatus -ne "COMPLETED") {
    Write-Error "Detect task did not complete successfully. Final status=$finalStatus"
    exit 4
}

Write-Host "[4/5] Fetch detect result"
$resultResp = Invoke-RestMethod -Method Get -Uri "$JavaBase/api/v1/detect/results/$taskId" -Headers @{ Authorization = "Bearer $token" }
$data = $resultResp.data
$imageUrl = $data.imageUrl
$artifacts = $data.detectedArtifacts

if (-not $artifacts -or $artifacts.Count -lt 1) {
    Write-Error "Result has no detectedArtifacts"
    exit 5
}

$first = $artifacts[0]

$bucketCheck = $false
if ($imageUrl -and $ExpectedBucket) {
    $bucketCheck = $imageUrl -like "*${ExpectedBucket}*"
}

$summary = [ordered]@{
    ok = $true
    javaBase = $JavaBase
    taskId = $taskId
    status = $finalStatus
    imageUrl = $imageUrl
    imageUrlContainsExpectedBucket = $bucketCheck
    detectedLabel = $first.label
    confidence = $first.confidence
}

Write-Host "[5/5] E2E summary"
$summary | ConvertTo-Json -Depth 6
