#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
. "$SCRIPT_DIR/backup-lifecycle.sh"

POSTGRES_DB=${POSTGRES_DB:-gestionale}
BACKUP_DIR=${BACKUP_DIR:-"$PROJECT_ROOT/backups"}
case "$BACKUP_DIR" in
  /*) ;;
  *) BACKUP_DIR="$PROJECT_ROOT/$BACKUP_DIR" ;;
esac

safe_db_name=$(printf '%s' "$POSTGRES_DB" | tr -c 'A-Za-z0-9_-' '_')
BACKUP_FILE_PREFIX=${BACKUP_FILE_PREFIX:-"gestionale_${safe_db_name}_"}
BACKUP_MAX_AGE_HOURS=${BACKUP_MAX_AGE_HOURS:-26}
validate_positive_integer BACKUP_MAX_AGE_HOURS "$BACKUP_MAX_AGE_HOURS"

backup_file=${1:-$(latest_backup "$BACKUP_DIR" "$BACKUP_FILE_PREFIX")}
if [ -z "$backup_file" ] || [ ! -s "$backup_file" ]; then
  echo "Nessun backup disponibile per il controllo di freschezza." >&2
  exit 1
fi

verify_backup_checksum "$backup_file"
now=$(date +%s)
modified=$(file_mtime_epoch "$backup_file")
age_seconds=$((now - modified))
max_age_seconds=$((BACKUP_MAX_AGE_HOURS * 3600))

if [ "$age_seconds" -lt 0 ] || [ "$age_seconds" -gt "$max_age_seconds" ]; then
  echo "Backup assente o troppo vecchio rispetto all'RPO configurato." >&2
  exit 1
fi

echo "Backup recente e integro: $backup_file"
