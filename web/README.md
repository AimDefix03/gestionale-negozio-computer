# Gestionale Web

Prima base della migrazione del gestionale da applicazione Swing a web app moderna.

## Struttura

- `backend`: API Spring Boot con prodotti, account, ruoli, magazzino, ordini, documenti simulati e audit log.
- `frontend`: interfaccia React + TypeScript con workspace multi-sezione e viste diverse per ruolo.

## Account iniziale

Nessun account viene creato automaticamente. Su database vuoto il backend richiede il bootstrap esplicito del primo super admin tramite variabili ambiente; valori locali noti vengono rifiutati in ogni profilo. Dopo il primo avvio il bootstrap deve essere nuovamente disabilitato.

La registrazione pubblica crea esclusivamente account `CUSTOMER`. Dipendenti e amministratori vengono creati dal pannello `Account`; la creazione di amministratori e consentita solo al super admin.

## Avvio backend

Avviare prima PostgreSQL locale:

```bash
docker compose up -d postgres
```

Il database di sviluppo usa credenziali locali non produttive:

- database: `gestionale`
- owner: `gestionale_owner` senza login
- migrator: `gestionale_migrator`
- runtime: `gestionale_runtime`

Poi avviare il backend:

```bash
cd web/backend
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

Non esiste un profilo predefinito: lo sviluppo deve attivare `dev` esplicitamente. La configurazione puo essere sovrascritta con:

```bash
GESTIONALE_DB_URL=jdbc:postgresql://localhost:5432/gestionale
GESTIONALE_DB_OWNER_USERNAME=gestionale_owner
GESTIONALE_DB_RUNTIME_USERNAME=gestionale_runtime
GESTIONALE_DB_RUNTIME_PASSWORD=<password-runtime-locale>
GESTIONALE_DB_MIGRATOR_USERNAME=gestionale_migrator
GESTIONALE_DB_MIGRATOR_PASSWORD=<password-migrator-locale>
GESTIONALE_BOOTSTRAP_SUPER_ADMIN_ENABLED=true
GESTIONALE_BOOTSTRAP_SUPER_ADMIN_USERNAME=nome_super_admin_locale
GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD=secret_forte_unico
```

Non sono fornite credenziali predefinite. Il runtime applicativo non puo creare, alterare o eliminare oggetti database; Flyway usa il migrator separato e assume il ruolo owner `NOLOGIN` soltanto durante le migrazioni.

Le migrazioni database sono gestite da Flyway in `web/backend/src/main/resources/db/migration`.

Le variabili ambiente sono documentate in `docs/CONFIGURATION.md`.

I controlli automatici di qualita sono documentati in `docs/CI_CD.md`.

Le scansioni automatiche di sicurezza sono documentate in `docs/SECURITY_SCANS.md`.

Lo stack Docker prod-like e documentato in `docs/DOCKER.md`.

Template backend:

```bash
cp web/backend/.env.example web/backend/.env
```

API principali disponibili:

- `GET /api/products`
- `GET /api/products/{code}`
- `POST /api/products`
- `PUT /api/products/{code}`
- `DELETE /api/products/{code}`
- `POST /api/products/bulk-delete`
- `POST /api/accounts/login`
- `POST /api/accounts/register`
- `GET /api/accounts`
- `POST /api/accounts`
- `GET /api/inventory/movements`
- `POST /api/inventory/movements`
- `GET /api/orders`
- `POST /api/orders`
- `GET /api/documents`
- `POST /api/documents/invoice`
- `POST /api/documents/credit-note`
- `GET /api/audit`

## Avvio frontend

```bash
cd web/frontend
npm install
npm run dev
```

Il frontend usa il proxy Vite verso `http://localhost:8080`.

## Prossimi step

1. Introdurre backup e restore PostgreSQL.
2. Aggiungere secret scanning dedicato e policy licenze.
3. Aggiungere rate limiting infrastrutturale.
4. Estendere gli smoke test end-to-end a magazzino, documenti, account e audit.
5. Preparare registry immagini e deploy dimostrativo.
