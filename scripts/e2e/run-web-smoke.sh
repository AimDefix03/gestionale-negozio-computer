#!/bin/sh
set -eu

: "${E2E_USERNAME:?Set E2E_USERNAME with a dedicated E2E super admin username}"
: "${E2E_PASSWORD:?Set E2E_PASSWORD with a dedicated E2E super admin password}"
: "${GESTIONALE_E2E_DB_PASSWORD:?Set GESTIONALE_E2E_DB_PASSWORD with a dedicated E2E database password}"

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
COMPOSE_FILE="$ROOT_DIR/docker-compose.prod-like.yml"
SECRETS_COMPOSE_FILE="$ROOT_DIR/docker-compose.secrets.yml"
BOOTSTRAP_SECRET_COMPOSE_FILE="$ROOT_DIR/docker-compose.bootstrap-secret.yml"
PROJECT_NAME=gestionale-e2e
SECRET_DIR=$(mktemp -d "${TMPDIR:-/tmp}/gestionale-e2e-secrets.XXXXXX")

printf '%s' "$GESTIONALE_E2E_DB_PASSWORD" >"$SECRET_DIR/database-password"
printf '%s' "$E2E_PASSWORD" >"$SECRET_DIR/bootstrap-password"
chmod 0700 "$SECRET_DIR"
chmod 0444 "$SECRET_DIR/database-password" "$SECRET_DIR/bootstrap-password"

export POSTGRES_DB=gestionale_e2e
export GESTIONALE_DB_USERNAME=gestionale_e2e
export GESTIONALE_DB_PASSWORD=
export GESTIONALE_DB_PASSWORD_FILE=
export GESTIONALE_DB_PASSWORD_SECRET_FILE="$SECRET_DIR/database-password"
export GESTIONALE_BOOTSTRAP_SUPER_ADMIN_ENABLED=true
export GESTIONALE_BOOTSTRAP_SUPER_ADMIN_USERNAME=$E2E_USERNAME
export GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD=
export GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD_FILE=
export GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD_SECRET_FILE="$SECRET_DIR/bootstrap-password"
export GESTIONALE_POSTGRES_CONTAINER_NAME=gestionale-e2e-postgres
export GESTIONALE_BACKEND_CONTAINER_NAME=gestionale-e2e-backend
export GESTIONALE_FRONTEND_CONTAINER_NAME=gestionale-e2e-frontend
export GESTIONALE_POSTGRES_PORT=55433
export GESTIONALE_BACKEND_PORT=18080
export GESTIONALE_MANAGEMENT_PORT=19090
export GESTIONALE_FRONTEND_PORT=18081
export PLAYWRIGHT_BASE_URL=http://127.0.0.1:18081

compose() {
  docker compose -p "$PROJECT_NAME" \
    -f "$COMPOSE_FILE" \
    -f "$SECRETS_COMPOSE_FILE" \
    -f "$BOOTSTRAP_SECRET_COMPOSE_FILE" \
    "$@"
}

stop_stack() {
  compose down --volumes --remove-orphans >/dev/null 2>&1 || true
}

cleanup() {
  stop_stack
  rm -rf "$SECRET_DIR"
}

trap cleanup EXIT INT TERM

stop_stack
compose up -d --build postgres backend

attempt=0
until compose exec -T backend wget -qO- "http://127.0.0.1:$GESTIONALE_MANAGEMENT_PORT/actuator/health/readiness" | grep -q UP; do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 40 ]; then
    compose logs --no-color backend
    exit 1
  fi
  sleep 3
done

compose exec -T backend wget -qO- "http://127.0.0.1:$GESTIONALE_MANAGEMENT_PORT/actuator/health/liveness" | grep -q UP
sh "$ROOT_DIR/scripts/security/verify-actuator-exposure.sh" http://127.0.0.1:18080
sh "$ROOT_DIR/scripts/security/verify-container-secrets.sh" "$GESTIONALE_POSTGRES_CONTAINER_NAME" "$GESTIONALE_BACKEND_CONTAINER_NAME"

compose up -d --build frontend

attempt=0
until curl -fsS http://127.0.0.1:18081/health | grep -q ok; do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 20 ]; then
    compose logs --no-color frontend
    exit 1
  fi
  sleep 2
done

sh "$ROOT_DIR/scripts/security/verify-container-hardening.sh" \
  "$GESTIONALE_POSTGRES_CONTAINER_NAME" \
  "$GESTIONALE_BACKEND_CONTAINER_NAME" \
  "$GESTIONALE_FRONTEND_CONTAINER_NAME"
sh "$ROOT_DIR/scripts/security/verify-browser-security.sh" "$PLAYWRIGHT_BASE_URL"

cd "$ROOT_DIR/web/frontend"
npm ci
npx playwright install chromium
npm run typecheck:e2e
npm run test:e2e

"$ROOT_DIR/scripts/security/verify-login-rate-limit.sh" "$PLAYWRIGHT_BASE_URL"
compose logs --no-color frontend | grep -q 'limit_req=REJECTED'
