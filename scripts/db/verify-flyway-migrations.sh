#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
MIGRATION_DIR="$ROOT_DIR/web/backend/src/main/resources/db/migration"
POSTGRES_CONTAINER=${1:-gestionale-prodlike-postgres}
DATABASE_NAME=${2:-${POSTGRES_DB:-gestionale}}
DATABASE_USER=${3:-${GESTIONALE_DB_USERNAME:-gestionale_app}}

latest_expected=$(
  find "$MIGRATION_DIR" -maxdepth 1 -type f -name 'V*__*.sql' -print |
    sed -E 's#^.*/V([0-9]+)__.*#\1#' |
    sort -n |
    tail -n 1
)
expected_count=$(
  find "$MIGRATION_DIR" -maxdepth 1 -type f -name 'V*__*.sql' -print |
    wc -l |
    tr -d '[:space:]'
)

if [ -z "$latest_expected" ] || [ "$expected_count" -eq 0 ]; then
  printf 'Nessuna migrazione Flyway versionata trovata in %s.\n' "$MIGRATION_DIR" >&2
  exit 1
fi

latest_applied=$(docker exec "$POSTGRES_CONTAINER" psql \
  -U "$DATABASE_USER" \
  -d "$DATABASE_NAME" \
  -tA \
  -v ON_ERROR_STOP=1 \
  -c "select coalesce((select version from flyway_schema_history where success = true order by installed_rank desc limit 1), '');")
applied_count=$(docker exec "$POSTGRES_CONTAINER" psql \
  -U "$DATABASE_USER" \
  -d "$DATABASE_NAME" \
  -tA \
  -v ON_ERROR_STOP=1 \
  -c "select count(*) from flyway_schema_history where success = true and version is not null;")
failed_count=$(docker exec "$POSTGRES_CONTAINER" psql \
  -U "$DATABASE_USER" \
  -d "$DATABASE_NAME" \
  -tA \
  -v ON_ERROR_STOP=1 \
  -c "select count(*) from flyway_schema_history where success = false;")

if [ "$failed_count" -ne 0 ]; then
  printf 'Flyway contiene %s migrazioni fallite.\n' "$failed_count" >&2
  exit 1
fi

if [ "$applied_count" -ne "$expected_count" ]; then
  printf 'Migrazioni Flyway incomplete: attese %s, applicate %s.\n' "$expected_count" "$applied_count" >&2
  exit 1
fi

if [ "$latest_applied" != "$latest_expected" ]; then
  printf 'Versione Flyway inattesa: attesa V%s, applicata V%s.\n' "$latest_expected" "$latest_applied" >&2
  exit 1
fi

printf 'Migrazioni Flyway verificate: %s versioni applicate, ultima V%s.\n' "$applied_count" "$latest_applied"
