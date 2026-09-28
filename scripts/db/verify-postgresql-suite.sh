#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
MODE=${1:-fast}

case "$MODE" in
  fast|nightly) ;;
  *)
    printf 'Modalita PostgreSQL non supportata: %s\n' "$MODE" >&2
    exit 2
    ;;
esac

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker e richiesto per la suite PostgreSQL." >&2
  exit 2
fi

if ! docker info >/dev/null 2>&1; then
  echo "Il daemon Docker non e disponibile." >&2
  exit 2
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven e richiesto per la suite PostgreSQL." >&2
  exit 2
fi

(
  cd "$ROOT_DIR/web/backend"
  mvn -B -Ppostgresql-it test
)

sh "$ROOT_DIR/scripts/db/verify-historical-fixtures.sh" small upgrades

if [ "$MODE" = "nightly" ]; then
  sh "$ROOT_DIR/scripts/db/verify-historical-fixtures.sh" large upgrade-v18
fi

printf 'Gate PostgreSQL %s completato.\n' "$MODE"
