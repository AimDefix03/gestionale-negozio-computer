#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
MIGRATION_DIR="$ROOT_DIR/web/backend/src/main/resources/db/migration"
FIXTURE_DIR="$ROOT_DIR/web/backend/src/test/resources/db/fixtures"
PROFILE=${1:-small}
MODE=${2:-snapshots}
PROFILE_FILE="$FIXTURE_DIR/profiles/$PROFILE.properties"
POSTGRES_IMAGE=${FIXTURE_POSTGRES_IMAGE:-postgres:16-alpine}
CONTAINER_NAME="gestionale-fixtures-$(date +%s)-$$"
POSTGRES_USER=fixture_admin
POSTGRES_PASSWORD=$(od -An -N24 -tx1 /dev/urandom | tr -d ' \n')
CURRENT_VERSION=35

case "$PROFILE" in
  small|medium|large) ;;
  *)
    printf 'Profilo fixture non supportato: %s\n' "$PROFILE" >&2
    exit 2
    ;;
esac

case "$MODE" in
  snapshots|upgrades|upgrade-v18) ;;
  *)
    printf 'Modalita fixture non supportata: %s\n' "$MODE" >&2
    exit 2
    ;;
esac

if [ ! -f "$PROFILE_FILE" ]; then
  printf 'Profilo fixture mancante: %s\n' "$PROFILE_FILE" >&2
  exit 2
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker e richiesto per la verifica PostgreSQL delle fixture." >&2
  exit 2
fi

profile_value() {
  key=$1
  value=$(sed -n "s/^${key}=//p" "$PROFILE_FILE")
  case "$value" in
    ''|*[!0-9]*)
      printf 'Valore non valido per %s in %s.\n' "$key" "$PROFILE_FILE" >&2
      exit 2
      ;;
  esac
  if [ "$value" -le 0 ]; then
    printf 'Valore non positivo per %s in %s.\n' "$key" "$PROFILE_FILE" >&2
    exit 2
  fi
  printf '%s' "$value"
}

PRODUCT_COUNT=$(profile_value PRODUCT_COUNT)
PARTNER_COUNT=$(profile_value PARTNER_COUNT)
ORDER_COUNT=$(profile_value ORDER_COUNT)

verify_checksums() {
  if command -v sha256sum >/dev/null 2>&1; then
    (cd "$FIXTURE_DIR" && sha256sum -c SHA256SUMS)
  elif command -v shasum >/dev/null 2>&1; then
    (cd "$FIXTURE_DIR" && shasum -a 256 -c SHA256SUMS)
  else
    echo "Serve sha256sum oppure shasum per verificare le fixture." >&2
    exit 2
  fi
}

cleanup() {
  fixture_label=$(docker inspect --format '{{ index .Config.Labels "it.giovannidefilippo.gestionale.fixture-run" }}' "$CONTAINER_NAME" 2>/dev/null || true)
  if [ "$fixture_label" = "$CONTAINER_NAME" ]; then
    docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
  fi
}

wait_for_postgres() {
  attempt=1
  while [ "$attempt" -le 60 ]; do
    if docker exec "$CONTAINER_NAME" pg_isready -U "$POSTGRES_USER" -d postgres >/dev/null 2>&1 \
      && docker exec "$CONTAINER_NAME" psql -X -U "$POSTGRES_USER" -d postgres -tAc "select 1" >/dev/null 2>&1; then
      sleep 1
      if docker exec "$CONTAINER_NAME" psql -X -U "$POSTGRES_USER" -d postgres -tAc "select 1" >/dev/null 2>&1; then
        return 0
      fi
    fi
    attempt=$((attempt + 1))
    sleep 1
  done
  echo "PostgreSQL fixture non pronto dopo 60 secondi." >&2
  return 1
}

psql_file() {
  database=$1
  file=$2
  docker exec -i "$CONTAINER_NAME" psql \
    -X \
    -U "$POSTGRES_USER" \
    -d "$database" \
    -v ON_ERROR_STOP=1 < "$file"
}

psql_value() {
  database=$1
  query=$2
  docker exec "$CONTAINER_NAME" psql \
    -X \
    -U "$POSTGRES_USER" \
    -d "$database" \
    -v ON_ERROR_STOP=1 \
    -tA \
    -c "$query"
}

apply_migration() {
  database=$1
  version=$2
  migration_file=$(find "$MIGRATION_DIR" -maxdepth 1 -type f -name "V${version}__*.sql" -print)
  migration_count=$(printf '%s\n' "$migration_file" | sed '/^$/d' | wc -l | tr -d '[:space:]')
  if [ "$migration_count" -ne 1 ]; then
    printf 'Attesa una sola migration V%s, trovate %s.\n' "$version" "$migration_count" >&2
    exit 1
  fi
  psql_file "$database" "$migration_file"
}

load_volume() {
  database=$1
  docker exec -i "$CONTAINER_NAME" psql \
    -X \
    -U "$POSTGRES_USER" \
    -d "$database" \
    -v ON_ERROR_STOP=1 \
    -v product_count="$PRODUCT_COUNT" \
    -v partner_count="$PARTNER_COUNT" \
    -v order_count="$ORDER_COUNT" < "$FIXTURE_DIR/volume/core.sql"
}

assert_snapshot() {
  database=$1
  version=$2
  if [ "$version" -ge 29 ]; then
    psql_file "$database" "$FIXTURE_DIR/assertions/v29.sql"
  fi
  if [ "$version" -ge 30 ]; then
    psql_file "$database" "$FIXTURE_DIR/assertions/v30.sql"
  fi
  if [ "$version" -ge 31 ]; then
    psql_file "$database" "$FIXTURE_DIR/assertions/v31.sql"
  fi
  if [ "$version" -ge 32 ]; then
    psql_file "$database" "$FIXTURE_DIR/assertions/v32.sql"
  fi
  if [ "$version" -ge 33 ]; then
    psql_file "$database" "$FIXTURE_DIR/assertions/v33.sql"
  fi
  if [ "$version" -ge 34 ]; then
    psql_file "$database" "$FIXTURE_DIR/assertions/v34.sql"
  fi
  if [ "$version" -ge 35 ]; then
    psql_file "$database" "$FIXTURE_DIR/assertions/v35.sql"
  fi
  if [ "$version" -lt 29 ]; then
    psql_file "$database" "$FIXTURE_DIR/assertions/v${version}.sql"
  fi
  docker exec "$CONTAINER_NAME" psql \
    -X \
    -U "$POSTGRES_USER" \
    -d "$database" \
    -v ON_ERROR_STOP=1 \
    -tA \
    -c "select 'products=' || count(*) from products union all select 'orders=' || count(*) from customer_orders order by 1;"
}

build_snapshot() {
  target=$1
  database="fixture_v${target}"
  docker exec "$CONTAINER_NAME" createdb -U "$POSTGRES_USER" "$database"

  version=1
  while [ "$version" -le 14 ]; do
    apply_migration "$database" "$version"
    version=$((version + 1))
  done
  psql_file "$database" "$FIXTURE_DIR/v14/base.sql"
  load_volume "$database"

  if [ "$target" -ge 16 ]; then
    apply_migration "$database" 15
    apply_migration "$database" 16
    psql_file "$database" "$FIXTURE_DIR/v16/delta.sql"
  fi

  if [ "$target" -ge 18 ]; then
    apply_migration "$database" 17
    apply_migration "$database" 18
    psql_file "$database" "$FIXTURE_DIR/v18/delta.sql"
  fi

  if [ "$target" -ge 19 ]; then
    apply_migration "$database" 19
  fi

  if [ "$target" -ge 20 ]; then
    apply_migration "$database" 20
  fi

  if [ "$target" -ge 21 ]; then
    apply_migration "$database" 21
  fi

  if [ "$target" -ge 22 ]; then
    apply_migration "$database" 22
  fi

  assert_snapshot "$database" "$target"
  printf 'Snapshot V%s verificato su PostgreSQL con profilo %s.\n' "$target" "$PROFILE"
}

assert_numbering_preflight() {
  database=$1
  collisions=$(psql_value "$database" "
      select
          (select count(*) from (select lower(code) from fiscal_documents group by lower(code) having count(*) > 1) duplicate_codes)
        + (select count(*) from (select type, fiscal_year, sequence_number from fiscal_documents group by type, fiscal_year, sequence_number having count(*) > 1) duplicate_sequences);")
  if [ "$collisions" -ne 0 ]; then
    printf 'Preflight fallito su %s: rilevate %s collisioni documentali.\n' "$database" "$collisions" >&2
    exit 1
  fi
}

assert_canonical_preflight() {
  database=$1
  collisions=$(psql_value "$database" "
      select
          (select count(*) from (select lower(trim(username)) from user_accounts group by lower(trim(username)) having count(*) > 1) duplicate_users)
        + (select count(*) from (select lower(trim(code)) from products group by lower(trim(code)) having count(*) > 1) duplicate_products)
        + (select count(*) from (select lower(trim(code)) from business_partners group by lower(trim(code)) having count(*) > 1) duplicate_partners);")
  if [ "$collisions" -ne 3 ]; then
    printf 'Preflight canonico inatteso su %s: attese 3 collisioni sintetiche, rilevate %s.\n' "$database" "$collisions" >&2
    exit 1
  fi
}

apply_canonical_remediation() {
  database=$1
  psql_file "$database" "$FIXTURE_DIR/remediation/pre-v23-canonical-resolution.sql"
  collisions=$(psql_value "$database" "
      select
          (select count(*) from (select lower(trim(username)) from user_accounts group by lower(trim(username)) having count(*) > 1) duplicate_users)
        + (select count(*) from (select lower(trim(code)) from products group by lower(trim(code)) having count(*) > 1) duplicate_products)
        + (select count(*) from (select lower(trim(code)) from business_partners group by lower(trim(code)) having count(*) > 1) duplicate_partners);")
  if [ "$collisions" -ne 0 ]; then
    printf 'Riconciliazione canonica incompleta su %s: restano %s collisioni.\n' "$database" "$collisions" >&2
    exit 1
  fi
}

assert_first_document_after_upgrade() {
  database=$1
  next_value=$(psql_value "$database" "select next_value from document_number_counters where document_type = 'SIMULATED_INVOICE' and fiscal_year = 2026;")
  maximum=$(psql_value "$database" "select max(sequence_number) from fiscal_documents where type = 'SIMULATED_INVOICE' and fiscal_year = 2026;")
  if [ "$next_value" -ne $((maximum + 1)) ]; then
    printf 'Contatore non consecutivo su %s: massimo %s, prossimo %s.\n' "$database" "$maximum" "$next_value" >&2
    exit 1
  fi

  docker exec -i "$CONTAINER_NAME" psql \
    -X \
    -U "$POSTGRES_USER" \
    -d "$database" \
    -v ON_ERROR_STOP=1 \
    -v sequence_number="$next_value" <<'SQL' >/dev/null
begin;
update document_number_counters
set next_value = next_value + 1,
    version = version + 1
where document_type = 'SIMULATED_INVOICE'
  and fiscal_year = 2026
  and next_value = :sequence_number;

insert into fiscal_documents (
    code, type, status, created_at, related_order_code, customer, payment_method,
    taxable_amount, vat_rate, vat_amount, total_amount, created_by, created_by_role,
    reason, disclaimer, fiscal_year, sequence_number, document_prefix
)
select
    settings.invoice_prefix || '-2026-' || lpad(cast(:sequence_number as varchar), settings.number_padding, '0'),
    'SIMULATED_INVOICE', 'ISSUED', current_timestamp,
    'UPGRADE-CHECK-' || cast(:sequence_number as varchar), 'Cliente upgrade', 'Carta',
    10.00, 0.2200, 2.20, 12.20, 'fixture-upgrade', 'SYSTEM',
    'Verifica primo documento dopo upgrade.', 'DOCUMENTO SIMULATO NON FISCALE',
    2026, :sequence_number, settings.invoice_prefix
from company_settings settings
where settings.id = 1;
commit;
SQL

  inserted=$(psql_value "$database" "select count(*) from fiscal_documents where type = 'SIMULATED_INVOICE' and fiscal_year = 2026 and sequence_number = $next_value;")
  if [ "$inserted" -ne 1 ]; then
    printf 'Primo documento post-upgrade non creato su %s.\n' "$database" >&2
    exit 1
  fi
}

verify_upgrade() {
  source=$1
  source_database="fixture_v${source}"
  database="upgrade_v${source}"
  docker exec "$CONTAINER_NAME" createdb -U "$POSTGRES_USER" -T "$source_database" "$database"

  version=$((source + 1))
  while [ "$version" -le 21 ]; do
    apply_migration "$database" "$version"
    version=$((version + 1))
  done

  assert_numbering_preflight "$database"
  started_at=$(psql_value "$database" "select floor(extract(epoch from clock_timestamp()) * 1000)::bigint;")
  apply_migration "$database" 22
  finished_at=$(psql_value "$database" "select floor(extract(epoch from clock_timestamp()) * 1000)::bigint;")
  duration_ms=$((finished_at - started_at))

  assert_snapshot "$database" 22
  assert_first_document_after_upgrade "$database"
  assert_canonical_preflight "$database"
  apply_canonical_remediation "$database"

  version=23
  while [ "$version" -le "$CURRENT_VERSION" ]; do
    apply_migration "$database" "$version"
    version=$((version + 1))
  done

  assert_snapshot "$database" "$CURRENT_VERSION"
  printf 'Upgrade V%s -> V%s verificato su PostgreSQL, profilo %s, lock V22 %s ms.\n' "$source" "$CURRENT_VERSION" "$PROFILE" "$duration_ms"
}

verify_checksums
trap cleanup EXIT HUP INT TERM

docker run \
  --detach \
  --rm \
  --name "$CONTAINER_NAME" \
  --label "it.giovannidefilippo.gestionale.fixture-run=$CONTAINER_NAME" \
  --env POSTGRES_USER="$POSTGRES_USER" \
  --env POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
  --env POSTGRES_DB=postgres \
  "$POSTGRES_IMAGE" >/dev/null

wait_for_postgres
case "$MODE" in
  snapshots)
    build_snapshot 14
    build_snapshot 16
    build_snapshot 18
    build_snapshot 19
    build_snapshot 20
    build_snapshot 21
    build_snapshot 22
    printf 'Fixture storiche V14, V16, V18, V19, V20, V21 e V22 verificate senza porte o volumi persistenti.\n'
    ;;
  upgrades)
    build_snapshot 14
    build_snapshot 16
    build_snapshot 18
    verify_upgrade 14
    verify_upgrade 16
    verify_upgrade 18
    printf 'Fixture popolate V14, V16 e V18 aggiornate fino a V%s.\n' "$CURRENT_VERSION"
    ;;
  upgrade-v18)
    build_snapshot 18
    verify_upgrade 18
    printf 'Fixture volumetrica V18 aggiornata fino a V%s.\n' "$CURRENT_VERSION"
    ;;
esac
