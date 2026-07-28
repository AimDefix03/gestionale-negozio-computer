#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$SCRIPT_DIR/common.sh"

export BACKUP_APPLY_RETENTION=true
backup_file=$("$SCRIPT_DIR/backup.sh")
BACKUP_DIR="$BACKUP_DIR" \
POSTGRES_DB="$POSTGRES_DB" \
BACKUP_FILE_PREFIX="$BACKUP_FILE_PREFIX" \
BACKUP_MAX_AGE_HOURS="${BACKUP_MAX_AGE_HOURS:-26}" \
  "$SCRIPT_DIR/check-backup-freshness.sh" "$backup_file" >/dev/null

echo "Backup schedulato completato: $backup_file"
