#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$SCRIPT_DIR/common.sh"
. "$SCRIPT_DIR/backup-lifecycle.sh"
require_database_identity RESTORE

backup_file=${1:-}

if [ -z "$backup_file" ]; then
  echo "Uso: scripts/db/restore.sh percorso/backup.dump --yes" >&2
  exit 2
fi

if [ ! -s "$backup_file" ]; then
  echo "Backup non trovato o vuoto: $backup_file" >&2
  exit 2
fi

verify_backup_checksum "$backup_file"

if [ "${CONFIRM_RESTORE:-false}" != "yes" ] && [ "${2:-}" != "--yes" ]; then
  echo "Restore bloccato: aggiungi --yes oppure CONFIRM_RESTORE=yes." >&2
  echo "Attenzione: il database $POSTGRES_DB verra ricreato." >&2
  exit 2
fi

wait_for_postgres

if ! compose exec -T postgres pg_restore --list < "$backup_file" >/dev/null; then
  echo "Restore bloccato: archivio PostgreSQL non valido." >&2
  exit 1
fi

compose stop backend frontend >/dev/null 2>&1 || true

compose exec -T postgres sh -eu -c '
  password=${GESTIONALE_DB_RESTORE_PASSWORD:-}
  if [ -n "${GESTIONALE_DB_RESTORE_PASSWORD_FILE:-}" ]; then
    password=$(cat "$GESTIONALE_DB_RESTORE_PASSWORD_FILE")
  fi
  [ -n "$password" ]
  export PGPASSWORD=$password
  export PGOPTIONS="-c role=$GESTIONALE_DB_OWNER_USERNAME"
  exec dropdb -h 127.0.0.1 --if-exists -U "$GESTIONALE_DB_RESTORE_USERNAME" "$POSTGRES_DB"
'

compose exec -T postgres sh -eu -c '
  password=${GESTIONALE_DB_RESTORE_PASSWORD:-}
  if [ -n "${GESTIONALE_DB_RESTORE_PASSWORD_FILE:-}" ]; then
    password=$(cat "$GESTIONALE_DB_RESTORE_PASSWORD_FILE")
  fi
  [ -n "$password" ]
  export PGPASSWORD=$password
  exec createdb \
    -h 127.0.0.1 \
    -U "$GESTIONALE_DB_RESTORE_USERNAME" \
    -O "$GESTIONALE_DB_OWNER_USERNAME" \
    "$POSTGRES_DB"
'

compose exec -T postgres sh -eu -c '
  password=${GESTIONALE_DB_RESTORE_PASSWORD:-}
  if [ -n "${GESTIONALE_DB_RESTORE_PASSWORD_FILE:-}" ]; then
    password=$(cat "$GESTIONALE_DB_RESTORE_PASSWORD_FILE")
  fi
  [ -n "$password" ]
  export PGPASSWORD=$password
  exec pg_restore \
    -h 127.0.0.1 \
    -U "$GESTIONALE_DB_RESTORE_USERNAME" \
    -d "$POSTGRES_DB" \
    --role="$GESTIONALE_DB_OWNER_USERNAME" \
    --clean \
    --if-exists \
    --no-owner \
    --no-privileges
' < "$backup_file"

compose exec -T postgres sh -eu -c '
  password=${GESTIONALE_DB_RESTORE_PASSWORD:-}
  if [ -n "${GESTIONALE_DB_RESTORE_PASSWORD_FILE:-}" ]; then
    password=$(cat "$GESTIONALE_DB_RESTORE_PASSWORD_FILE")
  fi
  [ -n "$password" ]
  export PGPASSWORD=$password
  export PGOPTIONS="-c role=$GESTIONALE_DB_OWNER_USERNAME"
  exec psql \
    -h 127.0.0.1 \
    -U "$GESTIONALE_DB_RESTORE_USERNAME" \
    -d "$POSTGRES_DB" \
    -v ON_ERROR_STOP=1 \
    -v database_name="$POSTGRES_DB" \
    -v owner_role="$GESTIONALE_DB_OWNER_USERNAME" \
    -v migrator_role="$GESTIONALE_DB_MIGRATOR_USERNAME" \
    -v runtime_role="$GESTIONALE_DB_RUNTIME_USERNAME" \
    -v backup_role="$GESTIONALE_DB_BACKUP_USERNAME" \
    -v restore_role="$GESTIONALE_DB_RESTORE_USERNAME" \
    -f /usr/local/share/gestionale/postgresql-database-grants.sql
' >/dev/null

compose exec -T postgres sh -eu -c '
  password=${GESTIONALE_DB_RESTORE_PASSWORD:-}
  if [ -n "${GESTIONALE_DB_RESTORE_PASSWORD_FILE:-}" ]; then
    password=$(cat "$GESTIONALE_DB_RESTORE_PASSWORD_FILE")
  fi
  [ -n "$password" ]
  export PGPASSWORD=$password
  exec psql -h 127.0.0.1 -U "$GESTIONALE_DB_RESTORE_USERNAME" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -c "select 1"
' >/dev/null

if [ "${RESTART_APP_SERVICES:-true}" = "true" ]; then
  compose up -d backend frontend >/dev/null
fi

echo "Restore completato da $backup_file"
