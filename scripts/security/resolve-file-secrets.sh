#!/bin/sh
set -eu

fail() {
  printf 'Configurazione segreto non valida per %s.\n' "$1" >&2
  exit 78
}

validate_name() {
  case "$1" in
    ''|[0-9]*|*[!A-Z0-9_]*) fail "$1" ;;
  esac
}

resolve_secret() {
  spec=$1
  optional=false

  case "$spec" in
    \?*)
      optional=true
      name=${spec#\?}
      ;;
    *)
      name=$spec
      ;;
  esac

  validate_name "$name"
  file_name="${name}_FILE"
  eval "value=\${$name-}"
  eval "file_value=\${$file_name-}"

  if [ -n "$value" ] && [ -n "$file_value" ]; then
    fail "$name"
  fi

  if [ -n "$file_value" ]; then
    if [ ! -f "$file_value" ] || [ ! -r "$file_value" ]; then
      fail "$name"
    fi

    value=$(cat "$file_value")
    if [ -z "$value" ]; then
      fail "$name"
    fi

    case "$value" in
      *'
'*) fail "$name" ;;
    esac

    export "$name=$value"
    unset "$file_name"
    return
  fi

  if [ -n "$value" ]; then
    case "$value" in
      *'
'*) fail "$name" ;;
    esac
    export "$name=$value"
    return
  fi

  unset "$name" "$file_name"
  if [ "$optional" = false ]; then
    fail "$name"
  fi
}

while [ "$#" -gt 0 ] && [ "$1" != "--" ]; do
  resolve_secret "$1"
  shift
done

if [ "$#" -eq 0 ]; then
  printf 'Comando di avvio mancante.\n' >&2
  exit 64
fi

shift
if [ "$#" -eq 0 ]; then
  printf 'Comando di avvio mancante.\n' >&2
  exit 64
fi

exec "$@"
