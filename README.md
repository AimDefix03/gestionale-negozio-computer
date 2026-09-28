# Gestionale Negozio Computer

Gestionale web per catalogo, magazzino, ordini, pagamenti, documenti simulati, configurazione aziendale, report operativi, anagrafiche, account e audit.

La linea produttiva e composta da backend Spring Boot, frontend React e PostgreSQL. La precedente applicazione Java Swing e conservata esclusivamente come riferimento funzionale e didattico.

## Stato del progetto

Il progetto e un prototipo web avanzato con MVP tecnico verificato. I finding P0 sono chiusi, il runtime Spring usa una linea OSS supportata, i runner Docker sono isolati per project e run ID, gli upgrade storici critici sono corretti, i ledger di magazzino e finanziario sono autorevoli e le mutazioni critiche sono idempotenti. PostgreSQL usa identita separate per ownership, migrazioni, runtime, backup e restore; i Gate A, B e C sono chiusi tecnicamente. La maturita e stimata al 95-97% nel repository reale dopo lo Step 4.3. Validazione operatore e fasi produttive successive restano necessarie prima di produzione o commercializzazione.

- Il backend applica autorizzazioni, validazioni e regole aziendali lato server.
- Il database evolve tramite migrazioni Flyway versionate.
- Il runtime PostgreSQL opera senza privilegi DDL; Flyway, backup e restore usano identita dedicate.
- I flussi critici usano audit, claim idempotenti durevoli e codici richiesta correlati.
- Le build web e legacy sono separate.
- Il sistema non e dichiarato conforme alla fatturazione elettronica, alla normativa fiscale italiana o al GDPR.
- Multi-tenancy, onboarding SaaS, branding cliente, provisioning automatico e aggiornamento della flotta sono proposte target, non funzionalita disponibili.
- Lo scope e temporaneamente congelato: sono ammessi P0/P1, test e correzioni necessarie secondo `docs/PRODUCT_SCOPE.md`.

## Funzionalita web

- Account con ruoli e permessi granulari.
- Username, codici prodotto e codici anagrafica con identita canonica case/space-insensitive garantita dal database e valore visuale separato.
- Sessioni persistenti con scadenza assoluta, timeout inattivita, rotazione token, BCrypt, password policy, lockout backend e rate limiting Nginx separato per login e registrazione.
- Subject di sessione basato sull'ID stabile dell'account e revoca globale dei token dopo disabilitazione, cambio password, reset, revoca esplicita o cambio ruolo.
- Registrazione self-service riservata ai clienti; gli account dello staff vengono creati soltanto dal flusso amministrativo autorizzato.
- Account operativi storici con provenienza self-service o non dimostrabile messi in quarantena fino alla revisione manuale del super admin.
- Catalogo prodotti con stato attivo/disattivato.
- Giacenza fisica, stock riservato e disponibilita vendibile separati dall'anagrafica prodotto.
- Ledger autorevole di magazzino con saldo iniziale, rettifiche motivate, origine dei movimenti e riconciliazione.
- Inventario fisico con conteggi tracciati, differenze approvate da un responsabile distinto e rettifiche collegate al ledger.
- Retry e doppio click sulle mutazioni critiche protetti da claim atomico, replay deterministico e chiavi frontend legate all'intento.
- Anagrafiche clienti e fornitori.
- Ordini fornitore con righe, date previste, ricezioni amministrative parziali e annullo motivato del residuo.
- Collegamento verificato tra account cliente e anagrafica tramite ID stabile.
- Ordini con workflow bozza, conferma, evasione e annullamento.
- Ownership ordini basata su ID account/partner, con snapshot descrittivi separati e storico ambiguo non accessibile ai clienti.
- Pagamenti strutturati con metodo, stato, importi e valuta; lo storico senza evidenza resta `UNRECONCILED` fino a revisione tracciata del super admin.
- Configurazione aziendale protetta con dati emittente obbligatori, fuso IANA, IVA predefinita e controllo versione.
- Documenti fiscali simulati con snapshot cliente/azienda, timezone, disclaimer esplicito e numerazione annuale atomica per tipo.
- Report vendite e magazzino filtrati lato server, esportabili in CSV, Excel e PDF.
- Dashboard aggregata con ordini per stato, incassi lordi, rimborsi/storni, netto e valore potenziale di vendita chiaramente distinto dal valore contabile.
- Log JSON correlati, metriche Prometheus e regole di alert operative.
- Paginazione, filtri server-side e interfaccia responsive.
- Workspace accessibile da tastiera con dialog a focus controllato, semantica tabelle, contrasto verificato e matrice axe su Chromium, Firefox e WebKit.
- Bozze locali per vista ed entita con dirty guard su navigazione, chiusura scheda e logout; le password non vengono persistite.
- Backup PostgreSQL atomici con checksum, retention, controllo freschezza e restore drill isolato.

## Stack

Backend:

- Java 17
- Spring Boot 4.1
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

Verifica fixture storiche su PostgreSQL isolato:

```bash
scripts/db/verify-historical-fixtures.sh small
```

Le fixture sintetiche V14, V16, V18, V19, V20, V21 e V22 e il target di upgrade V35 sono dichiarati in `web/backend/src/test/resources/db/fixtures/manifest.json`.

Upgrade correttivi e numerazione concorrente su PostgreSQL:

```bash
scripts/db/verify-historical-fixtures.sh small upgrades
scripts/db/verify-payment-numbering-postgres.sh
```

La procedura amministrativa e il preflight sono descritti in `docs/PAYMENT_RECONCILIATION.md`.

Gate PostgreSQL obbligatorio equivalente alla CI:

```bash
scripts/db/verify-postgresql-suite.sh fast
```

Il profilo `fast` esegue su PostgreSQL reale la suite trasversale di concorrenza, autorizzazione e invarianti, quindi aggiorna fixture popolate V14, V16 e V18 fino a V35. Il profilo `nightly` aggiunge il percorso volumetrico `large`.

Verifica isolata della concorrenza delle sessioni su PostgreSQL:

```bash
scripts/db/verify-session-concurrency.sh
```

Verifica isolata dell'ownership ordini su PostgreSQL:

```bash
scripts/db/verify-order-ownership.sh
```

Verifica isolata degli identificatori canonici su PostgreSQL:

```bash
scripts/db/verify-canonical-identifiers-postgres.sh
```

Verifica isolata del ledger di magazzino e della concorrenza con riserve su PostgreSQL:

```bash
scripts/db/verify-inventory-ledger-postgres.sh
```

Verifica isolata dell'idempotenza concorrente su PostgreSQL:

```bash
scripts/db/verify-idempotency-postgres.sh
```

Prima di V23 su un database esistente eseguire il preflight e la procedura in `docs/CANONICAL_IDENTIFIERS.md`.

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

La suite browser verifica anche CSP, header e cache Nginx, accessibilita axe, tastiera, bozze, viewport 390/768/1440 e download su Chromium, Firefox e WebKit. Su uno stack gia avviato il controllo dedicato e:

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

Gate completo dell'MVP, comprensivo di sette workflow verticali browser e upgrade PostgreSQL popolati:

```bash
scripts/e2e/run-mvp-gate.sh
```

Il gate richiede 19 test Playwright/axe verdi, il profilo PostgreSQL `fast` e gli upgrade V14/V16/V18 fino alla migrazione corrente V35. La validazione guidata con operatore reale resta una prova separata.

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
- `docs/CANONICAL_IDENTIFIERS.md`
- `docs/SECURITY.md`
- `docs/TESTING.md`
- `docs/DEPLOYMENT.md`
- `docs/SECRET_ROTATION.md`
- `docs/BACKUP_RESTORE.md`
- `docs/OBSERVABILITY.md`
- `docs/PROD_LIKE_VERIFICATION.md`
- `docs/PRODUCT_SCOPE.md`
- `docs/WORKFLOW_BENCHMARKS.md`
- `docs/ROADMAP.md`
- `docs/MULTI_TENANCY_ARCHITECTURE.md`
- `docs/PRIVACY_RETENTION_EINVOICING_ARCHITECTURE.md`
- `docs/CUSTOMER_LIFECYCLE_AND_RELEASE_ARCHITECTURE.md`

## Autore

Giovanni De Filippo
