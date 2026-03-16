param(
    [string]$EnvFile = "d:\VScodeProjects\JiGuYunYu_3_8_anzhuoliantiao\.env.backend",
    [string]$ImagePath = "",
    [string]$TextPath = "",
    [string]$ArtifactName = "E2E-Artifact-QingTongShenShu",
    [string]$Era = "Shang",
    [string]$Location = "SanXingDui",
    [string]$Tags = "test,qingtong-shenshu,homepage",
    [string]$Status = "APPROVED",
    [string]$PythonExe = "d:/VScodeProjects/JiGuYunYu_3_8_anzhuoliantiao/.venv/Scripts/python.exe",
    [string]$ImageUrlOverride = ""
)

$ErrorActionPreference = "Stop"

function Resolve-AssetPath([string]$CandidatePath, [string]$Pattern) {
    if ($CandidatePath -and (Test-Path -LiteralPath $CandidatePath)) {
        return (Resolve-Path -LiteralPath $CandidatePath).Path
    }
    $assetsDir = Join-Path (Get-Location) "assets"
    if (-not (Test-Path -LiteralPath $assetsDir)) {
        throw "Assets directory not found: $assetsDir"
    }
    $match = Get-ChildItem -LiteralPath $assetsDir -File -Filter $Pattern | Sort-Object Name | Select-Object -First 1
    if (-not $match) {
        throw "No asset matched pattern '$Pattern' under $assetsDir"
    }
    return $match.FullName
}

function Import-DotEnv([string]$Path) {
    if (-not (Test-Path $Path)) {
        throw "Env file not found: $Path"
    }
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) { return }
        $parts = $line.Split("=", 2)
        if ($parts.Count -ne 2) { return }
        $name = $parts[0].Trim()
        $value = $parts[1].Trim()
        if ($name) { Set-Item -Path "Env:$name" -Value $value }
    }
}

function Escape-Sql([string]$value) {
    if ($null -eq $value) { return "" }
    $escaped = $value -replace "\\", "\\\\"
    $escaped = $escaped -replace "'", "''"
    $escaped = $escaped -replace "`r`n", "\n"
    $escaped = $escaped -replace "`n", "\n"
    return $escaped
}

Import-DotEnv -Path $EnvFile

$ImagePath = Resolve-AssetPath -CandidatePath $ImagePath -Pattern "*.png"
$TextPath = Resolve-AssetPath -CandidatePath $TextPath -Pattern "*.txt"

if (-not (Test-Path -LiteralPath $ImagePath)) { throw "Image not found: $ImagePath" }
if (-not (Test-Path -LiteralPath $TextPath)) { throw "Text not found: $TextPath" }

$descriptionRaw = Get-Content -Path $TextPath -Raw -Encoding UTF8
$description = Escape-Sql $descriptionRaw
$nameEsc = Escape-Sql $ArtifactName
$eraEsc = Escape-Sql $Era
$locEsc = Escape-Sql $Location
$tagsEsc = Escape-Sql $Tags
$statusEsc = Escape-Sql $Status

$bucket = $env:BOS_BUCKET
if (-not $bucket) { throw "BOS_BUCKET missing in env" }

$ossEndpoint = $env:BOS_ENDPOINT
if (-not $ossEndpoint) { throw "BOS_ENDPOINT missing in env" }

function Invoke-OssRoundTrip([string]$Endpoint) {
    return (& $PythonExe .\scripts\oss_roundtrip_test.py `
      --image-path "$ImagePath" `
      --endpoint "$Endpoint" `
      --access-key "$env:BOS_ACCESS_KEY" `
      --secret-key "$env:BOS_SECRET_KEY" `
      --bucket "$bucket" `
      --keep-object)
}

Write-Host "[1/3] Upload image to object storage via OSS roundtrip tool..."
$imageUrl = ""
if ($ImageUrlOverride) {
    $imageUrl = $ImageUrlOverride
    Write-Host "[1/3] Skip OSS direct upload, using provided image URL override."
} else {
    $ossRaw = Invoke-OssRoundTrip -Endpoint $ossEndpoint
    $ossResult = $null
    if ($LASTEXITCODE -eq 0) {
        $ossResult = $ossRaw | ConvertFrom-Json
    }

    if (($LASTEXITCODE -ne 0) -or (-not $ossResult) -or (-not $ossResult.ok)) {
        $fallbackEndpoint = $null
        if ($ossEndpoint -like "*-internal.*") {
            $fallbackEndpoint = $ossEndpoint -replace "-internal", ""
        }
        if ($fallbackEndpoint) {
            Write-Host "[1/3] Internal endpoint failed, retrying with public endpoint: $fallbackEndpoint"
            $ossRaw = Invoke-OssRoundTrip -Endpoint $fallbackEndpoint
            if ($LASTEXITCODE -eq 0) {
                $ossResult = $ossRaw | ConvertFrom-Json
            }
        }
    }

    if (($LASTEXITCODE -ne 0) -or (-not $ossResult) -or (-not $ossResult.ok)) {
        throw "oss_roundtrip_test failed after retry: $ossRaw"
    }
    $imageUrl = [string]$ossResult.url
}
$imageUrlEsc = Escape-Sql $imageUrl

Write-Host "[2/3] Upsert artifact into MySQL as APPROVED for homepage..."
$mysqlUser = if ($env:MYSQL_USER) { $env:MYSQL_USER } else { "root" }
$mysqlPwd = if ($env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD } else { $env:MYSQL_ROOT_PASSWORD }
$mysqlDb = $env:MYSQL_DATABASE
if (-not $mysqlPwd) { throw "MYSQL_PASSWORD / MYSQL_ROOT_PASSWORD missing in env" }
if (-not $mysqlDb) { throw "MYSQL_DATABASE missing in env" }

$sql = @"
INSERT INTO artifacts (name, description, image_url, thumbnail_url, tags, location, era, status)
VALUES ('$nameEsc', '$description', '$imageUrlEsc', '$imageUrlEsc', '$tagsEsc', '$locEsc', '$eraEsc', '$statusEsc')
ON DUPLICATE KEY UPDATE
  description = VALUES(description),
  image_url = VALUES(image_url),
  thumbnail_url = VALUES(thumbnail_url),
  tags = VALUES(tags),
  location = VALUES(location),
  era = VALUES(era),
  status = VALUES(status),
  updated_at = CURRENT_TIMESTAMP;
"@

& docker exec jigu-mysql mysql "-u$mysqlUser" "-p$mysqlPwd" "-D" "$mysqlDb" "-e" "$sql" | Out-Null

Write-Host "[3/3] Verify artifact exists in DB..."
$verifySql = "SELECT id,name,status,image_url FROM artifacts WHERE name='$nameEsc' LIMIT 1;"
$verifyOut = & docker exec jigu-mysql mysql "-u$mysqlUser" "-p$mysqlPwd" "-D" "$mysqlDb" "-e" "$verifySql"

$result = [ordered]@{
    ok = $true
    artifactName = $ArtifactName
    imageUrl = $imageUrl
    verify = ($verifyOut -join "`n")
}

$result | ConvertTo-Json -Depth 4
