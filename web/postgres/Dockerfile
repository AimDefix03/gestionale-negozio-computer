FROM postgres:16-alpine

COPY scripts/security/resolve-file-secrets.sh /usr/local/bin/resolve-file-secrets

RUN chmod 0555 /usr/local/bin/resolve-file-secrets

USER postgres

ENTRYPOINT ["/usr/local/bin/resolve-file-secrets", "POSTGRES_PASSWORD", "--", "docker-entrypoint.sh"]

CMD ["postgres"]
