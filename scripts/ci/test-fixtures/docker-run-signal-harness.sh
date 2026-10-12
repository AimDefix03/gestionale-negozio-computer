#!/bin/sh
set -eu

if [ "$#" -ne 7 ]; then
  exit 64
fi

ROOT_DIR=$1
PROJECT_NAME=$2
RUN_ID=$3
PHASE=$4
SIGNAL=$5
IMAGE=$6
TEST_ID=$7
INVENTORY_FILE=${TMPDIR:-/tmp}/$PROJECT_NAME-inventory.txt
TEST_LABEL=it.giovannidefilippo.gestionale.safety-test

. "$ROOT_DIR/scripts/ci/docker-run-safety.sh"

cleanup() {
  status=$?
  cleanup_status=0
  trap - EXIT HUP INT TERM
  docker_run_cleanup "$PROJECT_NAME" "$RUN_ID" "$INVENTORY_FILE" || cleanup_status=$?
  if [ "$status" -eq 0 ] && [ "$cleanup_status" -ne 0 ]; then
    status=$cleanup_status
  fi
  exit "$status"
}

handle_signal() {
  exit $((128 + $1))
}

trap cleanup EXIT
trap 'handle_signal 1' HUP
trap 'handle_signal 2' INT
trap 'handle_signal 15' TERM

case "$PHASE" in
  preflight) ;;
  network|volume|container)
    docker network create \
      --label "com.docker.compose.project=$PROJECT_NAME" \
      --label "it.giovannidefilippo.gestionale.run-id=$RUN_ID" \
      --label "$TEST_LABEL=$TEST_ID" \
      "$PROJECT_NAME-network" >/dev/null
    ;;
  *) exit 64 ;;
esac

case "$PHASE" in
  volume|container)
    docker volume create \
      --label "com.docker.compose.project=$PROJECT_NAME" \
      --label "it.giovannidefilippo.gestionale.run-id=$RUN_ID" \
      --label "$TEST_LABEL=$TEST_ID" \
      "$PROJECT_NAME-volume" >/dev/null
    ;;
esac

if [ "$PHASE" = container ]; then
  docker create \
    --name "$PROJECT_NAME-container" \
    --network "$PROJECT_NAME-network" \
    --label "com.docker.compose.project=$PROJECT_NAME" \
    --label "it.giovannidefilippo.gestionale.run-id=$RUN_ID" \
    --label "$TEST_LABEL=$TEST_ID" \
    "$IMAGE" sh -c 'sleep 300' >/dev/null
fi

case "$SIGNAL" in
  HUP) kill -HUP $$ ;;
  INT) kill -INT $$ ;;
  TERM) kill -TERM $$ ;;
  KILL) kill -KILL $$ ;;
  *) exit 64 ;;
esac
