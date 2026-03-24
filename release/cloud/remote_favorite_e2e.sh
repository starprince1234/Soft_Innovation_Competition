#!/usr/bin/env bash
set -euo pipefail

BASE="http://127.0.0.1:28080"
USER_NAME="fav_e2e_$(date +%s)"
PASS="123456"

register_payload=$(printf '{"username":"%s","password":"%s"}' "$USER_NAME" "$PASS")
curl -sS -X POST "$BASE/api/v1/auth/register" -H 'Content-Type: application/json' -d "$register_payload" >/dev/null || true

login_json=$(curl -sS -X POST "$BASE/api/v1/auth/login" -H 'Content-Type: application/json' -d "$register_payload")
TOKEN=$(echo "$login_json" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
if [ -z "$TOKEN" ]; then
  echo "[e2e] login failed: $login_json"
  exit 1
fi
AUTH="Authorization: Bearer $TOKEN"

art_json=$(curl -sS "$BASE/api/v1/artifacts?page=0&size=1")
ARTIFACT_ID=$(echo "$art_json" | sed -n 's/.*"id":\([0-9][0-9]*\).*/\1/p' | head -n1)
if [ -z "$ARTIFACT_ID" ]; then
  echo "[e2e] no artifact id found: $art_json"
  exit 1
fi

add_json=$(curl -sS -X PUT "$BASE/api/v1/favorites/$ARTIFACT_ID" -H "$AUTH")
exists_json=$(curl -sS "$BASE/api/v1/favorites/$ARTIFACT_ID/exists" -H "$AUTH")
list_json=$(curl -sS "$BASE/api/v1/favorites" -H "$AUTH")
del_json=$(curl -sS -X DELETE "$BASE/api/v1/favorites/$ARTIFACT_ID" -H "$AUTH")

echo "[e2e] user=$USER_NAME artifact=$ARTIFACT_ID"
echo "[e2e] add=$add_json"
echo "[e2e] exists=$exists_json"
echo "[e2e] list=$list_json"
echo "[e2e] delete=$del_json"
