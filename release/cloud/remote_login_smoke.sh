#!/usr/bin/env bash
set -euo pipefail
BASE="http://127.0.0.1:28080"
for u in admin arc_li user; do
  resp=$(curl -sS -X POST "$BASE/api/v1/auth/login" -H 'Content-Type: application/json' -d "{\"username\":\"$u\",\"password\":\"123456\"}")
  echo "[$u] $resp"
done
