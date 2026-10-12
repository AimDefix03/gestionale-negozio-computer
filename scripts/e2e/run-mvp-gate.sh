#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)

sh "$ROOT_DIR/scripts/ci/run-prod-like-verification.sh"
sh "$ROOT_DIR/scripts/db/verify-postgresql-suite.sh" fast

printf 'Gate E2E MVP completato: workflow verticali, PostgreSQL e upgrade popolati verificati.\n'
