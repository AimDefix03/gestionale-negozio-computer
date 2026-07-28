#!/bin/sh
set -eu

CONTAINER=${1:-gestionale-prodlike-prometheus}

fail() {
  printf '%s\n' "$1" >&2
  exit 1
}

configured_user=$(docker inspect --format '{{.Config.User}}' "$CONTAINER")
case "$configured_user" in
  ''|0|0:0|root|root:root)
    fail "Prometheus non dichiara un utente non-root."
    ;;
esac

[ "$(docker exec "$CONTAINER" id -u)" != 0 ] || fail "Prometheus viene eseguito come root."
[ "$(docker inspect --format '{{.HostConfig.ReadonlyRootfs}}' "$CONTAINER")" = true ] || fail "Root filesystem Prometheus non read-only."
[ "$(docker inspect --format '{{.HostConfig.Privileged}}' "$CONTAINER")" = false ] || fail "Prometheus e privilegiato."
docker inspect --format '{{json .HostConfig.SecurityOpt}}' "$CONTAINER" | grep -q 'no-new-privileges' || fail "no-new-privileges assente."
docker inspect --format '{{json .HostConfig.CapDrop}}' "$CONTAINER" | grep -q 'ALL' || fail "Capability non eliminate."
docker inspect --format '{{json .HostConfig.Tmpfs}}' "$CONTAINER" | grep -q '"/tmp"' || fail "tmpfs /tmp assente."

writable_mounts=$(docker inspect --format '{{range .Mounts}}{{if .RW}}{{println .Destination}}{{end}}{{end}}' "$CONTAINER")
[ "$writable_mounts" = /prometheus ] || fail "Prometheus deve scrivere soltanto nel volume /prometheus."

readonly_configs=$(docker inspect --format '{{range .Mounts}}{{if not .RW}}{{println .Destination}}{{end}}{{end}}' "$CONTAINER")
printf '%s\n' "$readonly_configs" | grep -q '^/etc/prometheus/prometheus.yml$' || fail "Configurazione Prometheus non montata read-only."
printf '%s\n' "$readonly_configs" | grep -q '^/etc/prometheus/alerts.yml$' || fail "Regole alert non montate read-only."

printf 'Hardening Prometheus verificato.\n'
