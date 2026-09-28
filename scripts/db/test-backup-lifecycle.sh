#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$SCRIPT_DIR/backup-lifecycle.sh"

work_dir=$(mktemp -d "${TMPDIR:-/tmp}/gestionale-backup-lifecycle.XXXXXX")
trap 'rm -rf "$work_dir"' EXIT INT TERM

archive="$work_dir/gestionale_test_20260724T010000Z.dump"
printf 'archive-content' > "$archive"
checksum_file="${archive}.sha256"
write_backup_checksum "$archive" "${archive}.sha256"
if [ "$checksum_file" != "${archive}.sha256" ]; then
  echo "La funzione checksum ha contaminato le variabili del chiamante." >&2
  exit 1
fi
verify_backup_checksum "$archive"

printf 'corruption' >> "$archive"
if verify_backup_checksum "$archive" >/dev/null 2>&1; then
  echo "Un archivio corrotto e stato accettato." >&2
  exit 1
fi

printf 'archive-content' > "$archive"
write_backup_checksum "$archive" "${archive}.sha256"

if ! acquire_backup_lock "$work_dir" backup-test; then
  echo "Impossibile acquisire il lock iniziale." >&2
  exit 1
fi
if acquire_backup_lock "$work_dir" backup-test; then
  echo "Il lock concorrente non e stato bloccato." >&2
  exit 1
fi
release_backup_lock

for stamp in 20260724T020000Z 20260724T030000Z 20260724T040000Z; do
  candidate="$work_dir/gestionale_test_${stamp}.dump"
  printf '%s' "$stamp" > "$candidate"
  write_backup_checksum "$candidate" "${candidate}.sha256"
done

prune_backups "$work_dir" gestionale_test_ 3650 2
remaining=$(find "$work_dir" -maxdepth 1 -type f -name 'gestionale_test_*.dump' | wc -l | tr -d ' ')
if [ "$remaining" -ne 2 ]; then
  echo "Retention per quantita non applicata correttamente." >&2
  exit 1
fi

old_archive="$work_dir/gestionale_test_20200101T000000Z.dump"
printf 'old' > "$old_archive"
write_backup_checksum "$old_archive" "${old_archive}.sha256"
touch -t 202001010000 "$old_archive" "${old_archive}.sha256"
prune_backups "$work_dir" gestionale_test_ 1 10
if [ -e "$old_archive" ] || [ -e "${old_archive}.sha256" ]; then
  echo "Retention per eta non applicata correttamente." >&2
  exit 1
fi

BACKUP_MAX_AGE_HOURS=1 BACKUP_DIR="$work_dir" POSTGRES_DB=test "$SCRIPT_DIR/check-backup-freshness.sh" >/dev/null

echo "Ciclo di vita backup verificato: checksum, lock, retention e freschezza."
