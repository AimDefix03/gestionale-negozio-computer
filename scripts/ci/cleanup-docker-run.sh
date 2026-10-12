#!/bin/sh
set -eu

if [ "$#" -ne 2 ]; then
  printf 'Uso: %s <compose-project-name> <run-id>\n' "$0" >&2
  exit 64
fi

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
PROJECT_NAME=$1
RUN_ID=$2
INVENTORY_FILE=${DOCKER_RUN_INVENTORY_FILE:-${TMPDIR:-/tmp}/$PROJECT_NAME-recovery-inventory.txt}

. "$ROOT_DIR/scripts/ci/docker-run-safety.sh"

case "$PROJECT_NAME" in
  gestionale-prodlike-*) PREFIX=gestionale-prodlike- ;;
  gestionale-e2e-*) PREFIX=gestionale-e2e- ;;
  *)
    printf 'Il recupero accetta soltanto progetti prod-like o E2E con prefisso controllato.\n' >&2
    exit 64
    ;;
esac

docker_run_validate_project_name "$PROJECT_NAME" "$PREFIX"
docker_run_validate_id "$RUN_ID"
docker_run_cleanup "$PROJECT_NAME" "$RUN_ID" "$INVENTORY_FILE"
