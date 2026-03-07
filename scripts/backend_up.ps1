$ErrorActionPreference = 'Stop'

Set-Location (Join-Path $PSScriptRoot '..')

if (-not (Test-Path '.env.backend')) {
  Copy-Item '.env.backend.example' '.env.backend'
  Write-Host 'Generated .env.backend. Please edit it if needed before startup.'
}

docker compose --env-file .env.backend -f docker-compose.backend.yml up -d

docker compose --env-file .env.backend -f docker-compose.backend.yml ps
