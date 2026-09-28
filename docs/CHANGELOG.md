# Changelog

## 2026-09-05 - Step 4.3 inventario fisico e rettifiche approvate

- Introdotte sessioni di inventario con snapshot teorico, conteggio per riga, differenza, invio, approvazione separata e annullo motivato.
- Le operazioni di magazzino restano attive durante il conteggio; l'approvazione applica la differenza osservata alla giacenza corrente senza perdere i movimenti intermedi.
- Eliminata la rettifica diretta dalla UI e resa non operativa nell'API; ogni correzione passa da evidenza, motivazione e approvazione.
- Aggiunta la migrazione Flyway V35 con lifecycle, unicita delle sessioni attive, separazione submitter/approvatore e collegamento uno-a-uno al ledger.
- Aggiunto il permesso `APPROVE_INVENTORY_COUNTS`, assegnato ad admin e super admin ma non ai dipendenti.
- Aggiunti test su concorrenza, doppia approvazione, riserve, codici duplicati case-insensitive, autorizzazioni, upgrade storico e interfaccia React.
- Verificati 295 test backend, 105 test PostgreSQL, 202 test frontend, typecheck applicativo/E2E, build Vite e upgrade V14/V16/V18 fino a V35.

## 2026-08-29 - Step 4.2 ricezione merce e costo

- La ricezione fornitore applica nella stessa transazione aggiornamento ordine, carico fisico, movimento autorevole e costo effettivo.
- Introdotta la migrazione Flyway V34 con ultimo costo, media ponderata mobile, quantita valorizzata, collegamenti ordine-ricezione-movimento e vincoli di posting.
- Lo storico privo di evidenza resta non valorizzato; nessun costo viene inferito dal prezzo di vendita o da carichi manuali.
- Dashboard, catalogo e report staff distinguono valore potenziale di vendita, valore noto a costo, copertura e margine potenziale; la projection cliente non espone costi.
- Ricezioni parziali, retry, concorrenza sullo stesso prodotto, baseline, arrotondamento e upgrade popolati sono protetti da test.
- Verificati 288 test backend, 99 test PostgreSQL, 202 test frontend, typecheck applicativo/E2E, build Vite e upgrade V14/V16/V18 fino a V34.

## 2026-08-28 - Step 4.1 ordini fornitore

- Introdotto il modulo `purchase` con ordini fornitore, righe, stati, ricezioni amministrative e annullo motivato del residuo.
- Aggiunta la migrazione Flyway V33 con tabelle, sequenze, vincoli e indici additivi; nessun dato storico viene inferito o riscritto.
- Fornitore e prodotti usano ID stabili e snapshot separati; i prodotti referenziati non sono rinominabili o eliminabili.
- Tutte le mutazioni sono autorizzate, idempotenti, auditabili e serializzate sulle transizioni concorrenti.
- Aggiunta la vertical slice React per elenco paginato, filtri, bozza multi-riga, dettaglio, residui, ricezioni e capability backend.
- Ricezione amministrativa separata esplicitamente da stock fisico e costo, rinviati allo Step 4.2.
- Verificati 285 test backend, 98 test PostgreSQL, 200 test frontend, typecheck applicativo/E2E, build Vite e upgrade popolati V14/V16/V18 fino a V33.

## Step 3.10 - Gate E2E dell'MVP

- Aggiunta una suite verticale seriale per creazione dipendente, vendita assistita, ciclo stock-ordine-riserva-evasione, acconto-annullamento-storno, reso multi-riga-ricezione-rimborso, scadenza sessione durante una query e autorizzazione negativa cliente.
- Introdotto `scripts/e2e/run-mvp-gate.sh` come ingresso unico per verifica prod-like e gate PostgreSQL `fast`, con arresto immediato al primo errore.
- Corretto il rinnovo sessione: la rotazione del token non reinizializza piu il workspace e non cancella il feedback dell'operazione in corso.
- Corretto il token colore di errore per raggiungere il contrasto WCAG AA.
- Aggiunto un contratto backend che protegge scenari obbligatori, composizione del runner, dipendenze CI/PostgreSQL e copertura Firefox/WebKit.
- Suite verificate: 277 test backend, 198 test frontend, typecheck applicativo/E2E, build Vite, audit npm con zero vulnerabilita e 19/19 Playwright/axe.
- Gate PostgreSQL `fast` completato con migrazioni V1-V32 e upgrade popolati V14/V16/V18; prod-like verificato con hardening, osservabilita, rate limiting, backup/restore e cleanup.
- Gate C e F-31 chiusi tecnicamente nel repository reale; la validazione operatore resta esplicitamente separata.

## Step 3.9 - Accessibilita e preservazione delle bozze

- Tabelle e liste dati dotate di caption, intestazioni semantiche e nomi accessibili; form e azioni verificati tramite axe e albero di accessibilita.
- Dialog modali centralizzati con focus trap, `Escape`, focus iniziale e ripristino del controllo di origine senza attivare il workspace sottostante.
- Menu e schede navigabili da tastiera, focus visibile, contrasto corretto, target touch da almeno 44 px e supporto a `prefers-reduced-motion`.
- Draft store in `sessionStorage` per vista ed entita, con dirty guard su cambio vista, chiusura scheda, logout e uscita browser; password escluse dalla persistenza.
- Parser download difensivo per `Content-Disposition`, sanitizzazione filename e revoca URL compatibile con WebKit.
- Titoli e nomi accessibili della vendita cliente allineati a `Nuovo ordine`.
- Suite estesa a 198 test frontend in 39 file e 13 test Playwright/axe su Chromium desktop/mobile/tablet, Firefox e WebKit; verifica prod-like completa superata.
- Backend e database invariati: 274 test backend e 32 migrazioni Flyway confermati.

## Step 3.8 - Resi multi-riga e feedback operativo

- Builder resi esteso a piu prodotti dello stesso ordine, con quantita gia restituita o impegnata e residuo restituibile calcolati dal backend.
- Protezione del residuo anche in concorrenza: due richieste sulla stessa ultima unita non possono creare due resi validi.
- Note di revisione e selezioni operative indicizzate tramite ID stabile del reso, evitando associazioni errate tra resi distinti.
- Ogni reso espone il proprio ledger dei rimborsi con codice transazione, importo, causale, riferimento, operatore e timestamp.
- Outcome dei comandi tipizzati in successo, avviso persistito ed errore; i form vengono azzerati soltanto dopo una persistenza confermata.
- Contratto API aggiornato in modo additivo; nessuna migrazione database richiesta.
- Suite estesa a 274 test backend, 188 test frontend e 97 test PostgreSQL nel profilo `fast`; build Vite di produzione completata.

## Step 3.7 - KPI, timezone e configurazione aziendale

- Dashboard ordini separata per bozze, confermati, evasi e annullati; valori economici distinti tra lordo ordini, incassato, rimborsato e netto incassato.
- Valore della giacenza rinominato in valore potenziale di vendita, senza rappresentarlo come valore contabile o valore a costo.
- Contratto temporale esplicito: eventi API generali in UTC, documenti con offset del fuso aziendale salvato nello snapshot e filtri report interpretati nel fuso IANA configurato.
- Configurazione aziendale estesa con fuso orario, elenco dati mancanti e blocco della generazione documentale finche i dati emittente obbligatori non sono completi.
- Numerazione documentale basata sull'anno civile aziendale, inclusi confine di Capodanno e transizioni DST; disclaimer simulato preservato integralmente.
- Migrazione `V32` per fuso aziendale e snapshot temporale dei documenti, con default conservativi per i dati storici.
- Contratti API, manuale, README e definizioni KPI aggiornati; copertura aggiunta sulla matrice stati, sui valori finanziari, sui fusi e sulla configurazione incompleta.

## Step 3.6 - Capability e aggregazioni server-side

- Aggiunti dettagli operativi dedicati per prodotto e ordine con storici mirati indipendenti dalle liste paginate.
- Capability di prodotto, ordine, reso e documento calcolate nei service di dominio e pubblicate nei DTO come fonte autorevole per il frontend.
- Nota credito e azioni ordine non dipendono piu dal tipo di documento, dai filtri o dalla pagina correntemente caricata.
- Dashboard frontend collegata esclusivamente all'aggregazione backend, senza fallback costruiti dalle prime otto righe delle liste.
- Frontend aggiornato per mantenere il dettaglio selezionato fuori pagina e mostrare soltanto le azioni consentite dal server.
- Suite estesa a 265 test backend e 183 test frontend; build Vite di produzione completata.

## Step 3.5.6 - Vertical slice amministrazione

- Account, audit, configurazione aziendale e monitoraggio estratti da `App.tsx` in `features/administration/useAdministrationFlow`.
- Nuovo `AdministrationExperience` per comporre loading, empty state, errore, retry, dati precedenti durante il refresh e le quattro viste amministrative.
- Client account/audit/configurazione/sistema collegati a query cancellabili; account e audit sono debounced e latest-request-wins, mentre monitoraggio e stato sistema sono caricati in modo lazy e mantenuti tra le viste.
- Comandi account conservano command guard, aggiornamento locale, refresh mirato, protezioni sull'account corrente e form di ri-autenticazione in caso di errore.
- `App.tsx` ridotto da 707 a 477 righe senza modificare endpoint, payload, backend o schema dati.
- Suite frontend estesa a 179 test in 35 file; Step 3.5 completato con tutte le sei vertical slice previste.

## Step 3.5.5 - Vertical slice documenti e report

- Query documenti, report vendite e inventario, stato pagina, causale nota credito, emissione documenti ed export estratti da `App.tsx` in `features/documents/useDocumentReportFlow`.
- Nuovo `DocumentReportExperience` per comporre loading, errore, retry, dati precedenti durante il refresh e pagine documenti/report.
- Client documenti e report estesi con `AbortSignal`; query documenti debounced e latest-request-wins, report caricati in modo lazy e mantenuti disponibili tra le viste.
- Emissione fattura e nota credito mantengono command guard, aggiornamento locale e refresh mirato; una richiesta fallita conserva la causale inserita.
- Export protetti dai doppi invii dello stesso formato e dotati di feedback distinto per completamento ed errore.
- `App.tsx` ridotto da 797 a 707 righe senza modificare endpoint, payload, backend o schema dati.
- Suite frontend estesa a 146 test in 33 file; Step 3.5 resta in lavorazione al quinto dei sei domini previsti.

## Step 3.5.4 - Vertical slice partner

- Query, pagina, form, modifica e comandi anagrafici estratti da `App.tsx` in `features/partners/usePartnerFlow`.
- Nuovo `PartnerExperience` per loading, errore, retry e composizione di elenco e form clienti/fornitori.
- Lookup degli account cliente reso permission-aware e cancellabile; `fetchAccounts` propaga `AbortSignal` anche durante il caricamento multipagina.
- Creazione, modifica, disattivazione e link/unlink account mantengono command guard, aggiornamento locale e refresh mirato; gli errori non cancellano il form.
- `App.tsx` ridotto da 921 a 797 righe senza modificare endpoint, payload, backend o schema dati.
- Suite frontend estesa a 127 test in 31 file; Step 3.5 resta in lavorazione al quarto dei sei domini previsti.

## Step 3.5.3 - Vertical slice ordini, pagamenti e resi

- Query ordini e clienti vendita, riconciliazione finanziaria, bozza/carrello e comandi operativi estratti da `App.tsx` in `features/orders/useOrderFlow`.
- Nuovo `OrderExperience` per comporre vendita assistita, vendita cliente e gestione ordini mantenendo esplicite le dipendenze da catalogo, inventario, dashboard e documenti.
- Client ordini e anagrafiche estesi con `AbortSignal`; query debounced, annullamento richieste obsolete e latest-request-wins applicati alla slice.
- Tutti i comandi di ordine, pagamento e reso mantengono command guard, aggiornamento locale e refresh mirato; una creazione fallita non cancella la bozza.
- `App.tsx` ridotto da 1.241 a 921 righe senza modificare endpoint, payload, backend o schema dati.
- Suite frontend estesa a 113 test in 29 file; Step 3.5 resta in lavorazione al terzo dei sei domini previsti.

## Step 3.5.2 - Vertical slice prodotti e catalogo

- Query, pagine staff/cliente, lookup, selezione, focus, modifica e comandi prodotto estratti da `App.tsx` in `features/catalog/useCatalogFlow`.
- Nuovo `CatalogExperience` per loading, errore, retry e composizione di catalogo operativo e cliente.
- Client prodotti esteso con `AbortSignal`; ricerca debounced, annullamento richieste obsolete e latest-request-wins applicati alla slice.
- CRUD e disattivazione mantengono command guard, aggiornamento locale e refresh mirato senza retry automatico delle mutazioni.
- `App.tsx` ridotto da 1.421 a 1.241 righe senza modificare endpoint, payload, backend o schema dati.
- Suite frontend estesa a 96 test in 27 file; Step 3.5 resta in lavorazione al secondo dei sei domini previsti.

## Step 3.5.1 - Vertical slice sessione

- Stato e comandi di autenticazione estratti da `App.tsx` in `features/session/useSessionFlow`.
- Pagina di accesso, rinnovo e cambio password composti tramite `SessionEntry` e `SessionOverlays`.
- Login e rinnovo protetti da invii concorrenti; logout locale garantito anche in caso di errore remoto.
- Reset delle altre slice invocato tramite callback esplicita dopo logout e cambio password.
- `App.tsx` ridotto da 1.531 a 1.421 righe senza modificare API, backend o schema dati.
- Suite frontend estesa a 85 test in 25 file; Step 3.5 ancora in lavorazione al primo dei sei domini previsti.

## Step 3.4 - Query state e sessione frontend

- `SessionProvider` centralizzato per identita autenticata, token in memoria, scadenza, rinnovo e gestione dei `401` con sessione attiva.
- Hook riutilizzabile `usePaginatedResource` con stati espliciti, refresh, `AbortController` e protezione latest-request-wins.
- Vista Account migrata come primo dominio pilota, con filtri remoti sottoposti a debounce e feedback di caricamento, aggiornamento ed errore.
- Trasporto HTTP esteso con sottoscrizione alla scadenza sessione e propagazione del segnale di cancellazione.
- Copertura frontend estesa a 78 test in 23 file, inclusi `401`, `403`, `500`, offline, richieste fuori ordine e scadenza imminente.
- Nessun retry automatico aggiunto alle mutazioni con esito potenzialmente ambiguo.

## Step 3.3 - Lifecycle account

- Disabilitazione e riabilitazione non distruttive con motivazione, audit e revoca delle sessioni.
- Cambio password personale con conferma della password corrente e reset amministrativo controllato.
- Revoca esplicita di tutte le sessioni e cambio ruolo con invalidazione dei token precedenti.
- Login basato soltanto su username e password; ruolo e permessi vengono derivati dal backend.
- Account, username, ownership degli ordini e storico audit preservati dopo la disabilitazione.
- Migrazione `V31` con stato account, metadati di disabilitazione, vincolo di coerenza e indice operativo.
- Pannello account aggiornato con filtri di stato e azioni amministrative senza cancellazione fisica.

## Step 3.2 - Projection e dashboard cliente

- Endpoint cliente separati `/api/customer/catalog` e `/api/customer/dashboard`, autorizzati e modellati nel backend senza riutilizzare i DTO operativi.
- Catalogo commerciale privo di ID interni, giacenza, quantita riservata, disponibilita numerica, valore potenziale di vendita e stato di disattivazione; disponibilita espressa soltanto come `AVAILABLE`, `LIMITED` o `UNAVAILABLE`.
- Dashboard cliente limitata ai conteggi per stato e agli ordini recenti appartenenti all'ID account stabile della sessione.
- Endpoint operativi `/api/products` e `/api/dashboard` negati a `CUSTOMER`; prodotti disattivati esclusi dal catalogo commerciale.
- Frontend cliente con tipi, client, pagine e tabella dedicati; nessun limite numerico di stock viene trasferito al carrello.
- Contract test per ogni ruolo, ownership degli ordini e assenza esplicita dei campi interni nelle risposte cliente.

## Step 3.1 - Vendita assistita

- Nuova scheda `Vendita` separata dalla gestione catalogo, disponibile in base al permesso `CREATE_ORDERS`.
- Selezione cliente censito tramite partner ID stabile oppure cliente occasionale esplicito; il self-service resta vincolato all'account autenticato.
- Carrello operativo con correzione quantita, riepilogo cliente, prezzi, totale e pagamento prima della creazione della sola bozza.
- Snapshot persistenti di tipo cliente e descrizione prodotto tramite migrazione `V30`, senza inferire dati mancanti sullo storico.
- Audit dedicato `CREATE_ASSISTED_ORDER`, controllo stock e idempotenza sul payload strutturato.

Tutte le modifiche rilevanti del gestionale web vengono registrate in questo file.

## Non rilasciato

### Aggiunto

- Governance dei nuovi workflow tramite `WORKFLOW_BENCHMARKS.md`, con template decisionale obbligatorio, divieto di copia e tracciabilita verso roadmap, test e validazione operatore.
- Benchmark WB-001 della vendita assistita basato su documentazione ufficiale Odoo, ERPNext e Business Central, con invarianti locali, adattamento minimo e protocollo di accettazione reale per lo Step 3.1.
- Contratto backend statico che impedisce la rimozione dei campi obbligatori, delle fonti indipendenti e delle protezioni di stock, identita cliente e separazione degli stati.
- Gate PostgreSQL obbligatorio con Testcontainers per sessioni, idempotenza, stock, vincoli univoci, numerazioni e autorizzazioni negative, mantenendo H2 come feedback rapido.
- Upgrade popolati V14/V16/V18 fino a V29 con fixture versionate, riconciliazione canonica esplicita, checksum e asserzioni sugli invarianti finali.
- Profilo CI PostgreSQL `fast` sulle pull request e `nightly` settimanale con fixture volumetrica `large`.
- Migrazione `V29` per riparare claim idempotenti legacy `IN_PROGRESS` privi di token/lease e impedirne la ricomparsa tramite vincolo database.
- Scrittura concorrente dei tentativi login serializzata con lock pessimista e retry limitato sulle collisioni di primo inserimento.
- Cronologia dei tentativi login resa monotona anche quando richieste concorrenti terminano fuori ordine.
- Ruoli PostgreSQL separati per owner `NOLOGIN`, migrazioni Flyway, runtime applicativo, backup read-only e restore, con grant futuri controllati tramite default privileges.
- Provisioning e rotazione espliciti dei ruoli database, inizializzazione dei database nuovi e verifica isolata che il runtime non possa eseguire DDL.
- Credenziali e mount secret distinti per responsabilita, porta PostgreSQL prod-like vincolata a loopback e backup/restore privi delle credenziali applicative.
- Migrazione `V27` con collegamento strutturale rimborso-reso, movimento iniziale `RECONCILIATION`, vincoli su tipo, stato e cronologia e nessuna alterazione degli importi verificati.
- Ledger finanziario autorevole per incassi, rimborsi, reversal e riconciliazioni, con proiezioni di pagamento e reso ricalcolate dai movimenti.
- Report read-only `GET /api/financial-reconciliation`, controllo programmato, metriche a cardinalita finita e pannello operativo riservato a `VIEW_REPORTS`.
- Test su drift, rollback a meta operazione, vincoli SQL negativi, rimborsi concorrenti e upgrade V26-V27.
- Migrazione `V26` con metadati di annullamento, reversal finanziario collegato all'ordine, vincoli di coerenza e backfill conservativo degli annullamenti storici.
- Annullamento atomico di bozze e ordini confermati incassati: storno manuale dell'intero netto, rilascio riserve, audit, lock concorrente e replay idempotente.
- Form operativo per riferimento e causale dello storno, storico dell'annullamento e test dedicati su H2, PostgreSQL 16 e frontend.
- Esecutore frontend condiviso per le mutazioni con esiti distinti tra comando salvato, refresh fallito, comando fallito e invio duplicato.
- Aggiornamenti locali immediati dalla risposta autorevole, refresh mirati per dominio e avvisi che impediscono di ripetere comandi gia registrati.
- Protezione dai doppi invii e test di regressione per `POST 200` seguito da `GET 500`, timeout, retry e doppio click.
- Migrazione `V25` e protocollo idempotente durevole con claim atomico, fingerprint account/operazione/payload, lease, replay della risposta, retry recuperabili, retention e verifica concorrente su PostgreSQL.
- Gestione frontend delle chiavi per intento: doppio click e retry dopo timeout o risposta persa mantengono la chiave, mentre un esito definitivo o un payload diverso ne genera una nuova.
- Migrazione `V23` con identita canoniche separate per username, codice prodotto e codice anagrafica, vincoli database `CHECK`/`UNIQUE` e preservazione dei valori visuali.
- Preflight e procedura manuale per collisioni storiche case/space-insensitive, senza rinomina o fusione automatica dei record.
- Traduzione uniforme dei conflitti canonici in `409 RESOURCE_CONFLICT` e test sequenziali/concorrenti su H2 e PostgreSQL 16.
- Migrazione correttiva `V22` per pagamenti storici `UNRECONCILED`, metadati di riconciliazione e contatori documentali inizializzati a `max + 1`.
- Procedura di riconciliazione super admin con ri-autenticazione, idempotenza, audit critico e valori finanziari ignoti non esposti come zero.
- Preflight collisioni, upgrade PostgreSQL V14/V16/V18, primo documento post-upgrade e misure su 5.005 e 50.005 ordini.
- Runner PostgreSQL dedicato alla riconciliazione e alla numerazione documentale concorrente.
- Gate OWASP Dependency-Check backend con soglia CVSS 7, report HTML/JSON e avvio fail-closed verificato in CI.
- Baseline dipendenze aggiornata a Spring Boot 4.1.0, Spring Security 7.1.0, Tomcat 11.0.24, pgJDBC 42.7.12 e Log4j 2.25.5; scansione finale su 66 dipendenze senza vulnerabilita note.
- Test di regressione per baseline Spring, configurazione bootstrap, confinamento H2 e header di sicurezza su risposte 2xx, 4xx e 5xx.
- Runner Docker prod-like ed E2E isolati con project name casuali, run ID su ogni risorsa, inventario diagnostico e cleanup per ID verificati.
- Test runtime con volume sentinella, collisione project name, segnali `HUP`/`INT`/`TERM` e recovery controllato dopo `SIGKILL`.
- Comando di recovery `scripts/ci/cleanup-docker-run.sh` fail-closed su ownership incompleta o mista.
- Fixture storiche incrementali e anonimizzate per gli schemi V14, V16 e V18, con casi mirati per integrita, ownership e riconciliazione.
- Manifest delle anomalie riproducibili, profili di volume `small`, `medium` e `large` e checksum SHA-256 dei dati di test.
- Verificatore PostgreSQL isolato con container effimero, label univoca, nessuna porta host, nessun volume persistente e cleanup confinato.
- Contratto backend per checksum, anonimizzazione, profili e copertura degli stati dei workflow storici.
- Runner prod-like unico con credenziali effimere, build `--pull`, verifica Flyway, sicurezza, osservabilita, Playwright, backup/restore e controllo dei residui Docker.
- Esecuzione prod-like settimanale in GitHub Actions con timeout e artefatto diagnostico pubblicato anche in caso di errore.
- Gate prod-like riattivato come dipendenza obbligatoria del quality gate CI dopo la chiusura di F-03.
- ADR e proposta architetturale multi-tenant con distinzione tra tenant, azienda legale, sede e magazzino, isolamento RLS, modello ibrido pooled/dedicato e migrazione expand/contract.
- ADR e proposta architetturale separata per privacy, retention, legal hold, conservazione elettronica e fatturazione elettronica tramite provider, con workflow, rollback e gate professionali.
- ADR e proposta architetturale per lifecycle cliente, branding sicuro, provisioning pooled/dedicato e flotta release con artefatti immutabili, coorti ed expand/contract.
- Governance documentale con roadmap, architettura, modello dominio, sicurezza, testing, deployment e registro rischi.
- Backend Spring Boot con PostgreSQL, Flyway, profili `dev`, `test` e `prod`.
- Contratto errori API stabile con codici applicativi e `requestId`.
- Sessioni persistenti con token hashati, logout e cleanup sicurezza.
- Protezione login con lockout persistente dopo tentativi falliti.
- Ruoli e permessi granulari per catalogo, magazzino, ordini, documenti, account, audit e anagrafiche.
- Audit log con origine richiesta, tipo entita e correlazione tramite `X-Request-Id`.
- Paginazione e filtri server-side per catalogo, ordini, movimenti, audit e anagrafiche.
- Workflow ordini con bozza, conferma, evasione e annullamento.
- Stock riservato e disponibilita vendibile.
- Documenti simulati con fattura simulata, nota credito simulata e snapshot cliente.
- Anagrafiche clienti e fornitori.
- Idempotenza per operazioni critiche tramite `Idempotency-Key`.
- Bootstrap controllato del primo super admin.
- Template `.env.example` e documentazione configurazione ambiente.
- Checklist release.
- Workflow GitHub Actions con hygiene repository, backend verify, frontend build e quality gate.
- Documentazione CI/CD.
- Workflow sicurezza con dependency review, audit npm, inventory Maven e CodeQL.
- Configurazione Dependabot per Maven, npm e GitHub Actions.
- Documentazione delle scansioni sicurezza.
- Dockerfile backend e frontend con build multi-stage.
- Stack Docker prod-like con PostgreSQL, backend, frontend Nginx e health check.
- Documentazione Docker prod-like.
- Script backup e restore PostgreSQL con verifica automatica su database isolato.
- Documentazione dedicata a backup, restore e accesso PostgreSQL.
- Endpoint protetto `/api/system/status` con stato applicazione, database, runtime, sessioni, audit ed errori API recenti.
- Vista frontend Monitoraggio per super admin con riepilogo operativo e anomalie recenti.
- Stato prodotto `discontinued` con endpoint di disattivazione e indicazione frontend nel catalogo.
- Password policy backend centralizzata e applicata a registrazione pubblica e creazione account amministrativa.
- Vincolo database sui documenti simulati per impedire fatture o note credito duplicate sullo stesso ordine.
- Vincoli database essenziali su campi business critici: quantita, prezzi, sconti, totali, aliquote e campi obbligatori.
- Endpoint `/api/documents` paginato con filtri `q` e `type`.
- Endpoint `/api/accounts` paginato con filtri `q` e `role`.
- Query repository dedicata per scorte basse basate sulla disponibilita vendibile.
- Endpoint `/api/dashboard` con statistiche aggregate, ultimi ordini e movimenti recenti filtrati per permessi.
- Endpoint `/api/products/lookup` con dati catalogo leggeri per filtri e select del frontend.
- Clock applicativo UTC centralizzato tramite `TimeProvider`.
- Pagine React separate per autenticazione, dashboard, catalogo, anagrafiche, magazzino, ordini, documenti, account, audit e monitoraggio.
- Shell workspace e navigazione a schede isolate in componenti e hook dedicati.
- Client API React separati per catalogo, anagrafiche, account, magazzino, ordini, documenti, audit, dashboard e monitoraggio.
- Suite frontend con Vitest, jsdom e React Testing Library per componenti, trasporto HTTP e flussi API critici.
- Suite Playwright Chromium con smoke test browser sullo stack Docker reale e script locale isolato.
- Modello pagamento ordine strutturato con metodo controllato, stato, importi, valuta, timestamp e relazione uno-a-uno persistita.
- Migrazione Flyway `V15` con backfill dei metodi pagamento storici e vincoli database sugli importi e sugli stati.
- Verifica Spring Modulith sui moduli business `user`, `product`, `partner`, `inventory`, `order` e `document`.
- Porta `ProductOrderUsage` per rimuovere la dipendenza del catalogo dal repository ordini.
- Build Maven web-first dalla root e build Swing separata tramite `pom-legacy.xml`.
- Ledger immutabile `payment_transactions` per incassi e rimborsi con codici sequenziali, operatore, causale e riferimento.
- Workflow resi con richiesta, approvazione, rifiuto, ricezione, rimborso parziale/completo e reintegro magazzino.
- Migrazione Flyway `V16` con tabelle resi, righe reso, movimenti finanziari e vincoli sui saldi.
- Scheda operativa ordini frontend con saldi, storico movimenti, incassi e gestione resi.
- Modulo `company` con configurazione aziendale centralizzata, aliquota IVA predefinita e controllo di versione.
- Migrazione Flyway `V17` con configurazione aziendale, contatori documentali annuali e snapshot dell'emittente.
- Numerazioni documentali atomiche distinte per tipo ed esercizio, con prefissi e padding configurabili prima dell'utilizzo.
- Pagina frontend amministrativa per identita aziendale, contatti, IVA e anteprima numerazioni.
- Modulo `reporting` con report vendite e magazzino filtrati lato server tramite porte read-only dei domini ordini e prodotti.
- Export CSV UTF-8, workbook Excel `.xlsx` e PDF landscape con limite esplicito di 10.000 righe.
- Pagina React Report con metriche, dettagli, top prodotti, filtri ed export responsive.
- Manuale utente operativo con matrice permessi, procedure per ruolo, workflow ordini/pagamenti/resi, gestione errori e limiti dichiarati.
- Rate limiting Nginx per IP sull'endpoint login, con soglie configurabili, risposta JSON `429`, `Retry-After` e correlation ID.
- Script `verify-login-rate-limit.sh` integrato negli smoke test e nella CI con verifica degli header e dei log Nginx.
- Rate limiting Nginx indipendente per la registrazione pubblica, con variabili dedicate e script `verify-registration-rate-limit.sh`.
- Test di regressione backend che rifiutano ruoli operativi nella registrazione anonima e verificano audit `SELF_SERVICE`.
- Migrazione Flyway `V18` con ultima attivita persistita per le sessioni esistenti.
- Migrazione Flyway `V19` con provenienza account, stato di verifica operativa, classificazione conservativa dei record storici e revoca delle sessioni degli account operativi non verificati.
- Migrazione Flyway `V20` con subject di sessione basato su ID account, versione credenziali, revoca conservativa delle sessioni legacy e foreign key `ON DELETE CASCADE`.
- Migrazione Flyway `V21` con ownership ordini basata su account e anagrafica tramite ID stabili, stato `UNRESOLVED` per lo storico ambiguo e migrazione deterministica solo per codice anagrafica esatto.
- Collegamento amministrativo univoco tra anagrafica cliente e account `CUSTOMER`, con endpoint, controlli permessi e interfaccia dedicata.
- Runner PostgreSQL isolato e test di regressione per omonimie, rename, account non collegati, display name duplicati e transizioni cliente.
- Report di sicurezza account riservato al super admin con classificazione, sessioni ed evidenze audit per area sensibile.
- Workflow di verifica manuale degli account operativi con ri-autenticazione e obbligo di nuovo login dopo la revisione.
- Rinnovo sessione autenticato con rotazione atomica del token e revoca immediata del precedente.
- Revoca globale dei token dopo cambio password, cambio ruolo o eliminazione account, con protezione delle race tra logout, rotazione e aggiornamento attivita.
- Liste, dettaglio e transizioni ordine cliente autorizzati tramite ID account; username e nome cliente restano esclusivamente snapshot descrittivi.
- Confine transazionale delle sessioni reso scrivibile per supportare correttamente i lock pessimisti anche su PostgreSQL.
- Timeout assoluto e di inattivita configurabili, con aggiornamento periodico dell'ultima attivita.
- Metriche amministrative delle sessioni allineate al timeout di inattivita.
- ADR 0004 sulla scelta tra header token e cookie HttpOnly/CSRF per il deployment browser-first.
- CSP Nginx in enforcement senza `unsafe-inline` o `unsafe-eval`, con script, stili, connessioni e risorse limitati alla stessa origine.
- Header browser uniformi per framing, isolamento origine, referrer, MIME sniffing e funzionalita sensibili.
- Script `verify-browser-security.sh` integrato nello smoke locale e nella CI per validare CSP, API proxy e caching.
- Probe Actuator separate per liveness e readiness, con readiness collegata allo stato del database.
- Script `verify-actuator-exposure.sh` e controlli CI per impedire l'esposizione Actuator sulla porta API.
- Errore API strutturato `404 RESOURCE_NOT_FOUND` per rotte inesistenti, senza falsa segnalazione di errore interno.
- Resolver fail-closed per secret diretti o file-based, immagini backend/PostgreSQL dedicate e override Compose con mount read-only.
- Verifica automatica dell'assenza di password dirette nella configurazione container e scansione Gitleaks della storia Git.
- Procedura operativa per rotazione, rollback e rimozione del secret bootstrap.
- Runtime container non-root per PostgreSQL, backend Java e frontend Nginx su porta non privilegiata.
- Root filesystem read-only, `no-new-privileges`, capability Linux eliminate e scritture confinate a tmpfs e volume PostgreSQL.
- Verifica automatica `verify-container-hardening.sh` integrata nello smoke test e nella CI.
- Backup PostgreSQL atomici con checksum SHA-256, lock anti-concorrenza, retention e controllo di freschezza.
- Timer systemd persistenti per backup giornaliero e restore drill settimanale dell'ultimo backup reale.
- Verifiche automatiche del lifecycle backup e della configurazione dei timer integrate nella CI.
- Log backend JSON in produzione con servizio, ambiente, correlation ID, metodo, percorso, stato e durata.
- Metriche Prometheus JVM, HTTP, datasource e contatori applicativi per autenticazione, sessioni ed errori API.
- Servizio Prometheus prod-like opzionale con retention configurabile, hardening non-root e accesso host limitato a loopback.
- Regole alert per indisponibilita backend, frequenza 5xx, login anomali, pressione heap e saturazione pool database.
- Script di validazione Prometheus, verifica runtime di log/metriche/alert e controllo hardening integrati nella CI.
- Runbook operativo `docs/OBSERVABILITY.md`.

### Modificato

- Form, carrello e dati di input delle operazioni vengono azzerati soltanto dopo una risposta confermata; gli esiti ambigui preservano la bozza per un retry sicuro.
- Migrazione progressiva dal gestionale Swing verso una web app modulare.
- Separazione tra profili locali e produzione.
- Rimozione di dati operativi derivabili dal client nei payload critici.
- Documentazione di deployment aggiornata con bootstrap super admin e variabili ambiente.
- Test backend resi piu riproducibili configurando Mockito tramite Byte Buddy agent in Maven Surefire.
- Ciclo di vita prodotti reso piu sicuro: codice bloccato dopo uso in ordini, cancellazione impedita con stock riservato o ordini collegati, prodotti disattivati non acquistabili.
- Duplicati documentali tradotti in `409 RESOURCE_CONFLICT` con errore API stabile.
- Password locale di bootstrap sviluppo/test aggiornata nello storico e successivamente rimossa dalla configurazione distribuita.
- Modello dati rafforzato con check constraint su prodotti, ordini, righe ordine, movimenti magazzino, documenti simulati, righe documento e anagrafiche.
- Vista frontend Documenti aggiornata con ricerca, filtro tipo e controlli di paginazione.
- Vista frontend Account aggiornata con ricerca, filtro ruolo e controlli di paginazione.
- Dashboard frontend aggiornata per leggere il conteggio scorte basse da query server-side.
- Dashboard frontend collegata alla risposta aggregata backend.
- Caricamento iniziale frontend alleggerito: usa dashboard aggregata, lookup prodotti e pagine correnti invece di scaricare tutte le liste operative complete.
- Mapping JPA di ordini e documenti resi piu efficienti: righe collegate lazy con batch loading e back-reference `ManyToOne` lazy.
- Timestamp backend standardizzati: ordini, documenti, movimenti, audit, idempotenza, anagrafiche, sessioni, errori API e monitoraggio usano la sorgente temporale applicativa UTC.
- Frontend rifattorizzato: `App.tsx` mantiene orchestrazione e workflow, mentre pagine, layout, componenti condivisi, tipi UI e formattatori sono moduli separati.
- Trasporto HTTP frontend centralizzato con gestione condivisa di token, errori, request id, paginazione completa e chiavi di idempotenza.
- Layout frontend reso fluido senza larghezza minima globale, con breakpoint progressivi per notebook, tablet e viewport mobili e tabelle confinate in contenitori scrollabili.
- Dipendenze frontend fissate a versioni esplicite e strumenti di build spostati tra le dev dependency.
- CI estesa con build e verifica dello stack Docker prod-like.
- CI estesa con verifica backup/restore PostgreSQL.
- CI frontend estesa con esecuzione di `npm test` prima della build.
- CI prod-like estesa con typecheck E2E, smoke test browser e artefatti diagnostici Playwright.
- Script E2E reso riproducibile con ricreazione del solo volume PostgreSQL dedicato e avvio sequenziale di backend e frontend per preservare i log di errore.
- Script E2E corretto per ricostruire sempre anche l'immagine frontend prima degli smoke test.
- Login frontend corretto per mostrare l'errore di autenticazione senza aprire il rinnovo sessione quando non esiste ancora un utente autenticato.
- Checkout frontend aggiornato con scelta tra carta, bonifico bancario e contanti; la vista ordini mostra separatamente metodo e stato pagamento.
- API ordini arricchita con dettaglio pagamento, mantenendo il campo testuale storico come snapshot compatibile.
- Documentazione principale riscritta in ottica web-first con confini modulari e strategia di dismissione Swing espliciti.
- Documenti simulati aggiornati per usare IVA configurata e conservare snapshot immutabili di emittente, cliente e aliquota.
- Backend dello stack prod-like pubblicato solo su loopback per impedire il bypass remoto del reverse proxy.
- Immagine frontend aggiornata con template Nginx runtime e validazione della configurazione durante la build.
- Nginx reso autoritativo sugli header browser, eliminando valori duplicati provenienti dal backend.
- Cache frontend differenziata: shell HTML `no-store` e asset Vite versionati `immutable`.
- Campi credenziali frontend allineati agli scopi `username`, `current-password` e `new-password`.
- Actuator produzione spostato sulla porta management interna `9090`, limitato a `health` e configurato senza dettagli operativi.
- Nginx frontend spostato internamente sulla porta non privilegiata `8080` con entrypoint runtime non-root e validazione fail-closed delle soglie login.
- Registrazione pubblica ridotta al solo contratto `username` e `password`; il backend assegna sempre `CUSTOMER` e il frontend non mostra piu il selettore ruolo.
- Validazione sessione resa fail-closed per gli account operativi in quarantena, preservando la revoca anche quando la richiesta termina con `401`.

### Sicurezza

- Password hashate con BCrypt.
- Registrazione pubblica limitata esclusivamente al ruolo `CUSTOMER`, senza ruolo controllabile dal client.
- Creazione di account staff confinata all'endpoint amministrativo autenticato con ri-autenticazione.
- Creazione admin riservata al super admin.
- Blocco autocancellazione account corrente.
- Blocco credenziali locali di default nel bootstrap produzione.
- Validazione password forte per bootstrap produzione.
- Validazione password forte applicata lato backend alla creazione degli account.
- Vincolo database a protezione da duplicati concorrenti nei documenti simulati.
- Scadenze sessione, lockout login, errori API e audit generati tramite clock applicativo centralizzato.
- Login e rinnovo sessione protetti da header `no-store`/`no-cache`; token sovradimensionati rifiutati prima dell'hashing.
- Metodi e stati pagamento limitati da enum applicative e check constraint database; annullamento consentito direttamente solo per pagamenti pendenti.
- Permessi distinti `RECORD_PAYMENTS`, `REFUND_PAYMENTS`, `REQUEST_RETURNS` e `MANAGE_RETURNS`; endpoint finanziari e resi protetti, idempotenti e auditati.
- Permesso `MANAGE_COMPANY_SETTINGS` riservato al super admin; aggiornamenti configurazione auditati e protetti da versione ottimistica.
- Permesso `VIEW_REPORTS`, audit dedicato degli export, protezione CSV da Formula Injection e header download `no-store`/`nosniff`.
- Difesa login a due livelli: limite per IP sul reverse proxy e lockout persistente per username nel backend.
- Framing, plugin, script e stili inline bloccati dalla CSP; permessi browser non necessari disabilitati.
- Endpoint Actuator generici, informativi e metrici negati anche agli utenti autenticati; soltanto liveness e readiness sono pubbliche sul management plane interno.
- Dipendenza transitiva PostCSS aggiornata a `8.5.23` per correggere l'advisory sulla lettura delle source map.
- Account operativi storici self-service o di provenienza non dimostrabile messi in quarantena senza cancellazione o declassamento automatico.

### Verifiche

- Test backend attuali: 178, verificati localmente con Java 23 tramite `mvn -B test` in `web/backend`.
- Fixture V14, V16, V18, V19, V20 e V21 verificate su PostgreSQL 16 pulito con il profilo `small`; checksum, asserzioni e cleanup completati senza risorse residue.
- Test PostgreSQL 16 mirati sulla concorrenza delle sessioni: 6 passati, con runner isolato e cleanup senza risorse residue.
- Test Swing legacy attuali: 41, verificati tramite `mvn -f pom-legacy.xml test`.
- Test frontend attuali: 32 in 11 file, verificati tramite `npm test`.
- Smoke test browser attuali: 3, verificati con Chromium contro PostgreSQL, Spring Boot e Nginx/React reali, inclusa l'assenza di violazioni CSP.
- Build frontend verificata con `npm run build`.
- Workflow locale equivalente alla CI verificato su backend e frontend.
- Audit npm frontend verificato senza vulnerabilita note.
- Backend e frontend verificati localmente nello stack prod-like con Actuator isolato, probe interne, CSP, rate limiting e flussi browser completi.
- Runner prod-like ricorrente verificato integralmente con 18 migrazioni Flyway, Prometheus, 3 smoke Playwright, backup/restore e cleanup Docker senza risorse residue.
- PostgreSQL, backend e frontend verificati con UID non-root, root filesystem read-only, `no-new-privileges`, capability eliminate e mount scrivibili confinati.
- Backup e restore PostgreSQL verificati su database isolato.
- Checksum corrotto, lock concorrente, retention, freschezza e unita systemd verificati con test shell dedicati.
- Audit npm verificato con zero vulnerabilita dopo l'aggiornamento PostCSS.
- Monitoraggio sistema verificato con test autorizzativi backend.
