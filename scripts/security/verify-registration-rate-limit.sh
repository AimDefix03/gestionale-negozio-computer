#!/bin/sh
set -eu

BASE_URL=${1:-http://127.0.0.1:8081}
MAX_ATTEMPTS=${REGISTER_RATE_LIMIT_TEST_MAX_ATTEMPTS:-30}
EXPECTED_RETRY_AFTER=${GESTIONALE_REGISTER_RATE_RETRY_AFTER_SECONDS:-60}
TEMP_DIR=$(mktemp -d)
USERNAME_PREFIX="registration-rate-probe-$(date +%s)-$$"

cleanup() {
  rm -rf "$TEMP_DIR"
}

trap cleanup EXIT INT TERM

attempt=1
limited=false

while [ "$attempt" -le "$MAX_ATTEMPTS" ]; do
  username="$USERNAME_PREFIX-$attempt"
  status=$(curl -sS \
    -D "$TEMP_DIR/headers" \
    -o "$TEMP_DIR/body" \
    -w '%{http_code}' \
    -H 'Content-Type: application/json' \
    -X POST \
    --data "{\"username\":\"$username\",\"password\":\"Registration-Probe-Strong-123!\"}" \
    "$BASE_URL/api/accounts/register")

  case "$status" in
    201|400)
      ;;
    429)
      limited=true
      break
      ;;
    *)
      cat "$TEMP_DIR/body"
      printf '\nUnexpected registration response: HTTP %s\n' "$status" >&2
      exit 1
      ;;
  esac

  attempt=$((attempt + 1))
done

if [ "$limited" != true ]; then
  printf 'Registration rate limit was not enforced after %s requests.\n' "$MAX_ATTEMPTS" >&2
  exit 1
fi

grep -Eiq "^Retry-After: ${EXPECTED_RETRY_AFTER}\\r?$" "$TEMP_DIR/headers"
grep -Eiq '^Content-Type: application/json' "$TEMP_DIR/headers"
grep -Eiq '^Cache-Control: no-store' "$TEMP_DIR/headers"
grep -Eiq '^X-Content-Type-Options: nosniff' "$TEMP_DIR/headers"
grep -Eiq '^X-Request-Id: [A-Fa-f0-9]+' "$TEMP_DIR/headers"
grep -q '"status":429' "$TEMP_DIR/body"
grep -q '"code":"RATE_LIMIT_EXCEEDED"' "$TEMP_DIR/body"
grep -q '"path":"/api/accounts/register"' "$TEMP_DIR/body"
grep -Eq '"requestId":"[A-Fa-f0-9]+"' "$TEMP_DIR/body"
curl -fsS "$BASE_URL/health" | grep -q ok

printf 'Registration rate limit enforced after %s requests with Retry-After %s.\n' "$attempt" "$EXPECTED_RETRY_AFTER"
