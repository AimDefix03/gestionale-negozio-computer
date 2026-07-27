FROM node:22-alpine AS build

WORKDIR /workspace

COPY package.json package-lock.json ./
RUN npm ci

COPY index.html tsconfig.json tsconfig.node.json vite.config.ts ./
COPY src ./src

RUN npm run build

FROM nginx:1.27-alpine

COPY nginx-main.conf /etc/nginx/nginx.conf
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY security-headers.conf /etc/nginx/security-headers.conf
COPY docker-entrypoint.sh /usr/local/bin/gestionale-frontend-entrypoint
COPY --from=build /workspace/dist /usr/share/nginx/html

RUN NGINX_LOGIN_RATE=10r/m \
    NGINX_LOGIN_BURST=10 \
    NGINX_LOGIN_RETRY_AFTER_SECONDS=60 \
    envsubst '${NGINX_LOGIN_RATE} ${NGINX_LOGIN_BURST} ${NGINX_LOGIN_RETRY_AFTER_SECONDS}' \
    < /etc/nginx/templates/default.conf.template \
    > /tmp/default.conf \
    && sed -i 's#http://backend:8080#http://127.0.0.1:8080#g' /tmp/default.conf \
    && nginx -t \
    && rm /tmp/default.conf \
    && rm -f /etc/nginx/conf.d/default.conf \
    && chmod 0555 /usr/local/bin/gestionale-frontend-entrypoint

USER nginx

EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/gestionale-frontend-entrypoint"]
