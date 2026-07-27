#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
COMPOSE_FILE="$ROOT_DIR/docker-compose.prod-like.yml"
SECRETS_COMPOSE_FILE="$ROOT_DIR/docker-compose.secrets.yml"
BOOTSTRAP_SECRET_COMPOSE_FILE="$ROOT_DIR/docker-compose.bootstrap-secret.yml"
RAW_RUN_ID=${PRODLIKE_RUN_ID:-${GITHUB_RUN_ID:-$$}-${GITHUB_RUN_ATTEMPT:-1}}
RUN_ID=$(printf '%s' "$RAW_RUN_ID" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9_.-' '-')
RUN_ID=${RUN_ID#-}
RUN_ID=${RUN_ID%-}

if [ -z "$RUN_ID" ]; then
  RUN_ID=$$
fi

PROJECT_NAME=${PRODLIKE_PROJECT_NAME:-gestionale-recurring-$RUN_ID}
DIAGNOSTICS_DIR=${PRODLIKE_DIAGNOSTICS_DIR:-${TMPDIR:-/tmp}/gestionale-prodlike-diagnostics-$RUN_ID}
SECRET_DIR=$(mktemp -d "${TMPDIR:-/tmp}/gestionale-prodlike-secrets.XXXXXX")
DATABASE_PASSWORD=$(openssl rand -hex 32)
BOOTSTRAP_PASSWORD="Aa1!$(openssl rand -hex 30)"

mkdir -p "$DIAGNOSTICS_DIR"
chmod 0700 "$SECRET_DIR"
printf '%s' "$DATABASE_PASSWORD" >"$SECRET_DIR/database-password"
printf '%s' "$BOOTSTRAP_PASSWORD" >"$SECRET_DIR/bootstrap-password"
chmod 0444 "$SECRET_DIR/database-password" "$SECRET_DIR/bootstrap-password"

if [ -n "${GITHUB_ACTIONS:-}" ]; then
  printf '::add-mask::%s\n' "$DATABASE_PASSWORD"
  printf '::add-mask::%s\n' "$BOOTSTRAP_PASSWORD"
fi

export COMPOSE_PROJECT_NAME=$PROJECT_NAME
export POSTGRES_DB=gestionale_recurring
export GESTIONALE_DB_USERNAME=gestionale_recurring
export GESTIONALE_DB_PASSWORD=
export GESTIONALE_DB_PASSWORD_FILE=
export GESTIONALE_DB_PASSWORD_SECRET_FILE="$SECRET_DIR/database-password"
export GESTIONALE_BOOTSTRAP_SUPER_ADMIN_ENABLED=true
export GESTIONALE_BOOTSTRAP_SUPER_ADMIN_USERNAME=recurring_super_admin
export GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD=
export GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD_FILE=
export GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD_SECRET_FILE="$SECRET_DIR/bootstrap-password"
export GESTIONALE_POSTGRES_CONTAINER_NAME="${PROJECT_NAME}-postgres"
export GESTIONALE_BACKEND_CONTAINER_NAME="${PROJECT_NAME}-backend"
export GESTIONALE_FRONTEND_CONTAINER_NAME="${PROJECT_NAME}-frontend"
export GESTIONALE_PROMETHEUS_CONTAINER_NAME="${PROJECT_NAME}-prometheus"
export GESTIONALE_POSTGRES_PORT=${PRODLIKE_POSTGRES_PORT:-55434}
export GESTIONALE_BACKEND_PORT=${PRODLIKE_BACKEND_PORT:-28080}
export GESTIONALE_MANAGEMENT_PORT=9090
export GESTIONALE_FRONTEND_PORT=${PRODLIKE_FRONTEND_PORT:-28081}
export GESTIONALE_PROMETHEUS_PORT=${PRODLIKE_PROMETHEUS_PORT:-29091}
export BACKUP_VERIFY_POSTGRES_PORT=${PRODLIKE_BACKUP_VERIFY_POSTGRES_PORT:-55433}
export RESTORE_DRILL_POSTGRES_PORT=${PRODLIKE_RESTORE_DRILL_POSTGRES_PORT:-55435}
export E2E_USERNAME=$GESTIONALE_BOOTSTRAP_SUPER_ADMIN_USERNAME
export E2E_PASSWORD=$BOOTSTRAP_PASSWORD
export PLAYWRIGHT_BASE_URL="http://127.0.0.1:$GESTIONALE_FRONTEND_PORT"

compose() {
  docker compose \
    -p "$PROJECT_NAME" \
    -f "$COMPOSE_FILE" \
    -f "$SECRETS_COMPOSE_FILE" \
    -f "$BOOTSTRAP_SECRET_COMPOSE_FILE" \
    --profile observability \
    "$@"
}

capture_diagnostics() {
  mkdir -p "$DIAGNOSTICS_DIR"
  {
    printf 'run_id=%s\n' "$RUN_ID"
    printf 'compose_project=%s\n' "$PROJECT_NAME"
    printf 'captured_at=%s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
    printf 'docker_version=%s\n' "$(docker version --format '{{.Server.Version}}' 2>/dev/null || printf unavailable)"
    printf 'compose_version=%s\n' "$(docker compose version --short 2>/dev/null || printf unavailable)"
  } >"$DIAGNOSTICS_DIR/metadata.txt"
  compose ps -a >"$DIAGNOSTICS_DIR/compose-ps.txt" 2>&1 || true
  compose logs --no-color >"$DIAGNOSTICS_DIR/compose.log" 2>&1 || true
  docker image inspect \
    gestionale-postgres:prod-like \
    gestionale-api:prod-like \
    gestionale-frontend:prod-like \
    prom/prometheus:v3.13.1 \
    >"$DIAGNOSTICS_DIR/images.json" 2>&1 || true

  if [ -d "$ROOT_DIR/web/frontend/playwright-report" ]; then
    cp -R "$ROOT_DIR/web/frontend/playwright-report" "$DIAGNOSTICS_DIR/"
  fi
  if [ -d "$ROOT_DIR/web/frontend/test-results" ]; then
    cp -R "$ROOT_DIR/web/frontend/test-results" "$DIAGNOSTICS_DIR/"
  fi
}

cleanup() {
  STATUS=$?
  CLEANUP_STATUS=0
  trap - EXIT INT TERM
  capture_diagnostics
  compose down -v --remove-orphans >"$DIAGNOSTICS_DIR/cleanup.log" 2>&1 || CLEANUP_STATUS=$?
  rm -rf "$SECRET_DIR"
  sh "$ROOT_DIR/scripts/ci/verify-prod-like-cleanup.sh" "$PROJECT_NAME" >>"$DIAGNOSTICS_DIR/cleanup.log" 2>&1 || CLEANUP_STATUS=$?

  if [ "$STATUS" -eq 0 ] && [ "$CLEANUP_STATUS" -ne 0 ]; then
    STATUS=$CLEANUP_STATUS
  fi
  exit "$STATUS"
}

trap cleanup EXIT INT TERM

SEEN_PORTS=" "
for PORT in \
  "$GESTIONALE_POSTGRES_PORT" \
  "$GESTIONALE_BACKEND_PORT" \
  "$GESTIONALE_FRONTEND_PORT" \
  "$GESTIONALE_PROMETHEUS_PORT" \
  "$BACKUP_VERIFY_POSTGRES_PORT" \
  "$RESTORE_DRILL_POSTGRES_PORT"
do
  case "$PORT" in
    ''|*[!0-9]*)
      printf 'Porta prod-like non valida: %s\n' "$PORT" >&2
      exit 1
      ;;
  esac
  case "$SEEN_PORTS" in
    *" $PORT "*)
      printf 'Le porte prod-like devono essere distinte: %s e configurata piu volte.\n' "$PORT" >&2
      exit 1
      ;;
  esac
  SEEN_PORTS="$SEEN_PORTS$PORT "
done

for command in docker openssl curl npm; do
  command -v "$command" >/dev/null 2>&1 || {
    printf 'Comando richiesto non disponibile: %s\n' "$command" >&2
    exit 1
  }
done

compose down -v --remove-orphans >/dev/null 2>&1 || true
compose config --quiet
sh "$ROOT_DIR/scripts/observability/verify-prometheus-config.sh"
compose build --pull
compose up -d postgres backend frontend

ATTEMPT=1
until compose exec -T backend wget -qO- \
  "http://127.0.0.1:$GESTIONALE_MANAGEMENT_PORT/actuator/health/readiness" |
  grep -q UP
do
  if [ "$ATTEMPT" -ge 40 ]; then
    printf 'Backend non pronto entro il tempo previsto.\n' >&2
    exit 1
  fi
  ATTEMPT=$((ATTEMPT + 1))
  sleep 3
done

compose exec -T backend wget -qO- \
  "http://127.0.0.1:$GESTIONALE_MANAGEMENT_PORT/actuator/health/liveness" |
  grep -q UP
sh "$ROOT_DIR/scripts/db/verify-flyway-migrations.sh" \
  "$GESTIONALE_POSTGRES_CONTAINER_NAME" \
  "$POSTGRES_DB" \
  "$GESTIONALE_DB_USERNAME"
sh "$ROOT_DIR/scripts/security/verify-actuator-exposure.sh" \
  "http://127.0.0.1:$GESTIONALE_BACKEND_PORT"
sh "$ROOT_DIR/scripts/security/verify-container-secrets.sh" \
  "$GESTIONALE_POSTGRES_CONTAINER_NAME" \
  "$GESTIONALE_BACKEND_CONTAINER_NAME"

ATTEMPT=1
until curl -fsS "$PLAYWRIGHT_BASE_URL/health" | grep -q ok; do
  if [ "$ATTEMPT" -ge 20 ]; then
    printf 'Frontend non pronto entro il tempo previsto.\n' >&2
    exit 1
  fi
  ATTEMPT=$((ATTEMPT + 1))
  sleep 2
done

sh "$ROOT_DIR/scripts/security/verify-container-hardening.sh" \
  "$GESTIONALE_POSTGRES_CONTAINER_NAME" \
  "$GESTIONALE_BACKEND_CONTAINER_NAME" \
  "$GESTIONALE_FRONTEND_CONTAINER_NAME"
sh "$ROOT_DIR/scripts/security/verify-browser-security.sh" "$PLAYWRIGHT_BASE_URL"

compose up -d prometheus
ATTEMPT=1
until curl -fsS "http://127.0.0.1:$GESTIONALE_PROMETHEUS_PORT/-/ready" |
  grep -q 'Prometheus Server is Ready'
do
  if [ "$ATTEMPT" -ge 30 ]; then
    printf 'Prometheus non pronto entro il tempo previsto.\n' >&2
    exit 1
  fi
  ATTEMPT=$((ATTEMPT + 1))
  sleep 2
done

ATTEMPT=1
until curl -fsS "http://127.0.0.1:$GESTIONALE_PROMETHEUS_PORT/api/v1/targets" |
  grep -q '"health":"up"'
do
  if [ "$ATTEMPT" -ge 30 ]; then
    printf 'Target backend Prometheus non disponibile entro il tempo previsto.\n' >&2
    exit 1
  fi
  ATTEMPT=$((ATTEMPT + 1))
  sleep 2
done

sh "$ROOT_DIR/scripts/observability/verify-runtime-observability.sh" \
  "$GESTIONALE_BACKEND_CONTAINER_NAME" \
  "http://127.0.0.1:$GESTIONALE_PROMETHEUS_PORT" \
  "$GESTIONALE_MANAGEMENT_PORT"
sh "$ROOT_DIR/scripts/observability/verify-prometheus-hardening.sh" \
  "$GESTIONALE_PROMETHEUS_CONTAINER_NAME"

cd "$ROOT_DIR/web/frontend"
npm ci
if [ "${PLAYWRIGHT_INSTALL_WITH_DEPS:-false}" = true ]; then
  npx playwright install --with-deps chromium
else
  npx playwright install chromium
fi
npm run typecheck:e2e
npm run test:e2e

cd "$ROOT_DIR"
sh "$ROOT_DIR/scripts/security/verify-login-rate-limit.sh" "$PLAYWRIGHT_BASE_URL"
compose logs --no-color frontend | grep -q 'limit_req=REJECTED'
sh "$ROOT_DIR/scripts/db/test-backup-lifecycle.sh"
sh "$ROOT_DIR/scripts/db/verify-backup-schedule.sh"
sh "$ROOT_DIR/scripts/db/verify-backup-restore.sh"

printf 'Verifica prod-like completata per il progetto %s.\n' "$PROJECT_NAME"
