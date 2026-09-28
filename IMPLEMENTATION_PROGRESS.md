# Implementation progress

## Stato generale

- Current step: 4.3, Inventario fisico e rettifiche approvate, completato, sincronizzato e verificato
- Last completed step nel repository reale: 4.3, Inventario fisico e rettifiche approvate
- Last update: 2026-09-28
- Roadmap completion: 32/48 step completati, sincronizzati e verificati nel repository reale (66.7%)
- Maturita realistica stimata: 95-97% nel repository reale
- Gate C verificato tecnicamente e chiuso nel repository reale
- Open P0: nessuno
- Open P1: F-17, F-18, F-19
- Open P2: F-29, F-30
- Open P3: nessuno

## Step

| Step | Stato | Commit/patch | Test | Note |
|---|---|---|---|---|
| 0.1 | COMPLETED | `9f9e23a` baseline; `945fcd0` CI; `62a8d14` security; `fa52524` gate finali; `8d9be88` hardening CodeQL | backend 155/155; frontend 32/32; legacy 41/41; CI, security e code scanning remoti verdi | PR #2 aperta in draft; `main` protetta con backend, frontend, migration e quality gate obbligatori |
| 0.2 | COMPLETED | checkpoint `2d783d5` | verifica backlog e coerenza documentale | Scope congelato; target MVP, responsabile decisionale e tre classi di backlog formalizzati |
| 0.3 | COMPLETED | checkpoint `2d783d5` | contratto fixture 5/5; backend 160/160; PostgreSQL V14/V16/V18 profilo `small` | Fixture sintetiche versionate, checksum, profili di volume e verificatore PostgreSQL isolato |
| 1.1 | COMPLETED | checkpoint `2d783d5` | backend 166/166; frontend 32/32; typecheck e build verdi; contratto gateway 1/1 | Registrazione pubblica limitata a CUSTOMER, staff confinato al flusso admin, audit SELF_SERVICE e rate limit dedicato |
| 1.2 | COMPLETED | checkpoint `2d783d5` | backend 171/171; test mirati 5/5; fixture PostgreSQL V14/V16/V18/V19 profilo `small` | Provenienza account, quarantena conservativa degli operativi storici non verificati, revoca sessioni e revisione manuale super admin |
| 1.3 | COMPLETED | checkpoint `2d783d5` | backend 178/178; PostgreSQL concorrenza 6/6; fixture V14/V16/V18/V19/V20 | Subject account ID stabile, versione credenziali, revoca globale e race revoca/touch serializzate |
| 1.4 | COMPLETED | checkpoint `2d783d5` | backend 188/188; frontend 33/33; PostgreSQL ownership 8/8; fixture V14/V16/V18/V19/V20/V21 | Ownership account/partner basata su ID, storico ambiguo `UNRESOLVED` e link account-partner esplicito |
| 1.5 | COMPLETED | checkpoint `2d783d5` | contratto 9/9; backend 194/194; runtime Docker sentinel/collision/HUP/INT/TERM/KILL; prod-like completo con Playwright 3/3 | Runner Docker isolati per project/run ID, cleanup per ownership e CI prod-like riattivata |
| 1.6 | COMPLETED | checkpoint `2d783d5` | backend 201/201; frontend 33/33; SCA 66 dipendenze/0 vulnerabilita; fail-closed 2/2 | Spring Boot 4.1, Security 7.1, bootstrap sicuro, H2 test-only e gate SCA |
| 2.1 | COMPLETED | checkpoint `2d783d5` | backend 204/204; frontend 34/34; PostgreSQL upgrade V14/V16/V18, medium/large e concorrenza 12/12 | V22 conserva V15/V17, inizializza contatori, introduce `UNRECONCILED` e procedura manuale protetta |
| 2.2 | COMPLETED | checkpoint `2d783d5` | backend 211/211; frontend 34/34; PostgreSQL canonical identity 5/5; migrazione 2/2 | Colonne canoniche V23, vincoli DB concorrenti, preflight e conflitti API uniformi |
| 2.3 | COMPLETED | checkpoint `2d783d5` | backend 219/219; frontend 37/37; PostgreSQL ledger 9/9; build Vite verde | Ledger autorevole V24, comandi inventariali, protezione riserve e riconciliazione |
| 2.4 | COMPLETED | checkpoint `2d783d5` | backend 224/224; frontend 42/42; PostgreSQL idempotenza 10/10; build Vite verde | Claim atomico V25, replay durevole, lease e chiavi frontend legate all'intento |
| 2.5 | COMPLETED | checkpoint `2d783d5` | backend 224/224; frontend 50/50; typecheck e build Vite verdi | Risposta comando applicata subito, refresh mirati, esiti distinti, protezione doppio invio e bozze preservate |
| 2.6 | COMPLETED | checkpoint `2d783d5` | backend 232/232; frontend 51/51; PostgreSQL 18/18; typecheck e build Vite verdi | Reversal manuale atomico, rilascio riserve, audit, idempotenza e UI operativa |
| 2.7 | COMPLETED | checkpoint `2d783d5` | backend 238/238; frontend 52/52; PostgreSQL 22/22; typecheck e build Vite verdi | Ledger finanziario autorevole, link rimborso-reso, report read-only, metriche e vincoli temporali |
| 2.8 | COMPLETED | checkpoint `2d783d5` | backend 239/239; frontend 52/52; build Vite; Playwright 3/3; PostgreSQL V1-V27, backup/restore e least privilege verdi | Owner `NOLOGIN`, migrator owner-scoped, runtime senza DDL, backup read-only, restore e rotazione separati |
| 2.9 | COMPLETED | checkpoint `2d783d5` | backend 243/243; PostgreSQL 95/95; frontend 52/52; typecheck e build Vite; upgrade popolati V14/V16/V18 e least privilege verdi | Testcontainers PostgreSQL obbligatorio, profili CI fast/nightly, fixture fino a V29 e concorrenza trasversale |
| 3.0 | COMPLETED | checkpoint `2d783d5` | backend 246/246; contratto benchmark 3/3; frontend 52/52; typecheck e build Vite verdi | Template vincolante e WB-001 vendita assistita basato su fonti ufficiali; validazione operatore predisposta per lo Step 3.1 |
| 3.1 | COMPLETED | checkpoint `2d783d5` | backend 251/251; frontend 56/56; typecheck e build Vite verdi; PostgreSQL upgrade V14/V16/V18 fino a V30 | Vendita separata dal catalogo, cliente censito per ID stabile o occasionale esplicito, snapshot righe, audit, self-service protetto e bozza preservata sugli errori; prova operatore ancora pendente |
| 3.2 | COMPLETED | checkpoint `2d783d5` | backend 258/258; frontend 61/61; typecheck e build Vite verdi | Projection server-side per ruolo, disponibilita commerciale discreta e dashboard limitata agli ordini dell'account cliente |
| 3.3 | COMPLETED | checkpoint `2d783d5` | backend 262/262; PostgreSQL 95/95; frontend 64/64; fixture V14/V16/V18 fino a V31; typecheck e build Vite verdi | Lifecycle account non distruttivo, credenziali e sessioni revocabili, login senza ruolo dichiarato dal client, lockout temporaneo atomico |
| 3.4 | COMPLETED | checkpoint `2d783d5` | backend 262/262; frontend 78/78 in 23 file; typecheck e build Vite verdi | Session provider centralizzato e pilot Account con query state, debounce, cancellazione richieste e latest-request-wins |
| 3.5 | COMPLETED (6/6) | checkpoint `2d783d5` | backend 262/262; frontend 179/179 in 35 file; typecheck applicativo/E2E e build Vite verdi | Sessione, catalogo, ordini/pagamenti/resi, partner, documenti/report e amministrazione estratti |
| 3.6 | COMPLETED | checkpoint `2d783d5` | backend 265/265; frontend 183/183 in 35 file; build Vite verde | Dettagli prodotto/ordine, capability server-side, storici mirati e dashboard senza fallback parziali |
| 3.7 | COMPLETED | checkpoint `2d783d5` | backend 271/271; frontend 184/184; build Vite; PostgreSQL upgrade V14/V16/V18 fino a V32 | KPI separati per stato e natura economica, fuso IANA aziendale, eventi con offset e dati emittente obbligatori |
| 3.8 | COMPLETED | checkpoint `2d783d5` | backend 274/274; frontend 188/188; PostgreSQL 97/97; build Vite verde | Resi multi-riga, residui autorevoli, note per return ID, outcome tipizzati e ledger rimborso isolato |
| 3.9 | COMPLETED | checkpoint `2d783d5` | backend 274/274; frontend 198/198 in 39 file; typecheck applicativo/E2E; build Vite; Playwright/axe 13/13; prod-like completo | Semantica tabelle e form, dialog accessibili, navigazione da tastiera, draft store cross-view, dirty guard, touch target e download difensivi |
| 3.10 | COMPLETED | checkpoint `2d783d5` | backend 277/277; frontend 198/198; typecheck applicativo/E2E; build Vite; Playwright/axe 19/19; PostgreSQL fast e upgrade popolati V14/V16/V18 fino a V32 | Sette workflow verticali MVP, matrice browser accessibile, runner unico e Gate C verificato tecnicamente |
| 4.1 | COMPLETED | checkpoint `2d783d5` | backend 285/285; PostgreSQL 98/98; frontend 200/200 in 40 file; typecheck applicativo/E2E e build Vite verdi; upgrade popolati V14/V16/V18 fino a V33 | Ordini fornitore con ID e snapshot stabili, stati, ricezioni amministrative parziali, annullo residuo, idempotenza, concorrenza, audit e UI; stock e costo restano invariati fino allo Step 4.2 |
| 4.2 | COMPLETED | checkpoint `2d783d5` | backend 288/288; PostgreSQL 99/99; frontend 202/202; typecheck applicativo/E2E e build Vite verdi; upgrade popolati V14/V16/V18 fino a V34 | Ricezione fisica, costo effettivo, media mobile e KPI con copertura costo; prova operatore ancora pendente |
| 4.3 | COMPLETED | checkpoint `2d783d5` | backend 295/295; PostgreSQL 105/105; frontend 202/202; typecheck applicativo/E2E e build Vite verdi; upgrade popolati V14/V16/V18 fino a V35 | Conteggio con snapshot, approvazione separata, compensazione dei movimenti intermedi, rettifica auditata e UI operativa; prova operatore ancora pendente |
| 4.4 | PENDING | - | - | Scorta minima e proposte riordino |
| 4.5 | PENDING | - | - | Importazione con staging |
| 4.6 | PENDING | - | - | Performance su volumi target |
| 4.7 | PENDING | - | - | Backup off-site e restore parallelo |
| 4.8 | PENDING | - | - | Restart, alert delivery e log centralizzati |
| 5.1 | PENDING | - | - | Supply-chain security |
| 5.2 | PENDING | - | - | Artifact immutabile, promozione e rollback |
| 5.3 | PENDING | - | - | Security hardening e pentest |
| 5.4 | PENDING | - | - | Load, concorrenza, failure e DR |
| 5.5 | PENDING | - | - | Privacy e retention |
| 5.6 | PENDING | - | - | Decisione perimetro pagamenti |
| 6.1 | PENDING | - | - | Packaging, onboarding e configurazione guidata |
| 6.2 | PENDING | - | - | Supportabilita e policy upgrade |
| 6.3 | PENDING | - | - | Permessi granulari e approvazioni |
| 6.4 | PENDING | - | - | Documenti fiscali tramite provider e consulenza |
| 6.5 | PENDING | - | - | Integrazioni e funzioni vendibili |

## Decisioni approvate

- `MASTER_SOURCE_AGENTE_AI.md` e la fonte operativa vincolante.
- Target produttivo: monolite modulare Spring Boot + React con PostgreSQL.
- La cartella `src/` e legacy e non riceve nuove funzioni salvo step esplicito.
- Un solo step viene eseguito per turno; avanzamento automatico disabilitato.
- Nessun deploy, commit, push, PR o operazione distruttiva senza autorizzazione esplicita.
- Nessuna nuova funzione commerciale prima della chiusura dei P0 e dei gate dipendenti.
- Il 2026-07-27 e stato autorizzato il commit della baseline e la configurazione della branch protection su `main`.
- Il 2026-07-27 e stato autorizzato il push della branch `miglioramenti-gestionale`.
- Il 2026-07-27 e stata autorizzata l'apertura della pull request e il completamento remoto dello Step 0.1.
- Il 2026-09-28 e stato creato il checkpoint verificato `2d783d5` per gli Step 0.2-4.3; la pubblicazione remota viene verificata separatamente.
- Dal 2026-07-28 lo scope e congelato almeno fino al Gate A: sono ammessi soltanto P0/P1, test e correzioni necessarie.
- Il target MVP e limitato a singola azienda, singolo magazzino, pagamenti manuali e documenti simulati.
- Giovanni De Filippo e il responsabile finale delle decisioni su stock, pagamenti e conservazione dello storico.
- Il backlog e classificato in stabilizzazione, MVP e dopo MVP; “dopo MVP” non rende facoltativi i gate di produzione.

## Blocker

- Nessun blocker tecnico aperto nello Step 4.3.
- La validazione con operatore reale resta `PENDING_OPERATOR_EXECUTION` e non viene dichiarata eseguita.
- I 61 file autorizzati dello Step 4.3 sono sincronizzati nel repository reale; la verifica byte per byte e le suite sul repository reale chiudono lo step.
- I 79 file autorizzati dello Step 4.2 sono sincronizzati nel repository reale e verificati byte per byte; nessun file estraneo all'inventario e stato sovrascritto.
- I 12 file autorizzati dello Step 3.10 sono sincronizzati nel repository reale e verificati byte per byte; nessun file estraneo all'inventario e stato sovrascritto.
- I 63 file autorizzati dello Step 3.9 restano sincronizzati nel repository reale e verificati byte per byte; nessun file estraneo all'inventario e stato sovrascritto.
- Il checkpoint applicativo `2d783d5` e stato creato; lo stato remoto viene verificato separatamente al termine del consolidamento.
- F-20A e chiuso nel repository reale con ricezione merce, costo documentato e KPI che distinguono copertura nota e quota non valorizzata.

## Verifiche Step 4.3

- Ogni sessione salva lo snapshot teorico per prodotto, il conteggio dell'operatore, la differenza e la motivazione senza modificare lo stock durante il rilevamento.
- Invio e approvazione sono separati: il responsabile deve essere distinto dall'operatore che ha eseguito il conteggio e deve possedere `APPROVE_INVENTORY_COUNTS`.
- Carichi, scarichi e vendite avvenuti durante il conteggio non vengono persi: in approvazione la differenza osservata viene applicata alla giacenza corrente sotto lock.
- La rettifica non puo portare la giacenza sotto la quantita riservata; doppia approvazione, sessione attiva duplicata e codici prodotto ambigui sono rifiutati.
- Ogni approvazione genera un solo movimento ledger collegato alla sessione e conserva attori, timestamp, conteggio, differenza e causale per audit e report.
- La rettifica diretta e stata rimossa dalla UI e rifiutata dall'API con un errore operativo gestito.
- Backend completo: 295/295 test; frontend completo: 202/202; typecheck applicativo/E2E e build Vite completati.
- PostgreSQL `fast`: 105/105 test su PostgreSQL 16, migrazioni V1-V35 e upgrade popolati V14, V16 e V18 completati.
- WB-007 e `IMPLEMENTED`; la prova guidata con operatore di magazzino e responsabile resta `PENDING_OPERATOR_EXECUTION`.
- I 61 file autorizzati coincidono byte per byte tra staging e repository reale; nessun file estraneo all'inventario e stato sovrascritto.

## Verifiche Step 4.2

- La ricezione fornitore applica ordine, riga ricevuta, carico fisico, movimento autorevole e valorizzazione nella stessa transazione.
- Costo concordato e costo effettivo restano distinti; scostamento, totale, stato posting e movimento collegato sono snapshot verificabili.
- Ultimo costo e media ponderata mobile usano quattro decimali `HALF_UP`; KPI aggregati e valori monetari usano due decimali.
- La quantita valorizzata non supera la giacenza fisica; stock storico, carichi manuali e resi privi di costo documentato non ricevono valori inventati.
- Due ricezioni concorrenti da ordini diversi sullo stesso prodotto producono due movimenti, una sola baseline e una media ponderata deterministica.
- Dashboard, catalogo e report dello staff espongono valore noto a costo, copertura e margine potenziale sulla sola quota valorizzata; il cliente non riceve informazioni di costo.
- Backend completo: 288/288 test; frontend completo: 202/202 in 41 file; typecheck applicativo/E2E e build Vite completati.
- PostgreSQL `fast`: 99/99 test, migrazioni V1-V34 e upgrade popolati V14, V16 e V18 completati senza inferire costi storici.
- WB-006 e `IMPLEMENTED`; la prova guidata con un operatore acquisti o magazzino resta `PENDING_OPERATOR_EXECUTION`.
- I 79 file autorizzati coincidono byte per byte tra staging e repository reale; lo Step 4.3 non e stato iniziato.

## Verifiche Step 4.1

- Il modulo `purchase` gestisce ordini fornitore in stato `DRAFT`, `SENT`, `PARTIALLY_RECEIVED`, `RECEIVED` e `CANCELED` con codice business stabile, optimistic version e lock pessimista sulle transizioni concorrenti.
- Fornitore e prodotti sono collegati tramite ID stabili; codice, nome, prezzo concordato, quantita e date previste restano snapshot storici dell'ordine.
- Creazione, invio, ricezione amministrativa e annullo sono autorizzati dal backend, idempotenti per intento e auditati. Le ricezioni cumulative non possono superare l'ordinato e l'annullo preserva quanto gia ricevuto.
- Il catalogo impedisce rinomina ed eliminazione di prodotti referenziati da ordini fornitore, preservando lo storico di approvvigionamento.
- Lo Step 4.1 non modifica giacenza fisica, ledger inventariale o costo: il posting della ricezione merce appartiene esclusivamente allo Step 4.2.
- Frontend: pagina acquisti paginata, filtri, bozza multi-riga, dettaglio, residui, ricezioni e capability server-side; gli errori preservano la bozza e il confine amministrativo della ricezione e dichiarato nella UI.
- Backend completo: 285/285 test passati con Java 23 sulla baseline Java 17; 33 migrazioni Flyway validate e applicate su schema H2 pulito.
- PostgreSQL `fast`: 98/98 test passati su PostgreSQL 16, inclusa la concorrenza sull'ultima quantita residua; upgrade popolati V14, V16 e V18 fino a V33 completati.
- Frontend completo: 200/200 test in 40 file; typecheck applicativo, typecheck E2E e build Vite di produzione completati.
- WB-005 e `IMPLEMENTED`; la prova guidata con un operatore acquisti resta `PENDING_OPERATOR_EXECUTION`.

## Verifiche Step 3.10

- Sette scenari verticali coprono creazione dipendente, vendita assistita, ciclo stock-ordine-riserva-evasione, acconto-annullamento-storno, reso multi-riga-ricezione-rimborso, scadenza sessione durante una query e autorizzazione negativa cliente.
- `scripts/e2e/run-mvp-gate.sh` compone il gate prod-like e il gate PostgreSQL `fast` in un solo ingresso operativo con arresto immediato sul primo errore.
- Playwright/axe: 19/19 test passati sullo stack reale; i sette workflow MVP sono eseguiti in Chromium e le verifiche di autenticazione e accessibilita coprono anche Firefox e WebKit.
- Il rinnovo della sessione non reinizializza piu il workspace e non cancella il feedback operativo; il flusso di retry dopo una query scaduta e verificato end-to-end.
- Il token colore di errore e stato corretto per raggiungere il contrasto WCAG AA senza alterare la gerarchia visiva.
- Backend completo: 277/277 test; frontend completo: 198/198 in 39 file; typecheck applicativo/E2E, build Vite e audit npm con zero vulnerabilita passati.
- PostgreSQL `fast`: migrazioni V1-V32, concorrenza e upgrade popolati V14, V16 e V18 completati preservando ordini e prodotti.
- Gate prod-like: hardening, CSP, cache, metriche, rate limiting, backup/restore e least privilege PostgreSQL verificati senza risorse residue.
- F-31 e Gate C risultano chiusi sul piano tecnico nel repository reale.
- La prova guidata con un operatore reale resta `PENDING_OPERATOR_EXECUTION` e non viene confusa con il gate automatico.

## Verifiche Step 3.9

- Tutte le tabelle applicative espongono `caption`, `thead`, intestazioni di colonna e intestazioni di riga coerenti; form, controlli e azioni hanno nomi accessibili verificati sull'albero semantico.
- `AccessibleDialog` applica focus trap, gestione `Escape`, focus iniziale e ripristino del controllo di origine senza propagare comandi al workspace sottostante.
- Menu orizzontali e schede sono navigabili da tastiera; il titolo `Nuovo ordine` e coerente per il cliente in menu, scheda, header e regione accessibile.
- Le bozze vengono conservate in `sessionStorage` per vista ed entita, senza password, e protette da dirty guard su cambio vista, chiusura scheda, logout e uscita browser.
- Contrasto, indicatori di focus, target touch da almeno 44 px, layout a 390/768/1440 px e `prefers-reduced-motion` sono coperti dalla matrice browser.
- Il download parser gestisce `filename*`, valori quotati o semplici, separatori e nomi non sicuri; la revoca URL differita e verificata anche su WebKit.
- Frontend completo: 198/198 test in 39 file, typecheck applicativo/E2E e build Vite passati; `npm audit` riporta 0 vulnerabilita.
- Playwright/axe: 13/13 test passati su Chromium 1440, Chromium 390, Chromium 768, Firefox 1440 e WebKit 1440, senza violazioni axe critiche o serie.
- Backend invariato: 274/274 test passati fuori dalla sandbox confinata; 32 migrazioni Flyway validate.
- Gate prod-like completo: hardening, CSP, cache, metriche, rate limiting, backup/restore e least privilege PostgreSQL verificati senza risorse residue.
- F-26 e F-34 risultano chiusi nel repository reale; lo Step 3.10 non e stato iniziato.

## Verifiche Step 3.8

- Il builder resi consente di selezionare piu righe dello stesso ordine e invia un solo comando con quantita positive; righe a zero non entrano nel payload.
- Ogni riga ordine espone quantita ordinata, quantita gia restituita o impegnata da resi attivi e quantita residua restituibile calcolate dal backend.
- Due richieste concorrenti sulla stessa quantita residua vengono serializzate dal lock dell'ordine: una sola richiesta puo riuscire e il database conserva un solo reso.
- Note di revisione, selezione del reso da rimborsare e dettaglio ledger usano l'ID stabile del reso; il codice descrittivo resta solo snapshot e fallback per transazioni storiche prive di ID.
- Ogni reso espone esclusivamente le proprie transazioni `REFUND`, con codice, importo, causale, riferimento, operatore e timestamp.
- Gli esiti comando sono tipizzati come `success`, `warning` o `error`; `warning` distingue un comando persistito con refresh fallito da un esito non salvato.
- Form di richiesta, revisione e rimborso vengono azzerati soltanto quando l'esito conferma la persistenza; errore e replay ambiguo conservano la bozza.
- Test backend completi: 274/274; test frontend completi: 188/188 in 35 file; build TypeScript/Vite completata.
- Gate PostgreSQL `fast`: 97/97 test taggati, migrazioni V1-V32 e upgrade popolati V14/V16/V18 completati.
- Nessuna migrazione o modifica distruttiva allo schema; il contratto API cambia soltanto in modo additivo.
- La scheda WB-004 e `IMPLEMENTED`; la prova con operatore reale resta pendente.
- F-26 e risolto sul perimetro dello Step 3.8; accessibilita estesa e draft store cross-view restano oggetto dello Step 3.9.

## Verifiche Step 3.7

- Dashboard ordini separata tra bozze, confermati, evasi e annullati; valori distinti tra lordo ordini, incassato, rimborsato e netto incassato.
- Il prezzo di vendita scontato moltiplicato per la giacenza viene denominato soltanto `potentialRetailStockValue` e “valore potenziale di vendita”; non e presentato come valore inventario o valore contabile.
- Tutti i KPI esposti sono definiti in `docs/KPI_DEFINITIONS.md` con fonte, formula, inclusioni, esclusioni e limiti interpretativi.
- La configurazione aziendale espone un fuso IANA validato e l'elenco dei dati emittente mancanti; la generazione documentale e le relative capability restano bloccate finche la configurazione non e completa.
- Gli eventi API generali espongono `OffsetDateTime` in UTC; i documenti conservano il fuso aziendale nello snapshot ed espongono l'offset coerente con l'istante di emissione.
- Anno e progressivo documentale sono determinati dall'istante corrente nel fuso aziendale; il caso `2026-12-31T23:30Z` in `Europe/Rome` produce correttamente esercizio 2027.
- I filtri giorno dei report vengono interpretati nel fuso aziendale e trasformati in intervalli UTC, incluse le giornate di 23 o 25 ore causate dal cambio DST.
- Il disclaimer resta esattamente `DOCUMENTO SIMULATO - NON VALIDO AI FINI FISCALI`; nessuna conformita fiscale viene dichiarata.
- Migrazione Flyway `V32` verificata su schema pulito e su upgrade popolati V14, V16 e V18; i documenti storici privi di evidenza del fuso ricevono conservativamente lo snapshot `UTC`.
- Backend completo: 271/271 test passati fuori dalla sandbox confinata, inclusi tre test Actuator su porte locali effimere.
- Frontend completo: 184/184 test passati in 35 file; build TypeScript/Vite di produzione completata senza errori.
- Patch censita rispetto alla baseline dello Step 3.6: 88 file modificati o aggiunti, nessuna cancellazione e nessun artefatto generato incluso.
- F-27 e chiuso. F-20A resta aperto soltanto per la valutazione a costo prevista dallo Step 4.2; F-29 resta aperto per il provider fiscale e la validazione professionale previsti dallo Step 6.4.

## Verifiche Step 3.6

- I dettagli operativi `GET /api/products/{code}/detail` e `GET /api/orders/{code}/detail` compongono la risorsa selezionata con storico mirato e capability calcolate dal backend.
- Prodotti, ordini, resi e documenti simulati espongono capability esplicite derivate da permessi, ownership, stato e vincoli di dominio; il frontend non ricostruisce piu le azioni da liste paginate.
- Lo storico prodotto usa query dedicate per movimenti e ordini collegati anche quando tali record non appartengono alla pagina globale corrente.
- La capability per creare una nota credito considera tutti i documenti collegati all'ordine, indipendentemente da filtro tipo e pagina attivi.
- La selezione ordine resta disponibile tramite dettaglio dedicato anche quando filtri o paginazione lo escludono dall'elenco visibile.
- La dashboard operativa usa esclusivamente l'aggregazione server-side e non ripiega piu sui primi otto prodotti, ordini o movimenti caricati dal client.
- Backend completo: 265/265 test passati fuori dalla sandbox confinata; la suite include 31 migrazioni Flyway e i tre test Actuator che richiedono porte locali effimere.
- Frontend completo: 183/183 test passati in 35 file; build TypeScript/Vite di produzione completata senza errori.
- Regressioni dedicate verificano storico fuori pagina, capability documento indipendente dai filtri, dettaglio ordine escluso dalla pagina e capability distinte dei resi.
- La patch censita contiene 51 file: 20 sorgenti backend, 2 test backend, 23 file frontend/test e 6 documenti; nessun file generato e incluso.
- Nessuna migrazione Flyway o modifica allo schema dati richiesta.
- F-24 e chiuso per correttezza funzionale; budget e indici su volumi target restano oggetto dello Step 4.6.

## Verifiche Step 3.5.6

- La sesta e ultima slice prevista dallo Step 3.5 estrae account, audit, configurazione aziendale e monitoraggio di sistema da `App.tsx` in un unico dominio Amministrazione coerente.
- `useAdministrationFlow` applica debounce, cancellazione tramite `AbortSignal` e latest-request-wins alle pagine account e audit; monitoraggio e stato sistema vengono caricati in modo lazy e conservati tra le viste.
- Creazione account, abilitazione/disabilitazione, reset password, revoca sessioni e cambio ruolo continuano a usare il `CommandExecutor`, senza retry automatico delle mutazioni e con aggiornamenti locali seguiti da refresh mirato.
- Form account e conferma di ri-autenticazione vengono azzerati soltanto dopo una risposta confermata e restano disponibili in caso di errore.
- Le regole che impediscono auto-disabilitazione, auto-cambio ruolo e operazioni non autorizzate restano applicate prima dell'invio e sono coperte da test.
- `AdministrationExperience` compone account, audit, configurazione aziendale e monitoraggio con loading, empty state, errore, retry, dati precedenti durante il refresh e avvisi di aggiornamento non riuscito.
- `App.tsx` passa da 707 a 477 righe e non possiede piu stato o handler dei domini Account, Audit, Configurazione aziendale e Monitoraggio.
- Frontend completo: 179/179 test passati in 35 file; i 33 test netti aggiunti coprono segnali, permessi, caricamento lazy, cache, `401`, `403`, `500`, offline, risposte fuori ordine, comandi, protezioni account, conservazione dei form e composizione delle pagine.
- Suite mirata: 48/48 test passati nei tre file interessati; typecheck applicativo, typecheck E2E e build Vite di produzione completati senza errori.
- Backend completo: 262/262 test passati fuori dalla sandbox confinata; la suite include tutte le 31 migrazioni Flyway. I tre test Actuator richiedono il bind di una porta locale effimera, non consentito dalla sandbox.
- Nessuna modifica a backend, endpoint, payload, schema dati o migrazioni Flyway.
- F-25 e chiuso sul piano tecnico: tutte le sei vertical slice previste sono estratte e lo Step 3.5 e completato al 100% interno.

## Verifiche Step 3.5.5

- La quinta delle sei slice previste dallo Step 3.5 estrae query documenti, report vendite e report inventario, stato pagina, causale nota credito e comandi da `App.tsx`.
- `useDocumentReportFlow` applica debounce, cancellazione tramite `AbortSignal` e latest-request-wins ai documenti; i report vengono caricati solo alla prima apertura e restano disponibili tornando tra le viste.
- Emissione fattura e nota credito continuano a usare il `CommandExecutor`, applicano la risposta confermata localmente e attivano un refresh mirato senza retry automatico delle mutazioni.
- La causale della nota credito viene conservata quando il comando fallisce; gli export impediscono duplicati dello stesso formato, espongono lo stato occupato e distinguono successo ed errore.
- `DocumentReportExperience` compone loading, errore, retry, dati precedenti durante il refresh e pagine documenti/report senza duplicare stato nel workspace; i retry falliti sono intercettati senza rejection non gestite.
- `App.tsx` passa da 797 a 707 righe e non possiede piu stato o handler del dominio Documenti/Report.
- Frontend completo: 146/146 test passati in 33 file; i 19 test netti aggiunti coprono permessi, segnali, caricamento lazy, cache, `401`, `403`, `500`, offline, risposte fuori ordine, comandi, conservazione causale, export e composizione delle pagine.
- Suite mirata: 35/35 test passati nei quattro file interessati; typecheck applicativo, typecheck E2E e build Vite di produzione completati senza errori.
- Backend completo: 262/262 test passati; la verifica include tutte le 31 migrazioni Flyway.
- Nessuna modifica a backend, endpoint, payload, schema dati o migrazioni Flyway.
- F-25 resta aperto: lo Step 3.5 e completato all'83.3% interno e l'accettazione finale verra valutata soltanto dopo la slice account/audit.

## Verifiche Step 3.5.4

- La quarta delle sei slice previste dallo Step 3.5 estrae query, pagina, form, modifica e comandi partner da `App.tsx`.
- `usePartnerFlow` applica debounce, cancellazione con `AbortSignal` e latest-request-wins alla pagina anagrafiche e al lookup degli account cliente; `fetchAccounts` propaga il segnale su tutte le pagine richieste.
- Creazione, modifica, disattivazione e collegamento/scollegamento account mantengono il `CommandExecutor`, gli aggiornamenti locali e il refresh mirato senza retry automatico delle mutazioni.
- Il form viene azzerato soltanto dopo una risposta confermata e resta disponibile in caso di errore; una disattivazione confermata rimuove l'anagrafica dai risultati attivi.
- Il lookup account e caricato soltanto con `MANAGE_ACCOUNTS` e viene aggiornato tramite callback esplicita quando il dominio Account cambia ruolo o stato di un cliente.
- `PartnerExperience` compone loading, errore, retry, elenco e form senza duplicare stato nel workspace.
- `App.tsx` passa da 921 a 797 righe e non possiede piu stato o handler del dominio Partner.
- Frontend completo: 127/127 test passati in 31 file; aggiunti 14 test su permessi, segnali, `401`, `403`, `500`, offline, risposte fuori ordine, form, comandi, disattivazione e composizione della pagina.
- Typecheck e build Vite di produzione completati senza errori.
- Backend completo: 262/262 test passati fuori sandbox; la verifica include tutte le 31 migrazioni Flyway, ownership partner/account e architettura modulare.
- Nessuna modifica a backend, endpoint, payload, schema dati o migrazioni Flyway.
- F-25 resta aperto: lo Step 3.5 e completato al 66.7% interno e l'accettazione finale verra valutata soltanto dopo tutte le sei slice.

## Verifiche Step 3.5.3

- La terza delle sei slice previste dallo Step 3.5 estrae query ordini e clienti vendita, riconciliazione finanziaria, bozza/carrello e comandi di ordine, pagamento e reso da `App.tsx`.
- `useOrderFlow` applica debounce, cancellazione con `AbortSignal` e latest-request-wins alle query; i client ordini, anagrafiche vendita e riconciliazione propagano il segnale fino al trasporto HTTP.
- Creazione bozza, conferma, evasione, annullamento, ricevuta, richiesta/approvazione/rifiuto/ricezione reso e rimborso conservano il `CommandExecutor`, gli aggiornamenti locali e i refresh mirati senza retry automatico delle mutazioni.
- La bozza viene svuotata soltanto dopo una creazione confermata e resta disponibile in caso di errore; permessi e ownership cliente continuano a limitare le azioni esposte.
- `OrderExperience` compone vendita assistita, vendita cliente e gestione ordini; catalogo, inventario, dashboard e documenti restano dipendenze esplicite tramite dati e callback.
- `App.tsx` passa da 1.241 a 921 righe e non possiede piu stato o handler del dominio ordini/pagamenti/resi.
- Frontend completo: 113/113 test passati in 29 file; aggiunti 17 test su ruoli, `401`, `403`, `500`, offline, richieste fuori ordine, conservazione bozza, comandi, capability e composizione della pagina.
- Typecheck e build Vite di produzione completati senza errori.
- Backend completo: 262/262 test passati fuori sandbox; la prima esecuzione confinata aveva bloccato soltanto il bind delle porte effimere del test Actuator con `Operation not permitted`.
- Nessuna modifica a backend, endpoint, payload, schema dati o migrazioni Flyway.
- F-25 resta aperto: lo Step 3.5 e completato al 50% interno e l'accettazione finale verra valutata soltanto dopo tutte le sei slice.

## Verifiche Step 3.5.2

- La seconda delle sei slice previste dallo Step 3.5 estrae query, pagine staff/cliente, lookup, selezione, focus, modifica e comandi prodotto da `App.tsx`.
- `useCatalogFlow` usa query debounced e risorse annullabili con semantica latest-request-wins; il client prodotti propaga `AbortSignal` fino al trasporto HTTP.
- `CatalogExperience` compone loading, errore, retry, catalogo cliente, catalogo operativo, dettaglio e form, mantenendo magazzino e ordini come callback/dati espliciti.
- Creazione, modifica, eliminazione singola/multipla e disattivazione conservano la pipeline comandi esistente, gli aggiornamenti locali e il refresh mirato senza retry automatico delle mutazioni.
- `App.tsx` passa da 1.421 a 1.241 righe e non possiede piu stato o handler CRUD del catalogo.
- Frontend completo: 96/96 test passati in 27 file; aggiunti 11 test su staff/cliente, `401`, `403`, `500`, offline, risposta fuori ordine, creazione, fallimento, eliminazione e composizione della pagina.
- Typecheck e build Vite di produzione completati senza errori.
- Backend completo: 262/262 test passati; la verifica di non regressione include tutte le 31 migrazioni Flyway e il test di architettura modulare.
- Nessuna modifica a backend, endpoint, payload, schema dati o migrazioni Flyway.
- F-25 resta aperto: lo Step 3.5 e completato al 33.3% interno e l'accettazione finale verra valutata soltanto dopo tutte le sei slice.

## Verifiche Step 3.5.1

- La prima delle sei slice previste dallo Step 3.5 estrae autenticazione, registrazione, rinnovo, logout, cambio password e relativi form da `App.tsx`.
- `useSessionFlow` possiede stato form e command handler della sessione; `SessionEntry` e `SessionOverlays` compongono pagina di accesso e modali senza duplicare la logica nel workspace.
- I doppi submit concorrenti del login vengono bloccati prima della richiesta; logout remoto fallito pulisce comunque token e dati locali.
- Il rinnovo conserva il fallback controllato al login quando il token e gia scaduto, senza introdurre retry automatici sulle mutazioni applicative.
- Il cambio password chiude la sessione, azzera lo stato delle altre slice tramite callback esplicita e conserva il messaggio necessario al nuovo accesso.
- `App.tsx` passa da 1.531 a 1.421 righe; mantiene temporaneamente il reset aggregato delle altre slice, che saranno estratte una per volta nei cinque sotto-step successivi.
- Frontend completo: 85/85 test passati in 25 file; aggiunti 7 test su login, registrazione, errore rete, doppio invio, rinnovo, cambio password, logout e cablaggio pagina/modali.
- Typecheck e build Vite di produzione completati senza errori.
- Backend completo: 262/262 test passati; la verifica di non regressione include tutte le 31 migrazioni Flyway e il test di architettura modulare.
- Nessuna modifica a backend, API, schema dati o migrazioni Flyway.
- F-25 resta aperto: lo Step 3.5 e completato al 16.7% interno e l'accettazione finale verra valutata soltanto dopo tutte le sei slice.

## Verifiche Step 3.4

- `SessionProvider` e l'unico proprietario frontend di identita autenticata, token, scadenza, avviso e invalidazione centralizzata della sessione.
- Un `401` ricevuto con token attivo notifica il provider e apre il rinnovo; i `401` pubblici del login restano errori di autenticazione ordinari.
- Il pilot Account usa `usePaginatedResource` con stati `loading`, `refreshing`, `error` e `data`, refresh esplicito e aggiornamento locale della risposta alle mutazioni.
- Filtri account sottoposti a debounce di 300 ms; ogni nuova query annulla la precedente tramite `AbortController` e una sequenza monotona garantisce che soltanto l'ultima risposta possa aggiornare lo stato.
- Errori `401`, `403`, `500`, offline e risposte fuori ordine sono coperti da test; le promise delle query vengono gestite internamente senza rejection non intercettate.
- Le mutazioni non applicano retry automatici ambigui e continuano a usare l'esecutore dei comandi e le chiavi di idempotenza introdotti negli step precedenti.
- Suite backend completa eseguita fuori sandbox: 262/262 test passati, inclusi i tre test Actuator su porte locali effimere.
- Frontend: 78/78 test passati in 23 file con il comando standard `npm test`; typecheck e build Vite di produzione completati.
- Nessuna modifica a backend, API, schema dati o migrazioni Flyway.
- F-23 e chiuso sul piano tecnico per il dominio pilota Account. L'estensione alle altre vertical slice avverra durante la decomposizione progressiva, senza anticipare lo Step 3.5.

## Verifiche Step 3.3

- Il login accetta soltanto username e password: il ruolo viene derivato lato server dall'account autenticato e un eventuale campo legacy `role` non puo elevare i privilegi.
- Il lifecycle account e non distruttivo: disabilitazione e riabilitazione preservano storico, ownership e audit; username e identita canonica restano riservati e non possono essere ricreati dopo la disabilitazione.
- Un account disabilitato non puo autenticarsi e le sessioni gia emesse vengono invalidate tramite incremento della versione credenziali e revoca server-side.
- Il cambio password personale richiede la password corrente; reset amministrativo, cambio ruolo, disabilitazione, riabilitazione e revoca globale delle sessioni applicano autorizzazioni backend, ri-autenticazione e audit.
- Un amministratore gestisce dipendenti e clienti; soltanto il super admin gestisce account amministrativi. L'auto-disabilitazione e la gestione del super admin sono bloccate.
- La migrazione Flyway `V31__account_lifecycle.sql` introduce stato, autore, motivo e timestamp di disabilitazione, con vincolo di coerenza e indice operativo; gli account storici restano abilitati senza perdita di dati.
- Il lockout login resta temporaneo, atomico e protetto da lock pessimista con retry limitato; lo stato account permanente non viene confuso con il blocco anti-abuso.
- Suite backend standard eseguita nel repository reale: 262/262 test passati, inclusi i tre test Actuator su porte locali effimere.
- PostgreSQL 16 eseguito nel repository reale: 95/95 test passati con tutte le migrazioni V1-V31; upgrade popolati V14, V16 e V18 completati fino a V31 con checksum e assertion lifecycle verdi.
- Frontend eseguito nel repository reale: 64/64 test passati in 20 file; typecheck e build Vite di produzione completati.
- WB-003 e `IMPLEMENTED`; la prova guidata con operatore reale resta `PENDING_OPERATOR_EXECUTION`. Il recupero pubblico password resta intenzionalmente rinviato finche non esiste un canale verificato.
- F-31A e chiuso sul piano tecnico. Lo Step 3.4 resta intenzionalmente non iniziato.

## Verifiche Step 3.2

- `CUSTOMER` usa `/api/customer/catalog`: la risposta espone soltanto dati commerciali e i livelli `AVAILABLE`, `LIMITED` e `UNAVAILABLE`; ID, stock fisico, riserve, disponibilita numerica, stato interno e valore potenziale di vendita sono assenti dal JSON.
- I prodotti disattivati non compaiono nella projection cliente. L'ordinamento per quantita non e disponibile e il carrello non riceve un massimo numerico ricavato dallo stock.
- `/api/customer/dashboard` usa l'ID account stabile della sessione e restituisce conteggi per stato e ordini recenti propri, senza KPI globali, ricavi, catalogo o movimenti di magazzino.
- Gli endpoint operativi `/api/products` e `/api/dashboard` restituiscono `403` a `CUSTOMER`; personale e cliente non possono scambiarsi le projection dedicate.
- Suite backend completa fuori sandbox: 258/258 test passati, inclusi i tre test Actuator su porte locali effimere e i contract test per forma risposta, ownership e matrice ruoli.
- Frontend: 61/61 test passati in 18 file; typecheck e build Vite di produzione completati.
- Nessuna migrazione Flyway necessaria: lo step separa autorizzazioni, query, DTO, client e presentazione senza cambiare lo schema dati.
- WB-002 e marcato `IMPLEMENTED`; la prova guidata con un operatore reale resta `PENDING_OPERATOR_EXECUTION` e non viene dichiarata completata.
- F-14 e chiuso sul piano tecnico. Lo Step 3.3 resta intenzionalmente non iniziato.

## Verifiche Step 3.1

- `Gestione catalogo` e `Vendita` sono flussi separati: il catalogo mantiene le operazioni anagrafiche, mentre la vendita espone ricerca prodotti, carrello, destinatario e creazione della sola bozza.
- Il personale seleziona un cliente censito tramite ID stabile del `BusinessPartner` oppure dichiara esplicitamente un cliente occasionale; non vengono create anagrafiche fittizie.
- Un account `CUSTOMER` puo creare esclusivamente ordini self-service associati alla propria identita stabile e non puo indicare o impersonare un altro cliente.
- Prezzo scontato e descrizione prodotto vengono congelati nella riga ordine; le modifiche successive del catalogo non alterano lo storico della bozza.
- La creazione assistita produce audit `CREATE_ASSISTED_ORDER` con operatore autenticato; conferma, riserva stock, evasione e pagamento restano operazioni indipendenti.
- Errori di validazione, stock insufficiente, rete o server non svuotano il carrello; soltanto la risposta confermata azzera la bozza locale. L'idempotency key protegge retry e doppio invio concorrente.
- Migrazione Flyway `V30__assisted_sales_customer_and_line_snapshots.sql`: tipo cliente esplicito, snapshot descrizione riga, vincoli e indice. I record storici restano `LEGACY_UNRESOLVED` senza inferenze non verificabili.
- Suite backend completa fuori sandbox: 251/251 test passati, inclusi 4 test del workflow assistito, 11 test di idempotenza e 3 test Actuator su porte locali effimere.
- PostgreSQL reale: checksum fixture verdi; snapshot V14/V16/V18 e upgrade popolati V14, V16 e V18 completati fino a V30.
- Frontend: 56/56 test passati in 16 file; typecheck e build Vite di produzione completati.
- F-15 e chiuso sul piano tecnico. La prova guidata con un operatore reale resta un'evidenza operativa separata e deve produrre l'esito `OPERATOR_VALIDATED` oppure `REJECTED`; non viene sostituita dal Gate C automatico.

## Verifiche Step 3.0

- `WORKFLOW_BENCHMARKS.md` introduce il template obbligatorio in sei parti, gli esiti decisionali e il divieto esplicito di copiare prodotti terzi.
- WB-001 confronta fonti ufficiali Odoo, ERPNext e Business Central e traduce le osservazioni negli invarianti gia presenti nel monolite.
- La vendita assistita futura mantiene cliente identificato stabilmente, cliente occasionale esplicito, bozza separata da conferma/evasione/pagamento, stock autorevole, audit e idempotenza.
- Il protocollo operatore definisce dati sintetici, sette scenari, criteri di esito ed evidenze; nello Step 3.0 era predisposto per la successiva implementazione e ora resta correttamente marcato `PENDING_OPERATOR_EXECUTION`.
- Tre test contrattuali statici proteggono template, fonti, invarianti, anti-copia e registro delle schede future.
- Suite backend completa fuori sandbox: 246/246 test passati, inclusi i tre test Actuator su porte locali effimere. Il primo tentativo confinato aveva prodotto soltanto tre `SocketException: Operation not permitted` ambientali.
- Frontend invariato verificato con installazione pulita: 52/52 test, typecheck applicativo, typecheck E2E e build Vite di produzione completati.
- Nessun codice applicativo, contratto API o migrazione Flyway e stato modificato.

## Verifiche Step 2.9

- Il profilo Maven `postgresql-it` usa Testcontainers con PostgreSQL 16, applica V1-V29 e mantiene Hibernate in `validate`; 95/95 test critici sono passati sul database reale.
- La matrice PostgreSQL copre sessioni, lockout concorrente, identita canoniche, idempotenza, stock, ledger, numerazioni, annullamenti, pagamenti, riconciliazione e autorizzazioni negative.
- I tentativi login concorrenti usano lock pessimista, retry limitato e timestamp monotoni; richieste completate fuori ordine non possono violare la cronologia del blocco.
- Gli upgrade popolati V14, V16 e V18 arrivano a V29 dopo una riconciliazione canonica esplicita e verificano storico ordini, ledger, idempotenza, annullamenti e mismatch finanziari intenzionali.
- La migrazione V29 ripara in modo conservativo i claim legacy `IN_PROGRESS` privi di token o lease e introduce il vincolo che ne impedisce la ricomparsa.
- Il job CI `Database migrations` esegue il profilo `fast` su push e pull request e il profilo `nightly` settimanale con fixture volumetrica `large`.
- Suite backend completa fuori sandbox: 243/243 test passati, inclusi i test Actuator su porte locali effimere.
- Frontend: 52/52 test passati in 15 file; typecheck e build Vite di produzione completati.
- Least privilege PostgreSQL verificato dopo V29: runtime DML senza DDL, backup read-only, migrator owner-scoped, restore dedicato e rotazione credenziali.
- F-21 e F-28 erano chiusi al termine dello Step 2.9; in quel momento F-31 restava aperto fino al gate E2E MVP dello Step 3.10.

## Verifiche Step 2.8

- PostgreSQL usa cinque identita distinte: owner `NOLOGIN`, migrator con `SET ROLE` controllato, runtime DML-only, backup read-only e restore dedicato con `CREATEDB` limitato al recovery operativo.
- L'utente runtime non puo eseguire `CREATE`, `ALTER` o `DROP`; il test applicativo di riconciliazione finanziaria usa la stessa identita runtime dopo l'applicazione di V1-V27 tramite migrator e owner.
- Backup e restore non riusano le credenziali applicative. La prova di restore ricrea un database isolato, verifica i dati e completa il cleanup.
- La rotazione aggiorna separatamente le credenziali dei ruoli e dimostra che la password precedente viene rifiutata.
- La porta PostgreSQL pubblicata dai profili locali e prod-like e vincolata al loopback; la documentazione prescrive di rimuoverla nel deployment reale.
- Suite backend sequenziale fuori sandbox: 239/239 test passati, inclusi i test Actuator su porte locali effimere.
- Frontend: 52/52 test passati in 15 file; typecheck e build Vite di produzione completati.
- Stack prod-like completo: 27 migrazioni Flyway validate, 3/3 smoke Playwright, rate limiting login e registrazione, hardening container, CSP, metriche, backup/restore e least privilege verificati.
- Il runner prod-like ha completato il cleanup delle risorse isolate e ha terminato con exit code 0.
- F-12 e chiuso. Lo Step 2.9 resta intenzionalmente non iniziato.

## Verifiche Step 2.7

- `payment_transactions` e la fonte finanziaria autorevole per incassi, rimborsi, reversal e riconciliazioni; le proiezioni di pagamento e reso vengono ricalcolate dai movimenti.
- Ogni rimborso e collegato al reso tramite foreign key; codice reso e metadati restano snapshot descrittivi e non sostituiscono l'identita relazionale.
- La migrazione `V27` crea un solo movimento `RECONCILIATION` per i pagamenti storici gia verificati con importo positivo, senza cambiare o inventare importi.
- Vincoli database impediscono collegamenti incompatibili, rimborsi senza reso, riconciliazioni duplicate e cronologie di pagamento o reso impossibili.
- `GET /api/financial-reconciliation` e il job programmato rilevano drift di importi, stati, collegamenti e timestamp senza correggere silenziosamente il ledger.
- Metriche Prometheus a cardinalita finita espongono anomalie correnti, totale rilevato, ultimo successo ed esito delle esecuzioni.
- La UI mostra il report agli utenti con `VIEW_REPORTS` e distingue chiaramente stato coerente e anomalie operative.
- Test backend sequenziale fuori sandbox: 238/238 passati, incluse tre prove Actuator su porte locali effimere.
- PostgreSQL 16.14: 22/22 test passati su V1-V27, migrazione, vincoli negativi, rollback e rimborsi concorrenti; container effimero rimosso.
- Frontend: 52/52 test in 15 file, typecheck applicativo ed E2E e build Vite di produzione completati.
- F-22 e chiuso. Le evidenze storiche mai registrate restano intenzionalmente soggette a riconciliazione umana; il sistema non formula conclusioni finanziarie false.

## Verifiche Step 2.6

- `POST /api/orders/{code}/cancel` accetta causale obbligatoria e riferimento contabile obbligatorio quando esiste un incasso netto.
- L'annullamento di un ordine incassato registra una sola transazione immutabile `REVERSAL` per l'intero netto, porta il pagamento a `REFUNDED`, azzera il netto e collega lo storno all'ordine.
- Bozze e ordini confermati senza incassi restano annullabili; il pagamento pendente passa a `CANCELED` e le riserve dell'ordine confermato vengono liberate.
- Stato ordine, pagamento, reversal, scorte e audit sono modificati nella stessa transazione; un errore durante il rilascio di una riga ripristina anche gli aggiornamenti gia eseguiti.
- Lock pessimista sull'ordine, vincolo univoco sul reversal e `Idempotency-Key` impediscono doppio annullo in concorrenza o dopo risposta persa.
- La migrazione `V26` conserva gli annullamenti storici con metadati espliciti e impedisce reversal senza ordine/riferimento, piu reversal per ordine e metadati di annullamento su ordini attivi.
- La UI operativa mostra importo da stornare, riferimento, causale, attore e storico; il comando usa la response autorevole senza richiedere conferme generiche.
- Suite backend completa: 232 test passati fuori dalla sandbox, inclusi i test Actuator su porte locali effimere.
- PostgreSQL 16.14: 18 test mirati passati sulle 26 migrazioni, inclusi acconto, saldo, rollback, due operatori concorrenti e replay idempotente; container effimero rimosso.
- Frontend: 51 test in 14 file, typecheck applicativo e build Vite di produzione completati.
- F-08 e chiuso. F-22 resta aperto fino alla riconciliazione finanziaria dello Step 2.7.

## Verifiche Step 2.5

- Il nuovo hook `useCommandExecution` separa gli esiti `saved`, `saved-refresh-failed`, `failed` e `duplicate`, evitando di confondere il risultato del comando con quello del refresh successivo.
- La risposta autorevole della mutazione viene applicata immediatamente allo stato locale; il refresh successivo interessa soltanto pagine, lookup e aggregazioni dipendenti dall'entita modificata.
- Se il comando e confermato ma la presentazione locale o il refresh falliscono, l'interfaccia comunica esplicitamente che l'operazione e stata salvata e invita ad aggiornare i dati senza ripeterla.
- Form prodotto, dati account, movimenti e carrello vengono azzerati soltanto dopo una risposta confermata; timeout ed errori di rete conservano i dati inseriti per un retry sicuro.
- Chiavi attive per operazione e disabilitazione dei controlli di mutazione impediscono doppi invii concorrenti senza bloccare link o funzioni di sola lettura.
- Test frontend dedicati: comando `200` seguito da refresh `500`, timeout del comando, retry dopo timeout e doppio click con una sola richiesta; aggiunte anche regressioni sullo stato paginato e sulla preservazione/reset dei form.
- Suite frontend completa: 50 test passati in 14 file; typecheck applicativo e build Vite di produzione completati.
- Suite backend completa: 224 test passati fuori dalla sandbox, inclusi i tre test Actuator che richiedono porte HTTP locali effimere. Il primo tentativo confinato aveva prodotto soltanto tre `SocketException: Operation not permitted` ambientali.
- Nessuna API, migrazione Flyway o regola di dominio e stata modificata in questo step.
- Il finding F-13 e chiuso; F-30 resta aperto fino allo step dedicato alla gestione trasversale degli errori.

## Verifiche Step 2.4

- La migrazione `V25` trasforma i record idempotenti in claim durevoli con account ID stabile, fingerprint versionato, token di ownership, lease, scadenza e stati `IN_PROGRESS`, `COMPLETED` e `FAILED_RETRYABLE`.
- Il vincolo univoco su account, operazione e chiave assegna atomicamente un solo esecutore; i concorrenti osservano lo stato senza attendere il lock della mutazione.
- Mutazione business e risposta serializzata vengono confermate nella stessa transazione, evitando una modifica senza replay registrato dopo un crash tra i due commit.
- La stessa chiave e lo stesso payload restituiscono la risposta completata; una chiave riusata con payload diverso produce `409 IDEMPOTENCY_CONFLICT`.
- Un claim ancora attivo oltre l'attesa breve produce `409 IDEMPOTENCY_IN_PROGRESS` e `Retry-After`; un claim scaduto o fallito in modo ripetibile puo essere riacquisito in sicurezza.
- Il frontend genera chiavi per intento e conserva la stessa chiave dopo timeout, errore di rete, errore server, risposta persa o claim ancora attivo; successo, errore definitivo o payload cambiato chiudono l'intento.
- Cleanup programmato e TTL frontend eliminano record e intenti scaduti dopo la retention predefinita di 24 ore.
- Test H2 mirati: 10 casi controller e 1 upgrade V24-V25, inclusi doppio click, due thread, timeout, retry dopo errore, replay e payload diverso.
- Test PostgreSQL 16.14: 10 casi passati sulle 25 migrazioni, inclusa la concorrenza reale con una sola mutazione; container effimero rimosso senza volumi persistenti.
- Suite backend completa: 224 test passati con Java 23 e bytecode Java 17, inclusi i test Actuator su porte locali effimere.
- Frontend: 42 test in 12 file, typecheck E2E e build Vite di produzione completati.
- Il finding F-31B e chiuso; F-13 e stato successivamente chiuso dallo Step 2.5 e F-30 resta aperto fino al relativo step di gestione errori.

## Verifiche Step 2.3

- La migrazione `V24` rende i movimenti di magazzino un ledger autorevole collegato al prodotto tramite ID stabile, con delta firmato, origine, snapshot prima/dopo e vincoli di coerenza.
- I movimenti precedenti a `V24` restano osservazioni legacy non autorevoli; ogni prodotto storico riceve una baseline di migrazione esplicitamente non verificata, senza inventare evidenze operative.
- L'API anagrafica prodotto non accetta piu la quantita: ogni nuovo prodotto nasce a giacenza zero e lo stock varia solo tramite saldo iniziale, rettifica, carico/scarico, evasione o reso.
- Il saldo iniziale e unico per prodotto; rettifiche e movimenti richiedono causale, rispettano lo stock riservato e scrivono ledger e proiezione nella stessa transazione.
- Evasioni e resi registrano movimenti con origine esplicita; cancellare un prodotto con giacenza, riserve o riferimenti ordine viene rifiutato.
- Il report di riconciliazione classifica `BALANCED`, `MISSING_INITIAL_BALANCE`, `UNVERIFIED_INITIAL_BALANCE`, `CHAIN_BROKEN` e `LEDGER_DRIFT` senza modificare automaticamente lo storico.
- Test dedicati H2: saldo iniziale, catena e somma del ledger, drift, baseline migrata, rettifiche sotto riserva, concorrenza, endpoint protetti e payload prodotto incapace di variare la quantita.
- Test dedicati PostgreSQL 16.14: 9 passati su 24 migrazioni, baseline storica, vincoli, saldo iniziale, rettifiche concorrenti e protezione delle riserve; container effimero rimosso.
- Suite backend completa: 219 test passati con Java 23 e bytecode Java 17, inclusi i test Actuator su porte locali effimere.
- Frontend: 37 test in 12 file e build Vite di produzione completati; form prodotto e pagina inventario seguono il nuovo contratto.
- Il finding F-05 e chiuso; F-21 resta aperto fino alla suite PostgreSQL trasversale obbligatoria dello Step 2.9.
- Rischio residuo: le baseline migrate devono essere riconciliate manualmente contro evidenze reali prima di poter essere considerate verificate.

## Verifiche Step 2.2

- Username, codice prodotto e codice anagrafica conservano un valore visuale separato e usano `lower(trim(valore))` come identita canonica.
- La migrazione `V23` valorizza le colonne canoniche storiche e applica vincoli `NOT NULL`, `CHECK` e `UNIQUE`; eventuali collisioni interrompono l'upgrade senza rinominare o fondere dati automaticamente.
- Il preflight `scripts/db/preflight-canonical-identifiers.sql` individua le collisioni prima dell'upgrade; la procedura manuale e documentata in `docs/CANONICAL_IDENTIFIERS.md`.
- Creazione e modifica non dipendono da pre-check per l'unicita: il database decide anche sotto concorrenza e i vincoli canonici noti vengono tradotti in `409 RESOURCE_CONFLICT` con messaggi di dominio stabili.
- Test dedicati H2: 7 passati su valori visuali, lookup case/space-insensitive, collisioni sequenziali e concorrenti, risposta HTTP 409, upgrade pulito e arresto su storico ambiguo.
- Test dedicati PostgreSQL 16: 5 passati su account, prodotti e anagrafiche, inclusi tre inserimenti concorrenti; tutte le 23 migrazioni applicate e container effimero rimosso.
- Suite backend completa: 211 test passati con Java 23 e bytecode Java 17, inclusi i test Actuator su porte locali effimere.
- Frontend: 34 test in 11 file e build Vite di produzione completati.
- Il finding F-11 e chiuso; F-21 resta aperto fino alla suite PostgreSQL trasversale obbligatoria dello Step 2.9.

## Verifiche Step 2.1

- La nuova migrazione `V22` corregge gli esiti di V15 e V17 senza alterare le migration distribuite: i contatori documentali mancanti o arretrati partono da `max(sequence_number) + 1` per tipo e anno fiscale.
- I pagamenti storici senza transazioni o altra evidenza verificabile passano da un falso `PENDING` a `UNRECONCILED`; importi pagati, rimborsati e residui restano intenzionalmente non dichiarati finche non vengono riconciliati.
- Incassi e report economici non trattano gli ordini `UNRECONCILED` come insoluti; il frontend mostra “Da verificare” e blocca le azioni finanziarie incompatibili.
- La riconciliazione manuale richiede `SUPER_ADMIN`, ri-autenticazione, `Idempotency-Key`, riferimento e causale; produce metadata persistenti e audit critico.
- Preflight PostgreSQL rileva collisioni di codice/tupla prima dell'upgrade; l'allocatore serializzato salta eventuali codici storici gia presenti e il primo documento successivo viene verificato automaticamente.
- Upgrade PostgreSQL 16 verificati: V14-V22 163 ms, V16-V22 147 ms, V18-V22 131 ms; V18-V22 medium con 5.005 ordini 165 ms e large con 50.005 ordini 588 ms.
- Test PostgreSQL applicativi su pagamenti e numerazione concorrente: 12 passati; fixture, preflight, primo documento e cleanup completati senza volumi persistenti.
- Suite backend completa: 204 test passati con Java 23 e bytecode Java 17; JAR Spring Boot generato.
- Frontend: 34 test in 11 file, typecheck applicativo/E2E e build Vite completati.
- I finding F-09 e F-10 sono chiusi; F-21 resta aperto fino alla suite PostgreSQL trasversale obbligatoria dello Step 2.9.

## Verifiche Step 1.6

- Backend aggiornato a Spring Boot `4.1.0`, Spring Security `7.1.0` e Spring Modulith `2.1.0`; il controllo runtime conferma una linea non affetta da CVE-2026-22732.
- Il primo gate OWASP ha bloccato correttamente Tomcat `11.0.22` e pgJDBC `42.7.11`; la baseline finale usa Tomcat `11.0.24`, pgJDBC `42.7.12` e Log4j `2.25.5`.
- OWASP Dependency-Check `12.2.2` ha analizzato 66 dipendenze: 0 dipendenze vulnerabili e 0 vulnerabilita nel report HTML/JSON finale, con soglia fail-closed CVSS `7.0`.
- Nessun profilo applicativo viene attivato implicitamente; il JAR senza profilo e il profilo `prod` senza segreti terminano entrambi in modo fail-closed.
- Il bootstrap del super admin e disabilitato per default e rifiuta le credenziali locali note in ogni profilo senza conservarne la password in chiaro nel codice produttivo.
- H2 e confinato allo scope test; la security chain non contiene bypass `/h2-console` e applica `X-Frame-Options: DENY`.
- Header di sicurezza verificati su risposte 2xx, 4xx e 5xx; i contratti runtime/configurazione aggiungono 7 test dedicati.
- Suite backend completa dopo gli aggiornamenti: 201 test passati con Java 23 e bytecode Java 17; JAR Spring Boot generato.
- Frontend confermato: 33 test in 11 file, typecheck applicativo/E2E e build Vite completati; `npm ci` non ha rilevato vulnerabilita.
- I finding F-04, F-16 e F-33 sono chiusi; tutti i criteri del Gate A risultano soddisfatti.

## Verifiche Step 1.5

- I runner prod-like ed E2E generano project name casuali con prefissi rispettivamente `gestionale-prodlike-` e `gestionale-e2e-` e rifiutano collisioni prima di creare risorse.
- Container, volumi e rete ricevono sia la label Compose del progetto sia il run ID univoco; l'inventario viene salvato nella diagnostica.
- Il cleanup non usa `docker compose down` e rimuove soltanto ID inventariati dopo avere verificato l'ownership di tutte le risorse associate al progetto.
- Se una risorsa del progetto e priva del run ID atteso, il cleanup si arresta senza rimuovere alcuna risorsa.
- I trap gestiscono `HUP`, `INT` e `TERM`; un `SIGKILL` puo lasciare risorse etichettate, recuperabili con `scripts/ci/cleanup-docker-run.sh <project> <run-id>`.
- Test runtime Docker superato con volume sentinella persistente, collisione project name, interruzioni nelle fasi preflight/rete/volume/container e recovery dopo `SIGKILL`.
- Contratti backend Docker e workflow: 9 test passati.
- Suite backend completa: 194 test passati.
- Gate prod-like completo: 21 migrazioni Flyway, hardening, secret, CSP, osservabilita, rate limit login/registrazione, backup/restore e 3 smoke test Playwright completati.
- Cleanup finale verificato: nessun container, volume o rete residuo per il project/run ID della prova.
- Il job prod-like e nuovamente attivo in CI ed e una dipendenza obbligatoria del quality gate.
- Il finding F-03 e chiuso; non restano finding P0 aperti.

## Verifiche Step 1.4

- La migrazione Flyway `V21` aggiunge agli ordini `customer_account_id`, `partner_id` e lo stato esplicito `ACCOUNT`, `PARTNER` o `UNRESOLVED`.
- Nome cliente e codice anagrafica restano snapshot descrittivi e non partecipano piu alle decisioni di autorizzazione.
- Gli ordini storici vengono collegati soltanto quando `customer_code` identifica deterministicamente un'anagrafica; nessun nome o username viene usato per inferire l'ownership.
- Gli ordini storici ambigui restano `UNRESOLVED` e non sono accessibili ai clienti.
- Le anagrafiche cliente possono essere associate esplicitamente a un solo account `CUSTOMER`; il frontend amministrativo usa l'ID account e mostra lo stato del collegamento.
- Creazione, lista, dettaglio, annullamento e richiesta di reso del cliente usano l'ID stabile della sessione; rinominare username o display name non trasferisce ordini.
- Eliminare un account scollega le foreign key tramite `ON DELETE SET NULL`; ricreare lo stesso username non eredita ordini precedenti.
- Test H2 dedicati a migrazione, omonimie, rename, account non collegato, display name duplicati e transizioni: 9 passati.
- Test PostgreSQL 16 dedicati all'ownership e al contratto API: 8 passati tramite runner isolato e cleanup completato.
- Fixture PostgreSQL 16 profilo `small`: snapshot V14, V16, V18, V19, V20 e V21 caricati e validati con checksum corretti.
- Suite backend completa: 188 test passati, inclusi i test Actuator su porte locali effimere.
- Suite frontend completa: 33 test passati; typecheck applicativo/E2E e build Vite completati.
- La prova PostgreSQL ha rilevato una transazione sessione erroneamente read-only durante un lock pessimista; il confine transazionale e stato corretto e coperto da regressione.
- Nessun runner prod-like o Playwright E2E eseguito prima dello Step 1.5.
- Il finding F-02 e chiuso; F-03 resta l'unico P0 aperto.

## Verifiche Step 1.3

- La migrazione Flyway `V20` aggiunge `credential_version` agli account e collega ogni sessione tramite `account_id` con foreign key `ON DELETE CASCADE`.
- Lo username resta uno snapshot diagnostico; autenticazione, conteggi e revoca usano l'ID stabile dell'account.
- Le sessioni legacy vengono revocate intenzionalmente durante la migrazione per evitare che token emessi con il subject precedente sopravvivano al cambio di identita.
- Cambio password e cambio ruolo incrementano la versione credenziali e revocano tutte le sessioni dell'account.
- Eliminare e ricreare lo stesso username non rende nuovamente valido alcun vecchio token.
- Logout, rotazione e aggiornamento dell'ultima attivita acquisiscono un lock pessimista sulla sessione; revoca e rotazione prevalgono su touch concorrenti.
- Test H2 dedicati al subject stabile: 6 passati.
- Test PostgreSQL 16 dedicati alla concorrenza delle sessioni: 6 passati tramite runner isolato, senza porte host o volumi persistenti e con cleanup completato.
- Fixture PostgreSQL 16 profilo `small`: snapshot V14, V16, V18, V19 e V20 caricati e validati con checksum corretti.
- Suite backend completa: 178 test passati con Java 23 e compilazione `release 17`.
- Suite frontend completa: 32 test passati; typecheck applicativo/E2E e build Vite completati.
- Nessun runner prod-like o Playwright E2E eseguito prima dello Step 1.5.
- I finding F-06 e F-07 sono chiusi per il perimetro applicativo dello step; la suite PostgreSQL autorevole estesa resta pianificata nello Step 2.9.

## Verifiche Step 1.2

- La migrazione Flyway `V19` aggiunge provenienza e stato di verifica operativa agli account senza eliminare o declassificare dati storici.
- Le registrazioni self-service `CUSTOMER` sono censite separatamente dagli account `EMPLOYEE` sospetti; account admin, super admin, bootstrap e staff creato dal pannello amministrativo sono preservati.
- Gli account operativi self-service o di provenienza non dimostrabile restano in quarantena fino a revisione manuale; login e sessioni esistenti vengono rifiutati.
- Prima della revoca la migrazione registra un audit critico `QUARANTINE_OPERATIONAL_ACCOUNT`, preservando l'evidenza della decisione.
- Il report super admin espone provenienza, classificazione, sessioni emesse/potenzialmente attive e conteggi audit per magazzino, ordini, pagamenti, resi e documenti.
- La verifica manuale richiede sessione `SUPER_ADMIN` e ri-autenticazione; abilita soltanto i login futuri e non riattiva i token precedentemente revocati.
- Un test inizialmente fallito ha rilevato che l'eccezione `401` annullava la revoca transazionale; il confine transazionale e stato corretto e la revoca ora resta persistente.
- Test mirati finali: 5 test passati su servizio, autorizzazione controller e migrazione incrementale V18-V19.
- Suite backend completa: 171 test passati con Java 23 e compilazione `release 17`.
- Verificatore PostgreSQL 16 con profilo `small`: snapshot V14, V16, V18 e V19 caricati e validati; checksum verificati e container effimero rimosso.
- Nessuna modifica frontend richiesta dal contratto dello step; nessun runner prod-like o E2E eseguito prima dello Step 1.5.
- Il finding F-01 e chiuso. F-07 e stato successivamente chiuso dallo Step 1.3.

## Verifiche Step 1.1

- Contratto pubblico ridotto a `username` e `password`; qualsiasi campo `role` valido viene rifiutato con `422 VALIDATION_FAILED`.
- Il servizio self-service assegna sempre `CUSTOMER` e registra l'audit con attore `SELF_SERVICE`.
- Gli account `EMPLOYEE` e `ADMIN` restano creabili soltanto dall'endpoint amministrativo autenticato e ri-autenticato.
- Il frontend non mostra il selettore ruolo durante la registrazione e accede automaticamente come cliente dopo la creazione.
- Nginx dispone di una zona rate limit dedicata alla registrazione, configurabile separatamente dal login.
- Test di regressione mirati inizialmente falliti in modo deterministico sul comportamento vulnerabile: 5 errori backend su 6 casi e 2 errori frontend su 5 casi.
- Test backend mirati finali: 6 test passati; fixture legacy coinvolte: 37 test passati.
- Suite backend completa: 166 test passati con Java 23 e compilazione `release 17`.
- Suite frontend completa: 32 test passati; typecheck applicativo ed E2E e build Vite completati.
- Sintassi di entrypoint e verificatori shell valida; `git diff --check` completato senza errori.
- Nessuna migrazione Flyway necessaria: lo step non modifica lo schema dati.
- Il test runtime del rate limit e lo smoke Playwright aggiornato sono predisposti ma non eseguiti, per rispettare la stop condition precedente allo Step 1.5.
- Il finding F-01 resta formalmente aperto fino al censimento degli account storici e alla revoca delle sessioni nello Step 1.2.

## Verifiche Step 0.3

- Fixture incrementali e completamente sintetiche create per gli schemi V14, V16 e V18.
- Stati ordine, pagamenti, resi, documenti, collisioni case-insensitive, ownership storica, drift magazzino e riconciliazione finanziaria coperti dal manifest.
- Profili deterministici disponibili: `small` con 20 prodotti, 10 anagrafiche e 40 ordini; `medium` con 500, 250 e 5.000; `large` con 5.000, 2.500 e 50.000.
- Checksum SHA-256 verificati con successo su tutti gli 11 file dichiarati.
- Contratto Java `HistoricalFixtureContractTest`: 5 test passati.
- Verificatore PostgreSQL 16 eseguito con profilo `small`: snapshot V14, V16 e V18 caricati e validati su database puliti.
- Conteggi verificati per ogni snapshot: 25 prodotti e 45 ordini, inclusi i record mirati aggiunti ai volumi del profilo.
- Container PostgreSQL effimero eseguito senza porte host o volumi persistenti e rimosso dopo il controllo della label univoca; nessuna risorsa residua.
- Suite backend completa: 160 test passati con Java 23 e compilazione `release 17`.
- Nessun runner prod-like o E2E eseguito e nessuno stack Docker esistente modificato.
- I criteri tecnici del Gate 0 sono soddisfatti localmente; la pubblicazione Git della patch resta separata e non autorizzata.

## Verifiche Step 0.2

- Freeze e politica delle eccezioni formalizzati in `docs/PRODUCT_SCOPE.md`.
- Target MVP verificato contro le assunzioni vincolanti di `MASTER_SOURCE_AGENTE_AI.md`.
- Responsabilita su stock, pagamenti e storico assegnata esplicitamente.
- Tutti i 46 step residui riclassificati in stabilizzazione, MVP e dopo MVP in `docs/ROADMAP.md`.
- Stato pubblico del progetto corretto da “MVP avanzato” a “prototipo avanzato con P0” nel `README.md`.
- Nessuna modifica a codice, database, API, dipendenze o runtime.
- Nessun runner prod-like/E2E eseguito prima dello Step 1.5.

## Verifiche Step 0.1

- Branch locale verificata: `miglioramenti-gestionale`.
- Scansione Gitleaks cronologia: 3 commit, nessuna rilevazione.
- Scansione Gitleaks baseline candidata: circa 1,69 MB, nessuna rilevazione.
- File sensibili o artefatti locali candidati: nessuno; sono presenti soltanto template `.env.example` e migrazioni Flyway intenzionali.
- Baseline pubblicata su `origin/miglioramenti-gestionale`: commit `9f9e23a`.
- Workflow CI pubblicato tramite sessione GitHub autenticata: commit `945fcd0`.
- Workflow security pubblicato tramite sessione GitHub autenticata: commit `62a8d14`.
- Workflow CI corretto per separare il gate migrazioni e sospendere l'esecuzione prod-like fino allo Step 1.5: commit `fa52524`.
- Alert CodeQL corretti senza dismiss manuali: identificatori casuali generati con CSPRNG e CSRF abilitato per default con esclusioni esplicite per API stateless: commit `8d9be88`.
- I commit locali originali sono preservati nelle branch `baseline-pre-publication-20260727` e `miglioramenti-gestionale-pre-publication`.
- Clone pulito creato dal repository reale e verificato senza file esterni non documentati.
- Backend nel clone pulito: `mvn -B verify`, 154 test passati, JAR generato; dopo l'hardening CodeQL: 155 test passati.
- Frontend nel clone pulito: `npm ci`; 32 test passati; typecheck applicativo ed E2E passati; build Vite passata.
- Legacy nel clone pulito: `mvn -B -f pom-legacy.xml test`, 41 test passati.
- Flyway: 18 migrazioni validate e applicate su schema H2 pulito durante i test.
- Compose locale e prod-like nel clone pulito: configurazioni valide con placeholder non sensibili.
- Stack prod-like ed E2E non avviati per rispettare la stop condition precedente allo Step 1.5.
- Runtime Maven verificato: Java 23.0.1 con compilazione `release 17`; il test della baseline passa anche su questo runtime.
- Artifact CI backend, frontend e diagnostica prod-like includono `github.sha`.
- Branch protection remota attiva su `main`: pull request obbligatoria, una approvazione, dismiss stale approvals, approvazione dell'ultimo push, conversazioni risolte, nessun bypass amministratore, force push e cancellazione disabilitati.
- Pull request draft aperta: `#2`, branch `miglioramenti-gestionale` verso `main`.
- Branch protection verificata con branch aggiornata obbligatoria e check richiesti: `Backend Spring Boot`, `Frontend React`, `Database migrations`, `Quality gate`.
- Run CI remoto finale `30341354456`: repository hygiene, backend, frontend, migrazioni e quality gate completati con successo; prod-like correttamente saltato.
- Run security remoto finale `30341354476`: Gitleaks, dependency review, npm audit, inventario dipendenze backend e CodeQL Java/TypeScript completati con successo.
- Check separato `Code scanning results / CodeQL` completato con successo: nessun nuovo alert aperto nella pull request.
- Dependency Graph del repository abilitato per rendere operativo il dependency review.

## Rischi residui

- F-20 e chiuso: baseline reale, clone pulito, CI remota e protezione branch sono verificati.
- Il gate migrazioni obbligatorio usa PostgreSQL 16 reale tramite Testcontainers; H2 resta soltanto feedback rapido della suite standard.
- I controlli security sono verdi ma non sono inclusi tra i quattro status check obbligatori richiesti dallo Step 0.1.
- Gli account operativi storici non verificati sono in quarantena; la revisione manuale deve essere completata prima di consentire nuovi login agli eventuali dipendenti legittimi.
- Le sessioni usano l'ID account stabile e una versione credenziali; il lifecycle non distruttivo di disabilitazione, riabilitazione e revoca e coperto dallo Step 3.3.
- Le race critiche di sessioni e lockout sono incluse nella suite PostgreSQL trasversale obbligatoria.
- Gli ordini usano ID account/partner stabili; gli snapshot testuali non autorizzano accessi e lo storico ambiguo resta `UNRESOLVED` fino a riconciliazione esplicita.
- Il rate limit della registrazione e stato verificato sullo stack prod-like aggiornato.
- Un arresto non intercettabile con `SIGKILL` puo lasciare risorse temporanee; il recupero richiede project name e run ID registrati nella diagnostica e il cleanup guardato rifiuta ownership miste.
- Gate A, Gate B e Gate C sono chiusi tecnicamente nel repository reale.
- I pagamenti storici `UNRECONCILED` richiedono una decisione umana basata su evidenze: la migrazione evita conclusioni false, ma non puo ricostruire informazioni mai registrate.
- Le misure di V22 sono state raccolte su fixture sintetiche in PostgreSQL 16 e descrivono questo ambiente di prova; vanno ripetute sui volumi e sull'infrastruttura del rilascio reale.
- F-21 e chiuso dalla suite PostgreSQL trasversale obbligatoria; F-31 e chiuso tecnicamente dallo Step 3.10.
- Il recupero pubblico della password resta rinviato finche non viene introdotto un canale verificato; non vengono usate domande di sicurezza o reset privi di verifica dell'identita.
- La validazione operatore di WB-001, WB-002 e WB-003 resta pendente e non viene sostituita dai test automatici.
