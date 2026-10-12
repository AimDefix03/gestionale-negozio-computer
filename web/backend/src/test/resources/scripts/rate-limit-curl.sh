#!/bin/sh
set -eu

headers=
body=
url=
while [ "$#" -gt 0 ]; do
  case "$1" in
    -D) headers=$2; shift 2 ;;
    -o) body=$2; shift 2 ;;
    -w|-H|-X|--data) shift 2 ;;
    -*) shift ;;
    *) url=$1; shift ;;
  esac
done

case "$url" in
  http://fixture.invalid/health)
    if [ "$RATE_LIMIT_FIXTURE" = BAD_HEALTH ]; then
      printf 'unavailable'
    else
      printf 'ok'
    fi
    exit 0
    ;;
  http://fixture.invalid/api/accounts/login|http://fixture.invalid/api/accounts/register) ;;
  *) exit 2 ;;
esac

ending='\r\n'
if [ "$RATE_LIMIT_FIXTURE" = LF ]; then ending='\n'; fi
retry_after=60
if [ "$RATE_LIMIT_FIXTURE" = WRONG_DELAY ]; then retry_after=600; fi
{
  printf 'HTTP/1.1 429 Too Many Requests%b' "$ending"
  printf 'Retry-After: %s%b' "$retry_after" "$ending"
  printf 'Content-Type: application/json%b' "$ending"
  printf 'Cache-Control: no-store%b' "$ending"
  printf 'X-Content-Type-Options: nosniff%b' "$ending"
  if [ "$RATE_LIMIT_FIXTURE" != MISSING_REQUEST_ID ]; then
    printf 'X-Request-Id: abc123%b' "$ending"
  fi
} >"$headers"
code=RATE_LIMIT_EXCEEDED
if [ "$RATE_LIMIT_FIXTURE" = WRONG_CODE ]; then code=OTHER_ERROR; fi
printf '{"status":429,"code":"%s","requestId":"abc123","path":"%s"}' \
  "$code" "${url#http://fixture.invalid}" >"$body"
case "$RATE_LIMIT_FIXTURE" in
  NO_LIMIT) printf '400' ;;
  UNEXPECTED_STATUS) printf '500' ;;
  *) printf '429' ;;
esac
