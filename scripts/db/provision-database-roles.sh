#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$SCRIPT_DIR/common.sh"

if [ "${CONFIRM_DATABASE_ROLE_PROVISIONING:-false}" != yes ]; then
  echo "Provisioning ruoli bloccato: imposta CONFIRM_DATABASE_ROLE_PROVISIONING=yes." >&2
  exit 2
fi

for identity in BOOTSTRAP MIGRATOR RUNTIME BACKUP RESTORE; do
  require_database_identity "$identity"
done

if [ -z "${GESTIONALE_DB_OWNER_USERNAME:-}" ]; then
  echo "GESTIONALE_DB_OWNER_USERNAME mancante." >&2
  exit 2
fi

wait_for_postgres

compose exec -T postgres sh -eu -c '
  resolve_password() {
    prefix=$1
    eval "value=\${${prefix}_PASSWORD:-}"
    eval "file=\${${prefix}_PASSWORD_FILE:-}"
    if [ -n "$file" ]; then
      value=$(cat "$file")
    fi
    [ -n "$value" ]
    printf %s "$value"
  }

  migrator_password=$(resolve_password GESTIONALE_DB_MIGRATOR)
  runtime_password=$(resolve_password GESTIONALE_DB_RUNTIME)
  backup_password=$(resolve_password GESTIONALE_DB_BACKUP)
  restore_password=$(resolve_password GESTIONALE_DB_RESTORE)

  exec psql \
    --username "$POSTGRES_USER" \
    --dbname "$POSTGRES_DB" \
    --set=database_name="$POSTGRES_DB" \
    --set=owner_role="$GESTIONALE_DB_OWNER_USERNAME" \
    --set=migrator_role="$GESTIONALE_DB_MIGRATOR_USERNAME" \
    --set=migrator_password="$migrator_password" \
    --set=runtime_role="$GESTIONALE_DB_RUNTIME_USERNAME" \
    --set=runtime_password="$runtime_password" \
    --set=backup_role="$GESTIONALE_DB_BACKUP_USERNAME" \
    --set=backup_password="$backup_password" \
    --set=restore_role="$GESTIONALE_DB_RESTORE_USERNAME" \
    --set=restore_password="$restore_password" \
    --set=legacy_role="${GESTIONALE_DB_LEGACY_OWNER_USERNAME:-}" \
    --file /usr/local/share/gestionale/postgresql-roles.sql
'

echo "Ruoli PostgreSQL applicati senza esporre le credenziali."
