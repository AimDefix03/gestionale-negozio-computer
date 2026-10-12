#!/bin/sh

DOCKER_RUN_PROJECT_LABEL=com.docker.compose.project
DOCKER_RUN_ID_LABEL=it.giovannidefilippo.gestionale.run-id

docker_run_validate_project_name() (
  project_name=$1
  required_prefix=$2
  allowed=$(printf '%s' "$project_name" | tr -cd 'a-z0-9_-')

  case "$project_name" in
    "$required_prefix"*) ;;
    *)
      printf 'Nome progetto Docker non consentito: %s. Prefisso richiesto: %s\n' "$project_name" "$required_prefix" >&2
      exit 64
      ;;
  esac

  if [ "$allowed" != "$project_name" ] || [ "$project_name" = "$required_prefix" ]; then
    printf 'Nome progetto Docker non valido: %s\n' "$project_name" >&2
    exit 64
  fi
)

docker_run_validate_id() (
  run_id=$1
  allowed=$(printf '%s' "$run_id" | tr -cd 'a-z0-9_.-')

  if [ -z "$run_id" ] || [ "$allowed" != "$run_id" ]; then
    printf 'Run ID Docker non valido: %s\n' "$run_id" >&2
    exit 64
  fi
)

docker_run_resource_ids() (
  resource_type=$1
  project_name=$2
  run_id=${3:-}

  case "$resource_type" in
    container)
      if [ -n "$run_id" ]; then
        docker ps -aq \
          --filter "label=$DOCKER_RUN_PROJECT_LABEL=$project_name" \
          --filter "label=$DOCKER_RUN_ID_LABEL=$run_id"
      else
        docker ps -aq --filter "label=$DOCKER_RUN_PROJECT_LABEL=$project_name"
      fi
      ;;
    volume)
      if [ -n "$run_id" ]; then
        docker volume ls -q \
          --filter "label=$DOCKER_RUN_PROJECT_LABEL=$project_name" \
          --filter "label=$DOCKER_RUN_ID_LABEL=$run_id"
      else
        docker volume ls -q --filter "label=$DOCKER_RUN_PROJECT_LABEL=$project_name"
      fi
      ;;
    network)
      if [ -n "$run_id" ]; then
        docker network ls -q \
          --filter "label=$DOCKER_RUN_PROJECT_LABEL=$project_name" \
          --filter "label=$DOCKER_RUN_ID_LABEL=$run_id"
      else
        docker network ls -q --filter "label=$DOCKER_RUN_PROJECT_LABEL=$project_name"
      fi
      ;;
    *)
      printf 'Tipo risorsa Docker non supportato: %s\n' "$resource_type" >&2
      exit 64
      ;;
  esac
)

docker_run_resource_label() (
  resource_type=$1
  resource_id=$2

  case "$resource_type" in
    container)
      docker inspect --format "{{ index .Config.Labels \"$DOCKER_RUN_ID_LABEL\" }}" "$resource_id"
      ;;
    volume)
      docker volume inspect --format "{{ index .Labels \"$DOCKER_RUN_ID_LABEL\" }}" "$resource_id"
      ;;
    network)
      docker network inspect --format "{{ index .Labels \"$DOCKER_RUN_ID_LABEL\" }}" "$resource_id"
      ;;
  esac
)

docker_run_assert_project_unused() (
  project_name=$1
  found=0

  for resource_type in container volume network; do
    resource_ids=$(docker_run_resource_ids "$resource_type" "$project_name")
    if [ -n "$resource_ids" ]; then
      printf 'Collisione progetto Docker %s: risorse %s gia presenti: %s\n' "$project_name" "$resource_type" "$resource_ids" >&2
      found=1
    fi
  done

  [ "$found" -eq 0 ]
)

docker_run_assert_project_owned() (
  project_name=$1
  run_id=$2
  invalid=0

  for resource_type in container volume network; do
    resource_ids=$(docker_run_resource_ids "$resource_type" "$project_name")
    for resource_id in $resource_ids; do
      actual_run_id=$(docker_run_resource_label "$resource_type" "$resource_id" 2>/dev/null || true)
      if [ "$actual_run_id" != "$run_id" ]; then
        printf 'Cleanup rifiutato: risorsa %s %s del progetto %s priva del run ID atteso %s.\n' \
          "$resource_type" "$resource_id" "$project_name" "$run_id" >&2
        invalid=1
      fi
    done
  done

  [ "$invalid" -eq 0 ]
)

docker_run_inventory() (
  project_name=$1
  run_id=$2
  inventory_file=$3
  temporary_file="$inventory_file.tmp.$$"

  {
    printf 'project=%s\n' "$project_name"
    printf 'run_id=%s\n' "$run_id"
    printf 'captured_at=%s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
    for resource_type in container volume network; do
      printf '[%ss]\n' "$resource_type"
      docker_run_resource_ids "$resource_type" "$project_name" "$run_id"
    done
  } >"$temporary_file"

  mv "$temporary_file" "$inventory_file"
)

docker_run_cleanup() (
  project_name=$1
  run_id=$2
  inventory_file=$3
  cleanup_status=0

  docker_run_validate_id "$run_id"
  docker_run_inventory "$project_name" "$run_id" "$inventory_file"
  docker_run_assert_project_owned "$project_name" "$run_id" || exit 1

  container_ids=$(docker_run_resource_ids container "$project_name" "$run_id")
  for container_id in $container_ids; do
    docker rm -f "$container_id" >/dev/null || cleanup_status=1
  done

  volume_ids=$(docker_run_resource_ids volume "$project_name" "$run_id")
  for volume_id in $volume_ids; do
    docker volume rm "$volume_id" >/dev/null || cleanup_status=1
  done

  network_ids=$(docker_run_resource_ids network "$project_name" "$run_id")
  for network_id in $network_ids; do
    docker network rm "$network_id" >/dev/null || cleanup_status=1
  done

  docker_run_inventory "$project_name" "$run_id" "$inventory_file.after"
  docker_run_assert_project_unused "$project_name" || cleanup_status=1
  exit "$cleanup_status"
)
