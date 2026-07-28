# Architecture

## Stato attuale

Il repository contiene:

```text
.
├── src/                  # Applicazione desktop Java Swing legacy
├── web/backend/          # API Spring Boot
├── web/frontend/         # Frontend React + TypeScript
├── pom.xml               # Build aggregata web-first
├── pom-legacy.xml        # Build Swing esplicita
└── README.md
```

## Stack verificato

Swing legacy:

- Java con release Maven 17.
- Swing + FlatLaf.
- JUnit 5.
- Persistenza locale via file `.dat`.

Backend web:

- Spring Boot 3.4.5.
- Java 17 configurato, eseguito localmente con Java 23.
- Spring Web, Validation, Data JPA, Security.
- Spring Modulith 1.3 per verifica dei confini business.
- PostgreSQL per sviluppo locale tramite Docker Compose.
- H2 in memoria per test automatici.
- Flyway per migrazioni versionate.
- Hibernate `ddl-auto: validate`.
- Apache POI per workbook Excel `.xlsx` reali.
- OpenPDF 2.0.x, compatibile con Java 17, per report PDF.
- Profili `dev`, `test` e `prod` separati.

Frontend web:

- React.
- TypeScript.
- Vite.
- Vitest, jsdom e React Testing Library per test automatici.
- Playwright Chromium per smoke test browser end-to-end.
- API client custom via `fetch`.
- Pagine operative separate in `src/pages`.
- Componenti condivisi e layout in `src/components`.
- Hook di interazione e navigazione in `src/hooks`.
- Tipi UI e formattatori separati dalla logica applicativa.

## Moduli backend web

- `common`: errori API, eccezioni, correlation ID, clock applicativo UTC e seed configurabile del super admin.
- `security`: filtro token sessione e configurazione Spring Security.
- `user`: account, ruoli, permessi, sessioni, password.
- `product`: catalogo prodotti.
- `partner`: anagrafiche clienti e fornitori.
- `inventory`: movimenti di magazzino.
- `order`: ordini cliente, righe, pagamento aggregato, ledger finanziario e resi.
- `document`: documenti simulati collegati agli ordini.
- `company`: configurazione aziendale, aliquota predefinita e policy di numerazione documentale.
- `reporting`: read model operativi per vendite e magazzino ed export CSV, Excel e PDF.
- `audit`: storico operazioni.

I moduli business verificati automaticamente sono `user`, `product`, `partner`, `inventory`, `order`, `document`, `company` e `reporting`. I package tecnici trasversali sono esclusi esplicitamente dalla verifica e sono documentati in `MODULE_BOUNDARIES.md`.

## Direzione target

Architettura target: monolite modulare web-first.

Motivazione:

- Il dominio e ancora in evoluzione.
- I moduli condividono transazioni e dati.
- I microservizi aggiungerebbero complessita non giustificata.
- Un monolite modulare consente separazione chiara senza frammentare deploy e database.

## Regole architetturali target

- Controller leggeri: HTTP, validazione, security context, DTO.
- Service applicativi: transazioni e regole operative.
- Entita JPA non esposte direttamente come API pubbliche.
- Migrazioni database versionate.
- Codici business generati tramite sequenze database.
- Errori API con contratto stabile e codice applicativo.
- Autorizzazione verificata lato backend.
- Sessioni persistite su database con token hashati, scadenza assoluta, timeout inattivita, revoca esplicita e rotazione atomica al rinnovo.
- Lockout login persistito su database.
- Cleanup programmato dei dati di sicurezza obsoleti.
- Audit su operazioni sensibili con contesto richiesta e tipo entita.
- Aggiornamenti stock centralizzati con optimistic locking sui prodotti.
- Disponibilita vendibile calcolata da giacenza fisica meno stock riservato.
- Documenti simulati con snapshot cliente separato dall'anagrafica viva.
- Configurazione aziendale singleton con optimistic locking; la numerazione blocca la riga di configurazione prima di allocare il progressivo annuale.
- Snapshot emittente e aliquota salvati nel documento; le modifiche successive non riscrivono lo storico.
- Query paginabili per liste operative.
- Mapping JPA prudenti: evitare `EAGER` sulle relazioni operative e usare batch loading o query dedicate quando servono dati collegati.
- Correlation ID propagato tra client, risposta API e log backend.
- Timestamp generati dal backend tramite `TimeProvider` basato su `Clock.systemUTC()`, sovrascrivibile nei test con clock fisso.
- Client frontend separati per dominio sotto `src/api`, con un unico `httpClient` responsabile di sessione, errori, request id e idempotenza.
- Token browser conservato solo in memoria e inviato tramite header custom; la scelta e le condizioni per una futura migrazione cookie/CSRF sono registrate nell'ADR 0004.
- Nginx applica una CSP same-origin in enforcement senza direttive permissive, uniforma gli header browser e separa la cache della shell da quella degli asset versionati.
- Layout frontend fluido: shell senza `min-width` globale, griglie responsive progressive e overflow orizzontale confinato alle tabelle operative.
- Test frontend separati tra componenti, trasporto HTTP e flussi dei client di dominio, eseguiti anche in CI.
- Smoke test browser eseguiti sullo stack prod-like reale con database E2E isolato e diagnostica Playwright su errore.
- Management plane separato nel profilo produzione: API sulla porta `8080`, Actuator interno sulla `9090`, esposizione limitata a `health` e `prometheus`, dettagli health nascosti e probe liveness/readiness distinte.
- Grafo dei moduli business verificato con Spring Modulith durante la suite Maven.
- Dipendenze inverse tra domini espresse tramite porte; il catalogo verifica l'uso negli ordini tramite `ProductOrderUsage` senza importare repository del modulo ordine.
- Report costruiti da porte di lettura pubbliche dei moduli `order` e `product`, senza esporre entita o repository interni.
- Export sincroni limitati a 10.000 righe, con filtri applicati dal database prima della materializzazione del file.
- Evoluzione multi-tenant pianificata con schema condiviso, contesto derivato dalla sessione, foreign key composte e PostgreSQL RLS; i clienti con requisiti superiori possono usare lo stesso modello su database dedicato. La proposta completa e nell'ADR 0005 e in `MULTI_TENANCY_ARCHITECTURE.md`.
- Evoluzione privacy e fiscale pianificata con contesti separati per richieste degli interessati, retention, legal hold, conservazione e fatturazione elettronica; adapter esterni isolano conservatore e canale SdI. La proposta completa e nell'ADR 0006 e in `PRIVACY_RETENTION_EINVOICING_ARCHITECTURE.md`.
- Evoluzione commerciale pianificata con control plane logicamente separato, onboarding a stati, provisioning idempotente pooled/dedicato, branding tramite token sicuri, entitlement server-side e release immutabili promosse per digest. Migrazioni e rollout della flotta seguono coorti ed expand/contract secondo ADR 0007 e `CUSTOMER_LIFECYCLE_AND_RELEASE_ARCHITECTURE.md`.

## Problemi attuali da correggere

- Docker Compose prod-like copre PostgreSQL, backend e frontend; resta da trasformarlo in una configurazione di produzione completa con TLS, secret manager e orchestrazione operativa.
- Nessun Maven Wrapper.
- La migrazione iniziale esiste e le evoluzioni principali sono versionate; restano da validare su dataset reali.
- Prodotti, anagrafiche, movimenti, ordini, documenti, account e audit sono paginati lato API e usati dal frontend con filtri dedicati; il catalogo espone giacenza, riservato e disponibile.
- Le scorte basse sono calcolate lato repository sulla disponibilita vendibile, evitando il caricamento completo del catalogo per questo indicatore.
- La dashboard espone un endpoint aggregato dedicato che compone statistiche prodotto, ordini e movimenti recenti rispettando i permessi del ruolo autenticato.
- Il frontend inizializza il workspace con dashboard aggregata, lookup leggero prodotti e pagine correnti, riducendo il caricamento completo delle liste operative.
- Le righe di ordini, documenti e resi, i movimenti pagamento e le collezioni resi sono lazy con batch loading; le back-reference sono lazy per evitare caricamenti automatici non richiesti.
- Il backend non usa chiamate dirette a `now()` nel codice produttivo: ordini, documenti, movimenti, audit, idempotenza, anagrafiche, sessioni, errori API e monitoraggio ricevono il tempo dal clock applicativo UTC.
- `App.tsx` e stato ridotto da 1.348 a 867 righe separando pagine, layout, componenti e hook; i client API sono divisi per dominio e pubblicati tramite un barrel condiviso. La shell e responsive su notebook e tablet, ma resta da distribuire lo stato di dominio e suddividere `styles.css`.
- Il frontend dispone di test Vitest/RTL e smoke Playwright su hardening browser, login, catalogo, registrazione cliente e ordine; i moduli amministrativi secondari restano da estendere lato browser.
- Le richieste operative di magazzino, ordini e documenti derivano `actor` e `role` dalla sessione autenticata.
- Il `requestId` e propagato via `X-Request-Id`, restituito nelle risposte, inserito negli errori API, registrato nei log richiesta e salvato negli eventi audit.
- Il seed super admin e governato da configurazione: attivo in `dev/test`, disattivato in `prod`.
- Il sistema corrente resta single-tenant: la proposta multi-azienda non e ancora implementata e non deve essere presentata come funzionalita disponibile.
- Privacy workflow, retention generale, conservazione a norma e fatturazione elettronica non sono implementati e non devono essere presentati come conformita disponibile.
- Onboarding SaaS, branding tenant, entitlement commerciali, provisioning e aggiornamento della flotta non sono implementati; deployment e bootstrap restano procedure della singola installazione.

## Legacy Swing

La parte Swing mantiene valore come storico e come dimostrazione dei pattern OOP. E esclusa dalla build predefinita e resta disponibile tramite `pom-legacy.xml` finche i flussi web non sono equivalenti o superiori. Non riceve nuove funzionalita commerciali.
