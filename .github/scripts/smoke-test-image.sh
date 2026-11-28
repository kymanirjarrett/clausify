#!/usr/bin/env bash
# Starts the built API image against a throwaway MySQL with the prod profile, waits for UP, and makes one
# authenticated request. Every value is a throwaway made up here; the Groq key is fake and never used.
# Usage: .github/scripts/smoke-test-image.sh <image>
set -euo pipefail

IMAGE="${1:?image name required}"
NET="clausify-smoke-$$"
DB="clausify-smoke-db-$$"
API="clausify-smoke-api-$$"
PORT="${SMOKE_PORT:-18080}"

cleanup() {
  status=$?
  if [ "$status" -ne 0 ]; then
    echo "::group::API logs"; docker logs "$API" 2>&1 | tail -200 || true; echo "::endgroup::"
  fi
  docker rm -f "$API" "$DB" >/dev/null 2>&1 || true
  docker network rm "$NET" >/dev/null 2>&1 || true
  exit "$status"
}
trap cleanup EXIT

docker network create "$NET" >/dev/null
docker run -d --name "$DB" --network "$NET" \
  -e MYSQL_ROOT_PASSWORD=smoke-root -e MYSQL_DATABASE=clausify mysql:8.4 >/dev/null

echo "Waiting for MySQL..."
for _ in $(seq 1 60); do
  if docker exec "$DB" mysqladmin ping -uroot -psmoke-root --silent >/dev/null 2>&1; then break; fi
  sleep 2
done
docker exec "$DB" mysqladmin ping -uroot -psmoke-root --silent >/dev/null

docker run -d --name "$API" --network "$NET" -p "$PORT:8080" \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL="jdbc:mysql://$DB:3306/clausify" -e DB_USERNAME=root -e DB_PASSWORD=smoke-root \
  -e GROQ_API_KEY=smoke-test-not-a-real-key \
  -e JWT_SECRET="$(head -c 48 /dev/urandom | base64)" \
  -e CORS_ALLOWED_ORIGINS=http://localhost \
  "$IMAGE" >/dev/null

echo "Waiting for the API to report UP (deadline 240 s)..."
deadline=$((SECONDS + 240))
until curl -fsS "http://localhost:$PORT/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; do
  if [ "$SECONDS" -ge "$deadline" ]; then echo "API did not become healthy in time"; exit 1; fi
  if [ "$(docker inspect -f '{{.State.Running}}' "$API")" != "true" ]; then echo "API container exited"; exit 1; fi
  sleep 3
done
echo "Health is UP."

token=$(curl -fsS -H 'Content-Type: application/json' \
  -d '{"name":"Smoke Test","email":"smoke@example.com","password":"Smoke-test-1"}' \
  "http://localhost:$PORT/api/auth/register" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
[ -n "$token" ] || { echo "Register returned no token"; exit 1; }

me=$(curl -fsS -H "Authorization: Bearer $token" "http://localhost:$PORT/api/auth/me")
echo "$me" | grep -q '"email":"smoke@example.com"' || { echo "Unexpected /api/auth/me response: $me"; exit 1; }
echo "Authenticated request OK. Smoke test passed."
