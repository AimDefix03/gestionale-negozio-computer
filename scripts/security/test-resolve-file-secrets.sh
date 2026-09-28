#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
RESOLVER="$ROOT_DIR/scripts/security/resolve-file-secrets.sh"
TEMP_DIR=$(mktemp -d "${TMPDIR:-/tmp}/gestionale-secrets-test.XXXXXX")
OUTPUT="$TEMP_DIR/output.log"

cleanup() {
  rm -rf "$TEMP_DIR"
}

trap cleanup EXIT INT TERM

expect_failure() {
  if "$@" >"$OUTPUT" 2>&1; then
    printf 'Il comando doveva fallire.\n' >&2
    exit 1
  fi
}

env EXAMPLE_SECRET='test' sh "$RESOLVER" EXAMPLE_SECRET -- sh -c '[ "$EXAMPLE_SECRET" = "test" ]'

printf 'file-test\n' >"$TEMP_DIR/secret"
env EXAMPLE_SECRET_FILE="$TEMP_DIR/secret" sh "$RESOLVER" EXAMPLE_SECRET -- sh -c '[ "$EXAMPLE_SECRET" = "file-test" ] && [ -z "${EXAMPLE_SECRET_FILE:-}" ]'

env -u OPTIONAL_SECRET -u OPTIONAL_SECRET_FILE sh "$RESOLVER" '?OPTIONAL_SECRET' -- sh -c '[ -z "${OPTIONAL_SECRET:-}" ]'

expect_failure env EXAMPLE_SECRET='conflict-test' EXAMPLE_SECRET_FILE="$TEMP_DIR/secret" sh "$RESOLVER" EXAMPLE_SECRET -- true
if grep -q 'conflict-test\|file-test\|gestionale-secrets-test' "$OUTPUT"; then
  printf 'Il resolver ha esposto dati sensibili.\n' >&2
  exit 1
fi

expect_failure env -u REQUIRED_SECRET -u REQUIRED_SECRET_FILE sh "$RESOLVER" REQUIRED_SECRET -- true
expect_failure env REQUIRED_SECRET_FILE="$TEMP_DIR/missing" sh "$RESOLVER" REQUIRED_SECRET -- true

: >"$TEMP_DIR/empty"
expect_failure env REQUIRED_SECRET_FILE="$TEMP_DIR/empty" sh "$RESOLVER" REQUIRED_SECRET -- true

printf 'first-line\nsecond-line\n' >"$TEMP_DIR/multiline"
expect_failure env REQUIRED_SECRET_FILE="$TEMP_DIR/multiline" sh "$RESOLVER" REQUIRED_SECRET -- true

expect_failure env REQUIRED_SECRET='first-line
second-line' sh "$RESOLVER" REQUIRED_SECRET -- true

printf 'Secret resolver: 9 verifiche superate.\n'
