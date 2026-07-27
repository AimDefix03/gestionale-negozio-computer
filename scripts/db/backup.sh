#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$SCRIPT_DIR/common.sh"
. "$SCRIPT_DIR/backup-lifecycle.sh"

umask 077
mkdir -p "$BACKUP_DIR"
timestamp=$(date -u +"%Y%m%dT%H%M%SZ")
backup_file=${1:-"$BACKUP_DIR/${BACKUP_FILE_PREFIX}${timestamp}.dump"}
backup_parent=$(dirname "$backup_file")
mkdir -p "$backup_parent"
tmp_file="${backup_file}.tmp"
checksum_file="${backup_file}.sha256"
tmp_checksum="${checksum_file}.tmp"
committed=false

if [ -e "$backup_file" ] || [ -e "$checksum_file" ]; then
  echo "Backup gia esistente: $backup_file" >&2
  exit 1
fi

cleanup() {
  rm -f "$tmp_file" "$tmp_checksum"
  if [ "$committed" != true ]; then
    rm -f "$backup_file" "$checksum_file"
  fi
  release_backup_lock
}

trap cleanup EXIT INT TERM

if ! acquire_backup_lock "$BACKUP_DIR" "${BACKUP_FILE_PREFIX}backup"; then
  echo "Backup gia in esecuzione: lock presente in $BACKUP_DIR." >&2
  exit 75
fi

wait_for_postgres

compose exec -T postgres pg_dump \
  -U "$GESTIONALE_DB_USERNAME" \
  -d "$POSTGRES_DB" \
  --format=custom \
  --no-owner \
  --no-privileges \
  > "$tmp_file"

if [ ! -s "$tmp_file" ]; then
  echo "Backup non creato: file vuoto." >&2
  exit 1
fi

if ! compose exec -T postgres pg_restore --list < "$tmp_file" >/dev/null; then
  echo "Backup non valido: pg_restore non riesce a leggere l'archivio." >&2
  exit 1
fi

write_backup_checksum "$tmp_file" "$tmp_checksum" "$(basename "$backup_file")"
mv "$tmp_file" "$backup_file"
mv "$tmp_checksum" "$checksum_file"
committed=true

if [ "${BACKUP_APPLY_RETENTION:-true}" = true ]; then
  prune_backups \
    "$BACKUP_DIR" \
    "$BACKUP_FILE_PREFIX" \
    "${BACKUP_RETENTION_DAYS:-14}" \
    "${BACKUP_RETENTION_COUNT:-30}"
fi

trap - EXIT INT TERM
release_backup_lock
echo "$backup_file"
