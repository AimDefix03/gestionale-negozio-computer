#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
COMPOSE_FILE=${COMPOSE_FILE:-"$PROJECT_ROOT/docker-compose.prod-like.yml"}
COMPOSE_OVERRIDE_FILE=${COMPOSE_OVERRIDE_FILE:-}
ENV_FILE=${ENV_FILE:-"$PROJECT_ROOT/.env.docker"}

if [ "${SKIP_ENV_FILE:-false}" != "true" ] && [ -f "$ENV_FILE" ]; then
  set -a
  . "$ENV_FILE"
  set +a
fi

POSTGRES_DB=${POSTGRES_DB:-gestionale}
BACKUP_DIR=${BACKUP_DIR:-"$PROJECT_ROOT/backups"}

case "$BACKUP_DIR" in
  /*) ;;
  *) BACKUP_DIR="$PROJECT_ROOT/$BACKUP_DIR" ;;
esac

if [ "$BACKUP_DIR" = / ]; then
  echo "BACKUP_DIR non puo essere la radice del filesystem." >&2
  exit 2
fi

if [ -z "${GESTIONALE_DB_USERNAME:-}" ]; then
  echo "GESTIONALE_DB_USERNAME mancante. Impostalo nell'ambiente o in .env.docker." >&2
  exit 2
fi

password_channels=0
[ -n "${GESTIONALE_DB_PASSWORD:-}" ] && password_channels=$((password_channels + 1))
[ -n "${GESTIONALE_DB_PASSWORD_FILE:-}" ] && password_channels=$((password_channels + 1))
[ -n "${GESTIONALE_DB_PASSWORD_SECRET_FILE:-}" ] && password_channels=$((password_channels + 1))

if [ "$password_channels" -eq 0 ]; then
  echo "Configura GESTIONALE_DB_PASSWORD, GESTIONALE_DB_PASSWORD_FILE oppure GESTIONALE_DB_PASSWORD_SECRET_FILE." >&2
  exit 2
fi

if [ "$password_channels" -ne 1 ]; then
  echo "Configura un solo canale per la password database." >&2
  exit 2
fi

if [ -n "${GESTIONALE_DB_PASSWORD_SECRET_FILE:-}" ]; then
  case "$GESTIONALE_DB_PASSWORD_SECRET_FILE" in
    /*) ;;
    *) GESTIONALE_DB_PASSWORD_SECRET_FILE="$PROJECT_ROOT/$GESTIONALE_DB_PASSWORD_SECRET_FILE" ;;
  esac
  if [ ! -r "$GESTIONALE_DB_PASSWORD_SECRET_FILE" ]; then
    echo "Il file secret database non e leggibile." >&2
    exit 2
  fi
  secrets_override="$PROJECT_ROOT/docker-compose.secrets.yml"
  if [ -n "$COMPOSE_OVERRIDE_FILE" ] && [ "$COMPOSE_OVERRIDE_FILE" != "$secrets_override" ]; then
    echo "COMPOSE_OVERRIDE_FILE incompatibile con il file secret database." >&2
    exit 2
  fi
  COMPOSE_OVERRIDE_FILE=$secrets_override
  export GESTIONALE_DB_PASSWORD_SECRET_FILE COMPOSE_OVERRIDE_FILE
fi

safe_db_name=$(printf '%s' "$POSTGRES_DB" | tr -c 'A-Za-z0-9_-' '_')
BACKUP_FILE_PREFIX=${BACKUP_FILE_PREFIX:-"gestionale_${safe_db_name}_"}
export BACKUP_DIR BACKUP_FILE_PREFIX

compose() {
  if [ -n "${COMPOSE_PROJECT_NAME:-}" ] && [ -n "$COMPOSE_OVERRIDE_FILE" ]; then
    docker compose -p "$COMPOSE_PROJECT_NAME" -f "$COMPOSE_FILE" -f "$COMPOSE_OVERRIDE_FILE" "$@"
  elif [ -n "${COMPOSE_PROJECT_NAME:-}" ]; then
    docker compose -p "$COMPOSE_PROJECT_NAME" -f "$COMPOSE_FILE" "$@"
  elif [ -n "$COMPOSE_OVERRIDE_FILE" ]; then
    docker compose -f "$COMPOSE_FILE" -f "$COMPOSE_OVERRIDE_FILE" "$@"
  else
    docker compose -f "$COMPOSE_FILE" "$@"
  fi
}

wait_for_postgres() {
  attempt=1
  while [ "$attempt" -le 30 ]; do
    if compose exec -T postgres pg_isready -U "$GESTIONALE_DB_USERNAME" -d "$POSTGRES_DB" >/dev/null 2>&1; then
      return 0
    fi
    attempt=$((attempt + 1))
    sleep 2
  done
  echo "PostgreSQL non pronto dopo l'attesa prevista." >&2
  return 1
}
