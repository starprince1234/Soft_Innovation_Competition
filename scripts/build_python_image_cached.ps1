param(
    [string]$ImageTag = "jigu-python:0.1.0",
    [string]$CacheDir = ".docker-buildx-cache"
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$pythonDir = Join-Path $projectRoot "JiGuYunYu_Python"
$cachePath = Join-Path $projectRoot $CacheDir

if (-not (Test-Path $cachePath)) {
    New-Item -ItemType Directory -Path $cachePath | Out-Null
}

Write-Host "[1/3] Ensure buildx builder exists..."
$builder = docker buildx ls | Select-String "jigu-builder"
if (-not $builder) {
    docker buildx create --name jigu-builder --use | Out-Null
} else {
    docker buildx use jigu-builder
}

Write-Host "[2/3] Build Python image with persistent local cache..."
docker buildx build `
  --load `
  --file "$pythonDir/Dockerfile" `
  --cache-from "type=local,src=$cachePath" `
  --cache-to "type=local,dest=$cachePath,mode=max" `
  --tag "$ImageTag" `
  "$pythonDir"

if ($LASTEXITCODE -ne 0) {
        throw "docker buildx build failed with exit code $LASTEXITCODE"
}

Write-Host "[3/3] Done. Rebuilds will reuse cached pip layers unless dependencies change."
