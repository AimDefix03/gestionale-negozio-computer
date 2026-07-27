#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
  printf 'Uso: %s <compose-project-name>\n' "$0" >&2
  exit 64
fi

PROJECT_NAME=$1
ATTEMPT=1
MAX_ATTEMPTS=5

while [ "$ATTEMPT" -le "$MAX_ATTEMPTS" ]; do
  CONTAINERS=$(docker ps -aq --filter "label=com.docker.compose.project=$PROJECT_NAME")
  VOLUMES=$(docker volume ls -q --filter "label=com.docker.compose.project=$PROJECT_NAME")
  NETWORKS=$(docker network ls -q --filter "label=com.docker.compose.project=$PROJECT_NAME")

  if [ -z "$CONTAINERS" ] && [ -z "$VOLUMES" ] && [ -z "$NETWORKS" ]; then
    printf 'Cleanup Docker verificato per il progetto %s.\n' "$PROJECT_NAME"
    exit 0
  fi

  ATTEMPT=$((ATTEMPT + 1))
  sleep 1
done

printf 'Cleanup Docker incompleto per il progetto %s.\n' "$PROJECT_NAME" >&2
[ -z "$CONTAINERS" ] || printf 'Container residui: %s\n' "$CONTAINERS" >&2
[ -z "$VOLUMES" ] || printf 'Volumi residui: %s\n' "$VOLUMES" >&2
[ -z "$NETWORKS" ] || printf 'Reti residue: %s\n' "$NETWORKS" >&2
exit 1
