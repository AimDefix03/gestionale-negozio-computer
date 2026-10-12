#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

if [ "${CONFIRM_DATABASE_ROLE_ROTATION:-false}" != yes ]; then
  echo "Rotazione bloccata: imposta CONFIRM_DATABASE_ROLE_ROTATION=yes." >&2
  exit 2
fi

CONFIRM_DATABASE_ROLE_PROVISIONING=yes \
GESTIONALE_DB_LEGACY_OWNER_USERNAME= \
  "$SCRIPT_DIR/provision-database-roles.sh"

echo "Password dei ruoli PostgreSQL ruotate. Riavvia il backend e verifica readiness prima di revocare i secret precedenti."
