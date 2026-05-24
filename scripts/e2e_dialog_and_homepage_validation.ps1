param(
    [string]$JavaBase = "http://127.0.0.1:8080",
    [string]$SeedScript = "$PSScriptRoot\seed_homepage_artifact_from_assets.ps1",
    [string]$SeedArtifactName = "E2E-Artifact-QingTongShenShu",
    [string]$ImagePath = "",
    [string]$TextPath = ""
)

$ErrorActionPreference = "Stop"


Write-Host "[2/6] Register and login test user..."
$username = "e2e_home_" + [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$password = "Passw0rd!123"
$registerPayload = @{ username = $username; password = $password } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$JavaBase/api/v1/auth/register" -ContentType "application/json" -Body $registerPayload | Out-Null

$loginPayload = @{ username = $username; password = $password } | ConvertTo-Json
$loginResp = Invoke-RestMethod -Method Post -Uri "$JavaBase/api/v1/auth/login" -ContentType "application/json" -Body $loginPayload
$token = $loginResp.data.token
if (-not $token) {
    throw "Login token missing"
}
$auth = @{ Authorization = "Bearer $token"; "Content-Type" = "application/json" }

Write-Host "[2/6] Upload test image via detect API (backend object storage path)..."
if (-not $ImagePath) {
    $repoRoot = Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")
    $ImagePath = (Get-ChildItem -LiteralPath (Join-Path $repoRoot "assets") -File -Filter "*.png" | Sort-Object Name | Select-Object -First 1).FullName
}
if (-not (Test-Path -LiteralPath $ImagePath)) {
    throw "Image not found for detect upload: $ImagePath"
}
$imgBytes = [System.IO.File]::ReadAllBytes($ImagePath)
$imgB64 = [Convert]::ToBase64String($imgBytes)
$detectPayload = @{ imageBase64 = $imgB64 } | ConvertTo-Json
$detectCreate = Invoke-RestMethod -Method Post -Uri "$JavaBase/api/v1/detect/tasks" -Headers $auth -Body $detectPayload
$taskId = $detectCreate.data.taskId
if (-not $taskId) {
    throw "Detect taskId missing: $($detectCreate | ConvertTo-Json -Depth 8)"
}

$detectedImageUrl = $null
for ($i = 0; $i -lt 25; $i++) {
    Start-Sleep -Seconds 2
    $result = Invoke-RestMethod -Method Get -Uri "$JavaBase/api/v1/detect/results/$taskId" -Headers @{ Authorization = "Bearer $token" }
    if ($result.data.imageUrl) {
        $detectedImageUrl = [string]$result.data.imageUrl
        break
    }
}
if (-not $detectedImageUrl) {
    throw "Detect result imageUrl missing for taskId=$taskId"
}

Write-Host "[3/6] Seed homepage artifact using detect image URL + test text..."
$seedArgs = @(
    "-ExecutionPolicy", "Bypass",
    "-File", $SeedScript,
    "-ArtifactName", $SeedArtifactName,
    "-ImageUrlOverride", $detectedImageUrl
)
if ($ImagePath) { $seedArgs += @("-ImagePath", $ImagePath) }
if ($TextPath) { $seedArgs += @("-TextPath", $TextPath) }
$seedRaw = & powershell @seedArgs | Out-String
if ($LASTEXITCODE -ne 0) {
    throw "Seed script failed: $seedRaw"
}
$jsonStart = $seedRaw.IndexOf("{")
if ($jsonStart -lt 0) {
    throw "Seed output has no JSON payload: $seedRaw"
}
$seedJson = $seedRaw.Substring($jsonStart)
$seed = $seedJson | ConvertFrom-Json
if (-not $seed.ok) {
    throw "Seed result not ok: $seedRaw"
}

Write-Host "[4/6] Validate homepage artifact list contains seeded item..."
$artResp = Invoke-RestMethod -Method Get -Uri "$JavaBase/api/v1/artifacts?page=0&size=20&name=$([uri]::EscapeDataString($SeedArtifactName))" -Headers @{ Authorization = "Bearer $token" }
$items = @($artResp.data.list)
if ($items.Count -lt 1) {
    throw "No artifacts returned for seeded name"
}
$target = $items | Where-Object { $_.name -eq $SeedArtifactName } | Select-Object -First 1
if (-not $target) {
    throw "Seeded artifact not found in API list"
}
if (-not $target.imageUrl) {
    throw "Seeded artifact imageUrl is empty"
}

Write-Host "[5/6] Validate image URL is reachable (HTTP 200)..."
try {
    $imgResp = Invoke-WebRequest -UseBasicParsing -Method Get -Uri $target.imageUrl -TimeoutSec 20
    if ($imgResp.StatusCode -ne 200) {
        throw "Image URL returned status $($imgResp.StatusCode)"
    }
} catch {
    throw "Image URL validation failed: $($_.Exception.Message)"
}

Write-Host "[6/6] Validate dialog with detect context (network search path, non-stub)..."
$dialogPayload = @{
    query = "what is this"
    artifactId = $target.id
    contextHistory = @(
        @{ role = "SYSTEM"; content = "[detect_context]\nqingtong shenshu bronze tree" }
    )
} | ConvertTo-Json -Depth 8

$dialogResp = Invoke-RestMethod -Method Post -Uri "$JavaBase/api/v1/dialog/requests" -Headers $auth -Body $dialogPayload
$ai = [string]$dialogResp.data.aiResponse
if (-not $ai) {
    throw "Dialog aiResponse is empty"
}
if ($ai -match "降级回答|stub-safe|LLM_STUB|本地降级") {
    throw "Dialog still falls back to stub: $ai"
}

Write-Host "[Done] Build E2E summary..."
$summary = [ordered]@{
    ok = $true
    user = $username
    artifactId = $target.id
    artifactName = $target.name
    artifactImageUrl = $target.imageUrl
    dialogAnswerPreview = ($ai.Substring(0, [Math]::Min(120, $ai.Length)))
}

$summary | ConvertTo-Json -Depth 6
