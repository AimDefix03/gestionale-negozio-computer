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

require_database_identity() {
  identity=$1
  username_variable="GESTIONALE_DB_${identity}_USERNAME"
  password_variable="GESTIONALE_DB_${identity}_PASSWORD"
  password_file_variable="${password_variable}_FILE"
  password_secret_variable="${password_variable}_SECRET_FILE"

  eval "username=\${$username_variable-}"
  eval "password=\${$password_variable-}"
  eval "password_file=\${$password_file_variable-}"
  eval "password_secret=\${$password_secret_variable-}"

  if [ -z "$username" ]; then
    printf '%s mancante.\n' "$username_variable" >&2
    exit 2
  fi

  password_channels=0
  [ -n "$password" ] && password_channels=$((password_channels + 1))
  [ -n "$password_file" ] && password_channels=$((password_channels + 1))
  [ -n "$password_secret" ] && password_channels=$((password_channels + 1))
  if [ "$password_channels" -ne 1 ]; then
    printf 'Configura un solo canale password per il ruolo %s.\n' "$identity" >&2
    exit 2
  fi
}

configure_secrets_override() {
  configured=false
  for identity in BOOTSTRAP MIGRATOR RUNTIME BACKUP RESTORE; do
    eval "secret_file=\${GESTIONALE_DB_${identity}_PASSWORD_SECRET_FILE-}"
    [ -n "$secret_file" ] && configured=true
  done

  [ "$configured" = true ] || return 0

  for identity in BOOTSTRAP MIGRATOR RUNTIME BACKUP RESTORE; do
    variable="GESTIONALE_DB_${identity}_PASSWORD_SECRET_FILE"
    eval "secret_file=\${$variable-}"
    if [ -z "$secret_file" ]; then
      printf '%s mancante per l override Compose dei segreti.\n' "$variable" >&2
      exit 2
    fi
    case "$secret_file" in
      /*) ;;
      *) secret_file="$PROJECT_ROOT/$secret_file" ;;
    esac
    if [ ! -r "$secret_file" ]; then
      printf 'Il file secret per il ruolo %s non e leggibile.\n' "$identity" >&2
      exit 2
    fi
    export "$variable=$secret_file"
  done

  secrets_override="$PROJECT_ROOT/docker-compose.secrets.yml"
  if [ -n "$COMPOSE_OVERRIDE_FILE" ] && [ "$COMPOSE_OVERRIDE_FILE" != "$secrets_override" ]; then
    echo "COMPOSE_OVERRIDE_FILE incompatibile con i file secret database." >&2
    exit 2
  fi
  COMPOSE_OVERRIDE_FILE=$secrets_override
  export COMPOSE_OVERRIDE_FILE
}

configure_secrets_override

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
  bootstrap_username=${GESTIONALE_DB_BOOTSTRAP_USERNAME:-postgres}
  attempt=1
  while [ "$attempt" -le 30 ]; do
    if compose exec -T postgres pg_isready -U "$bootstrap_username" -d "$POSTGRES_DB" >/dev/null 2>&1; then
      return 0
    fi
    attempt=$((attempt + 1))
    sleep 2
  done
  echo "PostgreSQL non pronto dopo l'attesa prevista." >&2
  return 1
}
