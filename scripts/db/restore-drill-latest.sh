#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
. "$SCRIPT_DIR/backup-lifecycle.sh"

ENV_FILE=${ENV_FILE:-"$PROJECT_ROOT/.env.docker"}
if [ -f "$ENV_FILE" ]; then
  set -a
  . "$ENV_FILE"
  set +a
fi

source_database=${POSTGRES_DB:-gestionale}
source_backup_dir=${BACKUP_DIR:-"$PROJECT_ROOT/backups"}
case "$source_backup_dir" in
  /*) ;;
  *) source_backup_dir="$PROJECT_ROOT/$source_backup_dir" ;;
esac
safe_source_database=$(printf '%s' "$source_database" | tr -c 'A-Za-z0-9_-' '_')
source_prefix=${BACKUP_FILE_PREFIX:-"gestionale_${safe_source_database}_"}
backup_file=$(latest_backup "$source_backup_dir" "$source_prefix")

if [ -z "$backup_file" ] || [ ! -s "$backup_file" ]; then
  echo "Restore drill non avviato: nessun backup reale disponibile." >&2
  exit 1
fi

BACKUP_DIR="$source_backup_dir" \
POSTGRES_DB="$source_database" \
BACKUP_FILE_PREFIX="$source_prefix" \
BACKUP_MAX_AGE_HOURS="${BACKUP_MAX_AGE_HOURS:-26}" \
  "$SCRIPT_DIR/check-backup-freshness.sh" "$backup_file" >/dev/null

work_dir=$(mktemp -d "${TMPDIR:-/tmp}/gestionale-restore-drill.XXXXXX")
for identity in bootstrap migrator runtime backup restore; do
  printf '%s' "$(openssl rand -hex 32)" > "$work_dir/database-$identity-password"
done
chmod 0444 "$work_dir"/database-*-password
timestamp=$(date -u +"%Y%m%d%H%M%S")

export SKIP_ENV_FILE=true
export COMPOSE_PROJECT_NAME="gestionale-restore-drill-$timestamp"
export COMPOSE_FILE="$PROJECT_ROOT/docker-compose.prod-like.yml"
export COMPOSE_OVERRIDE_FILE="$PROJECT_ROOT/docker-compose.secrets.yml"
export POSTGRES_DB=gestionale_restore_drill
export GESTIONALE_DB_BOOTSTRAP_USERNAME=gestionale_restore_drill_bootstrap
export GESTIONALE_DB_BOOTSTRAP_PASSWORD=
export GESTIONALE_DB_BOOTSTRAP_PASSWORD_FILE=
export GESTIONALE_DB_BOOTSTRAP_PASSWORD_SECRET_FILE="$work_dir/database-bootstrap-password"
export GESTIONALE_DB_OWNER_USERNAME=gestionale_restore_drill_owner
export GESTIONALE_DB_MIGRATOR_USERNAME=gestionale_restore_drill_migrator
export GESTIONALE_DB_MIGRATOR_PASSWORD=
export GESTIONALE_DB_MIGRATOR_PASSWORD_FILE=
export GESTIONALE_DB_MIGRATOR_PASSWORD_SECRET_FILE="$work_dir/database-migrator-password"
export GESTIONALE_DB_RUNTIME_USERNAME=gestionale_restore_drill_runtime
export GESTIONALE_DB_RUNTIME_PASSWORD=
export GESTIONALE_DB_RUNTIME_PASSWORD_FILE=
export GESTIONALE_DB_RUNTIME_PASSWORD_SECRET_FILE="$work_dir/database-runtime-password"
export GESTIONALE_DB_BACKUP_USERNAME=gestionale_restore_drill_backup
export GESTIONALE_DB_BACKUP_PASSWORD=
export GESTIONALE_DB_BACKUP_PASSWORD_FILE=
export GESTIONALE_DB_BACKUP_PASSWORD_SECRET_FILE="$work_dir/database-backup-password"
export GESTIONALE_DB_RESTORE_USERNAME=gestionale_restore_drill_restore
export GESTIONALE_DB_RESTORE_PASSWORD=
export GESTIONALE_DB_RESTORE_PASSWORD_FILE=
export GESTIONALE_DB_RESTORE_PASSWORD_SECRET_FILE="$work_dir/database-restore-password"
export GESTIONALE_POSTGRES_PORT=${RESTORE_DRILL_POSTGRES_PORT:-55434}
export GESTIONALE_BACKEND_PORT=18082
export GESTIONALE_FRONTEND_PORT=18083
export GESTIONALE_POSTGRES_CONTAINER_NAME="${COMPOSE_PROJECT_NAME}-postgres"
export GESTIONALE_BACKEND_CONTAINER_NAME="${COMPOSE_PROJECT_NAME}-backend"
export GESTIONALE_FRONTEND_CONTAINER_NAME="${COMPOSE_PROJECT_NAME}-frontend"
export RESTART_APP_SERVICES=false
export RESTORE_DRILL_MAX_SECONDS=${RESTORE_DRILL_MAX_SECONDS:-900}

. "$SCRIPT_DIR/common.sh"
validate_positive_integer RESTORE_DRILL_MAX_SECONDS "$RESTORE_DRILL_MAX_SECONDS"

cleanup() {
  compose down -v >/dev/null 2>&1 || true
  rm -rf "$work_dir"
}

trap cleanup EXIT INT TERM

started=$(date +%s)
compose up -d postgres >/dev/null
wait_for_postgres
CONFIRM_RESTORE=yes SKIP_ENV_FILE=true RESTART_APP_SERVICES=false "$SCRIPT_DIR/restore.sh" "$backup_file" >/dev/null

for table in flyway_schema_history products user_accounts customer_orders fiscal_documents; do
  exists=$(compose exec -T postgres sh -eu -c '
    export PGPASSWORD=$(cat "$GESTIONALE_DB_BACKUP_PASSWORD_FILE")
    exec psql -h 127.0.0.1 -U "$GESTIONALE_DB_BACKUP_USERNAME" -d "$POSTGRES_DB" -tA -v ON_ERROR_STOP=1 -c "$1"
  ' sh "select to_regclass('public.$table') is not null;")
  if [ "$exists" != t ]; then
    echo "Restore drill fallito: tabella $table assente." >&2
    exit 1
  fi
done

expected_version=$(find "$PROJECT_ROOT/web/backend/src/main/resources/db/migration" -maxdepth 1 -type f -name 'V*__*.sql' -print |
  sed -E 's#^.*/V([0-9]+)__.*#\1#' | sort -n | tail -n 1)
restored_version=$(compose exec -T postgres sh -eu -c '
  export PGPASSWORD=$(cat "$GESTIONALE_DB_BACKUP_PASSWORD_FILE")
  exec psql -h 127.0.0.1 -U "$GESTIONALE_DB_BACKUP_USERNAME" -d "$POSTGRES_DB" -tA -v ON_ERROR_STOP=1 -c "select max(cast(version as integer)) from flyway_schema_history where success;"
')

if [ -z "$expected_version" ] || [ "$restored_version" != "$expected_version" ]; then
  echo "Restore drill fallito: versione Flyway non allineata." >&2
  exit 1
fi

elapsed=$(($(date +%s) - started))
if [ "$elapsed" -gt "$RESTORE_DRILL_MAX_SECONDS" ]; then
  echo "Restore drill oltre il limite RTO configurato." >&2
  exit 1
fi

echo "Restore drill completato dall'ultimo backup reale in ${elapsed}s."
