#!/usr/bin/env bash
set -euo pipefail

BASE="http://127.0.0.1:28080"
USER_NAME="e2e_user_0315"
PASS_WORD="Pass123456"

curl -sS -X POST "$BASE/api/v1/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${USER_NAME}\",\"password\":\"${PASS_WORD}\"}" >/dev/null || true

LOGIN_JSON=$(curl -sS -X POST "$BASE/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${USER_NAME}\",\"password\":\"${PASS_WORD}\"}")

echo "LOGIN_JSON_PREVIEW=${LOGIN_JSON:0:220}"
TOKEN=$(echo "$LOGIN_JSON" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
if [[ -z "$TOKEN" ]]; then
  echo "E2E_FAIL: token not found"
  exit 1
fi

echo "TOKEN_LEN=${#TOKEN}"

DIALOG_JSON=$(curl -sS -X POST "$BASE/api/v1/dialog/requests" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"query":"请判断青铜器的大致时代并给出依据","artifactId":null,"conversationId":null,"contextHistory":[]}')

echo "DIALOG_JSON_PREVIEW=${DIALOG_JSON:0:360}"

echo "E2E_OK"
