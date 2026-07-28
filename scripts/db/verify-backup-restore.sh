#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
timestamp=$(date +"%Y%m%d%H%M%S")
project_name="gestionale-backup-verify-$timestamp"
work_dir=$(mktemp -d "${TMPDIR:-/tmp}/gestionale-backup-verify.XXXXXX")
secret_file="$work_dir/database-password"
printf '%s' "$(openssl rand -hex 32)" > "$secret_file"
chmod 0444 "$secret_file"

export SKIP_ENV_FILE=true
export COMPOSE_PROJECT_NAME="$project_name"
export COMPOSE_FILE="$PROJECT_ROOT/docker-compose.prod-like.yml"
export COMPOSE_OVERRIDE_FILE="$PROJECT_ROOT/docker-compose.secrets.yml"
export POSTGRES_DB="gestionale_verify"
export GESTIONALE_DB_USERNAME="gestionale_verify_user"
export GESTIONALE_DB_PASSWORD=
export GESTIONALE_DB_PASSWORD_FILE=
export GESTIONALE_DB_PASSWORD_SECRET_FILE="$secret_file"
export GESTIONALE_POSTGRES_PORT=${BACKUP_VERIFY_POSTGRES_PORT:-55433}
export GESTIONALE_BACKEND_PORT="18080"
export GESTIONALE_FRONTEND_PORT="18081"
export GESTIONALE_POSTGRES_CONTAINER_NAME="${project_name}-postgres"
export GESTIONALE_BACKEND_CONTAINER_NAME="${project_name}-backend"
export GESTIONALE_FRONTEND_CONTAINER_NAME="${project_name}-frontend"
export BACKUP_DIR="$work_dir/backups"
export RESTART_APP_SERVICES=false
export BACKUP_RETENTION_DAYS=14
export BACKUP_RETENTION_COUNT=5
export BACKUP_MAX_AGE_HOURS=1
export RESTORE_DRILL_MAX_SECONDS=${RESTORE_DRILL_MAX_SECONDS:-900}

. "$SCRIPT_DIR/common.sh"
. "$SCRIPT_DIR/backup-lifecycle.sh"

validate_positive_integer RESTORE_DRILL_MAX_SECONDS "$RESTORE_DRILL_MAX_SECONDS"
latest_migration_version=$(find "$PROJECT_ROOT/web/backend/src/main/resources/db/migration" -maxdepth 1 -type f -name 'V*__*.sql' -print |
  sed -E 's#^.*/V([0-9]+)__.*#\1#' | sort -n | tail -n 1)

if [ -z "$latest_migration_version" ]; then
  echo "Nessuna migrazione Flyway disponibile per il restore drill." >&2
  exit 1
fi

cleanup() {
  compose down -v >/dev/null 2>&1 || true
  rm -rf "$work_dir"
}

trap cleanup EXIT INT TERM

compose up -d postgres >/dev/null
wait_for_postgres
drill_started=$(date +%s)

compose exec -T postgres psql -U "$GESTIONALE_DB_USERNAME" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 <<SQL >/dev/null
create table backup_restore_probe (
  id integer primary key,
  payload text not null
);
create table flyway_schema_history (
  version varchar(50),
  success boolean not null
);
create table products (id bigint primary key);
create table user_accounts (id bigint primary key);
create table customer_orders (id bigint primary key);
create table fiscal_documents (id bigint primary key);
insert into backup_restore_probe (id, payload) values (1, 'restore-ok-$timestamp');
insert into flyway_schema_history (version, success) values ('$latest_migration_version', true);
SQL

backup_file=$(SKIP_ENV_FILE=true BACKUP_DIR="$BACKUP_DIR" "$SCRIPT_DIR/backup.sh")

SKIP_ENV_FILE=true "$SCRIPT_DIR/verify-backup.sh" "$backup_file" >/dev/null
SKIP_ENV_FILE=true BACKUP_MAX_AGE_HOURS="$BACKUP_MAX_AGE_HOURS" "$SCRIPT_DIR/check-backup-freshness.sh" "$backup_file" >/dev/null

CONFIRM_RESTORE=yes SKIP_ENV_FILE=true RESTART_APP_SERVICES=false "$SCRIPT_DIR/restore.sh" "$backup_file" >/dev/null

restored_value=$(compose exec -T postgres psql -U "$GESTIONALE_DB_USERNAME" -d "$POSTGRES_DB" -tA -c "select payload from backup_restore_probe where id = 1;")

if [ "$restored_value" != "restore-ok-$timestamp" ]; then
  echo "Verifica restore fallita: valore atteso non trovato." >&2
  exit 1
fi

restored_table_count=$(compose exec -T postgres psql -U "$GESTIONALE_DB_USERNAME" -d "$POSTGRES_DB" -tA -c "select count(*) from information_schema.tables where table_schema = 'public';")
if [ "$restored_table_count" -lt 1 ]; then
  echo "Verifica restore fallita: nessuna tabella ripristinata." >&2
  exit 1
fi

SKIP_ENV_FILE=true \
BACKUP_DIR="$BACKUP_DIR" \
POSTGRES_DB="$POSTGRES_DB" \
BACKUP_FILE_PREFIX="$BACKUP_FILE_PREFIX" \
BACKUP_MAX_AGE_HOURS="$BACKUP_MAX_AGE_HOURS" \
RESTORE_DRILL_MAX_SECONDS="$RESTORE_DRILL_MAX_SECONDS" \
RESTORE_DRILL_POSTGRES_PORT="${RESTORE_DRILL_POSTGRES_PORT:-55434}" \
  "$SCRIPT_DIR/restore-drill-latest.sh" >/dev/null

drill_elapsed=$(($(date +%s) - drill_started))
if [ "$drill_elapsed" -gt "$RESTORE_DRILL_MAX_SECONDS" ]; then
  echo "Restore drill oltre il limite RTO configurato." >&2
  exit 1
fi

echo "Backup e restore verificati correttamente in ${drill_elapsed}s."
