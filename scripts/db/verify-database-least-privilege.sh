#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
POSTGRES_IMAGE=${LEAST_PRIVILEGE_POSTGRES_IMAGE:-gestionale-postgres:least-privilege-test}
CONTAINER_NAME="gestionale-least-privilege-$(openssl rand -hex 6)"
DATABASE_NAME=gestionale_least_privilege
BOOTSTRAP_ROLE=lp_bootstrap
OWNER_ROLE=lp_owner
MIGRATOR_ROLE=lp_migrator
RUNTIME_ROLE=lp_runtime
BACKUP_ROLE=lp_backup
RESTORE_ROLE=lp_restore
BOOTSTRAP_PASSWORD=$(openssl rand -hex 32)
MIGRATOR_PASSWORD=$(openssl rand -hex 32)
RUNTIME_PASSWORD=$(openssl rand -hex 32)
BACKUP_PASSWORD=$(openssl rand -hex 32)
RESTORE_PASSWORD=$(openssl rand -hex 32)
TEST_BOOTSTRAP_PASSWORD="Aa1!$(openssl rand -hex 20)"

cleanup() {
  docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
}

trap cleanup EXIT INT TERM

docker build -q -f "$ROOT_DIR/web/postgres/Dockerfile" -t "$POSTGRES_IMAGE" "$ROOT_DIR" >/dev/null
docker run -d \
  --name "$CONTAINER_NAME" \
  --label gestionale.test=database-least-privilege \
  --publish 127.0.0.1::5432 \
  --env POSTGRES_DB="$DATABASE_NAME" \
  --env POSTGRES_USER="$BOOTSTRAP_ROLE" \
  --env POSTGRES_PASSWORD="$BOOTSTRAP_PASSWORD" \
  --env GESTIONALE_DB_OWNER_USERNAME="$OWNER_ROLE" \
  --env GESTIONALE_DB_MIGRATOR_USERNAME="$MIGRATOR_ROLE" \
  --env GESTIONALE_DB_MIGRATOR_PASSWORD="$MIGRATOR_PASSWORD" \
  --env GESTIONALE_DB_RUNTIME_USERNAME="$RUNTIME_ROLE" \
  --env GESTIONALE_DB_RUNTIME_PASSWORD="$RUNTIME_PASSWORD" \
  --env GESTIONALE_DB_BACKUP_USERNAME="$BACKUP_ROLE" \
  --env GESTIONALE_DB_BACKUP_PASSWORD="$BACKUP_PASSWORD" \
  --env GESTIONALE_DB_RESTORE_USERNAME="$RESTORE_ROLE" \
  --env GESTIONALE_DB_RESTORE_PASSWORD="$RESTORE_PASSWORD" \
  "$POSTGRES_IMAGE" >/dev/null

attempt=1
while [ "$attempt" -le 30 ]; do
  if docker exec "$CONTAINER_NAME" pg_isready -U "$BOOTSTRAP_ROLE" -d "$DATABASE_NAME" >/dev/null 2>&1; then
    break
  fi
  attempt=$((attempt + 1))
  sleep 1
done

if [ "$attempt" -gt 30 ]; then
  echo "PostgreSQL least-privilege non pronto." >&2
  exit 1
fi

POSTGRES_PORT=$(docker port "$CONTAINER_NAME" 5432/tcp | sed -n 's/.*://p')
if [ -z "$POSTGRES_PORT" ]; then
  echo "Porta PostgreSQL effimera non disponibile." >&2
  exit 1
fi
CONTAINER_IP=$(docker inspect --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "$CONTAINER_NAME")
if [ -z "$CONTAINER_IP" ]; then
  echo "Indirizzo TCP del container PostgreSQL non disponibile." >&2
  exit 1
fi

cd "$ROOT_DIR/web/backend"
mvn -q \
  -Dtest=FinancialReconciliationIntegrationTest \
  -Dspring.datasource.url="jdbc:postgresql://127.0.0.1:${POSTGRES_PORT}/${DATABASE_NAME}" \
  -Dspring.datasource.driver-class-name=org.postgresql.Driver \
  -Dspring.datasource.username="$RUNTIME_ROLE" \
  -Dspring.datasource.password="$RUNTIME_PASSWORD" \
  -Dspring.flyway.url="jdbc:postgresql://127.0.0.1:${POSTGRES_PORT}/${DATABASE_NAME}" \
  -Dspring.flyway.user="$MIGRATOR_ROLE" \
  -Dspring.flyway.password="$MIGRATOR_PASSWORD" \
  -Dspring.flyway.init-sqls="SET ROLE $OWNER_ROLE" \
  -Dgestionale.bootstrap.super-admin.enabled=true \
  -Dgestionale.bootstrap.super-admin.username=least_privilege_test_admin \
  -Dgestionale.bootstrap.super-admin.password="$TEST_BOOTSTRAP_PASSWORD" \
  test

psql_as() {
  role=$1
  password=$2
  shift 2
  docker exec \
    -e PGPASSWORD="$password" \
    "$CONTAINER_NAME" \
    psql -X -h "$CONTAINER_IP" -U "$role" -d "$DATABASE_NAME" -v ON_ERROR_STOP=1 "$@"
}

expect_sql_failure() {
  role=$1
  password=$2
  statement=$3
  if psql_as "$role" "$password" -c "$statement" >/dev/null 2>&1; then
    printf 'Operazione vietata riuscita per il ruolo %s: %s\n' "$role" "$statement" >&2
    exit 1
  fi
}

owner_login=$(docker exec "$CONTAINER_NAME" psql -X -U "$BOOTSTRAP_ROLE" -d "$DATABASE_NAME" -tA -c "select rolcanlogin from pg_roles where rolname = '$OWNER_ROLE';")
runtime_attributes=$(docker exec "$CONTAINER_NAME" psql -X -U "$BOOTSTRAP_ROLE" -d "$DATABASE_NAME" -tA -c "select rolsuper or rolcreaterole or rolcreatedb or rolreplication or rolbypassrls from pg_roles where rolname = '$RUNTIME_ROLE';")
restore_createdb=$(docker exec "$CONTAINER_NAME" psql -X -U "$BOOTSTRAP_ROLE" -d "$DATABASE_NAME" -tA -c "select rolcreatedb and not rolsuper and not rolcreaterole from pg_roles where rolname = '$RESTORE_ROLE';")

[ "$owner_login" = f ] || { echo "Il ruolo owner non deve poter effettuare login." >&2; exit 1; }
[ "$runtime_attributes" = f ] || { echo "Il runtime possiede attributi amministrativi." >&2; exit 1; }
[ "$restore_createdb" = t ] || { echo "Il ruolo restore non possiede il profilo dedicato atteso." >&2; exit 1; }

psql_as "$RUNTIME_ROLE" "$RUNTIME_PASSWORD" -tA -c "select count(*) from products;" >/dev/null
psql_as "$BACKUP_ROLE" "$BACKUP_PASSWORD" -tA -c "select count(*) from customer_orders;" >/dev/null

psql_as "$MIGRATOR_ROLE" "$MIGRATOR_PASSWORD" -c "set role $OWNER_ROLE; create table least_privilege_probe (id integer primary key);" >/dev/null
expect_sql_failure "$RUNTIME_ROLE" "$RUNTIME_PASSWORD" "create table runtime_forbidden (id integer)"
expect_sql_failure "$RUNTIME_ROLE" "$RUNTIME_PASSWORD" "alter table least_privilege_probe add column forbidden text"
expect_sql_failure "$RUNTIME_ROLE" "$RUNTIME_PASSWORD" "drop table least_privilege_probe"
expect_sql_failure "$BACKUP_ROLE" "$BACKUP_PASSWORD" "insert into least_privilege_probe (id) values (1)"
expect_sql_failure "$BACKUP_ROLE" "$BACKUP_PASSWORD" "create table backup_forbidden (id integer)"
psql_as "$MIGRATOR_ROLE" "$MIGRATOR_PASSWORD" -c "set role $OWNER_ROLE; drop table least_privilege_probe;" >/dev/null

published_address=$(docker inspect --format '{{(index (index .NetworkSettings.Ports "5432/tcp") 0).HostIp}}' "$CONTAINER_NAME")
[ "$published_address" = 127.0.0.1 ] || { echo "PostgreSQL non e vincolato a loopback." >&2; exit 1; }

NEW_MIGRATOR_PASSWORD=$(openssl rand -hex 32)
NEW_RUNTIME_PASSWORD=$(openssl rand -hex 32)
NEW_BACKUP_PASSWORD=$(openssl rand -hex 32)
NEW_RESTORE_PASSWORD=$(openssl rand -hex 32)

docker exec "$CONTAINER_NAME" psql \
  -X \
  -U "$BOOTSTRAP_ROLE" \
  -d "$DATABASE_NAME" \
  -v database_name="$DATABASE_NAME" \
  -v owner_role="$OWNER_ROLE" \
  -v migrator_role="$MIGRATOR_ROLE" \
  -v migrator_password="$NEW_MIGRATOR_PASSWORD" \
  -v runtime_role="$RUNTIME_ROLE" \
  -v runtime_password="$NEW_RUNTIME_PASSWORD" \
  -v backup_role="$BACKUP_ROLE" \
  -v backup_password="$NEW_BACKUP_PASSWORD" \
  -v restore_role="$RESTORE_ROLE" \
  -v restore_password="$NEW_RESTORE_PASSWORD" \
  -v legacy_role= \
  -f /usr/local/share/gestionale/postgresql-roles.sql >/dev/null

if psql_as "$RUNTIME_ROLE" "$RUNTIME_PASSWORD" -c "select 1" >/dev/null 2>&1; then
  echo "La precedente password runtime e ancora valida dopo la rotazione." >&2
  exit 1
fi

psql_as "$RUNTIME_ROLE" "$NEW_RUNTIME_PASSWORD" -c "select 1" >/dev/null
psql_as "$MIGRATOR_ROLE" "$NEW_MIGRATOR_PASSWORD" -c "set role $OWNER_ROLE; select 1" >/dev/null
psql_as "$BACKUP_ROLE" "$NEW_BACKUP_PASSWORD" -c "select 1" >/dev/null
psql_as "$RESTORE_ROLE" "$NEW_RESTORE_PASSWORD" -c "select 1" >/dev/null

if docker exec -e PGPASSWORD="$NEW_RUNTIME_PASSWORD" "$CONTAINER_NAME" \
  createdb -h "$CONTAINER_IP" -U "$RUNTIME_ROLE" runtime_forbidden_database >/dev/null 2>&1; then
  echo "Il runtime e riuscito a creare un database." >&2
  exit 1
fi

docker exec -e PGPASSWORD="$NEW_RESTORE_PASSWORD" "$CONTAINER_NAME" \
  createdb -h "$CONTAINER_IP" -U "$RESTORE_ROLE" restore_role_probe
docker exec -e PGPASSWORD="$NEW_RESTORE_PASSWORD" "$CONTAINER_NAME" \
  dropdb -h "$CONTAINER_IP" -U "$RESTORE_ROLE" restore_role_probe

printf 'Ruoli PostgreSQL verificati: runtime senza DDL, backup read-only, migrator owner-scoped, restore dedicato e rotazione credenziali.\n'
