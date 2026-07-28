#!/bin/sh
set -eu

NGINX_LOGIN_RATE=${NGINX_LOGIN_RATE:-10r/m}
NGINX_LOGIN_BURST=${NGINX_LOGIN_BURST:-10}
NGINX_LOGIN_RETRY_AFTER_SECONDS=${NGINX_LOGIN_RETRY_AFTER_SECONDS:-60}

if ! printf '%s\n' "$NGINX_LOGIN_RATE" | grep -Eq '^[1-9][0-9]*r/[smh]$'; then
  printf 'Configurazione NGINX_LOGIN_RATE non valida.\n' >&2
  exit 78
fi

for value in "$NGINX_LOGIN_BURST" "$NGINX_LOGIN_RETRY_AFTER_SECONDS"; do
  if ! printf '%s\n' "$value" | grep -Eq '^[1-9][0-9]*$'; then
    printf 'Configurazione numerica Nginx non valida.\n' >&2
    exit 78
  fi
done

export NGINX_LOGIN_RATE NGINX_LOGIN_BURST NGINX_LOGIN_RETRY_AFTER_SECONDS

mkdir -p \
  /tmp/nginx-client-body \
  /tmp/nginx-proxy \
  /tmp/nginx-fastcgi \
  /tmp/nginx-uwsgi \
  /tmp/nginx-scgi

envsubst '${NGINX_LOGIN_RATE} ${NGINX_LOGIN_BURST} ${NGINX_LOGIN_RETRY_AFTER_SECONDS}' \
  < /etc/nginx/templates/default.conf.template \
  > /tmp/default.conf

exec nginx -c /etc/nginx/nginx.conf -g 'daemon off;'
