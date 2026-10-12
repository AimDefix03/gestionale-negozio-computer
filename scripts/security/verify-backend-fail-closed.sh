#!/bin/sh

set -eu

JAR_PATH="${1:-web/backend/target/gestionale-api-0.1.0.jar}"
TIMEOUT_SECONDS="${GESTIONALE_STARTUP_FAILURE_TIMEOUT_SECONDS:-30}"
JAVA_BIN="${JAVA_BIN:-$(command -v java)}"

if [ ! -f "$JAR_PATH" ]; then
    printf 'JAR backend non trovato: %s\n' "$JAR_PATH" >&2
    exit 1
fi

WORK_DIR="$(mktemp -d)"
ACTIVE_PID=""

cleanup() {
    if [ -n "$ACTIVE_PID" ] && kill -0 "$ACTIVE_PID" 2>/dev/null; then
        kill "$ACTIVE_PID" 2>/dev/null || true
        wait "$ACTIVE_PID" 2>/dev/null || true
    fi
    find "$WORK_DIR" -type f -delete
    rmdir "$WORK_DIR"
}

trap cleanup EXIT HUP INT TERM

expect_startup_failure() {
    label="$1"
    shift
    log_file="$WORK_DIR/$label.log"

    env -i PATH="$PATH" HOME="${HOME:-/tmp}" "$JAVA_BIN" -jar "$JAR_PATH" "$@" >"$log_file" 2>&1 &
    ACTIVE_PID=$!
    elapsed=0

    while kill -0 "$ACTIVE_PID" 2>/dev/null; do
        if [ "$elapsed" -ge "$TIMEOUT_SECONDS" ]; then
            printf '%s: il backend non si e arrestato entro %s secondi.\n' "$label" "$TIMEOUT_SECONDS" >&2
            cat "$log_file" >&2
            return 1
        fi
        sleep 1
        elapsed=$((elapsed + 1))
    done

    set +e
    wait "$ACTIVE_PID"
    status=$?
    set -e
    ACTIVE_PID=""

    if [ "$status" -eq 0 ]; then
        printf '%s: il backend e terminato con stato zero invece di fallire.\n' "$label" >&2
        cat "$log_file" >&2
        return 1
    fi
    if grep -q "Started GestionaleApiApplication" "$log_file"; then
        printf '%s: il backend ha raggiunto lo stato avviato prima dell arresto.\n' "$label" >&2
        cat "$log_file" >&2
        return 1
    fi

    printf '%s: arresto fail-closed verificato.\n' "$label"
}

expect_startup_failure no-profile
expect_startup_failure prod-without-secrets --spring.profiles.active=prod
