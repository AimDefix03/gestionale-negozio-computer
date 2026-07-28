# Docker

## Scopo

Il file `docker-compose.prod-like.yml` avvia uno stack completo del gestionale web:

- PostgreSQL 16;
- backend Spring Boot con profilo `prod`;
- frontend React servito da Nginx;
- proxy Nginx da `/api` verso il backend;
- rate limiting per IP sull'endpoint login, con errore JSON `429` e request ID;
- CSP in enforcement, header browser uniformi e cache sicura per shell e asset;
- health check su database, backend e frontend.
- processi non-root, filesystem root read-only, `no-new-privileges` e capability Linux eliminate.

Questo ambiente e pensato per test prod-like, demo controllate e CI. Non e ancora una configurazione di produzione definitiva.

## File principali

- `docker-compose.prod-like.yml`
- `docker-compose.secrets.yml`
- `docker-compose.bootstrap-secret.yml`
- `.env.docker.example`
- `web/backend/Dockerfile`
- `web/frontend/Dockerfile`
- `web/frontend/nginx.conf`
- `web/frontend/security-headers.conf`
- `scripts/db/backup.sh`
- `scripts/db/restore.sh`
- `scripts/db/test-backup-lifecycle.sh`
- `scripts/db/restore-drill-latest.sh`
- `scripts/db/verify-backup-restore.sh`
- `scripts/e2e/run-web-smoke.sh`
- `scripts/security/verify-login-rate-limit.sh`
- `scripts/security/verify-browser-security.sh`
- `scripts/security/verify-actuator-exposure.sh`
- `scripts/security/verify-container-secrets.sh`
- `scripts/security/verify-container-hardening.sh`
- `scripts/security/resolve-file-secrets.sh`

## Preparazione ambiente locale

Creare una copia locale del template:

```bash
cp .env.docker.example .env.docker
```

Aggiornare almeno:

- `GESTIONALE_DB_PASSWORD_SECRET_FILE` con un file esterno al repository;
- `GESTIONALE_BOOTSTRAP_SUPER_ADMIN_USERNAME`;
- `GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD_SECRET_FILE` solo per il primo avvio.

Il file `.env.docker` non deve essere tracciato da Git.

## Primo avvio con database vuoto

Al primo avvio di un database vuoto, il backend richiede bootstrap esplicito del super admin.

Impostare temporaneamente:

```bash
GESTIONALE_BOOTSTRAP_SUPER_ADMIN_ENABLED=true
```

Poi avviare lo stack:

```bash
docker compose --env-file .env.docker \
  -f docker-compose.prod-like.yml \
  -f docker-compose.secrets.yml \
  -f docker-compose.bootstrap-secret.yml \
  up --build
```

Dopo il primo login e la verifica dell'account super admin, spegnere lo stack, impostare:

```bash
GESTIONALE_BOOTSTRAP_SUPER_ADMIN_ENABLED=false
```

e riavviare senza `docker-compose.bootstrap-secret.yml`. Rimuovere quindi il file bootstrap dal secret manager.

La gestione completa dei segreti e descritta in `docs/SECRET_ROTATION.md`.

## URL locali

- Frontend: `http://localhost:8081`
- Backend API locale: `http://127.0.0.1:8080`
- PostgreSQL host locale: `localhost:5433`

La porta backend e pubblicata esclusivamente su `127.0.0.1` per diagnostica locale. Le richieste esterne devono attraversare Nginx; in produzione e preferibile non pubblicare affatto la porta backend. Actuator usa la porta interna `9090`, non pubblicata sull'host: il container espone `health` e `prometheus`, nasconde i dettagli health e distingue liveness da readiness.

Internamente i container comunicano cosi:

- frontend -> backend: `http://backend:8080`
- host/orchestratore -> frontend nel container: `http://frontend:8080`
- backend -> database: `jdbc:postgresql://postgres:5432/gestionale`
- orchestratore -> probe backend: `http://backend:9090/actuator/health/liveness` e `http://backend:9090/actuator/health/readiness`
- Prometheus -> metriche backend: `http://backend:9090/actuator/prometheus`

Prometheus e opzionale e si abilita con il profilo:

```bash
docker compose --env-file .env.docker \
  -f docker-compose.prod-like.yml \
  -f docker-compose.secrets.yml \
  --profile observability \
  up -d
```

La UI Prometheus e disponibile solo su loopback, per default su `http://127.0.0.1:9091`.

## Comandi utili

Build immagini:

```bash
docker compose --env-file .env.docker -f docker-compose.prod-like.yml -f docker-compose.secrets.yml build
```

Avvio:

```bash
docker compose --env-file .env.docker -f docker-compose.prod-like.yml -f docker-compose.secrets.yml up -d
```

Stato:

```bash
docker compose --env-file .env.docker -f docker-compose.prod-like.yml -f docker-compose.secrets.yml ps
```

Log backend:

```bash
docker compose --env-file .env.docker -f docker-compose.prod-like.yml -f docker-compose.secrets.yml logs -f backend
```

Accesso PostgreSQL via `psql` dentro al container:

```bash
docker compose --env-file .env.docker -f docker-compose.prod-like.yml -f docker-compose.secrets.yml exec postgres sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Backup database:

```bash
ENV_FILE=/etc/gestionale/backup.env scripts/db/backup.sh
```

Restore database:

```bash
CONFIRM_RESTORE=yes \
ENV_FILE=/etc/gestionale/backup.env \
scripts/db/restore.sh /var/backups/gestionale/nome-backup.dump
```

Verifica backup/restore su database isolato:

```bash
scripts/db/test-backup-lifecycle.sh
scripts/db/verify-backup-schedule.sh
scripts/db/verify-backup-restore.sh
```

Il backup giornaliero e il restore drill settimanale sono forniti come timer systemd. Configurazione, installazione, retention e obiettivi RPO/RTO sono descritti in `docs/BACKUP_RESTORE.md`.

Smoke test browser su stack E2E isolato:

```bash
E2E_USERNAME=nome_super_admin_e2e \
E2E_PASSWORD=password_e2e_forte \
GESTIONALE_E2E_DB_PASSWORD=password_database_e2e \
scripts/e2e/run-web-smoke.sh
```

Lo script usa container, porte e volume dedicati al progetto Compose `gestionale-e2e`, poi rimuove container, rete e volume E2E senza modificare i dati dello stack operativo. Oltre ai flussi browser, verifica il rate limiting del login e la relativa traccia nei log Nginx. Le password sono scritte in file effimeri, montate soltanto nei servizi autorizzati e controllate tramite `docker inspect` per escludere valori diretti dalla configurazione container.

Verifica isolata di CSP, header browser e cache su uno stack gia avviato:

```bash
scripts/security/verify-browser-security.sh http://127.0.0.1:8081
```

La verifica controlla la shell HTML, un asset JavaScript versionato e una risposta API proxy. La policy non ammette script o stili inline, `eval`, frame o plugin. La shell usa `no-store`, mentre gli asset con hash usano cache `immutable`.

Verifica hardening runtime dei container:

```bash
scripts/security/verify-container-hardening.sh \
  gestionale-prodlike-postgres \
  gestionale-prodlike-backend \
  gestionale-prodlike-frontend
```

Il controllo richiede utente immagine e UID runtime non-root, filesystem root read-only, `no-new-privileges`, `cap_drop: ALL` e tmpfs operativo. Backend e frontend non possono avere mount persistenti scrivibili; PostgreSQL puo scrivere soltanto nel volume dati e nei tmpfs `/tmp` e `/var/run/postgresql`.

Verifica dell'isolamento Actuator dalla porta API:

```bash
scripts/security/verify-actuator-exposure.sh http://127.0.0.1:8080
```

Verifica della configurazione e del runtime Prometheus:

```bash
scripts/observability/verify-prometheus-config.sh
scripts/observability/verify-runtime-observability.sh
scripts/observability/verify-prometheus-hardening.sh
```

Le probe operative sono verificabili soltanto dalla rete interna del container:

```bash
docker compose --env-file .env.docker -f docker-compose.prod-like.yml -f docker-compose.secrets.yml exec backend \
  wget -qO- http://127.0.0.1:9090/actuator/health/readiness
```

Verifica isolata del rate limiting su uno stack gia avviato:

```bash
scripts/security/verify-login-rate-limit.sh http://127.0.0.1:8081
```

Il controllo invia credenziali volutamente non valide e richiede una risposta `429 RATE_LIMIT_EXCEEDED` con `Retry-After`, request ID e header di sicurezza. Usarlo soltanto in ambienti di test: le richieste consumano temporaneamente il budget dell'IP chiamante.

Spegnimento senza cancellare i dati:

```bash
docker compose --env-file .env.docker -f docker-compose.prod-like.yml -f docker-compose.secrets.yml down
```

Spegnimento con cancellazione volume database:

```bash
docker compose --env-file .env.docker -f docker-compose.prod-like.yml -f docker-compose.secrets.yml down -v
```

## Controllo CI

La workflow `.github/workflows/ci.yml` costruisce le immagini, avvia lo stack prod-like e verifica:

- readiness e liveness backend sulla porta management interna;
- assenza di endpoint Actuator utilizzabili sulla porta API;
- frontend health;
- avvio con PostgreSQL reale.
- lifecycle backup, scheduling e restore PostgreSQL su database isolato.
- smoke test Playwright sul percorso commerciale principale.
- rate limiting login, risposta `429` e log Nginx `limit_req=REJECTED`.
- CSP, header di sicurezza e caching tramite verifica HTTP dedicata e smoke Chromium.
- password database e bootstrap montate come secret read-only, senza valori diretti nella configurazione container.
- processi container non-root, root filesystem read-only, capability eliminate e soli percorsi runtime autorizzati scrivibili.
- configurazione Prometheus, metriche backend, log JSON, regole alert e hardening del container di monitoraggio.
- tutte le migrazioni Flyway presenti e applicate con successo sul PostgreSQL reale.
- cleanup finale senza container, volumi o reti residue.

In CI il bootstrap super admin e temporaneo e confinato allo stack effimero della pipeline. La stessa procedura puo essere riprodotta con `scripts/ci/run-prod-like-verification.sh`, viene schedulata settimanalmente e conserva sempre la diagnostica descritta in `docs/PROD_LIKE_VERIFICATION.md`.

## Limiti attuali

Restano fuori da questo step:

- reverse proxy TLS;
- registry immagini;
- deploy remoto;
- replica backup off-site, cifratura e immutabilita gestite dall'infrastruttura;
- integrazione con un secret manager remoto specifico;
- aggregazione centralizzata dei log e canale Alertmanager dell'ambiente reale;
- TLS e gestione IP reale da proxy fidati;
- rate limiting condiviso tra piu repliche.
