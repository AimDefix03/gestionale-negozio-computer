#!/bin/sh
set -eu

if [ "$#" -ne 2 ]; then
  printf 'Uso: %s <container-postgres> <container-backend>\n' "$0" >&2
  exit 64
fi

postgres_container=$1
backend_container=$2

environment_for() {
  docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "$1"
}

mounts_for() {
  docker inspect --format '{{range .Mounts}}{{printf "%s|%t\n" .Destination .RW}}{{end}}' "$1"
}

assert_empty() {
  container=$1
  variable=$2
  value=$(environment_for "$container" | sed -n "s/^${variable}=//p")
  if [ -n "$value" ]; then
    printf 'Il container %s espone direttamente %s.\n' "$container" "$variable" >&2
    exit 1
  fi
}

assert_file_reference() {
  container=$1
  variable=$2
  expected=$3
  if ! environment_for "$container" | grep -Fqx "${variable}=${expected}"; then
    printf 'Il container %s non usa il riferimento file atteso per %s.\n' "$container" "$variable" >&2
    exit 1
  fi
}

assert_read_only_mount() {
  container=$1
  destination=$2
  if ! mounts_for "$container" | grep -Fqx "${destination}|false"; then
    printf 'Il container %s non monta %s in sola lettura.\n' "$container" "$destination" >&2
    exit 1
  fi
}

for mapping in \
  'POSTGRES_PASSWORD|/run/secrets/gestionale_db_bootstrap_password' \
  'GESTIONALE_DB_MIGRATOR_PASSWORD|/run/secrets/gestionale_db_migrator_password' \
  'GESTIONALE_DB_RUNTIME_PASSWORD|/run/secrets/gestionale_db_runtime_password' \
  'GESTIONALE_DB_BACKUP_PASSWORD|/run/secrets/gestionale_db_backup_password' \
  'GESTIONALE_DB_RESTORE_PASSWORD|/run/secrets/gestionale_db_restore_password'
do
  variable=${mapping%%|*}
  destination=${mapping#*|}
  assert_empty "$postgres_container" "$variable"
  assert_file_reference "$postgres_container" "${variable}_FILE" "$destination"
  assert_read_only_mount "$postgres_container" "$destination"
done

for mapping in \
  'GESTIONALE_DB_RUNTIME_PASSWORD|/run/secrets/gestionale_db_runtime_password' \
  'GESTIONALE_DB_MIGRATOR_PASSWORD|/run/secrets/gestionale_db_migrator_password'
do
  variable=${mapping%%|*}
  destination=${mapping#*|}
  assert_empty "$backend_container" "$variable"
  assert_file_reference "$backend_container" "${variable}_FILE" "$destination"
  assert_read_only_mount "$backend_container" "$destination"
done

assert_empty "$backend_container" GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD
bootstrap_file=$(environment_for "$backend_container" | sed -n 's/^GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD_FILE=//p')
if [ -n "$bootstrap_file" ]; then
  if [ "$bootstrap_file" != /run/secrets/gestionale_bootstrap_password ]; then
    printf 'Il container %s usa un riferimento bootstrap inatteso.\n' "$backend_container" >&2
    exit 1
  fi
  assert_read_only_mount "$backend_container" /run/secrets/gestionale_bootstrap_password
fi

printf 'Segreti container verificati: riferimenti file e mount read-only corretti.\n'
