FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY web/backend/pom.xml .
COPY web/backend/src ./src

RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --gid 10001 gestionale \
    && useradd --uid 10001 --gid 10001 --no-create-home --home-dir /app --shell /usr/sbin/nologin gestionale

WORKDIR /app

COPY --from=build /workspace/target/gestionale-api-0.1.0.jar app.jar
COPY scripts/security/resolve-file-secrets.sh /usr/local/bin/resolve-file-secrets

RUN chmod 0555 /usr/local/bin/resolve-file-secrets

USER 10001:10001

EXPOSE 8080 9090

ENTRYPOINT ["/usr/local/bin/resolve-file-secrets", "GESTIONALE_DB_PASSWORD", "?GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD", "--", "java", "-jar", "/app/app.jar"]
