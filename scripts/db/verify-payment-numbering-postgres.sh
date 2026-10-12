#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
BACKEND_DIR="$ROOT_DIR/web/backend"
POSTGRES_IMAGE=${PAYMENT_NUMBERING_TEST_POSTGRES_IMAGE:-postgres:16-alpine}
CONTAINER_NAME="gestionale-payment-numbering-test-$(date +%s)-$$"
POSTGRES_USER=payment_numbering_test
POSTGRES_DB=payment_numbering_test
POSTGRES_PASSWORD=$(od -An -N24 -tx1 /dev/urandom | tr -d ' \n')

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker e richiesto per i test PostgreSQL di pagamenti e numerazione." >&2
  exit 2
fi

cleanup() {
  test_label=$(docker inspect --format '{{ index .Config.Labels "it.giovannidefilippo.gestionale.payment-numbering-test" }}' "$CONTAINER_NAME" 2>/dev/null || true)
  if [ "$test_label" = "$CONTAINER_NAME" ]; then
    docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
  fi
}

wait_for_postgres() {
  attempt=1
  while [ "$attempt" -le 60 ]; do
    if docker exec "$CONTAINER_NAME" pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" >/dev/null 2>&1 \
      && docker exec "$CONTAINER_NAME" psql -X -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc "select 1" >/dev/null 2>&1; then
      return 0
    fi
    attempt=$((attempt + 1))
    sleep 1
  done
  echo "PostgreSQL non pronto dopo 60 secondi." >&2
  return 1
}

trap cleanup EXIT HUP INT TERM

docker run \
  --detach \
  --rm \
  --name "$CONTAINER_NAME" \
  --label "it.giovannidefilippo.gestionale.payment-numbering-test=$CONTAINER_NAME" \
  --publish 127.0.0.1::5432 \
  --env POSTGRES_USER="$POSTGRES_USER" \
  --env POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
  --env POSTGRES_DB="$POSTGRES_DB" \
  "$POSTGRES_IMAGE" >/dev/null

wait_for_postgres
POSTGRES_PORT=$(docker port "$CONTAINER_NAME" 5432/tcp | sed -n 's/.*://p')

if [ -z "$POSTGRES_PORT" ]; then
  echo "Porta PostgreSQL non rilevata." >&2
  exit 1
fi

(
  cd "$BACKEND_DIR"
  mvn -B \
    -Dtest=OrderPaymentIntegrationTest,FiscalDocumentCodeSequenceTest \
    -Dspring.datasource.url="jdbc:postgresql://127.0.0.1:${POSTGRES_PORT}/${POSTGRES_DB}" \
    -Dspring.datasource.driver-class-name=org.postgresql.Driver \
    -Dspring.datasource.username="$POSTGRES_USER" \
    -Dspring.datasource.password="$POSTGRES_PASSWORD" \
    clean test
)

printf 'Pagamenti storici e numerazione concorrente verificati su PostgreSQL %s senza volumi persistenti.\n' "$POSTGRES_IMAGE"
