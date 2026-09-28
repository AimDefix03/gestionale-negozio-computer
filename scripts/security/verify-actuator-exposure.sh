#!/bin/sh
set -eu

BASE_URL=${1:-http://127.0.0.1:8080}

for path in \
  /actuator/health \
  /actuator/health/liveness \
  /actuator/health/readiness \
  /actuator/info \
  /actuator/metrics \
  /actuator/prometheus
do
  status=$(curl -sS -o /dev/null -w '%{http_code}' "$BASE_URL$path")
  case "$status" in
    401|403|404)
      ;;
    *)
      printf 'Actuator esposto sulla porta applicativa: %s ha restituito HTTP %s\n' "$path" "$status" >&2
      exit 1
      ;;
  esac
done

printf 'Actuator non esposto sulla porta applicativa %s\n' "$BASE_URL"
