#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
service_file="$PROJECT_ROOT/deploy/systemd/gestionale-backup.service"
timer_file="$PROJECT_ROOT/deploy/systemd/gestionale-backup.timer"
drill_service_file="$PROJECT_ROOT/deploy/systemd/gestionale-restore-drill.service"
drill_timer_file="$PROJECT_ROOT/deploy/systemd/gestionale-restore-drill.timer"

grep -Fqx 'Type=oneshot' "$service_file"
grep -Fqx 'User=gestionale-backup' "$service_file"
grep -Fqx 'NoNewPrivileges=true' "$service_file"
grep -Fqx 'ProtectSystem=strict' "$service_file"
grep -Fqx 'ReadWritePaths=/var/backups/gestionale' "$service_file"
grep -Fqx 'ExecStart=/opt/gestionale/scripts/db/scheduled-backup.sh' "$service_file"
grep -Fqx 'OnCalendar=*-*-* 02:15:00' "$timer_file"
grep -Fqx 'Persistent=true' "$timer_file"
grep -Fqx 'RandomizedDelaySec=15m' "$timer_file"
grep -Fqx 'Type=oneshot' "$drill_service_file"
grep -Fqx 'User=gestionale-backup' "$drill_service_file"
grep -Fqx 'NoNewPrivileges=true' "$drill_service_file"
grep -Fqx 'ProtectSystem=strict' "$drill_service_file"
grep -Fqx 'ReadWritePaths=/var/backups/gestionale' "$drill_service_file"
grep -Fqx 'ExecStart=/opt/gestionale/scripts/db/restore-drill-latest.sh' "$drill_service_file"
grep -Fqx 'OnCalendar=Sun *-*-* 03:30:00' "$drill_timer_file"
grep -Fqx 'Persistent=true' "$drill_timer_file"
grep -Fqx 'RandomizedDelaySec=30m' "$drill_timer_file"

echo "Configurazione scheduling backup verificata."
