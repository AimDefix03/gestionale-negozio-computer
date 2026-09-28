#!/bin/sh
set -eu

: "${E2E_USERNAME:?Set E2E_USERNAME with a dedicated E2E super admin username}"
: "${E2E_PASSWORD:?Set E2E_PASSWORD with a dedicated E2E super admin password}"
: "${GESTIONALE_E2E_DB_PASSWORD:?Set GESTIONALE_E2E_DB_PASSWORD with a dedicated E2E database password}"

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
COMPOSE_FILE="$ROOT_DIR/docker-compose.prod-like.yml"
SECRETS_COMPOSE_FILE="$ROOT_DIR/docker-compose.secrets.yml"
BOOTSTRAP_SECRET_COMPOSE_FILE="$ROOT_DIR/docker-compose.bootstrap-secret.yml"
RUN_LABELS_COMPOSE_FILE="$ROOT_DIR/docker-compose.run-labels.yml"
PROJECT_PREFIX=gestionale-e2e-

. "$ROOT_DIR/scripts/ci/docker-run-safety.sh"

for command in docker openssl curl npm; do
  command -v "$command" >/dev/null 2>&1 || {
    printf 'Comando richiesto non disponibile: %s\n' "$command" >&2
    exit 1
  }
done

RANDOM_SUFFIX=$(openssl rand -hex 8)
RAW_RUN_ID=${E2E_RUN_ID:-local-$RANDOM_SUFFIX}
RUN_ID=$(printf '%s' "$RAW_RUN_ID" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9_.-' '-')
RUN_ID=${RUN_ID#-}
RUN_ID=${RUN_ID%-}
PROJECT_NAME=${E2E_PROJECT_NAME:-$PROJECT_PREFIX$RANDOM_SUFFIX}
DIAGNOSTICS_DIR=${E2E_DIAGNOSTICS_DIR:-${TMPDIR:-/tmp}/gestionale-e2e-diagnostics-$RUN_ID}
SECRET_DIR=$(mktemp -d "${TMPDIR:-/tmp}/gestionale-e2e-secrets.XXXXXX")
PROJECT_CLAIMED=0

docker_run_validate_project_name "$PROJECT_NAME" "$PROJECT_PREFIX"
docker_run_validate_id "$RUN_ID"

mkdir -p "$DIAGNOSTICS_DIR"
chmod 0700 "$SECRET_DIR"
printf '%s' "$GESTIONALE_E2E_DB_PASSWORD" >"$SECRET_DIR/database-bootstrap-password"
printf '%s' "$(openssl rand -hex 32)" >"$SECRET_DIR/database-migrator-password"
printf '%s' "$(openssl rand -hex 32)" >"$SECRET_DIR/database-runtime-password"
printf '%s' "$(openssl rand -hex 32)" >"$SECRET_DIR/database-backup-password"
printf '%s' "$(openssl rand -hex 32)" >"$SECRET_DIR/database-restore-password"
printf '%s' "$E2E_PASSWORD" >"$SECRET_DIR/bootstrap-password"
chmod 0444 "$SECRET_DIR"/*

export COMPOSE_PROJECT_NAME=$PROJECT_NAME
export GESTIONALE_RUN_ID=$RUN_ID
export POSTGRES_DB=gestionale_e2e
export GESTIONALE_DB_BOOTSTRAP_USERNAME=gestionale_e2e_bootstrap
export GESTIONALE_DB_BOOTSTRAP_PASSWORD=
export GESTIONALE_DB_BOOTSTRAP_PASSWORD_FILE=
export GESTIONALE_DB_BOOTSTRAP_PASSWORD_SECRET_FILE="$SECRET_DIR/database-bootstrap-password"
export GESTIONALE_DB_OWNER_USERNAME=gestionale_e2e_owner
export GESTIONALE_DB_MIGRATOR_USERNAME=gestionale_e2e_migrator
export GESTIONALE_DB_MIGRATOR_PASSWORD=
export GESTIONALE_DB_MIGRATOR_PASSWORD_FILE=
export GESTIONALE_DB_MIGRATOR_PASSWORD_SECRET_FILE="$SECRET_DIR/database-migrator-password"
export GESTIONALE_DB_RUNTIME_USERNAME=gestionale_e2e_runtime
export GESTIONALE_DB_RUNTIME_PASSWORD=
export GESTIONALE_DB_RUNTIME_PASSWORD_FILE=
export GESTIONALE_DB_RUNTIME_PASSWORD_SECRET_FILE="$SECRET_DIR/database-runtime-password"
export GESTIONALE_DB_BACKUP_USERNAME=gestionale_e2e_backup
export GESTIONALE_DB_BACKUP_PASSWORD=
export GESTIONALE_DB_BACKUP_PASSWORD_FILE=
export GESTIONALE_DB_BACKUP_PASSWORD_SECRET_FILE="$SECRET_DIR/database-backup-password"
export GESTIONALE_DB_RESTORE_USERNAME=gestionale_e2e_restore
export GESTIONALE_DB_RESTORE_PASSWORD=
export GESTIONALE_DB_RESTORE_PASSWORD_FILE=
export GESTIONALE_DB_RESTORE_PASSWORD_SECRET_FILE="$SECRET_DIR/database-restore-password"
export GESTIONALE_BOOTSTRAP_SUPER_ADMIN_ENABLED=true
export GESTIONALE_BOOTSTRAP_SUPER_ADMIN_USERNAME=$E2E_USERNAME
export GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD=
export GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD_FILE=
export GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD_SECRET_FILE="$SECRET_DIR/bootstrap-password"
export GESTIONALE_POSTGRES_CONTAINER_NAME="${PROJECT_NAME}-postgres"
export GESTIONALE_BACKEND_CONTAINER_NAME="${PROJECT_NAME}-backend"
export GESTIONALE_FRONTEND_CONTAINER_NAME="${PROJECT_NAME}-frontend"
export GESTIONALE_POSTGRES_PORT=${E2E_POSTGRES_PORT:-55433}
export GESTIONALE_BACKEND_PORT=${E2E_BACKEND_PORT:-18080}
export GESTIONALE_MANAGEMENT_PORT=${E2E_MANAGEMENT_PORT:-19090}
export GESTIONALE_FRONTEND_PORT=${E2E_FRONTEND_PORT:-18081}
export PLAYWRIGHT_BASE_URL="http://127.0.0.1:$GESTIONALE_FRONTEND_PORT"

compose() {
  docker compose -p "$PROJECT_NAME" \
    -f "$COMPOSE_FILE" \
    -f "$SECRETS_COMPOSE_FILE" \
    -f "$BOOTSTRAP_SECRET_COMPOSE_FILE" \
    -f "$RUN_LABELS_COMPOSE_FILE" \
    "$@"
}

capture_diagnostics() {
  mkdir -p "$DIAGNOSTICS_DIR"
  {
    printf 'run_id=%s\n' "$RUN_ID"
    printf 'compose_project=%s\n' "$PROJECT_NAME"
    printf 'captured_at=%s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  } >"$DIAGNOSTICS_DIR/metadata.txt"

  if [ "$PROJECT_CLAIMED" -eq 1 ]; then
    compose ps -a >"$DIAGNOSTICS_DIR/compose-ps.txt" 2>&1 || true
    compose logs --no-color >"$DIAGNOSTICS_DIR/compose.log" 2>&1 || true
    docker_run_inventory "$PROJECT_NAME" "$RUN_ID" "$DIAGNOSTICS_DIR/resources.txt" || true
  fi
}

cleanup() {
  status=$?
  cleanup_status=0
  trap - EXIT HUP INT TERM
  capture_diagnostics

  if [ "$PROJECT_CLAIMED" -eq 1 ]; then
    docker_run_cleanup \
      "$PROJECT_NAME" \
      "$RUN_ID" \
      "$DIAGNOSTICS_DIR/resources-before-cleanup.txt" \
      >"$DIAGNOSTICS_DIR/cleanup.log" 2>&1 || cleanup_status=$?
    sh "$ROOT_DIR/scripts/ci/verify-prod-like-cleanup.sh" \
      "$PROJECT_NAME" \
      "$RUN_ID" \
      >>"$DIAGNOSTICS_DIR/cleanup.log" 2>&1 || cleanup_status=$?
  else
    printf 'Cleanup Docker non necessario: progetto non acquisito.\n' \
      >"$DIAGNOSTICS_DIR/cleanup.log"
  fi

  rm -f "$SECRET_DIR"/*
  rmdir "$SECRET_DIR" 2>/dev/null || cleanup_status=1

  if [ "$status" -eq 0 ] && [ "$cleanup_status" -ne 0 ]; then
    status=$cleanup_status
  fi
  exit "$status"
}

handle_signal() {
  exit $((128 + $1))
}

trap cleanup EXIT
trap 'handle_signal 1' HUP
trap 'handle_signal 2' INT
trap 'handle_signal 15' TERM

docker_run_assert_project_unused "$PROJECT_NAME"
PROJECT_CLAIMED=1
compose config --quiet
compose up -d --build postgres backend

attempt=0
until compose exec -T backend wget -qO- \
  "http://127.0.0.1:$GESTIONALE_MANAGEMENT_PORT/actuator/health/readiness" | grep -q UP
do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 40 ]; then
    compose logs --no-color backend
    exit 1
  fi
  sleep 3
done

compose exec -T backend wget -qO- \
  "http://127.0.0.1:$GESTIONALE_MANAGEMENT_PORT/actuator/health/liveness" | grep -q UP
sh "$ROOT_DIR/scripts/security/verify-actuator-exposure.sh" \
  "http://127.0.0.1:$GESTIONALE_BACKEND_PORT"
sh "$ROOT_DIR/scripts/security/verify-container-secrets.sh" \
  "$GESTIONALE_POSTGRES_CONTAINER_NAME" \
  "$GESTIONALE_BACKEND_CONTAINER_NAME"

compose up -d --build frontend

attempt=0
until curl -fsS "$PLAYWRIGHT_BASE_URL/health" | grep -q ok; do
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
npx playwright install chromium firefox webkit
npm run typecheck:e2e
npm run test:e2e

sh "$ROOT_DIR/scripts/security/verify-login-rate-limit.sh" "$PLAYWRIGHT_BASE_URL"
sh "$ROOT_DIR/scripts/security/verify-registration-rate-limit.sh" "$PLAYWRIGHT_BASE_URL"
compose logs --no-color frontend | grep -q 'limit_req=REJECTED'

printf 'Smoke test E2E completato per il progetto %s (run %s).\n' "$PROJECT_NAME" "$RUN_ID"
