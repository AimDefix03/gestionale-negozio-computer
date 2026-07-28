#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
IMAGE=${PROMETHEUS_IMAGE:-prom/prometheus:v3.13.1}

docker run --rm \
  --entrypoint=/bin/promtool \
  -v "$ROOT_DIR/deploy/observability:/etc/prometheus:ro" \
  "$IMAGE" \
  check config /etc/prometheus/prometheus.yml

printf 'Configurazione Prometheus e regole di alert valide.\n'
