#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$SCRIPT_DIR/common.sh"
. "$SCRIPT_DIR/backup-lifecycle.sh"

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

compose exec -T postgres dropdb --if-exists -U "$GESTIONALE_DB_USERNAME" "$POSTGRES_DB"
compose exec -T postgres createdb -U "$GESTIONALE_DB_USERNAME" "$POSTGRES_DB"
compose exec -T postgres pg_restore \
  -U "$GESTIONALE_DB_USERNAME" \
  -d "$POSTGRES_DB" \
  --clean \
  --if-exists \
  --no-owner \
  --no-privileges \
  < "$backup_file"

compose exec -T postgres psql \
  -U "$GESTIONALE_DB_USERNAME" \
  -d "$POSTGRES_DB" \
  -v ON_ERROR_STOP=1 \
  -c "select 1" >/dev/null

if [ "${RESTART_APP_SERVICES:-true}" = "true" ]; then
  compose up -d backend frontend >/dev/null
fi

echo "Restore completato da $backup_file"
