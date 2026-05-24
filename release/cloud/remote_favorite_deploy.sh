#!/usr/bin/env bash
set -euo pipefail

cd /root/jigu-cloud/release/cloud

MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-}"
if [ -z "$MYSQL_ROOT_PASSWORD" ] && [ -f .env.prod ]; then
  MYSQL_ROOT_PASSWORD="$(grep -E '^MYSQL_ROOT_PASSWORD=' .env.prod | tail -n 1 | cut -d= -f2-)"
fi
if [ -z "$MYSQL_ROOT_PASSWORD" ]; then
  echo "MYSQL_ROOT_PASSWORD is required via environment or .env.prod" >&2
  exit 1
fi

docker exec -i jigu-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" jigu < /root/jigu-cloud/release/cloud/migrate_user_favorites.sql

echo "[check] user_favorites table count:" 
docker exec jigu-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -Nse "SELECT COUNT(1) FROM information_schema.tables WHERE table_schema='jigu' AND table_name='user_favorites';"

sed -i 's/^JIGU_JAVA_IMAGE=.*/JIGU_JAVA_IMAGE=jigu-java:prod-20260324-fav01/' .env.prod

echo "[deploy] restarting jigu-java only"
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d jigu-java

echo "[check] jigu-java image"
docker ps --filter name=jigu-java --format '{{.Names}} {{.Image}} {{.Status}}'

echo "[check] health endpoint"
curl -fsS http://127.0.0.1:28080/api/v1/health
