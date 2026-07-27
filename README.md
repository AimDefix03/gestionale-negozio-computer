# Gestionale Negozio Computer

Gestionale web per catalogo, magazzino, ordini, pagamenti, documenti simulati, configurazione aziendale, report operativi, anagrafiche, account e audit.

La linea produttiva e composta da backend Spring Boot, frontend React e PostgreSQL. La precedente applicazione Java Swing e conservata esclusivamente come riferimento funzionale e didattico.

## Stato del progetto

Il progetto e un MVP web avanzato in evoluzione verso un monolite modulare predisposto alla produzione.

- Il backend applica autorizzazioni, validazioni e regole aziendali lato server.
- Il database evolve tramite migrazioni Flyway versionate.
- I flussi critici usano audit, idempotenza e codici richiesta correlati.
- Le build web e legacy sono separate.
- Il sistema non e dichiarato conforme alla fatturazione elettronica, alla normativa fiscale italiana o al GDPR.
- Multi-tenancy, onboarding SaaS, branding cliente, provisioning automatico e aggiornamento della flotta sono proposte target, non funzionalita disponibili.

## Funzionalita web

- Account con ruoli e permessi granulari.
- Sessioni persistenti con scadenza assoluta, timeout inattivita, rotazione token, BCrypt, password policy, lockout backend e rate limiting Nginx sul login.
- Catalogo prodotti con stato attivo/disattivato.
- Giacenza fisica, stock riservato e disponibilita vendibile.
- Movimenti di magazzino e protezione da aggiornamenti concorrenti.
- Anagrafiche clienti e fornitori.
- Ordini con workflow bozza, conferma, evasione e annullamento.
- Pagamenti strutturati con metodo, stato, importi e valuta.
- Configurazione aziendale protetta con dati emittente, IVA predefinita e controllo versione.
- Documenti fiscali simulati con snapshot cliente/azienda e numerazione annuale atomica per tipo.
- Report vendite e magazzino filtrati lato server, esportabili in CSV, Excel e PDF.
- Dashboard aggregata, audit e monitoraggio operativo.
- Log JSON correlati, metriche Prometheus e regole di alert operative.
- Paginazione, filtri server-side e interfaccia responsive.
- Backup PostgreSQL atomici con checksum, retention, controllo freschezza e restore drill isolato.

## Stack

Backend:

- Java 17
- Spring Boot 3.4
- Spring Security
- Spring Data JPA
- Spring Modulith
- PostgreSQL
- Flyway
- Maven

Frontend:

- React
- TypeScript
- Vite
- Vitest e React Testing Library
- Playwright

Infrastruttura:

- Docker e Docker Compose
- GitHub Actions
- Nginx
- Prometheus

## Struttura

```text
.
├── pom.xml                 # Build aggregata web-first
├── pom-legacy.xml          # Build esplicita Swing
├── web/backend/            # API Spring Boot
├── web/frontend/           # Applicazione React
├── src/                    # Codice Swing legacy
├── scripts/                # Verifiche E2E, backup e restore
└── docs/                   # Documentazione tecnica e operativa
```

## Verifica

Build backend dalla root:

```bash
mvn verify
```

Frontend:

```bash
cd web/frontend
npm ci
npm test
npm run build
```

Smoke test browser su stack isolato:

```bash
E2E_USERNAME=nome_super_admin_e2e \
E2E_PASSWORD=password_e2e_forte \
GESTIONALE_E2E_DB_PASSWORD=password_database_e2e \
scripts/e2e/run-web-smoke.sh
```

Lo smoke verifica anche CSP, header browser e cache Nginx. Su uno stack gia avviato il controllo dedicato e:

```bash
scripts/security/verify-browser-security.sh http://127.0.0.1:8081
```

Verifica inoltre che PostgreSQL, backend e frontend siano eseguiti senza privilegi root, con filesystem root read-only e capability Linux eliminate:

```bash
scripts/security/verify-container-hardening.sh \
  gestionale-prodlike-postgres \
  gestionale-prodlike-backend \
  gestionale-prodlike-frontend
```

Verifica il ciclo backup e restore senza toccare il database operativo:

```bash
scripts/db/test-backup-lifecycle.sh
scripts/db/verify-backup-schedule.sh
scripts/db/verify-backup-restore.sh
```

Validazione e verifica dell'osservabilita:

```bash
scripts/observability/verify-prometheus-config.sh
scripts/observability/verify-runtime-observability.sh
scripts/observability/verify-prometheus-hardening.sh
```

Verifica completa e isolata dello stack prod-like:

```bash
scripts/ci/run-prod-like-verification.sh
```

Il runner costruisce le immagini aggiornando le basi, verifica Flyway, sicurezza, osservabilita, smoke test browser, backup/restore e assenza di risorse Docker residue.

## Avvio Docker

Preparare un file `.env.docker` partendo da `.env.docker.example` e creare i file secret fuori dal repository. L'avvio raccomandato e:

```bash
docker compose --env-file .env.docker \
  -f docker-compose.prod-like.yml \
  -f docker-compose.secrets.yml \
  up --build
```

Il bootstrap iniziale usa temporaneamente anche `docker-compose.bootstrap-secret.yml`. La procedura completa e la rotazione sono descritte in `docs/SECRET_ROTATION.md`.

## Legacy Swing

Swing non fa parte della build produttiva. Rimane verificabile con:

```bash
mvn -f pom-legacy.xml test
mvn -f pom-legacy.xml package
```

La strategia di dismissione e descritta in `docs/LEGACY_SWING.md`.

## Documentazione

- `docs/USER_MANUAL.md`
- `docs/ARCHITECTURE.md`
- `docs/MODULE_BOUNDARIES.md`
- `docs/API_CONTRACT.md`
- `docs/SECURITY.md`
- `docs/TESTING.md`
- `docs/DEPLOYMENT.md`
- `docs/SECRET_ROTATION.md`
- `docs/BACKUP_RESTORE.md`
- `docs/OBSERVABILITY.md`
- `docs/PROD_LIKE_VERIFICATION.md`
- `docs/MULTI_TENANCY_ARCHITECTURE.md`
- `docs/PRIVACY_RETENTION_EINVOICING_ARCHITECTURE.md`
- `docs/CUSTOMER_LIFECYCLE_AND_RELEASE_ARCHITECTURE.md`
- `docs/ROADMAP.md`

## Autore

Giovanni De Filippo
