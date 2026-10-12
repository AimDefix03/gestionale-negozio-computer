#!/bin/sh
set -eu

BACKEND_CONTAINER=${1:-gestionale-prodlike-backend}
PROMETHEUS_URL=${2:-http://127.0.0.1:9091}
MANAGEMENT_PORT=${3:-9090}

fail() {
  printf '%s\n' "$1" >&2
  exit 1
}

docker exec "$BACKEND_CONTAINER" wget -qO- \
  --header='X-Request-Id: observability-runtime-check' \
  http://127.0.0.1:8080/api/products >/dev/null 2>&1 || true
sleep 1

metrics=$(docker exec "$BACKEND_CONTAINER" wget -qO- "http://127.0.0.1:$MANAGEMENT_PORT/actuator/prometheus")
printf '%s\n' "$metrics" | grep -q '^jvm_memory_used_bytes' || fail "Metrica JVM assente."
printf '%s\n' "$metrics" | grep -q '^http_server_requests_seconds' || fail "Metrica HTTP assente."
printf '%s\n' "$metrics" | grep -q '^hikaricp_connections_active' || fail "Metrica pool database assente."
printf '%s\n' "$metrics" | grep -q '^gestionale_authentication_attempts_total' || fail "Metrica autenticazione assente."

curl -fsS "$PROMETHEUS_URL/-/ready" | grep -q 'Prometheus Server is Ready' || fail "Prometheus non pronto."
targets=$(curl -fsS "$PROMETHEUS_URL/api/v1/targets")
printf '%s\n' "$targets" | grep -q '"health":"up"' || fail "Target backend Prometheus non disponibile."
printf '%s\n' "$targets" | grep -q '"job":"gestionale-backend"' || fail "Target backend non configurato."

rules=$(curl -fsS "$PROMETHEUS_URL/api/v1/rules")
for alert in \
  GestionaleBackendUnavailable \
  GestionaleElevatedServerErrors \
  GestionaleRepeatedLoginFailures \
  GestionaleJvmHeapPressure \
  GestionaleDatabasePoolSaturation
do
  printf '%s\n' "$rules" | grep -q "\"name\":\"$alert\"" || fail "Regola alert $alert non caricata."
done

logs=$(docker logs --since 30s "$BACKEND_CONTAINER" 2>&1)
printf '%s\n' "$logs" | grep -q '"service":"gestionale-api"' || fail "Campo service assente dai log JSON."
printf '%s\n' "$logs" | grep -q '"environment":"production"' || fail "Campo environment assente dai log JSON."
printf '%s\n' "$logs" | grep -q '"message":"HTTP request completed"' || fail "Evento HTTP strutturato assente."
printf '%s\n' "$logs" | grep -q '"requestId":"observability-runtime-check"' || fail "Request ID assente dai log strutturati."
printf '%s\n' "$logs" | grep -q '"http_method":' || fail "Metodo HTTP assente dai log strutturati."
printf '%s\n' "$logs" | grep -q '"http_path":' || fail "Percorso HTTP assente dai log strutturati."
printf '%s\n' "$logs" | grep -q '"http_status":' || fail "Status HTTP assente dai log strutturati."
printf '%s\n' "$logs" | grep -q '"duration_ms":' || fail "Durata HTTP assente dai log strutturati."

printf 'Log strutturati, metriche e regole di alert verificati.\n'
