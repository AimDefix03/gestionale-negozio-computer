# Testing

## Stato verificato

Verificato il 2026-09-05.

Step 4.3 inventario fisico e rettifiche approvate:

```bash
cd web/backend
mvn clean test

cd ../..
scripts/db/verify-postgresql-suite.sh fast

cd web/frontend
npm test -- --run
npm run typecheck
npm run typecheck:e2e
npm run build
```

Risultato: 295/295 test backend e 202/202 test frontend passati; typecheck applicativo/E2E e build Vite verdi. Il gate PostgreSQL ha eseguito 105/105 test su PostgreSQL 16, applicato V1-V35 e aggiornato fixture popolate V14, V16 e V18 fino a V35. Sono coperti snapshot teorico, conteggio e differenza, invio, approvazione separata, carichi concorrenti, doppia approvazione, rettifiche sotto riserva, sessioni attive duplicate, codici case-insensitive, autorizzazioni negative, ledger collegato e UI operativa.

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

Risultato Step 3.10: 277 test backend passati. La suite include le 32 migrazioni Flyway, l'architettura modulare, i contratti KPI e temporali, il contratto benchmark WB-004, i resi multi-riga, il contratto del gate MVP e tre test di isolamento Actuator su porte locali effimere; questi ultimi richiedono un ambiente che consenta il bind locale e nella sandbox confinata falliscono correttamente con `Operation not permitted`. La verifica completa viene quindi eseguita fuori dalla sandbox confinata.

Gate E2E MVP Step 3.10:

```bash
scripts/e2e/run-mvp-gate.sh
```

Risultato: 19/19 test Playwright/axe passati sullo stack prod-like e gate PostgreSQL `fast` completato. La suite verticale copre creazione dipendente, cliente e vendita assistita, carico o rettifica con ordine-riserva-evasione, acconto-annullamento-storno, reso multi-riga-ricezione-rimborso, rinnovo dopo scadenza sessione durante una query e autorizzazione negativa cliente. Gli upgrade popolati V14, V16 e V18 raggiungono V32 preservando prodotti e ordini. Chromium esegue i workflow MVP; Firefox e WebKit coprono autenticazione e accessibilita.

Regressioni accessibilita e bozze Step 3.9:

```bash
cd web/frontend
npm ci
npm run typecheck
npm run typecheck:e2e
npm test
npm run build
```

Risultato: 198/198 test frontend passati in 39 file, typecheck applicativo ed E2E completati, build Vite riuscita e audit npm senza vulnerabilita. La suite copre focus trap, `Escape`, ripristino focus, nomi accessibili, semantica tabelle, bozze per vista/entita, dirty guard e parser download difensivo.

Matrice browser Step 3.9:

```bash
scripts/ci/run-prod-like-verification.sh
```

Risultato: 13/13 test Playwright/axe passati su Chromium 1440x900, Chromium 390x844, Chromium 768x1024, Firefox 1440x900 e WebKit 1440x900. Login, ciclo ordine, navigazione da tastiera, dialog, preservazione bozze, assenza di overflow e download sono verificati sullo stack reale. Nessuna violazione axe critica o seria rilevata. Il medesimo run ha verificato 32 migrazioni Flyway, rate limiting, hardening, CSP, metriche, backup/restore e least privilege PostgreSQL.

Regressioni mirate Step 3.8:

```bash
cd web/backend
mvn -Dtest=OrderPaymentIntegrationTest,OrderReturnConcurrencyTest test

cd ../frontend
npm test -- src/components/orders/OrderOperationsPanel.test.tsx src/features/orders/useOrderFlow.test.tsx src/hooks/useCommandExecution.test.tsx
```

Risultato: due resi su piu prodotti, quantita cumulative, rimborsi parziali, isolamento ledger per return ID e concorrenza sull'ultima unita verificati. Il frontend copre payload multi-riga, residui, note per ID, outcome tipizzati, reset dopo successo o avviso persistito e conservazione della bozza dopo errore o invio duplicato. Suite frontend completa: 188 test in 35 file; build TypeScript/Vite di produzione riuscita.

Regressioni mirate Step 3.6:

```bash
cd web/backend
mvn -Dtest=OperationalDetailControllerTest,FiscalDocumentPaginationControllerTest test

cd ../frontend
npm test -- src/features/catalog/useCatalogFlow.test.tsx src/features/orders/useOrderFlow.test.tsx src/features/documents/DocumentReportExperience.test.tsx src/components/orders/OrderOperationsPanel.test.tsx
```

Risultato: dettaglio prodotto con storico fuori pagina, dettaglio ordine escluso dai filtri, capability nota credito indipendente dalla pagina e capability separate dei resi verificati. Suite frontend completa: 183 test in 35 file; build TypeScript/Vite di produzione riuscita.

Contratto benchmark mirato:

```bash
cd web/backend
mvn -Dtest=WorkflowBenchmarkContractTest test
```

Il test rende verificabile la governance documentale, ma non sostituisce il test con un operatore reale definito in `WORKFLOW_BENCHMARKS.md`, che resta da eseguire dopo l'implementazione dello Step 3.1.

Gate PostgreSQL obbligatorio:

```bash
scripts/db/verify-postgresql-suite.sh fast
```

Il profilo `postgresql-it` avvia PostgreSQL 16 tramite Testcontainers, applica V1-V35 con Hibernate in `validate` e seleziona i test taggati `postgresql`. Nello Step 4.3 esegue 105 test e copre sessioni, tentativi login concorrenti, identita canoniche, stock, ledger, idempotenza, numerazioni, annullamenti, pagamenti, riconciliazione, resi concorrenti, vendita assistita, lifecycle account, fuso aziendale, snapshot temporali, ordini fornitore, ricezioni concorrenti con costo, inventario fisico concorrente e autorizzazioni negative. Il runner prosegue con gli upgrade popolati V14/V16/V18. H2 resta nella suite standard per feedback rapido, ma nessuna correzione P0/P1 dipendente dal database si affida soltanto a H2.

Il gate settimanale aggiunge il profilo volumetrico:

```bash
scripts/db/verify-postgresql-suite.sh nightly
```

Least privilege PostgreSQL:

```bash
scripts/db/verify-database-least-privilege.sh
```

Il runner costruisce PostgreSQL 16 in isolamento, applica le migrazioni fino alla versione corrente tramite migrator e owner, avvia un test backend con il runtime e prova i divieti DDL. Verifica inoltre backup read-only, restore dedicato e rotazione di tutte le password con rifiuto delle credenziali precedenti. Risultato verificato: runtime DML senza DDL, backup senza scrittura, migrator owner-scoped, restore capace di ricreare solo il database di prova e cleanup completato.

La proposta privacy e fiscale e protetta da quattro test contrattuali statici che verificano confini, assenza di dichiarazioni di conformita, workflow sicuri, separazione dei documenti elettronici e rollout a gate. La proposta commerciale aggiunge quattro contratti statici su control plane, onboarding riprendibile, branding sicuro, release immutabili, rollout per coorti e migrazioni expand/contract. Questi test proteggono decisioni architetturali, ma non dimostrano funzionalita non ancora implementate.

Il backend dichiara Java 17 come baseline. I test che usano Mockito vengono eseguiti con Byte Buddy agent configurato da Maven Surefire, quindi non richiedono argomenti manuali anche quando la JVM locale e piu recente.

Baseline runtime e avvio fail-closed:

```bash
cd web/backend
mvn -B -DskipTests package
cd ../..
scripts/security/verify-backend-fail-closed.sh web/backend/target/gestionale-api-0.1.0.jar
cd web/backend
mvn -B dependency-check:check
```

Il controllo verifica Spring Boot 4.1/Security 7.1, assenza di profilo predefinito, bootstrap senza valori noti, H2 confinato ai test e gate SCA CVSS 7.

Risultato verificato: JAR generato; arresto fail-closed superato nei due scenari; 66 dipendenze analizzate e nessuna vulnerabilita presente nel report OWASP finale. Le versioni effettive includono Tomcat 11.0.24, pgJDBC 42.7.12, Log4j 2.25.5 e Spring Security 7.1.0.

Fixture storiche anonimizzate:

```bash
scripts/db/verify-historical-fixtures.sh small
```

Il verificatore controlla i checksum, avvia un solo container PostgreSQL con nome e label univoci, senza porte host o volumi persistenti, e costruisce database puliti agli schemi V14, V16, V18, V19, V20, V21 e V22. Ogni snapshot carica dati sintetici per stati ordine, pagamenti, resi, documenti, collisioni case-insensitive, ownership storica, drift magazzino, riconciliazione finanziaria, quarantena degli account operativi storici, migrazione delle sessioni e ownership ordini verso ID stabili. I profili `small`, `medium` e `large` definiscono volumi deterministici; `small` e il gate rapido locale.

Il cleanup puo rimuovere esclusivamente il container creato dal verificatore dopo averne controllato la label. Lo script non usa Compose, non pubblica PostgreSQL e non interagisce con stack o database esistenti.

Risultato verificato con PostgreSQL 16 e profilo `small`: gli snapshot V14, V16, V18, V19, V20, V21 e V22 sono stati caricati su database puliti, le relative asserzioni sono passate e il container effimero e stato rimosso senza lasciare risorse residue. Il contratto Java dedicato aggiunge 5 test su checksum, copertura del manifest, anonimizzazione, profili e stati dei workflow.

Upgrade correttivi e concorrenza documentale:

```bash
scripts/db/verify-historical-fixtures.sh small upgrades
scripts/db/verify-historical-fixtures.sh medium upgrade-v18
scripts/db/verify-historical-fixtures.sh large upgrade-v18
scripts/db/verify-payment-numbering-postgres.sh
```

I primi tre comandi verificano preflight, upgrade V14/V16/V18 verso la versione corrente V34, quarantena dei pagamenti privi di evidenza, contatori `max + 1`, riconciliazione esplicita delle collisioni canoniche, ledger, idempotenza durevole, annullamenti, link finanziari, lockout, snapshot della vendita assistita, stato lifecycle degli account, contratto temporale, ordini fornitore e valorizzazione conservativa delle ricezioni. Il quarto esegue i test applicativi su PostgreSQL 16, compresi riconciliazione ed emissione concorrente. Le misure e la procedura operativa sono in `PAYMENT_RECONCILIATION.md`.

Identificatori canonici su PostgreSQL:

```bash
scripts/db/verify-canonical-identifiers-postgres.sh
```

Il runner applica V1-V23 su PostgreSQL 16 effimero e verifica cinque casi applicativi: conservazione dei valori visuali, lookup con casing/spazi diversi, collisioni sequenziali, tre coppie di inserimenti concorrenti e risposta API `409 RESOURCE_CONFLICT`. Il test migrazione separato verifica upgrade pulito V22-V23 e arresto su collisioni storiche. Risultato: 5 test PostgreSQL e 2 test migrazione passati; container rimosso senza volumi persistenti. Preflight e bonifica manuale sono descritti in `CANONICAL_IDENTIFIERS.md`.

Concorrenza sessioni su PostgreSQL:

```bash
scripts/db/verify-session-concurrency.sh
```

Il runner usa PostgreSQL 16 in un container isolato con nome e label univoci, senza porte host o volumi persistenti. Verifica sei casi sul subject account ID, inclusi revoca contro touch, rotazione contro touch, cambio password, cambio ruolo e disabilitazione con username storico non riutilizzabile. Risultato: 6 test passati e cleanup completato.

Ownership ordini su PostgreSQL:

```bash
scripts/db/verify-order-ownership.sh
```

Il runner applica tutte le 21 migrazioni su PostgreSQL 16 pulito e verifica otto casi di servizio e API: omonimie, rename, account non collegato, display name duplicati, ordini `UNRESOLVED`, accesso cliente, annullamento e richiesta reso. Risultato: 8 test passati e cleanup completato. Il controllo ha inoltre rilevato e corretto una transazione sessione read-only incompatibile con il lock pessimista PostgreSQL.

Idempotenza concorrente su PostgreSQL:

```bash
scripts/db/verify-idempotency-postgres.sh
```

Il runner applica V1-V25 su PostgreSQL 16 effimero e avvia i test del controller idempotente, inclusi due thread sulla stessa chiave, replay dopo risposta persa, timeout di un claim attivo, retry dopo errore, payload diverso e cleanup. Risultato verificato: 10 test passati, una sola mutazione per intento e container rimosso senza volumi persistenti.

Annullamento ordine su PostgreSQL:

```bash
scripts/db/verify-order-cancellation-postgres.sh
```

Il runner applica V1-V26 su PostgreSQL 16 effimero e verifica annullamento con acconto o saldo, riferimento obbligatorio, reversal unico, rollback a meta rilascio scorte, due operatori concorrenti e replay idempotente. Risultato: 18 test passati e container rimosso senza volumi persistenti.

Riconciliazione finanziaria su PostgreSQL:

```bash
scripts/db/verify-financial-reconciliation-postgres.sh
```

Il runner applica V1-V27 su PostgreSQL 16 effimero e verifica migrazione conservativa, collegamento rimborso-reso, ledger e proiezioni, vincoli SQL negativi, rollback dopo fallimento e due rimborsi concorrenti. Risultato: 22 test passati e container rimosso senza volumi persistenti.

Frontend web:

```bash
cd web/frontend
npm ci
npm test
npm run typecheck
npm run typecheck:e2e
npm run build
```

Risultato: installazione pulita completata, 179 test passati in 35 file, typecheck applicativo ed E2E e build completati. La slice Amministrazione copre account, audit, configurazione aziendale e monitoraggio, inclusi permessi, cancellazione richieste, caricamento lazy, cache, `401`, `403`, `500`, offline, risposte fuori ordine, comandi, protezioni dell'account corrente e conservazione dei form. Le slice precedenti continuano a coprire sessione, catalogo, ordini, pagamenti, resi, partner, documenti e report, inclusi doppio click, conservazione di bozze/form e retry ambiguo delle mutazioni.

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

Rate limiting registrazione su stack avviato:

```bash
scripts/security/verify-registration-rate-limit.sh http://127.0.0.1:8081
```

Lo script verifica la zona Nginx dedicata, la risposta `429 RATE_LIMIT_EXCEEDED`, `Retry-After`, gli header di sicurezza, il request ID e la disponibilita del frontend. La prova runtime sullo stack Docker aggiornato e stata completata nello Step 1.5.

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

Risultato verificato: immagini PostgreSQL/backend/frontend costruite, processi non-root e privilegi minimi confermati, readiness e liveness backend in stato `UP` sulla porta management interna, endpoint Actuator non utilizzabili dalla porta API, frontend `/health` raggiungibile, 19 test Playwright/axe passati e rate limiting login/registrazione applicato. In CI lo stack viene costruito e verificato automaticamente.

Verifica prod-like completa e ricorrente:

```bash
scripts/ci/run-prod-like-verification.sh
```

Il runner usa credenziali effimere e un project name casuale, controlla le 32 migrazioni Flyway sul database reale, esegue sicurezza, osservabilita, Playwright, rate limiting, backup/restore e least privilege, acquisisce inventario e diagnostica e certifica il cleanup tramite label project/run ID. La prova completa dello Step 3.10 ha superato 19/19 test Playwright/axe, i limiti login e registrazione, il ciclo backup/restore e la matrice dei ruoli PostgreSQL senza lasciare risorse residue. La CI lo esegue anche ogni lunedi alle 03:23 UTC.

Contratto e prova runtime della sicurezza dei runner:

```bash
cd web/backend
mvn -B -Dtest=DockerRunnerSafetyContractTest,RecurringProdLikeWorkflowTest test
cd ../..
scripts/ci/test-docker-run-safety.sh
```

La prova preserva un volume sentinella con dato persistente, rifiuta collisioni e verifica `HUP`, `INT`, `TERM` e recovery esterno dopo `SIGKILL`.

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
- autorizzazioni principali con Spring Security;
- capability di prodotto, ordine, reso e documento calcolate dal backend e indipendenti dalle liste paginate;
- dettagli prodotto/ordine con query mirate per storico e dashboard priva di fallback da pagine parziali;
- matrice autorizzazioni su prodotti, magazzino, ordini, documenti, account e audit;
- contratto errori API per sessione mancante e validazione;
- sessioni persistenti con token hashato, revoca logout, scadenza assoluta e timeout inattivita;
- rinnovo sessione con rotazione atomica, rifiuto replay del token precedente, password errata non distruttiva e header anti-cache;
- metriche sessione attiva coerenti con scadenza assoluta e timeout di inattivita;
- tentativi login falliti persistenti, lockout e cleanup programmato;
- rate limiting Nginx indipendente per login e registrazione pubblica, con errore `429`, request ID, `Retry-After` e verificatori dedicati;
- CSP Nginx in enforcement, header browser uniformi e cache differenziata tra shell HTML e asset versionati;
- separazione tra anagrafica prodotto e comandi inventariali: i payload prodotto non possono variare lo stock;
- ledger autorevole con saldo iniziale unico, delta firmato, origine, snapshot coerenti e report di riconciliazione;
- optimistic locking prodotto e protezione da rettifiche/scarichi stock concorrenti;
- stock riservato, disponibilita vendibile e blocco scarichi manuali su quantita prenotate;
- movimento magazzino con snapshot coerente di quantita precedente e nuova;
- baseline storiche classificate come non verificate, catena interrotta e drift del ledger rilevabili senza correzione automatica;
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
- security workflow con dependency review, audit npm, OWASP Dependency-Check, CodeQL e Dependabot.
- stack Docker prod-like con PostgreSQL reale, backend Spring Boot e frontend Nginx.
- backend prod-like pubblicato solo su loopback per evitare il bypass remoto del rate limiting Nginx.
- backup atomico PostgreSQL con checksum, lock, retention e freschezza;
- restore sintetico isolato e restore drill schedulato dell'ultimo backup reale.
- endpoint monitoraggio protetto con stato database, runtime, sessioni, audit sensibile ed errori API recenti;
- accesso al monitoraggio consentito al super admin e negato ai ruoli non autorizzati.
- protezioni sul ciclo di vita prodotto: codice immutabile dopo uso in ordini, blocco cancellazione con stock riservato o ordini collegati, disattivazione prodotto e ordine storico ancora evadibile.
- password policy backend applicata a registrazione pubblica e creazione account amministrativa;
- registrazione pubblica verificata per assegnazione esclusiva del ruolo `CUSTOMER`, rifiuto di `EMPLOYEE`, `ADMIN` e `SUPER_ADMIN`, audit `SELF_SERVICE` e mantenimento del flusso amministrativo per lo staff;
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
- suite frontend Vitest e React Testing Library con 179 test in 35 file su autenticazione, registrazione, password non valida, sessione scaduta, rinnovo e rotazione sessione, prodotti, ordini, pagamenti, annullamenti, resi, anagrafiche, documenti, report, export, paginazione, dashboard, account, audit, configurazione aziendale, monitoraggio e contratto errori HTTP.
- provider sessione verificato su `401` autenticato, `401` pubblico e avviso di scadenza imminente.
- hook paginato del pilot Account verificato per caricamento, refresh, `401`, `403`, `500`, errore offline, annullamento della richiesta obsoleta e risposta fuori ordine con semantica latest-request-wins.
- debounce dei filtri remoti e propagazione di `AbortSignal` dal client Account fino al trasporto HTTP verificati con test dedicati.
- vertical slice sessione verificata su login, registrazione, errore offline, doppio submit concorrente, rinnovo con token scaduto, cambio password, logout remoto fallito e collegamento tra pagina di accesso e modali.
- vertical slice catalogo verificata su projection staff/cliente, `401`, `403`, `500`, offline, annullamento richiesta obsoleta, latest-request-wins, creazione, errore comando, eliminazione e composizione tabella/dettaglio/form.
- vertical slice ordini verificata su query e clienti vendita, riconciliazione finanziaria, bozza, comandi di ordine, pagamento e reso, permessi, errori HTTP/rete, richieste obsolete e conservazione della bozza.
- vertical slice partner verificata su query e lookup account cancellabili, permessi, errori HTTP/rete, richieste fuori ordine, CRUD, disattivazione, collegamento account e conservazione del form.
- vertical slice documenti/report verificata su query cancellabili, latest-request-wins, caricamento lazy e cache report, emissione fattura/nota credito, conservazione causale, protezione export duplicati, errori e composizione delle pagine.
- esecuzione automatica dei test frontend nella pipeline CI prima della build Vite.
- smoke test Playwright su login negativo, login super admin, navigazione a schede, creazione prodotto, report magazzino con download CSV, verifica responsive senza overflow a 1024x768, registrazione cliente, carrello, creazione ordine e conferma ordine contro lo stack Docker reale.
- acquisizione automatica di report HTML, trace, screenshot e video Playwright in caso di errore CI.

## Gap principali

- Mancano test API funzionali approfonditi per tutti i controller.
- Mancano test repository significativi su query e vincoli.
- Manca il collegamento delle regole Prometheus a un Alertmanager e a un canale on-call dell'ambiente reale.
- Mancano test concorrenza approfonditi sulle transizioni ordine con stock riservato.
- Mancano test dedicati su rollback/compatibilita delle migrazioni, oltre alla validazione Flyway nei test backend.
- La copertura browser include i workflow verticali MVP e il rinnovo sessione, ma non e ancora esaustiva per ogni variante di documenti, audit, filtri avanzati e permessi granulari.
- Il gate CVE OWASP iniziale non sostituisce policy licenze, SBOM firmata o SCA professionale dello Step 5.1.
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
