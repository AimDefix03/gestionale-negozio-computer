#!/bin/sh
set -eu

if [ "$#" -ne 2 ]; then
  printf 'Uso: %s <compose-project-name> <run-id>\n' "$0" >&2
  exit 64
fi

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
PROJECT_NAME=$1
RUN_ID=$2
ATTEMPT=1
MAX_ATTEMPTS=5

. "$ROOT_DIR/scripts/ci/docker-run-safety.sh"

docker_run_validate_id "$RUN_ID"

while [ "$ATTEMPT" -le "$MAX_ATTEMPTS" ]; do
  if docker_run_assert_project_unused "$PROJECT_NAME"; then
    printf 'Cleanup Docker verificato per il progetto %s (run %s).\n' "$PROJECT_NAME" "$RUN_ID"
    exit 0
  fi

  ATTEMPT=$((ATTEMPT + 1))
  sleep 1
done

printf 'Cleanup Docker incompleto per il progetto %s (run %s).\n' "$PROJECT_NAME" "$RUN_ID" >&2
exit 1
