#!/bin/sh
set -eu

BASE_URL=${1:-http://127.0.0.1:8081}
ROOT_HEADERS=$(mktemp)
ROOT_BODY=$(mktemp)
API_HEADERS=$(mktemp)
ASSET_HEADERS=$(mktemp)

cleanup() {
  rm -f "$ROOT_HEADERS" "$ROOT_BODY" "$API_HEADERS" "$ASSET_HEADERS"
}

trap cleanup EXIT INT TERM

header_value() {
  file=$1
  name=$2
  awk -v name="$name" 'index(tolower($0), tolower(name) ":") == 1 { sub(/^[^:]*:[[:space:]]*/, ""); sub(/\r$/, ""); print; exit }' "$file"
}

require_equal() {
  actual=$1
  expected=$2
  label=$3
  if [ "$actual" != "$expected" ]; then
    echo "$label non valido: atteso '$expected', ricevuto '$actual'" >&2
    exit 1
  fi
}

require_contains() {
  actual=$1
  expected=$2
  label=$3
  case "$actual" in
    *"$expected"*) ;;
    *)
      echo "$label non contiene '$expected': $actual" >&2
      exit 1
      ;;
  esac
}

require_absent() {
  actual=$1
  forbidden=$2
  label=$3
  case "$actual" in
    *"$forbidden"*)
      echo "$label contiene il valore vietato '$forbidden': $actual" >&2
      exit 1
      ;;
    *) ;;
  esac
}

verify_common_headers() {
  file=$1
  csp=$(header_value "$file" Content-Security-Policy)
  require_contains "$csp" "default-src 'self'" "Content-Security-Policy"
  require_contains "$csp" "script-src 'self'" "Content-Security-Policy"
  require_contains "$csp" "script-src-attr 'none'" "Content-Security-Policy"
  require_contains "$csp" "style-src 'self'" "Content-Security-Policy"
  require_contains "$csp" "style-src-attr 'none'" "Content-Security-Policy"
  require_contains "$csp" "connect-src 'self'" "Content-Security-Policy"
  require_contains "$csp" "object-src 'none'" "Content-Security-Policy"
  require_contains "$csp" "frame-ancestors 'none'" "Content-Security-Policy"
  require_absent "$csp" "'unsafe-inline'" "Content-Security-Policy"
  require_absent "$csp" "'unsafe-eval'" "Content-Security-Policy"
  require_equal "$(header_value "$file" Cross-Origin-Opener-Policy)" "same-origin" "Cross-Origin-Opener-Policy"
  require_equal "$(header_value "$file" Cross-Origin-Resource-Policy)" "same-origin" "Cross-Origin-Resource-Policy"
  require_equal "$(header_value "$file" Referrer-Policy)" "strict-origin-when-cross-origin" "Referrer-Policy"
  require_equal "$(header_value "$file" X-Content-Type-Options)" "nosniff" "X-Content-Type-Options"
  require_equal "$(header_value "$file" X-Frame-Options)" "DENY" "X-Frame-Options"
}

curl -fsS -D "$ROOT_HEADERS" -o "$ROOT_BODY" "$BASE_URL/"
verify_common_headers "$ROOT_HEADERS"
require_equal "$(header_value "$ROOT_HEADERS" Cache-Control)" "no-store" "Cache-Control della shell HTML"
require_absent "$(header_value "$ROOT_HEADERS" Server)" "/" "Header Server"

curl -sS -D "$API_HEADERS" -o /dev/null "$BASE_URL/api/accounts/login"
verify_common_headers "$API_HEADERS"
if [ -z "$(header_value "$API_HEADERS" X-Request-Id)" ]; then
  echo "X-Request-Id assente dalla risposta API" >&2
  exit 1
fi

ASSET_PATH=$(sed -n 's#.*src="\(/assets/[^"?]*\.js\).*#\1#p' "$ROOT_BODY" | head -n 1)
if [ -z "$ASSET_PATH" ]; then
  echo "Asset JavaScript versionato non trovato nella shell HTML" >&2
  exit 1
fi

curl -fsS -D "$ASSET_HEADERS" -o /dev/null "$BASE_URL$ASSET_PATH"
verify_common_headers "$ASSET_HEADERS"
require_equal "$(header_value "$ASSET_HEADERS" Cache-Control)" "public, max-age=31536000, immutable" "Cache-Control degli asset"

echo "Header browser, CSP e cache frontend verificati."
