#!/usr/bin/env sh
set -eu

sha256_digest() {
  bl_digest_file=$1
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$bl_digest_file" | awk '{print $1}'
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$bl_digest_file" | awk '{print $1}'
  else
    echo "Nessun comando SHA-256 disponibile." >&2
    return 1
  fi
}

write_backup_checksum() {
  bl_write_archive=$1
  bl_write_checksum_file=$2
  bl_write_archive_name=${3:-$(basename "$bl_write_archive")}
  bl_write_digest=$(sha256_digest "$bl_write_archive")
  printf '%s  %s\n' "$bl_write_digest" "$bl_write_archive_name" > "$bl_write_checksum_file"
}

verify_backup_checksum() {
  bl_verify_archive=$1
  bl_verify_checksum_file=${2:-"${bl_verify_archive}.sha256"}

  if [ ! -s "$bl_verify_checksum_file" ]; then
    if [ "${ALLOW_UNVERIFIED_BACKUP:-no}" = yes ]; then
      echo "Avviso: restore esplicitamente autorizzato senza checksum." >&2
      return 0
    fi
    echo "Checksum mancante: $bl_verify_checksum_file" >&2
    return 1
  fi

  bl_verify_checksum_lines=$(wc -l < "$bl_verify_checksum_file" | tr -d ' ')
  if [ "$bl_verify_checksum_lines" -ne 1 ]; then
    echo "Formato checksum non valido." >&2
    return 1
  fi

  bl_verify_checksum_line=$(cat "$bl_verify_checksum_file")
  bl_verify_expected_digest=${bl_verify_checksum_line%%  *}
  bl_verify_expected_name=${bl_verify_checksum_line#*  }

  if ! printf '%s\n' "$bl_verify_expected_digest" | grep -Eq '^[0-9a-fA-F]{64}$'; then
    echo "Formato checksum non valido." >&2
    return 1
  fi

  if [ "$bl_verify_expected_name" != "$(basename "$bl_verify_archive")" ]; then
    echo "Il checksum non appartiene all'archivio richiesto." >&2
    return 1
  fi

  bl_verify_actual_digest=$(sha256_digest "$bl_verify_archive")
  if [ "$(printf '%s' "$bl_verify_expected_digest" | tr 'A-F' 'a-f')" != "$(printf '%s' "$bl_verify_actual_digest" | tr 'A-F' 'a-f')" ]; then
    echo "Checksum backup non valido." >&2
    return 1
  fi
}

acquire_backup_lock() {
  bl_lock_root=$1
  bl_lock_name=$2
  BACKUP_LOCK_DIR="$bl_lock_root/.${bl_lock_name}.lock"
  if ! mkdir "$BACKUP_LOCK_DIR" 2>/dev/null; then
    return 1
  fi
  printf 'pid=%s\nstarted_at=%s\n' "$$" "$(date -u +'%Y-%m-%dT%H:%M:%SZ')" > "$BACKUP_LOCK_DIR/owner"
  export BACKUP_LOCK_DIR
}

release_backup_lock() {
  if [ -n "${BACKUP_LOCK_DIR:-}" ] && [ -d "$BACKUP_LOCK_DIR" ]; then
    rm -f "$BACKUP_LOCK_DIR/owner"
    rmdir "$BACKUP_LOCK_DIR"
  fi
  BACKUP_LOCK_DIR=
  export BACKUP_LOCK_DIR
}

remove_backup_set() {
  bl_remove_archive=$1
  rm -f "$bl_remove_archive" "${bl_remove_archive}.sha256"
}

validate_positive_integer() {
  bl_integer_name=$1
  bl_integer_value=$2
  if ! printf '%s\n' "$bl_integer_value" | grep -Eq '^[1-9][0-9]*$'; then
    echo "$bl_integer_name deve essere un intero positivo." >&2
    return 1
  fi
}

prune_backups() {
  bl_prune_backup_dir=$1
  bl_prune_prefix=$2
  bl_prune_retention_days=$3
  bl_prune_retention_count=$4

  validate_positive_integer BACKUP_RETENTION_DAYS "$bl_prune_retention_days"
  validate_positive_integer BACKUP_RETENTION_COUNT "$bl_prune_retention_count"

  find "$bl_prune_backup_dir" -maxdepth 1 -type f -name "${bl_prune_prefix}*.dump" -mtime "+$bl_prune_retention_days" -print |
    while IFS= read -r bl_prune_archive; do
      remove_backup_set "$bl_prune_archive"
    done

  bl_prune_archives=$(find "$bl_prune_backup_dir" -maxdepth 1 -type f -name "${bl_prune_prefix}*.dump" -print | sort -r)
  printf '%s\n' "$bl_prune_archives" | awk -v keep="$bl_prune_retention_count" 'NF && NR > keep' |
    while IFS= read -r bl_prune_archive; do
      remove_backup_set "$bl_prune_archive"
    done
}

file_mtime_epoch() {
  bl_mtime_file=$1
  if stat -c %Y "$bl_mtime_file" >/dev/null 2>&1; then
    stat -c %Y "$bl_mtime_file"
  else
    stat -f %m "$bl_mtime_file"
  fi
}

latest_backup() {
  bl_latest_backup_dir=$1
  bl_latest_prefix=$2
  find "$bl_latest_backup_dir" -maxdepth 1 -type f -name "${bl_latest_prefix}*.dump" -print | sort -r | sed -n '1p'
}
