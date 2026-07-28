#!/bin/sh
set -eu

if [ "$#" -ne 3 ]; then
  printf 'Uso: %s <container-postgres> <container-backend> <container-frontend>\n' "$0" >&2
  exit 64
fi

postgres_container=$1
backend_container=$2
frontend_container=$3

fail() {
  printf '%s\n' "$1" >&2
  exit 1
}

assert_non_root() {
  container=$1
  configured_user=$(docker inspect --format '{{.Config.User}}' "$container")
  case "$configured_user" in
    ''|0|0:0|root|root:root)
      fail "Il container $container non dichiara un utente non-root."
      ;;
  esac

  runtime_uid=$(docker exec "$container" id -u)
  if [ "$runtime_uid" = 0 ]; then
    fail "Il processo del container $container viene eseguito come root."
  fi
}

assert_runtime_policy() {
  container=$1

  if [ "$(docker inspect --format '{{.HostConfig.ReadonlyRootfs}}' "$container")" != true ]; then
    fail "Il root filesystem del container $container non e read-only."
  fi

  if [ "$(docker inspect --format '{{.HostConfig.Privileged}}' "$container")" != false ]; then
    fail "Il container $container e privilegiato."
  fi

  if ! docker inspect --format '{{json .HostConfig.SecurityOpt}}' "$container" | grep -q 'no-new-privileges'; then
    fail "Il container $container non applica no-new-privileges."
  fi

  if ! docker inspect --format '{{json .HostConfig.CapDrop}}' "$container" | grep -q 'ALL'; then
    fail "Il container $container non elimina tutte le capability."
  fi

  if docker exec "$container" sh -c 'touch /.__gestionale_hardening_probe 2>/dev/null'; then
    docker exec "$container" rm -f /.__gestionale_hardening_probe >/dev/null 2>&1 || true
    fail "Il root filesystem del container $container risulta scrivibile."
  fi

  docker exec "$container" sh -c 'probe=/tmp/.gestionale-write-probe; : > "$probe"; rm -f "$probe"'
}

assert_tmpfs() {
  container=$1
  destination=$2
  if ! docker inspect --format '{{json .HostConfig.Tmpfs}}' "$container" | grep -q "\"$destination\""; then
    fail "Il container $container non monta $destination come tmpfs."
  fi
}

assert_no_writable_mounts() {
  container=$1
  writable_mounts=$(docker inspect --format '{{range .Mounts}}{{if .RW}}{{println .Destination}}{{end}}{{end}}' "$container")
  if [ -n "$writable_mounts" ]; then
    fail "Il container $container espone mount persistenti scrivibili non autorizzati."
  fi
}

assert_only_postgres_data_writable() {
  container=$1
  writable_mounts=$(docker inspect --format '{{range .Mounts}}{{if .RW}}{{println .Destination}}{{end}}{{end}}' "$container")
  if [ "$writable_mounts" != /var/lib/postgresql/data ]; then
    fail "Il container $container deve scrivere soltanto nel volume dati PostgreSQL."
  fi
}

for container in "$postgres_container" "$backend_container" "$frontend_container"; do
  assert_non_root "$container"
  assert_runtime_policy "$container"
  assert_tmpfs "$container" /tmp
done

assert_tmpfs "$postgres_container" /var/run/postgresql
assert_only_postgres_data_writable "$postgres_container"
assert_no_writable_mounts "$backend_container"
assert_no_writable_mounts "$frontend_container"

printf 'Hardening container verificato: processi non-root, filesystem read-only e privilegi minimi.\n'
