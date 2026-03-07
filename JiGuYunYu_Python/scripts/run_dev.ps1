# run_dev.ps1 —— Windows 开发环境一键启动

# 1. 检查 .env
if (-not (Test-Path ".env")) {
    Write-Host "[WARN] .env not found, copying from .env.example ..." -ForegroundColor Yellow
    if (Test-Path ".env.example") { Copy-Item ".env.example" ".env" }
    else { Write-Host "[ERROR] .env.example not found" -ForegroundColor Red; exit 1 }
}

# 2. 加载 .env 到当前 session
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
        [System.Environment]::SetEnvironmentVariable($Matches[1].Trim(), $Matches[2].Trim(), "Process")
    }
}

# 3. 设置 PYTHONPATH
$env:PYTHONPATH = (Get-Location).Path

# 4. 启动 uvicorn
Write-Host "Starting dev server on http://0.0.0.0:8000 ..." -ForegroundColor Cyan
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
