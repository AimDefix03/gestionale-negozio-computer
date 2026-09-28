#!/bin/sh
set -eu

ROLE_SQL=/usr/local/share/gestionale/postgresql-roles.sql

require_value() {
  name=$1
  eval "value=\${$name-}"
  if [ -z "$value" ]; then
    printf 'Configurazione PostgreSQL mancante: %s.\n' "$name" >&2
    exit 78
  fi
}

validate_identifier() {
  name=$1
  eval "value=\${$name}"
  case "$value" in
    ''|[0-9]*|*[!a-z0-9_]*)
      printf 'Identificatore PostgreSQL non valido per %s.\n' "$name" >&2
      exit 78
      ;;
    *) ;;
  esac
}

for variable in \
  POSTGRES_DB \
  POSTGRES_USER \
  GESTIONALE_DB_OWNER_USERNAME \
  GESTIONALE_DB_MIGRATOR_USERNAME \
  GESTIONALE_DB_MIGRATOR_PASSWORD \
  GESTIONALE_DB_RUNTIME_USERNAME \
  GESTIONALE_DB_RUNTIME_PASSWORD \
  GESTIONALE_DB_BACKUP_USERNAME \
  GESTIONALE_DB_BACKUP_PASSWORD \
  GESTIONALE_DB_RESTORE_USERNAME \
  GESTIONALE_DB_RESTORE_PASSWORD
do
  require_value "$variable"
done

for variable in \
  POSTGRES_USER \
  GESTIONALE_DB_OWNER_USERNAME \
  GESTIONALE_DB_MIGRATOR_USERNAME \
  GESTIONALE_DB_RUNTIME_USERNAME \
  GESTIONALE_DB_BACKUP_USERNAME \
  GESTIONALE_DB_RESTORE_USERNAME
do
  validate_identifier "$variable"
done

role_list="$POSTGRES_USER $GESTIONALE_DB_OWNER_USERNAME $GESTIONALE_DB_MIGRATOR_USERNAME $GESTIONALE_DB_RUNTIME_USERNAME $GESTIONALE_DB_BACKUP_USERNAME $GESTIONALE_DB_RESTORE_USERNAME"
for role in $role_list; do
  occurrences=$(printf '%s\n' $role_list | grep -Fxc "$role")
  if [ "$occurrences" -ne 1 ]; then
    printf 'I ruoli PostgreSQL devono avere nomi distinti.\n' >&2
    exit 78
  fi
done

psql \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --set=database_name="$POSTGRES_DB" \
  --set=owner_role="$GESTIONALE_DB_OWNER_USERNAME" \
  --set=migrator_role="$GESTIONALE_DB_MIGRATOR_USERNAME" \
  --set=migrator_password="$GESTIONALE_DB_MIGRATOR_PASSWORD" \
  --set=runtime_role="$GESTIONALE_DB_RUNTIME_USERNAME" \
  --set=runtime_password="$GESTIONALE_DB_RUNTIME_PASSWORD" \
  --set=backup_role="$GESTIONALE_DB_BACKUP_USERNAME" \
  --set=backup_password="$GESTIONALE_DB_BACKUP_PASSWORD" \
  --set=restore_role="$GESTIONALE_DB_RESTORE_USERNAME" \
  --set=restore_password="$GESTIONALE_DB_RESTORE_PASSWORD" \
  --set=legacy_role='' \
  --file "$ROLE_SQL"
