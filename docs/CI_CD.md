# CI/CD

## Scopo

La pipeline automatica principale si trova in `.github/workflows/ci.yml` e controlla il progetto a ogni push su `main`, pull request verso `main`, avvio manuale da GitHub Actions e ogni lunedi alle 03:23 UTC.

I controlli di sicurezza automatizzati si trovano in `.github/workflows/security.yml` e sono descritti in `docs/SECURITY_SCANS.md`.

Lo stack Docker prod-like e descritto in `docs/DOCKER.md`.

Le procedure backup/restore PostgreSQL sono descritte in `docs/BACKUP_RESTORE.md`.

In questa fase e una CI, non una CD completa: verifica qualita e produce artefatti temporanei, ma non pubblica automaticamente il gestionale in un ambiente reale.

## Job automatici

### Repository hygiene

Controlla che nel repository non vengano tracciati:

- file `.env` reali;
- cartelle locali di build come `target`, `dist` e `node_modules`;
- file `.dat` di persistenza locale;
- credenziali demo visibili nel frontend.

Verifica inoltre che la documentazione minima di rilascio sia presente.

### Backend Spring Boot

Esegue il backend con Java 17:

```bash
mvn -B verify
```

Questo compila il backend, applica il ciclo Maven fino a `verify`, esegue i test automatici e genera il jar.

La configurazione Maven del backend prepara automaticamente il Byte Buddy agent usato da Mockito nei test, quindi la pipeline non richiede flag JVM manuali oltre al comando Maven standard.

Su push verso `main`, il jar viene caricato come artefatto temporaneo della workflow. Il nome include `github.sha` per collegare in modo univoco il binario al commit sorgente.

### Frontend React

Esegue il frontend con Node 22:

```bash
npm ci
npm test
npm run build
```

Questo installa le dipendenze dal lockfile, esegue la suite Vitest, controlla TypeScript e genera la build Vite.

Su push verso `main`, la cartella `dist` viene caricata come artefatto temporaneo della workflow. Anche questo artefatto include `github.sha`; la diagnostica prod-like include inoltre identificativo e tentativo della workflow.

### Prod-like Docker stack

Il job richiama un solo entrypoint operativo:

```bash
sh scripts/ci/run-prod-like-verification.sh
```

Il runner costruisce con `--pull` e avvia:

- PostgreSQL;
- backend Spring Boot;
- frontend Nginx.

Le password database e bootstrap sono generate a ogni esecuzione, mascherate nei log, scritte in file effimeri e montate tramite gli override Compose dedicati. `verify-container-secrets.sh` controlla che i valori diretti siano vuoti e i mount siano read-only.

`verify-container-hardening.sh` controlla sul runtime effettivo che i tre container usino UID non-root, filesystem root read-only, `no-new-privileges` e nessuna capability Linux. Verifica inoltre che backend e frontend non abbiano mount scrivibili e che PostgreSQL persista esclusivamente nel volume dati autorizzato.

Verifica readiness e liveness del backend sulla porta management interna `9090`, controlla che Actuator non sia utilizzabile dalla porta API e verifica `/health` sul frontend. Controlla inoltre che il database reale abbia applicato con successo tutte le migrazioni Flyway presenti nel repository.

Valida la configurazione Prometheus con `promtool`, avvia il profilo `observability` e controlla metriche JVM/HTTP/database/applicative, campi dei log JSON, caricamento delle cinque regole alert e hardening non-root del container Prometheus.

Verifica CSP, header browser e caching con `scripts/security/verify-browser-security.sh`. Installa quindi Chromium tramite Playwright ed esegue tre smoke test sullo stack reale: hardening browser senza violazioni runtime, gestione dell'errore di login e ciclo super admin/cliente dalla creazione prodotto alla conferma ordine. In caso di errore conserva report HTML, trace, screenshot e video come artefatto della workflow.

Dopo gli smoke test esegue `scripts/security/verify-login-rate-limit.sh`, verifica il contratto `429` e richiede che i log Nginx contengano un rifiuto `limit_req=REJECTED`.

Verifica prima checksum, lock concorrente, retention, freschezza e configurazione dei timer:

```bash
sh scripts/db/test-backup-lifecycle.sh
sh scripts/db/verify-backup-schedule.sh
```

Esegue quindi una verifica backup/restore PostgreSQL su database isolato tramite:

```bash
sh scripts/db/verify-backup-restore.sh
```

La CI usa un archivio sintetico e uno stack effimero. Il restore drill settimanale dell'ultimo backup reale resta un controllo operativo dell'ambiente e non deve dipendere dai dati della pipeline.

Stato, log, immagini e diagnostica Playwright vengono acquisiti anche in caso di fallimento e pubblicati come artefatto. Il trap rimuove container, volumi, reti e secret effimeri; il job fallisce se il controllo finale rileva risorse Docker residue. La procedura completa e descritta in `docs/PROD_LIKE_VERIFICATION.md`.

### Quality gate

Passa solo se hygiene, backend, frontend e stack Docker prod-like sono completati correttamente.

## Cosa blocca una pull request

Una pull request deve essere considerata non pronta se fallisce uno di questi controlli:

- backend non compila;
- test backend falliscono;
- frontend non compila;
- test frontend falliscono;
- TypeScript segnala errori;
- file locali o sensibili vengono tracciati;
- credenziali demo ricompaiono nella UI pubblica.
- stack Docker prod-like o verifica backup/restore non passano.
- lifecycle backup, retention o configurazione dei timer non passano.
- configurazione Prometheus, raccolta metriche, log strutturati o regole alert non passano.
- smoke test Playwright o relativo typecheck non passano.
- verifica rate limiting login o relativa osservabilita Nginx non passano.
- verifica isolamento secret container o scansione Gitleaks non passano.
- verifica non-root e privilegi minimi dei container non passa.
- migrazioni Flyway incomplete, diagnostica assente o cleanup Docker incompleto.

## Protezione del branch `main`

La branch protection remota deve impedire push diretti e merge senza pull request. La configurazione minima richiesta e:

- almeno una approvazione;
- nuova approvazione dopo modifiche successive alla review;
- conversazioni di review risolte;
- branch aggiornata prima del merge;
- amministratori inclusi nelle regole;
- force push e cancellazione del branch disabilitati;
- check `Gestionale CI / Quality gate` obbligatorio;
- check `Gestionale Security / Gitleaks secret scan` obbligatorio;
- check `Gestionale Security / Dependency review` obbligatorio;
- check `Gestionale Security / Frontend npm audit` obbligatorio;
- check `Gestionale Security / Backend dependency inventory` obbligatorio;
- check `Gestionale Security / CodeQL analysis` obbligatorio.

L'attivazione e una modifica remota del repository: deve essere verificata nelle impostazioni GitHub dopo che workflow e baseline iniziale sono state pubblicate.

## Limiti attuali

La pipeline non include ancora:

- deploy automatico;
- copertura browser completa dei moduli magazzino, documenti, account, audit e rinnovo sessione.

Questi punti restano candidati per gli step successivi.

## Comandi locali equivalenti

Prima di aprire una pull request, eseguire:

```bash
cd web/backend
mvn test
```

```bash
cd web/frontend
npm ci
npm test
npm run build
```

```bash
cd web/frontend
npm run audit
```

Per eseguire localmente l'intera verifica prod-like:

```bash
scripts/ci/run-prod-like-verification.sh
```

Per eseguire soltanto gli smoke test in ambiente isolato:

```bash
E2E_USERNAME=nome_super_admin_e2e \
E2E_PASSWORD=password_e2e_forte \
GESTIONALE_E2E_DB_PASSWORD=password_database_e2e \
scripts/e2e/run-web-smoke.sh
```
