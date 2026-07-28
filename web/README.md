# Gestionale Web

Prima base della migrazione del gestionale da applicazione Swing a web app moderna.

## Struttura

- `backend`: API Spring Boot con prodotti, account, ruoli, magazzino, ordini, documenti simulati e audit log.
- `frontend`: interfaccia React + TypeScript con workspace multi-sezione e viste diverse per ruolo.

## Account iniziale

Per lo sviluppo viene creato automaticamente un super admin iniziale se il database e vuoto e il bootstrap locale e abilitato:

- username: `admin`
- password: `Admin123!`
- ruolo: `SUPER_ADMIN`

Queste credenziali sono solo locali. In produzione sono bloccate dal backend.

La registrazione pubblica consente solo `Dipendente` e `Cliente`. La creazione di altri account admin e prevista dal pannello `Account` ed e consentita solo al super admin.

## Avvio backend

Avviare prima PostgreSQL locale:

```bash
docker compose up -d postgres
```

Il database di sviluppo usa credenziali locali non produttive:

- database: `gestionale`
- username: `gestionale`
- password: `gestionale_dev_password`

Poi avviare il backend:

```bash
cd web/backend
mvn spring-boot:run
```

Il profilo predefinito e `dev`. La configurazione puo essere sovrascritta con:

```bash
GESTIONALE_DB_URL=jdbc:postgresql://localhost:5432/gestionale
GESTIONALE_DB_USERNAME=gestionale
GESTIONALE_DB_PASSWORD=gestionale_dev_password
```

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
