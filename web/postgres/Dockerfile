FROM postgres:16-alpine

COPY scripts/security/resolve-file-secrets.sh /usr/local/bin/resolve-file-secrets
COPY scripts/db/postgresql-roles.sql /usr/local/share/gestionale/postgresql-roles.sql
COPY scripts/db/postgresql-database-grants.sql /usr/local/share/gestionale/postgresql-database-grants.sql
COPY web/postgres/initdb/10-create-application-roles.sh /docker-entrypoint-initdb.d/10-create-application-roles.sh

RUN chmod 0555 \
    /usr/local/bin/resolve-file-secrets \
    /docker-entrypoint-initdb.d/10-create-application-roles.sh \
    && chmod 0444 \
    /usr/local/share/gestionale/postgresql-roles.sql \
    /usr/local/share/gestionale/postgresql-database-grants.sql

USER postgres

ENTRYPOINT ["/usr/local/bin/resolve-file-secrets", "POSTGRES_PASSWORD", "GESTIONALE_DB_MIGRATOR_PASSWORD", "GESTIONALE_DB_RUNTIME_PASSWORD", "GESTIONALE_DB_BACKUP_PASSWORD", "GESTIONALE_DB_RESTORE_PASSWORD", "--", "docker-entrypoint.sh"]

CMD ["postgres"]
