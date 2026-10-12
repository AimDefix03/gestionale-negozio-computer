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

- Spring Boot 4.1.0.
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
- `product`: anagrafica di catalogo e proiezione transazionale delle disponibilita, senza comandi pubblici di variazione stock.
- `partner`: anagrafiche clienti e fornitori.
- `purchase`: ordini fornitore, righe, ricezioni amministrative, stati e capability di approvvigionamento.
- `inventory`: comandi di saldo e rettifica, ledger autorevole e riconciliazione di magazzino.
- `order`: ordini cliente, righe, pagamento aggregato, ledger finanziario e resi.
- `document`: documenti simulati collegati agli ordini.
- `company`: configurazione aziendale, aliquota predefinita e policy di numerazione documentale.
- `reporting`: read model operativi per vendite e magazzino ed export CSV, Excel e PDF.
- `audit`: storico operazioni.

I moduli business verificati automaticamente sono `user`, `product`, `partner`, `purchase`, `inventory`, `order`, `document`, `company` e `reporting`. I package tecnici trasversali sono esclusi esplicitamente dalla verifica e sono documentati in `MODULE_BOUNDARIES.md`.

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
- Idempotenza durevole per le mutazioni critiche: claim univoco per account stabile, operazione e chiave, fingerprint del payload, lease recuperabile e stati espliciti.
- Mutazione business e risposta idempotente completata nella stessa transazione; i retry concorrenti osservano il claim senza bloccare e ricevono replay o un esito temporaneo deterministico.
- Il client conserva la chiave dell'intento dopo timeout, errori di rete, errori server o claim ancora in corso e la rinnova solo dopo un esito definitivo o una modifica del payload.
- DTO e comandi prodotto separati dai comandi inventariali: l'anagrafica non accetta quantita.
- Aggiornamenti fisici centralizzati nel modulo inventario con optimistic locking sulla proiezione prodotto.
- Ogni variazione fisica scrive nella stessa transazione un movimento autorevole con prodotto, delta firmato, origine e snapshot prima/dopo.
- Una sola baseline autorevole per prodotto; le baseline storiche introdotte dalla migrazione restano esplicitamente non verificate.
- Riconciliazione esplicita tra somma del ledger, catena dei movimenti e proiezione di giacenza, senza correzioni automatiche dello storico.
- Disponibilita vendibile calcolata da giacenza fisica meno stock riservato.
- Documenti simulati con snapshot cliente separato dall'anagrafica viva.
- Configurazione aziendale singleton con optimistic locking; la numerazione blocca la riga di configurazione prima di allocare il progressivo annuale.
- Snapshot emittente e aliquota salvati nel documento; le modifiche successive non riscrivono lo storico.
- Query paginabili per liste operative.
- Le liste paginate sono viste di navigazione e non fonti complete per decisioni di dominio; i dettagli prodotto e ordine vengono composti da query mirate.
- Le capability operative sono calcolate una sola volta nei service di dominio usando identita, permessi, ownership, stato e vincoli persistiti, quindi pubblicate nei DTO.
- Il frontend mostra azioni esclusivamente dalle capability ricevute e non ricostruisce transizioni da righe eventualmente escluse da filtri o paginazione.
- Mapping JPA prudenti: evitare `EAGER` sulle relazioni operative e usare batch loading o query dedicate quando servono dati collegati.
- L'ordine fornitore conserva riferimenti stabili e snapshot e serializza le transizioni concorrenti; la ricezione orchestra ordine, posting fisico e valorizzazione gestionale tramite il servizio inventario nella stessa transazione.
- Costo effettivo, media ponderata mobile e copertura sono registrati soltanto per quantita con evidenza; stock storico e carichi manuali senza costo restano esplicitamente non valorizzati.
- Correlation ID propagato tra client, risposta API e log backend.
- Timestamp generati dal backend tramite `TimeProvider` basato su `Clock.systemUTC()`, sovrascrivibile nei test con clock fisso.
- Client frontend separati per dominio sotto `src/api`, con un unico `httpClient` responsabile di sessione, errori, request id e chiavi idempotenti legate all'intento.
- `SessionProvider` centralizza identita autenticata, token in memoria, scadenza, rinnovo e invalidazione su `401`; le richieste pubbliche prive di sessione non attivano il flusso di rinnovo.
- Le risorse paginate delle slice Catalogo, Ordini, Partner, Documenti e Amministrazione usano `usePaginatedResource`: espone `loading`, `refreshing`, `error` e `data`, annulla richieste obsolete con `AbortController` e applica una sequenza latest-request-wins.
- I filtri remoti del pilot sono sottoposti a debounce; le mutazioni restano separate dalle query e non vengono ritentate automaticamente quando l'esito potrebbe essere ambiguo.
- La prima vertical slice sotto `src/features/session` possiede form e comandi per login, registrazione, rinnovo, logout e cambio password; `App.tsx` riceve soltanto il controller della sessione e callback esplicite di ingresso/uscita.
- La vertical slice `src/features/catalog` possiede query debounced, pagine staff/cliente, lookup, selezione, focus, stato di modifica e comandi prodotto. Le query propagano `AbortSignal` al trasporto HTTP e applicano latest-request-wins; magazzino e ordini entrano nella composizione soltanto tramite callback e dati espliciti.
- La vertical slice `src/features/orders` possiede query debounced di ordini e clienti vendita, riconciliazione finanziaria, bozza/carrello e comandi per ordine, pagamento e reso. Le query sono annullabili e latest-request-wins; catalogo, inventario, dashboard e documenti restano collegati tramite dati e callback esplicite.
- La vertical slice `src/features/partners` possiede query debounced, pagina, form, stato di modifica, lookup account cliente e comandi anagrafici. Il lookup e permission-aware e resta sincronizzato con il dominio Account tramite callback esplicite.
- La vertical slice `src/features/purchases` possiede lista paginata, filtri, bozza multi-riga, dettaglio, residui e comandi create/send/receive/cancel. Le azioni derivano dalle capability backend e gli errori non cancellano la bozza.
- La vertical slice `src/features/documents` possiede query documenti, report vendite e inventario, stato pagina, causale nota credito, emissione documenti ed export. Le query propagano `AbortSignal`, i report vengono attivati in modo lazy e le mutazioni conservano la pipeline comandi condivisa.
- La vertical slice `src/features/administration` possiede pagine account e audit, form e conferme account, configurazione aziendale, monitoraggio e relativi comandi. Le query sono permission-aware, cancellabili e latest-request-wins; monitoraggio e stato sistema sono lazy e restano in cache tra le viste, mentre le dipendenze con Partner sono espresse tramite callback esplicite.
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
- Prodotti, anagrafiche, movimenti, ordini, documenti, account e audit sono paginati lato API e usati dal frontend con filtri dedicati. Il catalogo operativo espone giacenza, riservato e disponibile soltanto al personale autorizzato; dettaglio e azioni non dipendono dalla pagina corrente.
- Il percorso `CUSTOMER` usa controller e DTO distinti: il catalogo espone solo disponibilita commerciale discreta e la dashboard compone esclusivamente conteggi e ordini dell'account stabile autenticato. L'autorizzazione della forma della risposta avviene prima della serializzazione.
- Le scorte basse sono calcolate lato repository sulla disponibilita vendibile, evitando il caricamento completo del catalogo per questo indicatore.
- La dashboard operativa espone un endpoint aggregato dedicato per statistiche prodotto, ordini e movimenti recenti e il frontend non usa fallback costruiti da pagine parziali; la dashboard cliente e una projection separata e non riusa il DTO operativo.
- Gli endpoint di dettaglio prodotto e ordine aggregano storici mirati e capability server-side; note credito e transizioni dei resi considerano anche record non presenti nei filtri o nelle pagine correnti.
- Il frontend inizializza il workspace con dashboard aggregata, lookup leggero prodotti e pagine correnti, riducendo il caricamento completo delle liste operative.
- Le righe di ordini, documenti e resi, i movimenti pagamento e le collezioni resi sono lazy con batch loading; le back-reference sono lazy per evitare caricamenti automatici non richiesti.
- Il backend non usa chiamate dirette a `now()` nel codice produttivo: ordini, documenti, movimenti, audit, idempotenza, anagrafiche, sessioni, errori API e monitoraggio ricevono il tempo dal clock applicativo UTC.
- Pagine, layout, componenti e client API sono separati per dominio e tutte le sei vertical slice previste sono estratte. `App.tsx` resta la shell di navigazione e coordina dashboard, inventario e callback esplicite tra domini; le ulteriori riduzioni dovranno restare incrementali e non introdurre un refactor big-bang.
- Il frontend dispone di test Vitest/RTL e smoke Playwright su hardening browser, login, catalogo, registrazione cliente e ordine; i moduli amministrativi secondari restano da estendere lato browser.
- Le richieste operative di magazzino, ordini e documenti derivano `actor` e `role` dalla sessione autenticata.
- Il `requestId` e propagato via `X-Request-Id`, restituito nelle risposte, inserito negli errori API, registrato nei log richiesta e salvato negli eventi audit.
- Il seed super admin e governato da configurazione: attivo in `dev/test`, disattivato in `prod`.
- Il sistema corrente resta single-tenant: la proposta multi-azienda non e ancora implementata e non deve essere presentata come funzionalita disponibile.
- Privacy workflow, retention generale, conservazione a norma e fatturazione elettronica non sono implementati e non devono essere presentati come conformita disponibile.
- Onboarding SaaS, branding tenant, entitlement commerciali, provisioning e aggiornamento della flotta non sono implementati; deployment e bootstrap restano procedure della singola installazione.

## Legacy Swing

La parte Swing mantiene valore come storico e come dimostrazione dei pattern OOP. E esclusa dalla build predefinita e resta disponibile tramite `pom-legacy.xml` finche i flussi web non sono equivalenti o superiori. Non riceve nuove funzionalita commerciali.
