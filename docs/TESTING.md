# Testing

## Stato verificato

Verificato il 2026-07-25.

Build web aggregata:

```bash
mvn verify
```

Risultato: backend compilato e suite completa eseguita dalla root.

Swing legacy:

```bash
mvn -f pom-legacy.xml test
```

Risultato: 41 test passati.

Backend web:

```bash
cd web/backend
mvn test
```

Risultato: 154 test passati con Java 23 locale, inclusi il controllo Spring Modulith sugli otto moduli business, le metriche operative, l'avvio di API e management plane su porte distinte e i contratti delle proposte architetturali multi-tenant, privacy/fiscale e lifecycle cliente/release.

La proposta privacy e fiscale e protetta da quattro test contrattuali statici che verificano confini, assenza di dichiarazioni di conformita, workflow sicuri, separazione dei documenti elettronici e rollout a gate. La proposta commerciale aggiunge quattro contratti statici su control plane, onboarding riprendibile, branding sicuro, release immutabili, rollout per coorti e migrazioni expand/contract. Questi test proteggono decisioni architetturali, ma non dimostrano funzionalita non ancora implementate.

Il backend dichiara Java 17 come baseline. I test che usano Mockito vengono eseguiti con Byte Buddy agent configurato da Maven Surefire, quindi non richiedono argomenti manuali anche quando la JVM locale e piu recente.

Frontend web:

```bash
cd web/frontend
npm ci
npm test
npm run typecheck
npm run typecheck:e2e
npm run build
```

Risultato: installazione pulita completata, 32 test passati in 11 file, typecheck applicativo/E2E e build completati.

Smoke test browser sullo stack reale:

```bash
E2E_USERNAME=nome_super_admin_e2e \
E2E_PASSWORD=password_e2e_forte \
GESTIONALE_E2E_DB_PASSWORD=password_database_e2e \
scripts/e2e/run-web-smoke.sh
```

Risultato: 3 test Playwright passati su Chromium contro PostgreSQL, Spring Boot e frontend Nginx isolati. Oltre ai flussi login e commerciale, la suite verifica CSP e header browser in enforcement senza errori runtime. Il flusso positivo verifica anche apertura del modulo Report, snapshot magazzino e avvio del download CSV. Lo script ricostruisce entrambe le immagini applicative, usa il progetto Compose `gestionale-e2e`, porte 18080/18081 e un volume dedicato, senza modificare il database operativo. Al termine verifica inoltre il rate limiting login e la presenza di `limit_req=REJECTED` nei log Nginx.

Hardening browser su stack avviato:

```bash
scripts/security/verify-browser-security.sh http://127.0.0.1:8081
```

Risultato verificato: policy CSP same-origin priva di `unsafe-inline` e `unsafe-eval`, framing e plugin bloccati, header browser coerenti su HTML, asset e API, shell `no-store`, asset versionati `immutable` e versione Nginx non esposta.

Rate limiting login su stack avviato:

```bash
scripts/security/verify-login-rate-limit.sh http://127.0.0.1:8081
```

Risultato verificato: limite applicato dopo 10 richieste immediate con configurazione E2E predefinita; risposta `429 RATE_LIMIT_EXCEEDED`, `Retry-After: 60`, JSON, `no-store`, `nosniff` e request ID coerenti; frontend health ancora disponibile.

CI:

```bash
GitHub Actions - Gestionale CI
```

Risultato locale equivalente: backend e frontend verificati con successo. La workflow automatica esegue hygiene repository, `mvn -B verify`, `npm ci`, `npm test`, `npm run build` e gli smoke test Playwright sullo stack prod-like.

Security workflow:

```bash
GitHub Actions - Gestionale Security
```

Risultato locale equivalente: `npm run audit`, inventory Maven, Gitleaks e validazione sintassi workflow completati.

Resolver e isolamento secret:

```bash
sh scripts/security/test-resolve-file-secrets.sh
sh scripts/security/verify-container-secrets.sh nome-container-postgres nome-container-backend
```

Il primo comando copre valore diretto, file, segreto opzionale, conflitto tra canali, assenza, file mancante, vuoto e multilinea. Il secondo verifica che lo stack non esponga valori password nella configurazione Docker e che i file siano montati read-only.

Hardening runtime dei container:

```bash
scripts/security/verify-container-hardening.sh \
  gestionale-prodlike-postgres \
  gestionale-prodlike-backend \
  gestionale-prodlike-frontend
```

Il controllo ispeziona configurazione e processo reale: UID non-root, root filesystem read-only, container non privilegiato, `no-new-privileges`, capability eliminate, tmpfs scrivibile e mount persistenti limitati.

Risultato verificato: PostgreSQL, backend e frontend eseguiti con UID non-root; scrittura sulla root rifiutata; tmpfs operativi; backend/frontend privi di mount scrivibili; PostgreSQL limitato al proprio volume dati.

Docker prod-like:

```bash
docker compose --env-file .env.docker -f docker-compose.prod-like.yml -f docker-compose.secrets.yml up --build
```

Risultato verificato: immagini PostgreSQL/backend/frontend costruite, processi non-root e privilegi minimi confermati, readiness e liveness backend in stato `UP` sulla porta management interna, endpoint Actuator non utilizzabili dalla porta API, frontend `/health` raggiungibile, tre smoke test Playwright passati e rate limiting login applicato. In CI lo stack viene costruito e verificato automaticamente.

Verifica prod-like completa e ricorrente:

```bash
scripts/ci/run-prod-like-verification.sh
```

Il runner usa credenziali e progetto Compose effimeri, aggiorna le immagini base, controlla le 18 migrazioni Flyway sul database reale, esegue sicurezza, osservabilita, Playwright e backup/restore, acquisisce diagnostica e certifica il cleanup. La CI lo esegue anche ogni lunedi alle 03:23 UTC.

Osservabilita prod-like:

```bash
scripts/observability/verify-prometheus-config.sh
scripts/observability/verify-runtime-observability.sh
scripts/observability/verify-prometheus-hardening.sh
```

Risultato verificato: configurazione e cinque regole validate con `promtool`; metriche JVM, HTTP, Hikari e applicative raccolte dal management plane; target Prometheus `UP`; log JSON con correlation ID e metadati HTTP; container Prometheus non-root con root filesystem read-only e mount limitati.

Backup/restore PostgreSQL:

```bash
scripts/db/test-backup-lifecycle.sh
scripts/db/verify-backup-schedule.sh
scripts/db/verify-backup-restore.sh
```

Risultato verificato: checksum valido e corrotto, lock concorrente, retention, freschezza e unita systemd controllati; database temporaneo creato, dato di prova inserito, backup atomico generato, database ricreato e dato ripristinato correttamente.

Il drill dell'ultimo backup reale si esegue separatamente nell'ambiente operativo:

```bash
ENV_FILE=/etc/gestionale/backup.env scripts/db/restore-drill-latest.sh
```

## Copertura funzionale attuale

Swing legacy:

- pattern Factory, Strategy, Command, Decorator;
- repository file-backed;
- servizi prodotto, auth, audit, magazzino, ordini, documenti simulati;
- robustezza password.

Backend web:

- prodotti;
- hashing BCrypt;
- autorizzazioni principali con Spring Security.
- matrice autorizzazioni su prodotti, magazzino, ordini, documenti, account e audit;
- contratto errori API per sessione mancante e validazione;
- sessioni persistenti con token hashato, revoca logout, scadenza assoluta e timeout inattivita;
- rinnovo sessione con rotazione atomica, rifiuto replay del token precedente, password errata non distruttiva e header anti-cache;
- metriche sessione attiva coerenti con scadenza assoluta e timeout di inattivita;
- tentativi login falliti persistenti, lockout e cleanup programmato;
- rate limiting login Nginx per IP con errore `429`, request ID, `Retry-After` e verifica dei log di rifiuto;
- CSP Nginx in enforcement, header browser uniformi e cache differenziata tra shell HTML e asset versionati;
- optimistic locking prodotto e protezione da scarichi stock concorrenti;
- stock riservato, disponibilita vendibile e blocco scarichi manuali su quantita prenotate;
- movimento magazzino con snapshot coerente di quantita precedente e nuova;
- codici ordine generati da sequenza database e documenti simulati numerati atomicamente per tipo ed esercizio, con test concorrenti;
- workflow ordine con bozza, conferma, evasione, annullamento, prenotazione stock e vincolo fattura su ordine evaso;
- snapshot cliente, azienda emittente e aliquota IVA nei documenti simulati, conservati anche nella nota credito;
- permessi granulari sulle transizioni ordine, incluso divieto di evasione ordine per cliente;
- correlation ID propagato in header, corpo errore, log richiesta e audit;
- audit arricchito con origine richiesta, `requestId` e tipo entita;
- paginazione e filtri server-side su prodotti, ordini, movimenti, documenti, account e audit;
- anagrafiche clienti/fornitori con creazione, aggiornamento, disattivazione e validazione duplicati;
- collegamento ordine-cliente registrato tramite `customerCode`;
- autorizzazioni su lettura e gestione anagrafiche;
- controlli frontend collegati alle viste paginabili principali.
- idempotenza sulle operazioni critiche di ordini, documenti e movimenti;
- bootstrap super admin controllato;
- template `.env.example` con placeholder e `.env` reali ignorati da Git.
- pipeline CI con controlli su repository hygiene, backend, frontend e quality gate.
- security workflow con dependency review, audit npm, inventory Maven, CodeQL e Dependabot.
- stack Docker prod-like con PostgreSQL reale, backend Spring Boot e frontend Nginx.
- backend prod-like pubblicato solo su loopback per evitare il bypass remoto del rate limiting Nginx.
- backup atomico PostgreSQL con checksum, lock, retention e freschezza;
- restore sintetico isolato e restore drill schedulato dell'ultimo backup reale.
- endpoint monitoraggio protetto con stato database, runtime, sessioni, audit sensibile ed errori API recenti;
- accesso al monitoraggio consentito al super admin e negato ai ruoli non autorizzati.
- protezioni sul ciclo di vita prodotto: codice immutabile dopo uso in ordini, blocco cancellazione con stock riservato o ordini collegati, disattivazione prodotto e ordine storico ancora evadibile.
- password policy backend applicata a registrazione pubblica e creazione account amministrativa;
- vincolo database sui documenti simulati per impedire fatture o note credito duplicate sullo stesso ordine;
- conflitti documentali tradotti in errore API stabile `409 RESOURCE_CONFLICT`;
- Flyway validato con 18 migrazioni applicate nei test backend;
- confini degli otto moduli business verificati con Spring Modulith, incluse le porte read-only del modulo report;
- proposta multi-tenant protetta da un contratto statico che richiede modello pooled/dedicato, tenant context fail-closed, foreign key composte, RLS forzata, ruolo runtime senza bypass, migrazione expand/contract e test cross-tenant;
- vincoli database essenziali su prodotti, ordini, righe, magazzino, documenti simulati e anagrafiche verificati con test negativi diretti.
- endpoint `/api/documents` paginato, filtrabile per ricerca testuale e tipo documento, verificato con test API.
- endpoint `/api/accounts` paginato, filtrabile per username e ruolo, verificato con test API.
- scorte basse calcolate su disponibilita vendibile tramite query repository, con prodotti esauriti esclusi dal conteggio low-stock.
- endpoint `/api/dashboard` protetto, con statistiche aggregate per super admin e vista cliente limitata ai propri ordini.
- endpoint `/api/products/lookup` protetto, con risposta leggera per filtri e select frontend senza campi catalogo pesanti.
- mapping JPA di ordini e documenti verificati con test architetturale: collezioni `OneToMany` lazy con batch loading e back-reference `ManyToOne` lazy.
- clock applicativo UTC verificato con test di integrazione su ordini, documenti, movimenti magazzino, anagrafiche, audit e sessioni.
- architettura frontend separata in pagine, layout, componenti comuni e hook, verificata tramite typecheck e build.
- client API frontend separati per dominio con trasporto HTTP condiviso, verificati tramite typecheck, build e assenza di import al precedente modulo monolitico.
- layout responsive verificato a 1.440x900, 1.024x768, 640x900 e 390x844 senza overflow orizzontale della pagina o errori console.
- configurazione aziendale protetta da permesso super admin, versione ottimistica, audit e validazione di aliquota/prefissi;
- vincoli database e test applicativi su progressivi documentali annuali, snapshot storici e blocco delle modifiche incompatibili alla numerazione;
- report vendite e magazzino verificati per aggregazioni, filtri, periodo massimo, limite righe, autorizzazioni e audit export;
- file CSV verificati per escaping e Formula Injection, workbook XLSX riaperti con Apache POI e PDF verificati tramite firma del formato;
- suite frontend Vitest e React Testing Library con 32 test su autenticazione, registrazione, password non valida, sessione scaduta, rinnovo e rotazione sessione, prodotti, ordini, pagamenti, resi, paginazione, dashboard, configurazione aziendale, report, download blob e contratto errori HTTP.
- esecuzione automatica dei test frontend nella pipeline CI prima della build Vite.
- smoke test Playwright su login negativo, login super admin, navigazione a schede, creazione prodotto, report magazzino con download CSV, verifica responsive senza overflow a 1024x768, registrazione cliente, carrello, creazione ordine e conferma ordine contro lo stack Docker reale.
- acquisizione automatica di report HTML, trace, screenshot e video Playwright in caso di errore CI.

## Gap principali

- Mancano test API funzionali approfonditi per tutti i controller.
- Mancano test repository significativi su query e vincoli.
- Manca il collegamento delle regole Prometheus a un Alertmanager e a un canale on-call dell'ambiente reale.
- Mancano test concorrenza approfonditi sulle transizioni ordine con stock riservato.
- Mancano test dedicati su rollback/compatibilita delle migrazioni, oltre alla validazione Flyway nei test backend.
- La copertura browser non include ancora magazzino, documenti, account, audit, rinnovo sessione e tutti i filtri avanzati.
- Manca SCA backend con CVE database dedicato.
- Gli smoke test E2E sono seriali e orientati al percorso critico; non sono ancora una suite browser esaustiva.
- Il rate limiting verificato e locale a una singola istanza Nginx; proxy fidati, NAT condivisi e coordinamento tra repliche richiedono test specifici dell'ambiente di deploy.
- Il gestionale resta single-tenant: ADR e proposta multi-tenant definiscono il target, ma isolamento, migrazioni e test PostgreSQL cross-tenant non sono ancora implementati.

## Standard per nuovi step

Ogni step deve includere almeno:

- caso valido;
- input non valido;
- utente non autenticato;
- utente senza permesso;
- regressione su regole gia presenti;
- contratto errore stabile per gli scenari negativi;
- test di build o compilazione.

Per step su database:

- migrazione applicata;
- vincoli verificati;
- rollback logico valutato;
- test con dati realistici.

Per step frontend:

- build TypeScript;
- stati vuoti e di errore;
- permessi e sessione scaduta;
- nessun overflow evidente nelle viste principali.
