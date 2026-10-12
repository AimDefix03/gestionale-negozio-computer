#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
HARNESS="$ROOT_DIR/scripts/ci/test-fixtures/docker-run-signal-harness.sh"
IMAGE=${DOCKER_RUN_SAFETY_IMAGE:-alpine:3.21}
TEST_ID=$(openssl rand -hex 8)
TEST_LABEL=it.giovannidefilippo.gestionale.safety-test
STATE_DIR=$(mktemp -d "${TMPDIR:-/tmp}/gestionale-docker-safety.XXXXXX")

. "$ROOT_DIR/scripts/ci/docker-run-safety.sh"

cleanup_test_resources() {
  trap - EXIT HUP INT TERM
  for container_id in $(docker ps -aq --filter "label=$TEST_LABEL=$TEST_ID"); do
    docker rm -f "$container_id" >/dev/null 2>&1 || true
  done
  for volume_id in $(docker volume ls -q --filter "label=$TEST_LABEL=$TEST_ID"); do
    docker volume rm "$volume_id" >/dev/null 2>&1 || true
  done
  for network_id in $(docker network ls -q --filter "label=$TEST_LABEL=$TEST_ID"); do
    docker network rm "$network_id" >/dev/null 2>&1 || true
  done
  find "$STATE_DIR" -type f -delete 2>/dev/null || true
  rmdir "$STATE_DIR" 2>/dev/null || true
}

trap cleanup_test_resources EXIT HUP INT TERM

docker image inspect "$IMAGE" >/dev/null 2>&1 || docker pull "$IMAGE" >/dev/null

SENTINEL_PROJECT="gestionale-safety-test-collision-$TEST_ID"
SENTINEL_VOLUME="$SENTINEL_PROJECT-volume"
docker volume create \
  --label "com.docker.compose.project=$SENTINEL_PROJECT" \
  --label "$TEST_LABEL=$TEST_ID" \
  "$SENTINEL_VOLUME" >/dev/null
docker run --rm \
  -v "$SENTINEL_VOLUME:/sentinel" \
  "$IMAGE" sh -c 'printf sentinel-proof >/sentinel/proof.txt'

if docker_run_assert_project_unused "$SENTINEL_PROJECT" >/dev/null 2>&1; then
  printf 'Il preflight non ha rilevato la collisione dello stack sentinella.\n' >&2
  exit 1
fi
if docker_run_cleanup "$SENTINEL_PROJECT" "foreign-$TEST_ID" "$STATE_DIR/collision.txt" >/dev/null 2>&1; then
  printf 'Il cleanup ha accettato una risorsa sentinella priva del proprio run ID.\n' >&2
  exit 1
fi
proof=$(docker run --rm -v "$SENTINEL_VOLUME:/sentinel:ro" "$IMAGE" cat /sentinel/proof.txt)
if [ "$proof" != sentinel-proof ]; then
  printf 'Il dato persistente dello stack sentinella e stato alterato.\n' >&2
  exit 1
fi

for signal in HUP INT TERM; do
  for phase in preflight network volume container; do
    signal_id=$(printf '%s' "$signal" | tr '[:upper:]' '[:lower:]')
    project="gestionale-safety-test-${signal_id}-${phase}-$TEST_ID"
    run_id="${signal_id}.${phase}.$TEST_ID"
    if "$HARNESS" "$ROOT_DIR" "$project" "$run_id" "$phase" "$signal" "$IMAGE" "$TEST_ID"; then
      printf 'Il test %s/%s doveva terminare per segnale.\n' "$signal" "$phase" >&2
      exit 1
    fi
    docker_run_assert_project_unused "$project"
  done
done

CRASH_PROJECT="gestionale-safety-test-crash-$TEST_ID"
CRASH_RUN_ID="kill.crash.$TEST_ID"
if "$HARNESS" "$ROOT_DIR" "$CRASH_PROJECT" "$CRASH_RUN_ID" container KILL "$IMAGE" "$TEST_ID"; then
  printf 'Il test KILL doveva terminare senza eseguire la trap.\n' >&2
  exit 1
fi
if docker_run_assert_project_unused "$CRASH_PROJECT" >/dev/null 2>&1; then
  printf 'Il test KILL non ha lasciato risorse da recuperare.\n' >&2
  exit 1
fi
docker_run_cleanup "$CRASH_PROJECT" "$CRASH_RUN_ID" "$STATE_DIR/crash.txt"
docker_run_assert_project_unused "$CRASH_PROJECT"

printf 'Runner Docker verificati: sentinel-proof, collision, HUP, INT, TERM e KILL.\n'
