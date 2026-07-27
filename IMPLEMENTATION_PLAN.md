# Implementation Plan

Legenda:

- `[ ]` non iniziato
- `[~]` in lavorazione
- `[x]` completato
- `[!]` bloccato

## Contesto verificato

- `[x]` Root progetto verificata: presenti `AGENTS.md`, `README.md`, `pom.xml`, `docker-compose.yml`, `src/`, `web/`.
- `[x]` Presente `ANALISI_COMPLETA_GESTIONALE.md` nella root, allo stesso livello di `AGENTS.md` e `README.md`.
- `[x]` Repository Git verificato.
- `[x]` Branch di lavoro creata: `miglioramenti-gestionale`.
- `[x]` Rilevate modifiche preesistenti non committate: non devono essere sovrascritte o revertite.

## Obiettivo

Portare la web app da MVP incompleto a MVP utilizzabile, stabile e predisposto alla produzione, trattando la parte Swing in root come legacy e lavorando principalmente in `web/backend` e `web/frontend`.

## Priorita e ordine attivita

### Fase 1 - Problemi P1

1. `[x]` Rendere riproducibili i test backend.
   - Problema: `mvn test` deve funzionare in modo documentato anche quando la JVM locale e piu recente di Java 17.
   - File coinvolti: `web/backend/pom.xml`, `.github/workflows/ci.yml`, `web/backend/Dockerfile`, `docs/TESTING.md`, `docs/CI_CD.md`, `docs/CHANGELOG.md`.
   - Dipendenze: Maven Surefire, Mockito, Byte Buddy agent, Java 17/23.
   - Test richiesti: `cd web/backend && mvn test`.
   - Criteri di accettazione: test backend eseguibili senza argomenti manuali extra; CI coerente; documentazione aggiornata.
   - Rischi: configurazione `argLine` fragile; differenze fra Java locale e CI.
   - Note verifica: Java locale `23.0.1`, Maven `3.9.16`; `mvn test` completato senza argomenti manuali extra dopo configurazione Byte Buddy agent in Surefire. Verifica complessiva aggiornata: 71 test backend passati.

2. `[x]` Bloccare modifica o cancellazione rischiosa dei prodotti.
   - Problema: prodotto oggi rinominabile/cancellabile anche se presente in ordini o con stock riservato.
   - File coinvolti: `Product`, `ProductService`, `ProductRepository`, `ProductResponse`, `ProductMapper`, `ProductController`, `OrderItem`, `CustomerOrderRepository`, frontend catalogo.
   - Dipendenze: workflow ordini, stock riservato, storico righe ordine.
   - Test richiesti: prodotto senza riferimenti modificabile; prodotto usato non rinominabile; prodotto con stock riservato non eliminabile; prodotto in ordine confermato non eliminabile; ordine confermato ancora evadibile.
   - Criteri di accettazione: storico ordini preservato; nessun ordine confermato diventa non evadibile; frontend mostra errore coerente.
   - Rischi: delete fisico attuale resta consentito solo per prodotti senza riferimenti; i prodotti usati devono essere disattivati.
   - Note verifica: aggiunti controlli su codice prodotto, stock riservato e ordini collegati; `mvn test` completato con 60 test passati; `npm run build` completato.

3. `[x]` Introdurre disattivazione prodotto.
   - Problema: serve alternativa professionale al delete fisico.
   - File coinvolti: nuova migrazione Flyway, `Product`, repository, service, controller, DTO, frontend.
   - Dipendenze: step 2.
   - Test richiesti: disattivazione prodotto usato; prodotto disattivato non acquistabile; prodotto disattivato visibile nello storico.
   - Criteri di accettazione: soft delete/stato `discontinued`; nessuna perdita storico.
   - Rischi: i filtri stock distinguono ancora la disponibilita vendibile, mentre lo stato disattivato viene esposto come attributo separato.
   - Note verifica: aggiunta migrazione `V12__product_discontinued.sql`, endpoint `POST /api/products/{code}/discontinue`, azione frontend e test di evasione ordine storico.

4. `[x]` Applicare password policy backend.
   - Problema: indicatore robustezza esiste, ma la policy non blocca sempre la creazione account.
   - File coinvolti: `PasswordStrengthService`, `UserService`, `SeedAdminInitializer`, `UserController`, test utente.
   - Dipendenze: regole bootstrap e ruoli admin/super admin.
   - Test richiesti: password vuota, corta, debole, simile allo username se previsto, valida, registrazione pubblica, creazione amministrativa.
   - Criteri di accettazione: policy centralizzata; nessun account creato con password debole; messaggi chiari.
   - Rischi: i valori locali restano solo per sviluppo/test e non devono essere riutilizzati in produzione.
   - Note verifica: policy centralizzata in `PasswordStrengthService`, applicata in `UserService` per registrazione pubblica e creazione account amministrativa; aggiunti test su password vuota, corta, debole, simile allo username, valida e creazione account admin; password locale di bootstrap aggiornata a `RootSecure123!`.

5. `[x]` Impedire documenti fiscali simulati duplicati a livello database.
   - Problema: il controllo applicativo non basta contro richieste concorrenti.
   - File coinvolti: nuova migrazione Flyway, `FiscalDocument`, `FiscalDocumentRepository`, `FiscalDocumentService`, `GlobalExceptionHandler`, test documenti/idempotenza.
   - Dipendenze: schema documenti e idempotenza.
   - Test richiesti: una sola fattura per ordine; una sola nota credito; replay stessa idempotency key; richiesta concorrente; violazione vincolo tradotta in `409`.
   - Criteri di accettazione: unique constraint su ordine collegato e tipo documento; niente documenti parziali.
   - Rischi: H2/PostgreSQL possono differire nei messaggi di violazione vincolo; il client riceve comunque un codice API stabile.
   - Note verifica: aggiunta migrazione `V13__fiscal_document_order_type_unique.sql`, unique constraint su ordine collegato e tipo documento, gestione `409 RESOURCE_CONFLICT`, test su doppia fattura, doppia nota credito, vincolo database e duplicato via API; `mvn test` completato con 71 test passati; `npm run build` completato.

### Fase 2 - Database, API e prestazioni

6. `[x]` Aggiungere vincoli database essenziali su prezzi, quantita, sconti, stock e campi obbligatori.
   - Problema: molte regole numeriche erano garantite dal codice applicativo, ma non tutte erano protette a livello database.
   - File coinvolti: `V14__business_data_constraints.sql`, `DatabaseBusinessConstraintTest`.
   - Dipendenze: schema prodotti, ordini, righe ordine, movimenti magazzino, documenti simulati, anagrafiche e account.
   - Test richiesti: vincoli su quantita, prezzi, sconti, totali, aliquote e campi testuali obbligatori.
   - Criteri di accettazione: database pulito migra fino a `V14`; stati impossibili rifiutati dal database; errori API mappati come conflitti dati quando emergono dai controller.
   - Rischi: database esistenti con dati sporchi potrebbero richiedere pulizia prima della migrazione.
   - Note verifica: aggiunti check constraint su campi business critici; `mvn test` completato con 78 test passati.
7. `[x]` Paginare `/api/documents`.
   - Problema: la lista documenti caricava tutti i record, poco scalabile per un gestionale reale.
   - File coinvolti: `FiscalDocumentController`, `FiscalDocumentService`, `FiscalDocumentRepository`, `FiscalDocumentPaginationControllerTest`, `web/frontend/src/api/products.ts`, `web/frontend/src/App.tsx`.
   - Dipendenze: `PageResponse`, `PageRequests`, permesso `MANAGE_DOCUMENTS`, vista frontend Documenti.
   - Test richiesti: endpoint con `page`, `size`, `q`, `type`; frontend compilabile.
   - Criteri di accettazione: `/api/documents` restituisce `PageResponse`, supporta ricerca e filtro tipo, frontend usa paginazione e non assume piu una lista completa nella vista.
   - Rischi: client esterni che si aspettavano un array devono adattarsi al formato paginato.
   - Note verifica: aggiunto test API su paginazione documenti; `mvn test` completato con 79 test passati; `npm run build` completato.
8. `[x]` Paginare `/api/accounts`.
   - Problema: la lista account caricava tutti gli utenti in un'unica risposta, poco scalabile e non coerente con le altre viste operative.
   - File coinvolti: `UserController`, `UserService`, `UserAccountRepository`, `UserAccountPaginationControllerTest`, `web/frontend/src/api/products.ts`, `web/frontend/src/App.tsx`, `web/frontend/src/styles.css`.
   - Dipendenze: `PageResponse`, `PageRequests`, permesso `MANAGE_ACCOUNTS`, vista frontend Account.
   - Test richiesti: endpoint con `page`, `size`, `q`, `role`; frontend compilabile.
   - Criteri di accettazione: `/api/accounts` restituisce `PageResponse`, supporta ricerca username e filtro ruolo, frontend usa paginazione e mantiene le protezioni su eliminazione account.
   - Rischi: client esterni che si aspettavano un array devono adattarsi al formato paginato.
   - Note verifica: aggiunto test API su paginazione account; `mvn test` completato con 80 test passati; `npm run build` completato.
9. `[x]` Ottimizzare low-stock con query repository.
   - Problema: l'elenco scorte basse veniva calcolato caricando tutti i prodotti e filtrandoli in memoria.
   - File coinvolti: `ProductRepository`, `ProductService`, `InventoryService`, `InventoryServiceTest`, `web/frontend/src/api/products.ts`, `web/frontend/src/App.tsx`.
   - Dipendenze: disponibilita vendibile `quantity - reservedQuantity`, soglia scorte basse, filtro stock `LOW`.
   - Test richiesti: prodotti con disponibilita bassa, prodotti esauriti, prodotti disponibili e prodotti con stock riservato.
   - Criteri di accettazione: il backend usa query repository per scorte basse; i prodotti esauriti non sono conteggiati come scorte basse; la dashboard usa un conteggio server-side.
   - Rischi: la semantica e piu precisa rispetto al vecchio filtro `<= 3`, quindi i prodotti a disponibilita zero sono ora separati dagli articoli a scorta bassa.
   - Note verifica: aggiunto test su disponibilita vendibile e scorte basse; `mvn test` completato con 81 test passati; `npm run build` completato.
10. `[x]` Creare endpoint dashboard aggregato.
   - Problema: la dashboard componeva numeri e preview partendo da liste complete caricate dal frontend.
   - File coinvolti: `DashboardController`, `DashboardService`, `DashboardResponse`, `ProductRepository`, `ProductService`, `ProductDashboardSummary`, `OrderService`, `CustomerOrderRepository`, `InventoryService`, `DashboardControllerTest`, `web/frontend/src/api/products.ts`, `web/frontend/src/App.tsx`.
   - Dipendenze: sessione autenticata, permessi ruolo, prodotti, ordini e movimenti magazzino.
   - Test richiesti: endpoint protetto, dashboard super admin con movimenti recenti, dashboard cliente limitata ai propri ordini e senza movimenti magazzino.
   - Criteri di accettazione: `GET /api/dashboard` restituisce statistiche aggregate e preview recenti in una singola risposta; i clienti vedono solo i propri ordini; il frontend usa la risposta aggregata nella dashboard.
   - Rischi: alcune liste complete restano caricate per le altre schermate fino allo step successivo di riduzione caricamenti.
   - Note verifica: aggiunto test API dashboard; `mvn test` completato con 84 test passati; `npm run build` completato.
11. `[x]` Ridurre caricamenti completi dal frontend.
   - Problema: il frontend caricava liste complete di prodotti, ordini, movimenti, documenti, account e audit anche quando usava gia viste paginabili.
   - File coinvolti: `ProductController`, `ProductService`, `ProductLookupResponse`, `ProductLookupControllerTest`, `web/frontend/src/api/products.ts`, `web/frontend/src/App.tsx`, `web/frontend/src/components/ProductTable.tsx`.
   - Dipendenze: endpoint paginati esistenti, dashboard aggregata, select prodotto per magazzino e filtri catalogo.
   - Test richiesti: lookup protetto, lookup leggero senza campi pesanti, build frontend.
   - Criteri di accettazione: il caricamento workspace usa dashboard, lookup leggero prodotti e pagine correnti; non scarica piu tutte le pagine operative per inizializzare l'app; i filtri catalogo e le select prodotto restano utilizzabili.
   - Rischi: il pannello dettaglio prodotto mostra movimenti e ordini collegati limitati ai dati paginati caricati nella vista corrente; per una vista storica completa servira un endpoint dettaglio prodotto dedicato.
   - Note verifica: aggiunto `GET /api/products/lookup`; rimossi dal caricamento principale frontend i fetch completi di prodotti, partner, ordini, movimenti, documenti, account e audit; `mvn test` completato con 86 test passati; `npm run build` completato.
12. `[x]` Analizzare e correggere fetch `EAGER` rischiosi.
   - Problema: ordini e documenti caricavano sempre le righe collegate tramite `FetchType.EAGER`; le relazioni inverse `ManyToOne` usavano il default eager di JPA.
   - File coinvolti: `CustomerOrder`, `OrderItem`, `FiscalDocument`, `FiscalDocumentLine`, `JpaFetchStrategyTest`.
   - Dipendenze: DTO ordini/documenti, transazioni service, paginazione API.
   - Test richiesti: mapping `OneToMany` lazy e batch, mapping `ManyToOne` lazy, regressione su workflow ordini/documenti.
   - Criteri di accettazione: niente collezioni sempre eager su liste operative; righe caricate solo quando il DTO o la transizione le richiede; test architetturale contro regressioni future.
   - Rischi: le liste continuano a includere righe nei DTO, quindi su dataset molto grandi sara utile introdurre in futuro DTO summary senza righe per viste tabellari.
   - Note verifica: `items` e `lines` convertiti a `LAZY` con `@BatchSize(50)`; back-reference `OrderItem.order` e `FiscalDocumentLine.document` convertite a `LAZY`; `mvn test` completato con 88 test passati; `npm run build` completato.
13. `[x]` Standardizzare date e timezone.
   - Problema: timestamp generati con `LocalDateTime.now()` e `Instant.now()` sparsi tra entity e servizi rendevano il comportamento dipendente dalla timezone della JVM e poco testabile.
   - File coinvolti: `TimeConfiguration`, `TimeProvider`, servizi ordini, documenti, magazzino, audit, idempotenza, anagrafiche, sessioni, metriche sicurezza, errori API e monitoraggio sistema.
   - Dipendenze: Spring context, JPA entity operative, sessioni, audit, idempotenza, frontend che serializza date esistenti.
   - Test richiesti: clock UTC fisso, timestamp ordine/documento/movimento/anagrafica/audit/sessione coerenti, regressione workflow ordini e documenti.
   - Criteri di accettazione: nessuna chiamata diretta a `now()` nel codice backend produttivo; sorgente temporale centralizzata e sovrascrivibile nei test; nessuna modifica breaking al contratto API.
   - Rischi: le colonne restano `timestamp` senza timezone per compatibilita dello schema; il backend interpreta e genera i valori in UTC tramite clock applicativo.
   - Note verifica: introdotto `Clock.systemUTC()` centralizzato e `TimeProvider`; entity operative ricevono timestamp dai servizi; errori API e sessioni usano il clock applicativo; `ApplicationTimeProviderIntegrationTest` verifica clock fisso UTC su ordine, documento, movimento, anagrafica, audit e sessione; `mvn test` completato con 89 test passati.

### Fase 3 - Frontend

14. `[x]` Dividere `App.tsx` in pagine, componenti e hook.
   - Problema: autenticazione, layout, navigazione e tutte le viste operative erano concentrate in un singolo componente da 1.348 righe.
   - File coinvolti: `web/frontend/src/App.tsx`, `pages/`, `components/common/`, `components/layout/`, `components/auth/`, `components/catalog/`, `components/orders/`, `hooks/`, `types/ui.ts`, `utils/formatters.ts`.
   - Dipendenze: contratti API esistenti, permessi utente, stato delle schede e workflow sessione.
   - Test richiesti: typecheck TypeScript, build Vite e regressione test backend.
   - Criteri di accettazione: markup delle pagine fuori da `App.tsx`; navigazione a schede isolata; componenti comuni riutilizzabili; nessun cambiamento ai flussi operativi.
   - Rischi: lo stato applicativo resta orchestrato centralmente fino alla successiva separazione dei client API e degli hook di dominio.
   - Note verifica: estratte dieci pagine, shell workspace, modale sessione, componenti catalogo/carrello/comuni, hook password e navigazione, tipi UI e formattatori; `App.tsx` ridotto da 1.348 a 867 righe.
15. `[x]` Separare API client per dominio mantenendo gestione condivisa di token, errori e request id.
   - Problema: `api/products.ts` concentrava in 631 righe tipi, trasporto HTTP e client di nove domini, aumentando accoppiamento e rischio di duplicare logica trasversale.
   - File coinvolti: `web/frontend/src/api/types.ts`, `httpClient.ts`, client di prodotto, partner, account, inventario, ordini, documenti, audit, dashboard e sistema, `api/index.ts` e import dei componenti frontend.
   - Dipendenze: contratti REST esistenti, token di sessione, `X-Request-Id`, `X-Reauth-Password` e `Idempotency-Key`.
   - Test richiesti: typecheck TypeScript, build Vite, ricerca import legacy e regressione backend.
   - Criteri di accettazione: un solo trasporto HTTP condiviso; client separati per dominio; nessuna modifica a endpoint, payload o header; nessun import dal vecchio modulo monolitico.
   - Rischi: il token resta mantenuto in memoria nel browser fino al successivo hardening delle sessioni; i client condividono intenzionalmente il barrel pubblico `api/index.ts`.
   - Note verifica: rimosso `api/products.ts`; creati nove client di dominio, un modulo tipi e un trasporto condiviso; nessun import legacy residuo; typecheck e build Vite completati; regressione backend completata con 89 test passati.
16. `[x]` Rimuovere dipendenza rigida da `min-width: 1180px`.
   - Problema: il `body` imponeva una larghezza minima di 1.180 px e la sola media query disponibile impilava indiscriminatamente quasi tutte le griglie, causando scroll orizzontale o layout eccessivamente verticali su notebook e tablet.
   - File coinvolti: `web/frontend/src/styles.css`.
   - Dipendenze: shell workspace, menu orizzontale, schede, KPI, pannelli, filtri, form, tabelle e schermata autenticazione.
   - Test richiesti: typecheck, build Vite, controllo overflow DOM, verifica visiva desktop, tablet landscape e viewport compatto.
   - Criteri di accettazione: nessuna larghezza minima globale; griglie progressive 4/2/1; tabelle scrollabili solo nel proprio contenitore; filtri e form leggibili; nessun overflow orizzontale della pagina.
   - Rischi: le tabelle operative conservano intenzionalmente una larghezza minima interna per non comprimere dati e azioni; lo smoke test autenticato completo resta assegnato allo step 18.
   - Note verifica: breakpoint progressivi a 1.280, 1.080, 860 e 640 px; controllati viewport 1.440x900, 1.024x768, 640x900 e 390x844 con `scrollWidth` uguale al viewport, nessun elemento fuori pagina e nessun errore console; typecheck e build completati; regressione backend con 89 test passati.
17. `[x]` Introdurre test frontend con Vitest e React Testing Library.
   - Problema: il frontend era verificato solo tramite typecheck e build, senza regressioni automatiche su componenti, trasporto HTTP e flussi operativi.
   - File coinvolti: `web/frontend/package.json`, `package-lock.json`, `vite.config.ts`, `src/test/setup.ts`, test di autenticazione, sessione, prodotto, dashboard, paginazione, client HTTP e client di dominio, `.github/workflows/ci.yml` e documentazione.
   - Dipendenze: Vitest, jsdom, React Testing Library, jest-dom e user-event.
   - Test richiesti: login, registrazione, password non valida, sessione scaduta, creazione prodotto, prodotto non eliminabile, creazione e conferma ordine, paginazione, dashboard e rinnovo sessione.
   - Criteri di accettazione: `npm ci`, `npm test`, typecheck e build riproducibili; test isolati; contratti API negativi verificati; suite eseguita dalla CI.
   - Rischi: la suite copre componenti e client in jsdom, ma non sostituisce uno smoke test browser contro lo stack reale.
   - Note verifica: 23 test frontend in 7 file passati dopo installazione pulita; typecheck e build Vite completati; CI estesa con `npm test`; regressione backend completata con 89 test passati.
18. `[x]` Aggiungere smoke test browser sui flussi principali contro lo stack web reale.
   - Problema: la suite Vitest verificava componenti e client in jsdom, ma non il collegamento reale tra browser, Nginx, Spring Boot e PostgreSQL.
   - File coinvolti: `web/frontend/playwright.config.ts`, `tsconfig.e2e.json`, `e2e/gestionale.smoke.spec.ts`, `package.json`, `package-lock.json`, `vite.config.ts`, `scripts/e2e/run-web-smoke.sh`, `.github/workflows/ci.yml`, `App.tsx`, `WorkspaceChrome.tsx`, `.gitignore` e documentazione.
   - Dipendenze: Playwright Chromium, stack Docker prod-like, credenziali bootstrap E2E conformi alla password policy e PostgreSQL isolato.
   - Test richiesti: login non valido con errore accessibile; login super admin; apertura catalogo tramite menu e scheda; creazione prodotto; registrazione cliente; aggiunta al carrello; creazione e conferma ordine; assenza di errori runtime nel flusso positivo.
   - Criteri di accettazione: typecheck E2E riproducibile; due smoke test browser verdi; esecuzione automatica sullo stack CI; report, trace, screenshot e video disponibili in caso di errore; nessun accesso al database operativo locale.
   - Rischi: lo smoke copre il percorso commerciale principale, ma non sostituisce test browser dedicati per magazzino, documenti, account, audit e rinnovo sessione.
   - Note verifica: Playwright 1.61.1 con Chromium; stack isolato su porte 18080/18081 e volume Docker dedicato; due test E2E passati in 6,7 secondi; corretto il feedback del login non valido; `npm test` con 23 test, typecheck applicativo/E2E e build Vite completati; test backend completati con 89 test passati.

### Fase 4 - Funzionalita commerciali

19. `[x]` Modellare pagamenti strutturati.
   - Problema: il metodo di pagamento e una stringa libera e il checkout usa sempre `Carta`, senza stato, importi distinti o valuta esplicita.
   - File coinvolti: nuova migrazione Flyway, dominio ordine/pagamento, API ordini, checkout e vista ordini frontend, test backend/frontend e documentazione.
   - Dipendenze: workflow ordini, idempotenza, documenti fiscali simulati e compatibilita dei dati esistenti.
   - Test richiesti: creazione pagamento strutturato, metodi ammessi, metodo non valido, importi iniziali, annullamento ordine, vincoli database e selezione metodo nel checkout.
   - Criteri di accettazione: ogni ordine ha un pagamento strutturato persistito; metodo e stato non sono stringhe arbitrarie; API espone importo richiesto, pagato, residuo e valuta; il frontend non forza un metodo fisso.
   - Rischi: il campo testuale storico resta temporaneamente come snapshot compatibile fino a una futura versione API; pagamenti parziali, transizioni di incasso e rimborsi restano nello step 20.
   - Note verifica: aggiunti entita `OrderPayment`, enum applicative e migrazione `V15` con backfill compatibile; checkout e vista ordini aggiornati; 99 test backend e 24 test frontend passati; build Vite completata; due smoke test Playwright passati sullo stack PostgreSQL/Spring Boot/Nginx reale. Lo script E2E ricrea esclusivamente il proprio volume dedicato e avvia backend e frontend in due fasi, evitando credenziali PostgreSQL obsolete e conservando log diagnostici utili.
19A. `[x]` Rafforzare i confini del monolite modulare e isolare Swing.
   - Problema: la build principale produce ancora il client Swing legacy e il backend non verifica automaticamente cicli o dipendenze scorrette tra domini.
   - File coinvolti: build Maven root e legacy, `web/backend/pom.xml`, dipendenza prodotto-ordini, test architetturali, documentazione architettura/build e istruzioni operative.
   - Dipendenze: Spring Boot 3.4.5, Spring Modulith 1.3.x, suite backend esistente e parita funzionale web gia documentata.
   - Test richiesti: verifica Spring Modulith dei moduli business; suite backend; build aggregata root; suite Swing esplicita; test e build frontend.
   - Criteri di accettazione: moduli business senza cicli; dipendenza `product -> order` rimossa tramite porta; build root web-first; Swing ancora verificabile con un comando dedicato ma escluso dal rilascio produttivo; documentazione coerente.
   - Rischi: i package tecnici trasversali restano esclusi temporaneamente dalla verifica modulare e dovranno essere ridotti o trasformati in moduli condivisi controllati in step successivi.
   - Note verifica: integrato Spring Modulith 1.3.12 e aggiunto il test sui sei moduli business; rimossa la dipendenza `product -> order` tramite la porta `ProductOrderUsage`; la build root `mvn verify` compila il prodotto web e completa 100 test backend; la build Swing resta separata in `pom-legacy.xml` con 41 test passati e JAR creato; 24 test frontend e build TypeScript/Vite completati; documentazione architetturale e operativa aggiornata.
20. `[x]` Gestire pagamenti parziali, stato pagamento, rimborsi e resi.
   - Problema: il pagamento strutturato conserva solo saldi aggregati e non registra movimenti; mancano un workflow resi, autorizzazioni dedicate e reintegro di magazzino tracciato.
   - File coinvolti: migrazione Flyway `V16`, dominio ordini/pagamenti/resi, permessi, API idempotenti, audit, magazzino, frontend ordini, test backend/frontend e documentazione.
   - Dipendenze: ordini evasi, disponibilita magazzino, sessioni, permessi granulari, audit, idempotenza e modello pagamento dello step 19.
   - Test richiesti: incasso parziale e completo, importo eccedente, annullamento con incasso, richiesta/approvazione/rifiuto/ricezione reso, reintegro stock, rimborso parziale/completo, limiti quantitativi e finanziari, autorizzazioni e idempotenza.
   - Criteri di accettazione: ogni variazione finanziaria produce un movimento immutabile; i saldi derivati restano coerenti; un reso segue transizioni esplicite; nessun rimborso supera incassato o valore ricevuto; le operazioni sensibili sono autorizzate, auditate e idempotenti.
   - Rischi: il flusso resta gestionale e non esegue transazioni reali con PSP o banche; resi e rimborsi non equivalgono automaticamente ad adempimenti fiscali o fatturazione elettronica.
   - Note verifica: aggiunta la migrazione `V16` con ledger finanziario immutabile, saldi di rimborso e workflow resi; introdotti permessi granulari, endpoint idempotenti, audit e reintegro di magazzino solo alla ricezione; pannello operativo ordini aggiornato per incassi, resi e rimborsi. `mvn verify` completato con 105 test backend passati e JAR prodotto; 16 migrazioni Flyway applicate da schema vuoto; 26 test frontend passati e build TypeScript/Vite completata; stack Docker prod-like validato su PostgreSQL 16 e Java 17 con 2 smoke test Playwright passati. Le credenziali E2E non sono persistite e i container/volumi isolati sono stati rimossi a fine verifica.
21. `[x]` Configurazione aziendale, IVA configurabile e numerazioni documentali.
   - Problema: l'aliquota IVA e fissata al 22%, i codici documento usano sequenze globali senza esercizio e i documenti non conservano l'identita dell'azienda emittente.
   - File coinvolti: migrazione Flyway `V17`, nuovo modulo `company`, dominio e API documenti, permessi, audit, frontend amministrazione, test backend/frontend e documentazione.
   - Dipendenze: documenti simulati, anagrafiche, audit, sessioni, permessi granulari, idempotenza e `TimeProvider` applicativo.
   - Test richiesti: lettura e aggiornamento configurazione, autorizzazioni super admin, versione obsoleta, validazione aliquota/prefissi, calcolo IVA configurato, snapshot azienda, progressivi distinti per tipo ed esercizio, concorrenza, vincoli database e frontend.
   - Criteri di accettazione: nessun dato aziendale fittizio; modifica configurazione protetta da versione e audit; nuovi documenti numerati atomicamente per anno e tipo; azienda e aliquota salvate come snapshot; documenti storici immutabili; errori API coerenti.
   - Rischi: resta supportata una sola aliquota predefinita per documento; numerazione e documenti rimangono simulati e non costituiscono fatturazione elettronica o conformita fiscale certificata.
   - Note verifica: aggiunta la migrazione `V17`, il modulo `company`, il permesso super admin dedicato, l'API versionata, i contatori annuali atomici e gli snapshot di emittente/IVA; introdotta la pagina frontend di configurazione e aggiornate le schede documento. `mvn -pl web/backend verify` completato con 111 test backend e JAR prodotto; 17 migrazioni Flyway applicate da schema vuoto; 28 test frontend in 10 file, typecheck applicativo/E2E e build Vite completati; stack Docker prod-like validato su PostgreSQL 16 e Java 17 con 2 smoke test Playwright passati. `git diff --check` completato senza errori; container, rete e volume E2E isolati rimossi a fine verifica.
22. `[x]` Export CSV/Excel/PDF e report operativi vendite/magazzino.
   - Dipendenze: read model ordini/prodotti, permesso dedicato, audit export, trasporto blob condiviso frontend.
   - Test richiesti: aggregazioni e filtri, autorizzazioni, escaping CSV, validità XLSX/PDF, download frontend ed errori sessione.
   - Criteri di accettazione: report server-side con limiti espliciti, tre formati reali, nessuna formula injection, interfaccia responsive, build e test completi verdi.
   - Rischi: consumo memoria su export estesi, semantica ambigua del fatturato, compatibilità Java delle librerie PDF.
   - Note verifica: introdotto il modulo `reporting` come ottavo modulo business, con porte read-only verso ordini e prodotti, permesso `VIEW_REPORTS`, audit export e limite di 10.000 righe. I report distinguono valore ordini, incassi, rimborsi, netto e residuo; lo snapshot magazzino distingue fisico, riservato e disponibile. CSV, XLSX e PDF sono formati reali, con neutralizzazione Formula Injection, header download sicuri e interfaccia React responsive. Nessuna migrazione Flyway richiesta. `mvn -pl web/backend clean verify` completato con 122 test; 31 test frontend, typecheck applicativo/E2E e build Vite completati; stack Docker PostgreSQL 16/Java 17/Nginx verificato con 2 smoke test Playwright, incluso download CSV e controllo dell'assenza di overflow orizzontale a 1024x768. Corretto lo script E2E affinche ricostruisca sempre anche l'immagine frontend; container, rete e volume isolati rimossi a fine verifica.
23. `[x]` Manuale utente operativo.
   - Problema: mancava una guida unica e verificata che spiegasse accesso, responsabilita dei ruoli, procedure operative, transizioni consentite, gestione degli errori e limiti del prodotto.
   - File coinvolti: `docs/USER_MANUAL.md`, `README.md`, `docs/ROADMAP.md`, `docs/RELEASE_CHECKLIST.md` e `docs/CHANGELOG.md`.
   - Dipendenze: matrice permessi backend, navigazione frontend, sessioni, catalogo, magazzino, anagrafiche, ordini, pagamenti, resi, documenti simulati, report, account, configurazione aziendale, monitoraggio e audit.
   - Test richiesti: controllo incrociato con ruoli e servizi backend, verifica dei percorsi documentali, controllo whitespace, backend verify, test/typecheck/build frontend.
   - Criteri di accettazione: procedure separate per ruolo, nessuna funzione inesistente documentata, operazioni sensibili e stati irreversibili evidenziati, limiti fiscali e commerciali dichiarati, manuale collegato dalla documentazione principale.
   - Rischi: il manuale deve essere aggiornato insieme a ogni variazione di ruolo, permesso, schermata o workflow; non sostituisce formazione aziendale, policy interne o consulenza legale/fiscale.
   - Note verifica: creato un manuale di 389 righe con matrice super admin/admin/dipendente/cliente, procedure passo-passo, cicli ordine e reso, sessione a 45 minuti, account protetti, report/export, gestione errori e limiti dichiarati. Le regole sono state confrontate con `UserRole`, `UserService`, `AuthSessionService`, `OrderService` e le pagine React. `mvn -pl web/backend verify` completato con 122 test e 17 migrazioni; 31 test frontend, typecheck e build Vite completati senza errori. Nessuna modifica al database o al comportamento applicativo richiesta.

### Fase 5 - Sicurezza e produzione

24. `[x]` Rate limiting e protezione login infrastrutturale.
   - Problema: il lockout persistente per username proteggeva il backend, ma il reverse proxy non limitava il volume di richieste per origine e il backend era pubblicato su tutte le interfacce host nello stack prod-like.
   - File coinvolti: `web/frontend/nginx.conf`, `web/frontend/Dockerfile`, `docker-compose.prod-like.yml`, `.env.docker.example`, `scripts/security/verify-login-rate-limit.sh`, `scripts/e2e/run-web-smoke.sh`, `.github/workflows/ci.yml` e documentazione operativa.
   - Dipendenze: Nginx `limit_req`, template runtime dell'immagine ufficiale Nginx, stack Docker prod-like, lockout applicativo persistente e correlation ID.
   - Test richiesti: rendering Compose, sintassi shell, validazione configurazione Nginx in build, risposta `429`, `Retry-After`, JSON coerente, header `no-store`/`nosniff`, request ID, log `REJECTED`, health frontend e smoke test browser.
   - Criteri di accettazione: login limitato per IP al reverse proxy; lockout backend invariato; backend pubblicato solo su loopback; errore `429 RATE_LIMIT_EXCEEDED` tracciabile; soglie configurabili; verifica automatica locale e CI.
   - Rischi: dietro proxy o load balancer il client IP deve essere ricostruito solo da proxy fidati; NAT condivisi possono richiedere soglie diverse; il rate limiting della singola istanza Nginx non e distribuito tra repliche.
   - Note verifica: limite predefinito `10r/m` con burst `10`, body login massimo 16 KiB e risposta `Retry-After: 60`; log Nginx include request ID e stato limitatore. Lo smoke prod-like ha applicato il limite dopo 10 richieste, mantenendo health e flussi browser operativi. `mvn -pl web/backend verify` completato con 122 test; 31 test frontend, typecheck applicativo/E2E e build Vite completati; 2 smoke test Playwright passati. Nessuna migrazione database richiesta.
25. `[x]` Hardening token/sessioni e valutazione cookie HttpOnly/CSRF.
   - Problema: il rinnovo frontend eseguiva un nuovo login senza revocare il token precedente; mancavano timeout di inattivita, rotazione atomica, protezione cache delle credenziali e una decisione esplicita sul modello cookie/CSRF.
   - File coinvolti: migrazione Flyway `V18`, entita/repository/service/controller sessioni, configurazione Spring Security, template ambiente, Compose prod-like, client account e rinnovo frontend, ADR 0004, test backend/frontend e documentazione operativa.
   - Dipendenze: sessioni persistenti hashate, Spring Security stateless, `TimeProvider`, audit, reverse proxy same-origin e modale di rinnovo esistente.
   - Test richiesti: rotazione riuscita, replay token precedente, rinnovo senza sessione, password errata senza revoca, confini scadenza assoluta/inattivita, header anti-cache, client frontend con token corrente, suite complete e migrazione su PostgreSQL reale.
   - Criteri di accettazione: token precedente inutilizzabile immediatamente dopo il rinnovo; rinnovo concorrente serializzato; timeout configurabili e validati; token mai persistito nel browser; scelta header/cookie e CSRF documentata; nessuna regressione su login, logout o form.
   - Rischi: il token in memoria resta leggibile da codice JavaScript compromesso; il rischio deve essere ridotto con CSP e hardening frontend nello Step 26. Una futura migrazione a cookie HttpOnly deve includere TLS, attributi cookie sicuri e protezione CSRF nello stesso rilascio.
   - Note verifica: introdotti timeout assoluto e di inattivita, touch periodico, rotazione transazionale con lock pessimista e revoca immediata del token precedente; login e rinnovo restituiscono header anti-cache. ADR 0004 conferma il token solo in memoria via `X-Session-Token` e documenta perche CSRF resta disabilitato finche non viene adottata autenticazione ambientale tramite cookie. La migrazione `V18` aggiunge e indicizza `last_used_at`; H2 e PostgreSQL pulito l'hanno applicata correttamente. `mvn -pl web/backend verify` completato con 129 test e 18 migrazioni; 32 test frontend in 11 file, typecheck applicativo/E2E e build Vite completati; stack Docker PostgreSQL 16/Java 17/Nginx verificato con 2 smoke test Playwright e rate limiting login. Metriche sessione attiva allineate anche al timeout di inattivita; diff check e cleanup dello stack completati.
26. `[x]` Content Security Policy e hardening frontend.
   - Problema: il token di sessione resta accessibile al runtime JavaScript e il reverse proxy non imponeva una policy browser esplicita contro script, stili, frame o risorse non autorizzate.
   - File coinvolti: configurazione e immagine Nginx frontend, flussi autenticazione/account React, smoke test Playwright, script verifica header, workflow CI e documentazione di sicurezza/deployment.
   - Dipendenze: frontend Vite con asset same-origin, reverse proxy Nginx, API same-origin, token mantenuto solo in memoria e stack Docker prod-like.
   - Test richiesti: sintassi Nginx, CSP su documenti e risposte proxy, assenza di `unsafe-inline`/`unsafe-eval`, blocco framing, header browser complementari, caching shell/asset, nessuna violazione CSP o errore runtime nei flussi Playwright.
   - Criteri di accettazione: CSP in enforcement compatibile con l'applicazione; script, stili e connessioni limitati alla stessa origine; framing e plugin bloccati; nessuna persistenza del token; controlli automatici locali e CI; build e flussi browser invariati.
   - Rischi: nuove integrazioni esterne, font, immagini, worker o provider di pagamento richiederanno una modifica esplicita e revisionata della policy; HSTS resta escluso finche TLS non viene terminato e verificato sul dominio reale.
   - Note verifica: introdotto un include Nginx unico con CSP in enforcement, `script-src-attr`/`style-src-attr 'none'`, blocco frame/plugin, isolamento origine, permissions policy e MIME hardening. Nginx rimuove gli header upstream per evitare valori contraddittori e applica la policy anche alle API; shell HTML `no-store`, asset Vite con hash `immutable` e dotfile negati. Lo script `verify-browser-security.sh` valida header, API proxy, assenza di direttive permissive e cache in locale/CI. `npm test` completato con 32 test in 11 file, typecheck applicativo/E2E e build Vite passati; immagine Nginx validata con `nginx -t`; 3 smoke test Playwright passati su PostgreSQL 16, Java 17 e Chromium senza violazioni CSP; rate limiting confermato dopo 9 richieste e stack E2E rimosso integralmente. Nessuna migrazione database richiesta.
27. `[x]` Actuator limitato in produzione e health/readiness.
   - Problema: gli endpoint `health` e `info` erano esposti sulla stessa porta delle API, esclusi dal filtro di sessione e usati tramite un health check generico che non distingueva processo vivo da servizio pronto.
   - File coinvolti: configurazione Spring, filtro e regole Spring Security, Dockerfile backend, Compose prod-like, template ambiente, script E2E, workflow CI, test backend e documentazione operativa.
   - Dipendenze: Spring Boot Actuator, availability state, datasource PostgreSQL, porta management interna, health check Docker e rete Compose.
   - Test richiesti: probe liveness/readiness pubbliche nel contesto applicativo, endpoint Actuator non autorizzati bloccati, soli endpoint health esposti, porta management non pubblicata, health check Docker su readiness, verifica esterna dell'assenza di Actuator sulla porta API, suite backend e smoke prod-like.
   - Criteri di accettazione: produzione con management plane separato su porta interna `9090`; esposizione limitata a `health`; dettagli nascosti; liveness distinta da readiness con database; nessun endpoint Actuator raggiungibile dalla porta API; CI e diagnostica allineate.
   - Rischi: la porta management resta raggiungibile dalla rete interna dei container e deve essere protetta anche da network policy nell'orchestratore reale; una readiness che include il database puo rimuovere temporaneamente l'istanza dal traffico durante indisponibilita PostgreSQL.
   - Note verifica: il profilo produzione avvia Actuator su una porta management interna dedicata e non pubblicata, espone solo `health`, nasconde i dettagli e separa liveness da readiness con controllo database. La security chain consente esclusivamente le due probe e nega ogni altro endpoint Actuator anche agli utenti autenticati. Il test d'integrazione avvia API e management su porte casuali distinte e verifica isolamento, stato `UP` e assenza di componenti; il gestore errori restituisce ora `404 RESOURCE_NOT_FOUND` per rotte inesistenti anziche convertirle in errori interni. `mvn -pl web/backend clean verify` completato con 134 test e 18 migrazioni; 32 test frontend e build Vite passati; stack Docker PostgreSQL 16/Java 17/Nginx verificato con probe interne, assenza di Actuator sulla porta API, 3 smoke test Playwright, CSP e rate limiting. Container, rete e volume E2E rimossi integralmente. Nessuna migrazione database richiesta.
28. `[x]` Segreti solo da variabili ambiente o secret manager.
   - Problema: il deployment prod-like accetta solo password in variabili ambiente, rendendole visibili nella configurazione del container; manca inoltre una verifica dedicata dei segreti nella storia Git e una procedura di rotazione ripetibile.
   - File coinvolti: entrypoint e immagini Docker backend/PostgreSQL, Compose prod-like e override secret, script E2E e sicurezza, workflow CI/security, template ambiente, test configurazione e documentazione operativa.
   - Dipendenze: Docker Compose secrets read-only, secret manager capace di iniettare variabili o file, Spring Boot profile `prod`, PostgreSQL 16, Gitleaks e stack E2E esistente.
   - Test richiesti: valore diretto, valore da file, variabile opzionale assente, conflitto valore/file, file mancante, file vuoto o multilinea, startup Docker con file secret, assenza valori sensibili dalla configurazione container, suite backend/frontend e scansione Gitleaks.
   - Criteri di accettazione: ogni password runtime arriva da variabile protetta o file montato; configurazioni ambigue o mancanti bloccano l'avvio; nessun segreto viene stampato; E2E usa realmente file secret; CI esegue secret scanning sulla storia; rotazione e rollback sono documentati.
   - Rischi: il resolver protegge la configurazione del container ma il processo applicativo deve comunque ricevere il segreto in memoria; permessi, cifratura e audit del secret manager restano responsabilita dell'infrastruttura reale.
   - Note verifica: aggiunto resolver POSIX fail-closed condiviso dalle immagini backend e PostgreSQL, con supporto esclusivo valore oppure `_FILE` e nessuna esposizione di valore o percorso negli errori. Gli override Compose montano password database e bootstrap come secret read-only; il bootstrap resta un override temporaneo. Lo smoke E2E genera file effimeri e `verify-container-secrets.sh` conferma valori diretti vuoti e mount read-only tramite `docker inspect`. Nove casi shell passati; Compose validato in modalita diretta, file e bootstrap; `mvn -B clean verify` completato con 134 test e 18 migrazioni; 32 test frontend e build Vite passati; Gitleaks v8.30.1 senza leak sul working tree candidato e sui 3 commit raggiungibili da `HEAD`; stack PostgreSQL 16/Java 17/Nginx verificato con Actuator isolato, CSP, 3 smoke Playwright e rate limiting. Rotazione, rollback e rimozione del secret bootstrap sono documentati. Nessuna migrazione database richiesta.
29. `[x]` Container non-root verificati.
   - Problema: il backend usa gia un utente applicativo, ma frontend e PostgreSQL non hanno una garanzia uniforme non-root; il Compose non impedisce escalation di privilegi e mantiene il filesystem root scrivibile.
   - File coinvolti: Dockerfile backend/frontend/PostgreSQL, configurazione Nginx, Compose prod-like, script E2E e sicurezza, workflow CI, test configurazione e documentazione operativa.
   - Dipendenze: immagini Java 17, Nginx Alpine e PostgreSQL 16, file secret read-only, health check e stack E2E esistente.
   - Test richiesti: utente immagine e UID runtime non-root, root filesystem read-only, `no-new-privileges`, capability eliminate, soli percorsi runtime autorizzati scrivibili, suite backend/frontend e smoke prod-like.
   - Criteri di accettazione: tutti i processi applicativi partono non-root; nessun container e privilegiato; root filesystem non modificabile; backend/frontend senza volume scrivibili; PostgreSQL scrive solo sul volume dati e tmpfs operativi; verifica automatica locale e CI.
   - Rischi: i vincoli Compose riducono l'impatto di una compromissione ma non sostituiscono isolamento del runtime, patching immagini, network policy o un orchestratore con policy admission.
   - Note verifica: backend fissato a UID/GID `10001`, PostgreSQL avviato come `postgres` e Nginx come `nginx` sulla porta non privilegiata `8080`. Compose impone root filesystem read-only, `no-new-privileges`, `cap_drop: ALL` e tmpfs con ownership minima; backend/frontend non hanno mount scrivibili e PostgreSQL persiste soltanto nel volume dati. `verify-container-hardening.sh` controlla utente immagine, UID runtime, policy Docker, rifiuto scrittura sulla root e percorsi consentiti ed e integrato in E2E e CI. `mvn -B clean verify` completato con 134 test e 18 migrazioni; 32 test frontend, typecheck applicativo/E2E e build Vite passati; stack PostgreSQL 16/Java 17/Nginx avviato con file secret, Actuator isolato, CSP verificata, 3 smoke Playwright passati e rate limiting confermato. Stack E2E rimosso al termine. Nessuna migrazione database richiesta.
30. `[x]` Backup PostgreSQL schedulato e restore testato.
   - Problema: backup e restore sono verificabili manualmente, ma mancano scheduling dichiarativo, lock anti-concorrenza, checksum, retention, controllo di freschezza e un runbook con obiettivi RPO/RTO.
   - File coinvolti: script database, template ambiente, unita systemd, CI, documentazione backup/deployment/release e piano di implementazione.
   - Dipendenze: PostgreSQL 16, Docker Compose prod-like, file secret, filesystem backup esterno al repository e timer dell'infrastruttura.
   - Test richiesti: checksum valido e corrotto, lock concorrente, retention per eta e quantita, freschezza, archivio `pg_restore` valido, backup/restore su stack PostgreSQL isolato e configurazione timer verificata.
   - Criteri di accettazione: backup atomico e non sovrascrivibile; ogni archivio ha checksum; restore non distruttivo prima delle validazioni; esecuzioni sovrapposte bloccate; retention confinata; timer persistente; restore drill automatico e documentato.
   - Rischi: un backup sullo stesso host non protegge da perdita del nodo; cifratura, replica off-site, immutabilita e credenziali del provider devono essere completate nell'infrastruttura reale.
   - Note verifica: introdotti backup custom atomici con `umask 077`, checksum SHA-256, lock, retention per eta e numero, controllo freschezza e validazione preventiva tramite `pg_restore`. Aggiunti timer systemd persistenti per backup giornaliero e restore drill settimanale isolato dell'ultimo backup reale, con verifica di tabelle critiche, versione Flyway e soglia temporale. Il test Docker ha individuato e corretto una collisione di variabili POSIX che impediva il commit del checksum e un nome Compose non portabile con lettere maiuscole. Test lifecycle e scheduling verdi; backup, restore sintetico e restore drill completati su due stack PostgreSQL isolati in 6 secondi, senza residui Docker. `mvn -B clean verify` completato con 134 test e 18 migrazioni validate; 32 test frontend, typecheck applicativo/E2E e build Vite passati. PostCSS transitivo aggiornato a `8.5.23` e `npm audit` completato con zero vulnerabilita. Nessuna migrazione database richiesta.
31. `[x]` Log strutturati, metriche e alert.
   - Problema: i log testuali non erano facilmente interrogabili, il management plane esponeva solo health e mancavano metriche applicative e alert verificabili.
   - File coinvolti: configurazione Spring produzione, filtro correlation ID, gestione errori, security chain, autenticazione/sessioni, dipendenze Micrometer, Compose prod-like, configurazione e regole Prometheus, script di verifica, CI e documentazione operativa.
   - Dipendenze: Spring Boot Actuator, Micrometer Prometheus, management port interna, Docker Compose e Prometheus 3.13.1.
   - Test richiesti: registrazione contatori con tag finiti, conteggio login/errori/sessioni, isolamento porta Prometheus, campi MDC e cleanup, configurazione Prometheus, regole alert, metriche runtime, log JSON, target `UP` e hardening container.
   - Criteri di accettazione: log JSON correlabili senza dati sensibili; metriche JVM/HTTP/database e applicative sul solo management plane; tag a cardinalita limitata; regole alert valide; Prometheus opzionale non-root e read-only; verifiche locali e CI; runbook aggiornato.
   - Rischi: il repository non configura un collettore log centralizzato, Alertmanager o un canale on-call; retention remota, alta disponibilita, network policy e calibrazione soglie restano responsabilita dell'ambiente reale.
   - Note verifica: aggiunto formato JSON Logstash con `service`, `environment`, `requestId`, metodo, percorso, stato e durata, senza query string, payload, password o token. Introdotti contatori Micrometer per autenticazione, sessioni ed errori API con tag enumerati; `/actuator/prometheus` e raggiungibile soltanto dalla porta management interna e resta assente dalla porta API. Prometheus prod-like usa retention configurabile, accesso host limitato a loopback, UID non-root, filesystem read-only, capability eliminate e cinque regole alert. `promtool`, verifica runtime di metriche/log/alert, isolamento Actuator e hardening Prometheus passati. `mvn -B clean verify` completato con 140 test e 18 migrazioni; 32 test frontend, typecheck applicativo/E2E e build Vite passati; Compose con profilo `observability` validato; stack isolato rimosso integralmente. Nessuna migrazione database richiesta.
32. `[x]` Docker prod-like verificato in modo ricorrente.
   - Problema: la verifica completa era distribuita in molti step CI, non veniva eseguita a calendario, non controllava esplicitamente lo stato Flyway e non certificava l'assenza di risorse Docker residue.
   - File coinvolti: runner e controlli in `scripts/ci` e `scripts/db`, workflow CI, test architetturale, documentazione testing/Docker/release e registro rischi.
   - Dipendenze: Docker Compose, PostgreSQL 16, Node.js 22, Playwright Chromium, Prometheus 3.13.1, OpenSSL e GitHub Actions.
   - Test richiesti: sintassi shell, contratto statico workflow/runner, build backend/frontend, migrazioni Flyway reali, runtime hardening, osservabilita, smoke browser, rate limiting, backup/restore e cleanup senza container, reti o volumi residui.
   - Criteri di accettazione: entrypoint locale unico; build immagini con `--pull`; credenziali effimere; esecuzione settimanale; timeout; diagnostica su successo/errore; verifica Flyway; cleanup certificato; quality gate invariato.
   - Rischi: registry immagini, GitHub Actions o download browser possono essere temporaneamente indisponibili; la verifica prod-like non sostituisce staging, TLS, secret manager e orchestratore reali.
   - Note verifica: introdotto un runner unico con progetto Compose e credenziali effimere, build `--pull`, schedulazione settimanale, timeout, diagnostica sempre acquisita e controllo finale delle label Docker. La prima esecuzione completa ha individuato una race tra readiness Prometheus e primo scrape backend, corretta attendendo esplicitamente il target `UP`; la seconda ha individuato una collisione tra la porta PostgreSQL applicativa e il restore drill, corretta con porte dedicate, configurabili e validate come distinte. `mvn -B clean verify` completato con 143 test e 18 migrazioni; test contrattuale mirato con 3 casi passato dopo le correzioni; 32 test frontend, build Vite e audit npm senza vulnerabilita passati. Il runner finale ha verificato Flyway V18, Actuator isolato, secret read-only, processi non-root, CSP, metriche e alert, 3 smoke Playwright, rate limiting dopo 10 richieste e backup/restore in 6 secondi. Diagnostica acquisita e cleanup certificato senza container, reti o volumi residui. Nessuna migrazione database richiesta.

### Fase 6 - Commercializzazione

33. `[x]` Proposta architetturale separata per multi-azienda/multi-tenant.
   - Problema: configurazione aziendale, account, prodotti, stock, ordini, documenti, audit e codici business condividono oggi un solo spazio dati; tenant, azienda legale, sede e magazzino non sono modellati.
   - File coinvolti: ADR 0005, proposta multi-tenant, architettura, dominio, perimetro prodotto, roadmap, registro rischi, changelog, README, workflow hygiene e test contrattuale backend.
   - Dipendenze: monolite modulare, PostgreSQL, Flyway, Spring Security, sessioni persistenti, configurazione aziendale, numerazioni, magazzino e backup.
   - Test richiesti: presenza documenti in CI, contratto statico su modello pooled/dedicato, tenant context fail-closed, foreign key composte, RLS forzata, ruolo runtime senza bypass, migrazione expand/contract e matrice test cross-tenant.
   - Criteri di accettazione: decisione distinta dall'implementazione; terminologia non ambigua; modello dati e sicurezza multilivello; strategia single-tenant compatibile; fasi, rollback, test e gate documentati; nessuna falsa dichiarazione di supporto SaaS.
   - Rischi: una proposta non mitiga ancora il rischio di data leak; RLS richiede PostgreSQL reale, ruoli database separati e disciplina transazionale; GDPR, fatturazione, onboarding, billing e provisioning restano decisioni separate.
   - Note verifica: ADR 0005 e proposta operativa approvati come target futuro, con schema condiviso e `tenant_id` obbligatorio, isolamento applicativo fail-closed, foreign key composte, RLS PostgreSQL forzata, ruoli database separati e opzione di deployment/database dedicato per tenant. Distinti tenant, azienda legale, sede e magazzino; documentate identita globali, membership, sessioni contestualizzate, stock per magazzino, migrazione expand/contract, rollback, osservabilita e matrice di test cross-tenant. Il contratto statico dedicato passa con 3 test; `mvn -B -pl web/backend clean verify` passa con 146 test e 18 migrazioni Flyway; 32 test frontend e build Vite passano. Nessuna migrazione o modifica runtime introdotta: il gestionale resta correttamente dichiarato single-tenant finche l'intero piano di migrazione non supera i gate su PostgreSQL reale.
34. `[x]` Proposta separata per GDPR, conservazione documentale e fatturazione elettronica.
   - Problema: dati personali, audit, backup e documenti simulati esistono, ma mancano inventario approvato, workflow diritti, retention generale, legal hold, conservazione a norma, XML fiscale, trasmissione SdI e ricevute.
   - File coinvolti: ADR 0006, proposta privacy/fiscale, architettura, dominio, perimetro prodotto, roadmap, registro rischi, changelog, README, workflow hygiene e test contrattuale backend.
   - Dipendenze: ADR 0005, ruoli e permessi, audit, idempotenza, documenti simulati, configurazione aziendale, backup/restore, secret manager futuro e provider da selezionare.
   - Test richiesti: confini espliciti tra privacy, backup, conservazione e fatturazione; retention dry-run e legal hold; erasure ledger e restore; documenti simulati separati; XML/ricevute immutabili; outbox/inbox; stati SdI distinti; sandbox e pilot.
   - Criteri di accettazione: nessuna falsa conformita; fonti ufficiali e data consultazione; ruoli organizzativi; inventario dati; workflow interessati; retention e incidenti; adapter provider; migrazione, rollback, test e gate professionali documentati.
   - Rischi: la proposta non rende il prodotto conforme; norme, schemi e provider cambiano; tempi di conservazione e basi giuridiche dipendono dal caso concreto; errori possono causare data leak, perdita di evidenze o documenti fiscalmente non emessi.
   - Note verifica: ADR 0006 e proposta operativa completati sulla base delle fonti ufficiali consultate il 2026-07-25. Sono stati separati privacy, retention, legal hold, backup, gestione documentale, conservazione elettronica e fatturazione elettronica; documentati workflow per i diritti, erasure ledger, incidenti, adapter provider, XML e ricevute immutabili, outbox/inbox, stati SdI, migrazione, rollback e gate professionali. Il contratto statico dedicato passa con 4 test; `mvn -B -pl web/backend clean verify` passa con 150 test e 18 migrazioni Flyway; 32 test frontend e build TypeScript/Vite passano. Nessuna migrazione o modifica runtime introdotta: le funzionalita restano correttamente dichiarate non attive e richiedono validazione legale, fiscale e del responsabile della conservazione prima dell'implementazione.
35. `[x]` Proposta onboarding clienti, branding, provisioning e aggiornamenti/migrazioni.
   - Problema: bootstrap, configurazione, deployment, migrazioni e aggiornamenti governano oggi una sola installazione; mancano lifecycle cliente, inventario installazioni, branding sicuro, entitlement, provisioning riprendibile, release immutabili e rollout di flotta.
   - File coinvolti: ADR 0007, proposta customer lifecycle/release, architettura, dominio, perimetro prodotto, roadmap, registro rischi, changelog, README, workflow hygiene e test contrattuale backend.
   - Dipendenze: ADR 0005 e 0006, monolite modulare, Flyway, CI/CD, immagini non-root, secret manager futuro, backup/restore, osservabilita e piattaforma di orchestrazione da selezionare.
   - Test richiesti: transizioni lifecycle, quattro occhi, provisioning idempotente e riprendibile, compensazioni, branding ostile, isolamento asset, entitlement backend, digest/firma/SBOM/provenance, canary/auto-pause, compatibility matrix, expand/contract, rollback e offboarding.
   - Criteri di accettazione: control plane separato dal data plane; nessun fork cliente; branding senza codice arbitrario; onboarding e offboarding a stati; provisioning pooled/dedicato; artefatto unico immutabile; rollout per coorti; migrazioni forward-only; rollback e gate di vendita documentati; nessuna funzione futura presentata come attiva.
   - Rischi: la proposta non rende il prodotto SaaS o vendibile; provider e piattaforma non sono scelti; provisioning o update errati possono causare data leak, downtime o perdita dati; billing e contratti commerciali restano decisioni separate.
   - Note verifica: ADR 0007 e proposta operativa completati con control plane separato, lifecycle cliente a stati, saga persistita, approvazioni, provisioning pooled/dedicato, branding tramite token sicuri, entitlement distinti da permessi e feature flag, manifest release, SemVer, firma, SBOM, provenance, canali, coorti, auto-pause, compatibility matrix, expand/contract, rollback e offboarding. Le fonti tecniche ufficiali sono state consultate il 2026-07-25. Il contratto statico dedicato passa con 4 test dopo aver reso espliciti attivazione, sospensione e pilot pooled/dedicato; `mvn -B -pl web/backend clean verify` passa con 154 test, 18 migrazioni Flyway e JAR prodotto; 32 test frontend e build TypeScript/Vite passano. Nessuna migrazione o modifica runtime introdotta: onboarding SaaS, branding tenant, provisioning e gestione della flotta restano correttamente dichiarati non attivi.

## Test minimi di riferimento

- Backend: `cd web/backend && mvn test`.
- Frontend: `cd web/frontend && npm ci && npm test && npm run build`.
- Docker prod-like: `docker compose -f docker-compose.prod-like.yml build` e `docker compose -f docker-compose.prod-like.yml up`, quando l'ambiente locale lo consente.

## Criteri generali di accettazione

- Comportamento implementato realmente.
- Test positivi e negativi presenti per ogni regola nuova.
- Backend compila e test backend passano.
- Frontend compila e test frontend passano quando presenti.
- Migrazioni Flyway funzionano su database pulito.
- Errori API coerenti con il contratto documentato.
- Autorizzazioni applicate lato backend.
- Frontend gestisce successo, errore e sessione scaduta.
- Documentazione aggiornata.
- Nessun segreto introdotto.
- Nessuna regressione nota lasciata aperta senza nota esplicita.

## Note operative

- Non modificare vecchie migrazioni Flyway salvo errore bloccante dimostrato.
- Ogni evoluzione database deve usare una nuova migrazione.
- Non usare la parte Swing come base produttiva.
- Non dichiarare conformita fiscale o legale senza integrazione specifica e validazione professionale.
- Commit non automatici: da creare solo su richiesta esplicita.
