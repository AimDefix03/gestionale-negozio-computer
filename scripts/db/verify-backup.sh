#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$SCRIPT_DIR/common.sh"
. "$SCRIPT_DIR/backup-lifecycle.sh"

backup_file=${1:-}

if [ -z "$backup_file" ] || [ ! -s "$backup_file" ]; then
  echo "Uso: scripts/db/verify-backup.sh percorso/backup.dump" >&2
  exit 2
fi

verify_backup_checksum "$backup_file"
wait_for_postgres
compose exec -T postgres pg_restore --list < "$backup_file" >/dev/null

echo "Backup verificato: $backup_file"
