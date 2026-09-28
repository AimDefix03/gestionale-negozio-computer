# Workflow Benchmarks

## Scopo

Questo documento governa la progettazione dei nuovi workflow del gestionale. Il benchmark serve a comprendere problemi operativi e invarianti gia affrontati da prodotti maturi; non autorizza a copiarne codice, testi, asset, grafica o identita visiva.

Le osservazioni descrivono comportamenti pubblicamente documentati alla data del 2026-08-27. Prima di implementare un flusso futuro, la relativa scheda deve essere aggiornata se le fonti o il perimetro sono cambiati.

## Regola di utilizzo

Ogni nuovo flusso deve avere una scheda approvata prima della modifica del codice. La scheda deve contenere, nell'ordine:

1. problema operativo;
2. comportamento osservato in due o piu gestionali maturi;
3. regola di dominio da preservare;
4. adattamento al monolite attuale;
5. cosa non implementare;
6. test di accettazione con un operatore reale.

Ogni decisione assume uno dei seguenti esiti:

- `ADOPT`: il comportamento e coerente con dominio e scope attuali;
- `ADAPT`: il principio e valido, ma va ridotto o modificato per il monolite corrente;
- `REJECT`: il comportamento viola un invariante o lo scope approvato;
- `DEFER`: il comportamento e utile ma appartiene a uno step successivo.

La scheda deve collegare decisioni, test automatici e prova operatore allo step di roadmap. Una funzionalita non puo essere dichiarata validata soltanto perche assomiglia a un prodotto noto.

## Template obbligatorio

### Identificazione

- Workflow:
- Step roadmap:
- Responsabile decisionale:
- Data benchmark:
- Stato: `DRAFT`, `READY_FOR_IMPLEMENTATION`, `IMPLEMENTED`, `OPERATOR_VALIDATED` oppure `REJECTED`.

### 1. Problema operativo

Descrivere attore, obiettivo, frequenza, errore da prevenire e conseguenza operativa.

### 2. Comportamento osservato in due o piu gestionali maturi

Per ogni prodotto indicare fonte ufficiale, data di consultazione, comportamento osservato e limite del confronto. Non dedurre regole non documentate.

### 3. Regola di dominio da preservare

Elencare invarianti di identita, autorizzazione, stock, denaro, documenti, audit e idempotenza coinvolti.

### 4. Adattamento al monolite attuale

Indicare moduli, contratti API, dati, UI, migrazioni e test interessati. Separare il cambiamento minimo dalle evoluzioni differite.

### 5. Cosa non implementare

Elencare esplicitamente funzioni fuori scope, scorciatoie rischiose e comportamenti proprietari da non riprodurre.

### 6. Test di accettazione con un operatore reale

Definire profilo operatore, ambiente, dati iniziali, passi, risultati attesi, evidenze da raccogliere e criteri di esito. Registrare nome o identificativo del valutatore, data e anomalie soltanto dopo l'esecuzione reale.

## Scheda WB-001 - Vendita assistita da personale

### Identificazione

- Workflow: creazione di un ordine per conto di un cliente da parte di dipendente o amministratore.
- Step roadmap: 3.1.
- Responsabile decisionale: proprietario del progetto per gli invarianti di stock e pagamento.
- Data benchmark: 2026-08-15.
- Stato: `IMPLEMENTED`; la prova con operatore reale resta da eseguire e non viene dichiarata completata.

### 1. Problema operativo

Un addetto vendita deve preparare rapidamente un ordine per un cliente censito oppure occasionale senza usare l'identita dell'operatore come cliente, senza confondere manutenzione catalogo e vendita e senza scaricare fisicamente lo stock prima dell'evasione.

Gli errori da prevenire sono:

- ordine attribuito al dipendente invece che al cliente;
- scelta del cliente tramite testo ambiguo o nome duplicato;
- vendita di prodotto disattivato o quantita non disponibile;
- doppia creazione dopo un invio ripetuto;
- incasso, conferma ed evasione trattati come un'unica azione implicita;
- perdita della bozza quando un refresh o una richiesta falliscono.

### 2. Comportamento osservato in due o piu gestionali maturi

#### Odoo Sales

La documentazione Odoo distingue preventivo, ordine di vendita, consegna, fattura e pagamento. Il cliente e richiesto sul preventivo; la conferma trasforma il preventivo in ordine e solo in seguito abilita i passaggi successivi. Decisione: `ADAPT`, mantenendo nel primo MVP la bozza ordine gia esistente senza introdurre un modulo preventivi separato.

Fonte ufficiale consultata il 2026-08-15:

- [Sales quotations - Odoo 18](https://www.odoo.com/documentation/18.0/applications/sales/sales/sales_quotations.html)
- [Create quotations - Odoo 18](https://www.odoo.com/documentation/18.0/applications/sales/sales/sales_quotations/create_quotations.html)

#### ERPNext

ERPNext separa il salvataggio in bozza dalla conferma tramite submit. L'ordine associa un cliente e righe con quantita, prezzi e disponibilita da verificare; la conferma rende disponibili consegna, fatturazione e pagamento come fasi successive. Decisione: `ADOPT` per separazione bozza/conferma e riferimento cliente stabile; `DEFER` per date promesse, magazzini multipli e acquisti collegati.

Fonti ufficiali consultate il 2026-08-15:

- [Sales Order - ERPNext](https://docs.frappe.io/erpnext/sales-order)
- [Customer - ERPNext](https://docs.frappe.io/erpnext/customer)

#### Microsoft Dynamics 365 Business Central

Business Central mantiene distinta la registrazione dell'ordine dalle operazioni di spedizione e fatturazione, supporta registrazioni parziali e conserva documenti e movimenti registrati. I campi critici di un documento registrato non vengono corretti sovrascrivendo lo storico, ma tramite operazioni di annullamento o storno. Decisione: `ADAPT` per separare conferma, evasione e registrazione finanziaria; `DEFER` per spedizioni parziali e contabilita generale.

Fonti ufficiali consultate il 2026-08-15:

- [Invoice sales - Business Central](https://learn.microsoft.com/en-us/dynamics365/business-central/sales-how-invoice-sales)
- [Posting sales documents - Business Central](https://learn.microsoft.com/en-us/dynamics365/business-central/ui-post-sales)

### 3. Regola di dominio da preservare

- Solo un account con `CREATE_ORDERS` puo avviare la vendita assistita; il cliente self-service non puo scegliere un altro cliente.
- Un cliente censito viene selezionato tramite ID stabile del `BusinessPartner`; codice e nome restano snapshot descrittivi.
- Il cliente occasionale deve essere dichiarato esplicitamente e non deve essere sostituito con username o identita del dipendente.
- Le righe ordine congelano codice, descrizione, quantita e prezzo applicato; successive modifiche al catalogo non riscrivono lo storico.
- La creazione produce una bozza e non riserva o scarica stock.
- La conferma riserva stock; l'evasione produce il movimento fisico autorevole e libera la riserva.
- Stato ordine, stato pagamento ed evasione restano indipendenti.
- Prodotti disattivati o quantita oltre la disponibilita vendibile non sono aggiungibili a un nuovo ordine.
- La creazione ordine conserva idempotenza per intento e restituisce errori di dominio gestibili senza perdere la bozza.
- Ogni transizione sensibile resta autorizzata dal backend e auditata.

### 4. Adattamento al monolite attuale

Decisione generale: `ADAPT`.

Il cambiamento minimo dello Step 3.1 dovra:

1. separare nella SPA la modalita `Vendita` dalla modalita `Gestione catalogo`;
2. rendere il carrello disponibile in base a `CREATE_ORDERS`, non in base all'assenza di `MANAGE_PRODUCTS`;
3. caricare una ricerca clienti attivi e inviare un ID partner stabile per il cliente censito;
4. offrire un percorso esplicito per il cliente occasionale, con nome obbligatorio e nessun collegamento partner fittizio;
5. mostrare prima dell'invio cliente, righe, quantita, prezzi, totale e metodo di pagamento previsto;
6. creare soltanto la bozza e lasciare conferma, evasione e registrazione pagamento nelle rispettive azioni;
7. riusare `useCommandExecution` e la stessa chiave per intento nei retry ambigui;
8. mantenere la bozza locale dopo errori di rete, conflitti o validazioni backend;
9. aggiornare API, test di autorizzazione, test di dominio, test frontend e smoke E2E nello stesso step.

L'API attuale accetta `customerCode` e il servizio risolve gia il partner, ma il nuovo contratto dovra usare l'identita stabile come input primario. La compatibilita del campo esistente andra mantenuta o deprecata esplicitamente, non rimossa in modo implicito.

### 5. Cosa non implementare

- Nessuna copia di layout, testi, icone o flussi proprietari dei prodotti analizzati.
- Nessun modulo preventivi completo nello Step 3.1.
- Nessun POS, cassa fiscale, scontrino, fattura elettronica o dichiarazione di conformita.
- Nessun pagamento automatico, PSP o autorizzazione bancaria.
- Nessun multi-magazzino, spedizione parziale, drop shipment o contabilita generale.
- Nessuna creazione automatica di anagrafiche fittizie per clienti occasionali.
- Nessuna conferma, evasione o incasso impliciti al click `Crea ordine`.
- Nessun affidamento esclusivo ai controlli frontend per identita cliente, disponibilita o permessi.

### 6. Test di accettazione con un operatore reale

Stato: `PENDING_OPERATOR_EXECUTION`. Il workflow e implementato e coperto da test automatici; questo documento non dichiara una validazione umana non avvenuta.

Profilo richiesto: una persona diversa dallo sviluppatore che abbia familiarita con vendita al banco o gestione ordini. Ambiente: build candidata isolata con dati sintetici, nessun dato o pagamento reale.

Dati iniziali:

- account dipendente con `CREATE_ORDERS` e senza `MANAGE_PRODUCTS`;
- account admin con entrambi i permessi;
- due clienti censiti con nomi simili ma ID distinti;
- un prodotto disponibile, uno disattivato e uno con disponibilita insufficiente;
- rete simulata con un errore recuperabile durante il primo invio.

Scenari obbligatori:

1. Il dipendente apre `Vendita`, seleziona il cliente censito corretto tramite ricerca, aggiunge due righe e crea una sola bozza.
2. L'operatore verifica che la bozza riporti cliente, righe, prezzi e totale corretti e che lo stock fisico non sia diminuito.
3. L'operatore crea una seconda bozza per cliente occasionale senza generare un'anagrafica fittizia.
4. Il sistema impedisce prodotto disattivato e quantita superiore alla disponibilita con un messaggio azionabile.
5. Dopo un errore di rete la bozza rimane compilata; il retry non crea un duplicato.
6. L'admin passa tra `Vendita` e `Gestione catalogo` senza confondere le azioni o perdere il carrello.
7. Un cliente self-service non puo selezionare o impersonare un altro cliente.

Criteri di esito:

- tutti gli scenari producono il risultato atteso senza assistenza dello sviluppatore;
- l'operatore identifica cliente, stato e prossima azione senza spiegazioni esterne;
- zero ordini duplicati, attribuzioni errate o movimenti stock anticipati;
- eventuali esitazioni, errori e tempi per scenario vengono registrati come evidenze e trasformati in correzioni o backlog approvato.

Evidenze da registrare dopo l'esecuzione: identificativo valutatore, data, build, durata per scenario, esito, screenshot non sensibili, request ID degli errori e decisione finale `OPERATOR_VALIDATED` oppure `REJECTED`.

## Scheda WB-002 - Catalogo e dashboard cliente

### Identificazione

- Workflow: consultazione del catalogo commerciale e del riepilogo personale da parte di un cliente autenticato.
- Step roadmap: 3.2.
- Responsabile decisionale: proprietario del progetto per la politica di disponibilita commerciale e di esposizione dei dati.
- Data benchmark: 2026-08-17.
- Stato: `IMPLEMENTED`; la prova con operatore reale resta da eseguire e non viene dichiarata completata.

### 1. Problema operativo

Il cliente deve poter consultare prodotti acquistabili e stato dei propri ordini senza ricevere dati interni usati da magazzino, amministrazione o personale operativo.

Gli errori da prevenire sono:

- esposizione di giacenza fisica, quantita riservata, disponibilita numerica o valore potenziale di vendita;
- dashboard cliente costruita con KPI globali, ordini altrui o movimenti di magazzino;
- occultamento dei campi soltanto nel frontend, dopo che i dati hanno gia attraversato la rete;
- uso dello stesso DTO per ruoli con finalita e autorizzazioni differenti;
- possibilita di inferire la quantita esatta tramite ordinamenti o limiti applicati dal client.

### 2. Comportamento osservato in due o piu gestionali maturi

#### Odoo

Il portale cliente Odoo presenta al soggetto autenticato i propri documenti commerciali, tra cui ordini e fatture, tramite l'area personale. Gli utenti portale hanno accesso di lettura alle informazioni condivise con loro. La visualizzazione della disponibilita prodotto e della quantita esatta e configurabile e non costituisce un dato da esporre obbligatoriamente. Decisione: `ADAPT`, mantenendo un catalogo cliente dedicato e una dashboard limitata agli ordini del relativo account.

Fonti ufficiali consultate il 2026-08-17:

- [Customer accounts - Odoo 19](https://www.odoo.com/documentation/19.0/applications/websites/ecommerce/customer_accounts.html)
- [Portal access - Odoo 19](https://www.odoo.com/documentation/19.0/applications/general/users/user_portals.html)
- [Product availability - Odoo 18](https://www.odoo.com/documentation/18.0/applications/websites/ecommerce/products.html)

#### ERPNext e Frappe Framework

Il portale cliente ERPNext collega l'accesso al cliente associato all'utente. Il framework Frappe applica permessi per ruolo, proprietario e campo sul server e rimuove i campi non autorizzati dalla risposta. Decisione: `ADOPT` per ownership stabile e field-level projection applicata prima della serializzazione; `DEFER` per un motore configurabile di field-level security.

Fonti ufficiali consultate il 2026-08-17:

- [Project customer portal - ERPNext](https://docs.frappe.io/erpnext/project-customer-portal)
- [Database query permissions - Frappe Framework](https://docs.frappe.io/framework/get_query)

#### Microsoft Dynamics 365 e Business Central

Dynamics 365 Commerce presenta nell'area account la cronologia e il dettaglio degli ordini dell'utente autenticato. Business Central riserva dettagli di disponibilita e prenotazione alle viste operative autorizzate; quando serve un'informazione commerciale, la disponibilita puo essere rappresentata con livelli discreti invece della quantita fisica. Decisione: `ADAPT` per dashboard personale e stato commerciale discreto; `REJECT` per l'esposizione al cliente delle viste interne di disponibilita.

Fonti ufficiali consultate il 2026-08-17:

- [Customer account management - Dynamics 365 Commerce](https://learn.microsoft.com/en-us/dynamics365/commerce/account-management)
- [Item tracking availability - Business Central](https://learn.microsoft.com/en-us/dynamics365/business-central/design-details-item-tracking-availability)
- [Item availability levels - Business Central](https://learn.microsoft.com/en-ca/dynamics365/business-central/sales-order-agent-item-availability)

### 3. Regola di dominio da preservare

- L'account `CUSTOMER` accede soltanto alla projection commerciale dedicata; i DTO operativi di prodotto e dashboard sono riservati al personale.
- Il catalogo cliente non contiene ID interni, giacenza fisica, quantita riservata, disponibilita numerica, valore potenziale di vendita o stato di disattivazione interno.
- I prodotti disattivati non compaiono nel catalogo cliente e non sono acquistabili.
- La disponibilita commerciale e un livello discreto calcolato sul server: `AVAILABLE` oltre tre unita vendibili, `LIMITED` da una a tre, `UNAVAILABLE` a zero.
- Il frontend non riceve un massimo numerico dal quale ricostruire la giacenza; la validazione della quantita richiesta resta autorevole nel backend al momento dell'ordine.
- La dashboard cliente usa l'ID account stabile della sessione e contiene soltanto conteggi e riepiloghi dei propri ordini.
- KPI globali, ricavi aziendali, movimenti di magazzino e dati di altri clienti non attraversano la rete verso `CUSTOMER`.
- L'autorizzazione della response shape avviene nel backend prima della serializzazione e non dipende dalla visibilita dei componenti React.

### 4. Adattamento al monolite attuale

Decisione generale: `ADAPT`.

Lo Step 3.2 implementa il seguente adattamento minimo:

1. introdurre endpoint cliente separati per catalogo e dashboard;
2. creare DTO cliente espliciti, senza riutilizzare `ProductResponse` o `DashboardResponse`;
3. impedire a `CUSTOMER` di raggiungere gli endpoint operativi `/api/products` e `/api/dashboard`;
4. filtrare i prodotti disattivati e tradurre la disponibilita vendibile in tre livelli discreti;
5. costruire la dashboard cliente tramite `customerAccountId`, con conteggi per stato e ultimi ordini propri;
6. usare nel frontend tipi, client e componenti distinti per il percorso cliente;
7. eliminare dal carrello cliente il limite derivato dalla quantita esatta, lasciando al backend la validazione finale;
8. aggiungere contract test positivi e negativi per cliente, personale e sessione assente;
9. verificare esplicitamente l'assenza dei campi interni nella risposta JSON e nell'interfaccia cliente.

Non e richiesta una migrazione dati: la separazione riguarda query, autorizzazioni, DTO e presentazione.

### 5. Cosa non implementare

- Nessuna copia di layout, testi, codice, asset o identita dei prodotti analizzati.
- Nessuna esposizione della quantita esatta mascherata tramite label, tooltip, attributi HTML, limiti del controllo quantita o ordinamento per stock.
- Nessun calcolo di disponibilita autorevole nel browser.
- Nessun riuso del DTO operativo con annotazioni condizionali dipendenti dal ruolo.
- Nessun listino cliente, promozione personalizzata, wishlist, tracking spedizione o recommendation engine nello Step 3.2.
- Nessun accesso cliente a movimenti, valore potenziale di vendita, stock riservato, audit o KPI aziendali.
- Nessuna dichiarazione di validazione operatore finche il test umano non e stato realmente eseguito.

### 6. Test di accettazione con un operatore reale

Stato: `PENDING_OPERATOR_EXECUTION`. Il workflow dovra essere coperto da test automatici, ma questo documento non dichiara una validazione umana non avvenuta.

Profilo richiesto: una persona diversa dallo sviluppatore che utilizzi un account cliente. Ambiente: build candidata isolata con prodotti e ordini sintetici appartenenti ad almeno due clienti.

Dati iniziali:

- account cliente A e cliente B con ordini distinti;
- un prodotto con oltre tre unita vendibili, uno con una-tre unita, uno esaurito e uno disattivato;
- almeno un ordine per ciascuno stato disponibile nella dashboard cliente A;
- un account dipendente con accesso alla vista operativa completa.

Scenari obbligatori:

1. Il cliente A consulta il catalogo e comprende la disponibilita commerciale senza vedere numeri di stock.
2. Il prodotto disattivato non compare; il prodotto esaurito non puo essere aggiunto alla bozza.
3. Il cliente aumenta la quantita senza ricevere un limite di giacenza dal browser; una quantita non valida viene rifiutata dal backend con messaggio azionabile e la bozza resta compilata.
4. La dashboard mostra soltanto conteggi e ultimi ordini del cliente A.
5. Il cliente A non vede ordini del cliente B, movimenti, KPI economici globali o valore potenziale di vendita.
6. Tentando gli endpoint operativi di catalogo e dashboard, il cliente riceve `403`.
7. Il dipendente continua a vedere i dati operativi necessari senza usare gli endpoint cliente.

Criteri di esito:

- nessun campo interno e presente nel payload cliente verificato tramite strumenti di rete;
- zero riferimenti a ordini di altri account;
- l'operatore comprende disponibilita, stato ordini e prossima azione senza spiegazioni esterne;
- eventuali esitazioni, errori e tempi vengono registrati e trasformati in correzioni o backlog approvato.

Evidenze da registrare dopo l'esecuzione: identificativo valutatore, data, build, payload di rete sanitizzati, durata per scenario, esito, screenshot non sensibili e decisione finale `OPERATOR_VALIDATED` oppure `REJECTED`.

## Scheda WB-003 - Lifecycle account e risposta alla compromissione

### Identificazione

- Workflow: ingresso, permanenza, cambio credenziali, sospensione e uscita di personale e clienti.
- Step roadmap: 3.3.
- Responsabile decisionale: proprietario del progetto per ruoli privilegiati, conservazione dello storico e canali di recupero.
- Data benchmark: 2026-08-17.
- Stato: `IMPLEMENTED`; la prova con operatore reale resta da eseguire.

### 1. Problema operativo

Un amministratore deve poter revocare rapidamente l'accesso a un dipendente uscente o a un account compromesso senza cancellare identita, ordini e audit. Un utente autenticato deve poter cambiare la propria password confermando quella corrente; un reset amministrativo deve essere eccezionale, tracciato e invalidare tutte le sessioni. Il login non deve chiedere al client di dichiarare un ruolo gia autorevole nel database.

Gli errori da prevenire sono:

- cancellazione fisica che rende incomprensibile lo storico;
- sessioni ancora valide dopo disabilitazione, reset o cambio ruolo;
- reset amministrativo senza ri-autenticazione dell'operatore;
- riuso dello username di un account disattivato;
- enumerazione di account tramite messaggi di login differenti;
- blocco permanente sfruttabile per negare il servizio a un altro utente;
- recupero pubblico senza email o altro canale verificato.

### 2. Comportamento osservato in due o piu gestionali maturi

#### Keycloak

La guida amministrativa distingue account abilitato, reset password, password temporanea con cambio richiesto e gestione delle sessioni. Un amministratore puo terminare le sessioni di un utente; il recupero self-service richiede un flusso di reset e un canale email configurato. Decisione: `ADAPT` per stato account, revoca completa e reset amministrativo protetto; `DEFER` per password temporanea e recupero via email finche il monolite non dispone di un canale verificato.

Fonte ufficiale consultata il 2026-08-17:

- [Keycloak Server Administration Guide](https://www.keycloak.org/docs/latest/server_admin/)

#### Microsoft Entra ID

La procedura ufficiale di risposta a uscita o compromissione combina blocco dei nuovi accessi e revoca delle sessioni/token. I privilegi richiesti sono piu elevati quando il soggetto e amministrativo. Decisione: `ADOPT` per disabilitazione piu revoca atomica; `ADAPT` per la matrice locale in cui soltanto il super admin gestisce gli account `ADMIN`.

Fonte ufficiale consultata il 2026-08-17:

- [Revoke user access in an emergency in Microsoft Entra ID](https://learn.microsoft.com/en-us/entra/identity/users/users-revoke-access)

### 3. Regola di dominio da preservare

- L'identita tecnica e l'ID account; username e ruolo sono attributi autorevoli del server e non input del login.
- Disabilitare un account blocca immediatamente nuovi login e revoca tutte le sessioni attive nella stessa transazione applicativa.
- Riabilitare un account non ripristina sessioni precedenti e non salta l'eventuale revisione operativa.
- Nessun endpoint operativo cancella fisicamente un account; username canonico e storico restano riservati senza riuso.
- Un operatore non puo disabilitare se stesso; il super admin bootstrap non e disabilitabile dal pannello.
- Soltanto il super admin puo disabilitare, riabilitare, resettare o cambiare ruolo a un `ADMIN`.
- Cambio password personale, reset amministrativo, revoca sessioni, cambio stato e cambio ruolo incrementano o rispettano la versione credenziali e invalidano i token precedenti.
- Le azioni amministrative sensibili richiedono ri-autenticazione e producono audit senza password o token.
- Errori di login per username inesistente, password errata, account disabilitato o ruolo in revisione restano indistinguibili.
- Il lockout e temporaneo, atomico e limitato; non modifica in modo permanente lo stato dell'account.

### 4. Adattamento al monolite attuale

Decisione generale: `ADAPT`.

Lo Step 3.3 implementa il cambiamento minimo seguente:

1. aggiungere a `user_accounts` stato abilitato e metadati di disabilitazione tramite una nuova migrazione Flyway;
2. sostituire la cancellazione fisica con disabilitazione compatibile e introdurre endpoint espliciti di disable/enable;
3. revocare tutte le sessioni in disable, cambio password, reset e cambio ruolo;
4. introdurre cambio password self-service con password corrente e reset amministrativo con ri-autenticazione;
5. introdurre revoca amministrativa di tutte le sessioni di un account;
6. rimuovere `role` dal contratto login, dal client e dalla UI;
7. estendere ricerca e risposta account con stato e metadati non sensibili;
8. mantenere lock pessimista, retry limitato, lock temporaneo e messaggi generici gia presenti per i tentativi concorrenti;
9. aggiornare test, documentazione e interfaccia account senza creare un flusso pubblico di recupero.

### 5. Cosa non implementare

- Nessuna cancellazione fisica o riutilizzazione dello username.
- Nessun reset pubblico, domanda segreta o recupero basato su dati anagrafici non verificati.
- Nessun invio email, SMS, OTP o MFA simulato.
- Nessuna password temporanea mostrata in log, audit o notifiche persistenti.
- Nessun blocco permanente automatico dell'account per errori di login.
- Nessun affidamento al ruolo inviato dal browser o ai soli controlli React.
- Nessuna copia di schermate, testi o codice dei prodotti analizzati.

### 6. Test di accettazione con un operatore reale

Stato: `PENDING_OPERATOR_EXECUTION`. L'implementazione sara coperta da test automatici; la validazione umana non viene anticipata.

Profilo richiesto: super admin e, in una sessione separata, un dipendente. Ambiente: build candidata isolata con account e dati sintetici.

Scenari obbligatori:

1. Il dipendente accede inserendo soltanto username e password e cambia la password confermando quella corrente.
2. Tutti i token precedenti del dipendente vengono rifiutati; il nuovo login funziona soltanto con la nuova password.
3. Il super admin disabilita il dipendente con una causale: la sessione attiva termina e nuovi login falliscono senza rivelare lo stato account.
4. Il super admin riabilita il dipendente; le vecchie sessioni restano invalide e il nuovo login riesce.
5. Il super admin revoca tutte le sessioni per una compromissione senza cambiare lo stato dell'account.
6. Un admin ordinario non puo gestire credenziali o stato di un altro admin e nessun operatore puo disabilitare se stesso.
7. Lo storico ordini e audit del dipendente resta consultabile dopo la disabilitazione.
8. Tentativi login concorrenti producono un conteggio atomico e un lock temporaneo, mai una disabilitazione permanente.

Criteri di esito:

- nessuna sessione sopravvive alle operazioni che impongono revoca;
- nessun account o riferimento storico viene eliminato;
- l'operatore distingue stato, azioni e conseguenze senza assistenza;
- password e token non compaiono in risposte, audit o log;
- anomalie e tempi vengono registrati e trasformati in correzioni o backlog approvato.

Evidenze da registrare dopo l'esecuzione: identificativo valutatore, data, build, account sintetici, request ID, audit prodotto, esito dei token precedenti e decisione finale `OPERATOR_VALIDATED` oppure `REJECTED`.

## Scheda WB-004 - Resi multi-riga e rimborso parziale

### Identificazione

- Workflow: richiesta e gestione di resi parziali su piu righe di un ordine evaso.
- Step roadmap: 3.8.
- Responsabile decisionale: proprietario del progetto per gli invarianti di stock e pagamento.
- Data benchmark: 2026-08-25.
- Stato: `IMPLEMENTED` in staging; la prova con operatore reale resta da eseguire e non viene dichiarata completata.

### 1. Problema operativo

Cliente e personale devono poter descrivere in un'unica richiesta piu articoli restituiti, anche in quantita parziali, senza superare quanto acquistato e senza associare note o rimborsi al reso sbagliato.

Gli errori da prevenire sono:

- doppia restituzione della stessa unita tramite richieste concorrenti;
- quantita copiata da una riga prodotto a un'altra;
- perdita della richiesta compilata dopo un errore non conclusivo;
- nota di approvazione o rifiuto applicata a un reso diverso;
- rimborso attribuito al solo codice leggibile anziche all'identita stabile del reso;
- reintegro stock prima del rientro fisico o rimborso oltre il valore residuo.

### 2. Comportamento osservato in due o piu gestionali maturi

#### Odoo Sales e Inventory

La documentazione Odoo crea il reso dalla consegna validata, propone le righe e consente di modificare quantita o rimuovere prodotti. Il rientro genera un'operazione di magazzino distinta, validata quando la merce viene ricevuta; dopo fatturazione il rimborso richiede anche una nota di credito e resta separato dal movimento fisico. Decisione: `ADOPT` per righe multiple, quantita parziali e separazione tra ricezione e rimborso; `DEFER` per integrazione contabile e provider di pagamento.

Fonti ufficiali consultate il 2026-08-25:

- [Returns and refunds - Odoo 18](https://www.odoo.com/documentation/18.0/applications/sales/sales/products_prices/returns.html)
- [Credit notes and refunds - Odoo 18](https://www.odoo.com/documentation/18.0/applications/finance/accounting/customer_invoices/credit_notes.html)

#### ERPNext

ERPNext crea il reso dal documento originale, conserva il collegamento alla vendita, permette di mantenere solo le righe interessate e limita la quantita cumulativa a quella originaria. Distingue reso logistico, nota credito e pagamento del rimborso, evitando un secondo movimento stock quando la merce e gia stata ricevuta. Decisione: `ADOPT` per identita del documento origine, quantita cumulative e ledger distinto; `ADAPT` per il workflow locale a stati gia presente.

Fonti ufficiali consultate il 2026-08-25:

- [Sales Return - ERPNext](https://docs.frappe.io/erpnext/sales-return)
- [Credit Note - ERPNext](https://docs.frappe.io/erpnext/credit-note)

### 3. Regola di dominio da preservare

- Il reso appartiene a un ordine evaso e contiene una o piu righe con quantita positiva.
- La somma delle quantita gia restituite o ancora impegnate da resi attivi e della nuova richiesta non supera mai la quantita ordinata.
- Il backend ricalcola il residuo nella transazione ed e l'unica autorita anche quando due client inviano richieste concorrenti.
- Ricezione fisica, rimborso e documento simulato restano operazioni distinte.
- La ricezione reintegra stock una sola volta e soltanto per un reso approvato.
- Il rimborso richiede un reso ricevuto e non supera ne il valore residuo del reso ne il netto incassato.
- Note operative, capability e movimenti finanziari sono associati all'ID stabile del reso; il codice e soltanto una rappresentazione leggibile.
- Ogni comando sensibile mantiene autorizzazione backend, idempotenza e audit.
- Un errore non confermato non cancella la bozza dell'operatore.

### 4. Adattamento al monolite attuale

Decisione generale: `ADAPT`.

Lo Step 3.8 implementa il cambiamento minimo seguente:

1. builder React su tutte le righe restituibili dell'ordine;
2. quantita ordinata, gia restituita o impegnata e residua esposte dal DTO backend;
3. validazione concorrente sullo stesso ordine mediante il lock gia presente nel dominio;
4. ID del reso esposto esplicitamente e usato come chiave UI;
5. transazioni `REFUND` aggregate per return ID e mostrate nel dettaglio del singolo reso;
6. outcome comando tipizzato in successo, avviso persistito o errore;
7. reset di richiesta, revisione e rimborso soltanto dopo persistenza confermata;
8. test backend H2/PostgreSQL e test frontend per righe multiple, quantita cumulative, isolamento note, rimborso parziale e conservazione form.

Il contratto resta additivo e non richiede una migrazione: identita, lock e ledger erano gia persistiti dal modello corrente.

### 5. Cosa non implementare

- Nessuna copia di schermate, testi, icone o codice dei prodotti analizzati.
- Nessuna autorizzazione di reso automatica basata solo sul client.
- Nessun reintegro stock alla semplice richiesta o approvazione.
- Nessun rimborso bancario, PSP, nota credito fiscale o dichiarazione di conformita.
- Nessuna associazione operativa basata sull'indice visuale o sul solo codice reso.
- Nessuna sostituzione automatica del prodotto o gestione RMA, seriali, lotti e garanzie nello Step 3.8.
- Nessuna correzione automatica di anomalie storiche prive di evidenza.

### 6. Test di accettazione con un operatore reale

Stato: `PENDING_OPERATOR_EXECUTION`. Implementazione e test automatici non sostituiscono una prova umana.

Profilo richiesto: un addetto resi o una persona diversa dallo sviluppatore che simuli cliente e staff. Ambiente: build candidata isolata con ordini, prodotti e pagamenti sintetici.

Dati iniziali:

- un ordine evaso con almeno due prodotti e quantita maggiori di uno;
- un incasso sufficiente a coprire un rimborso parziale;
- due sessioni staff concorrenti;
- un reso precedente su una parte della prima riga.

Scenari obbligatori:

1. L'operatore apre il dettaglio e comprende senza calcoli esterni quantita ordinata, impegnata e residua di entrambe le righe.
2. Inserisce due prodotti nella stessa richiesta e verifica che il riepilogo salvato mantenga prodotto e quantita corretti.
3. Un input non valido o un errore di rete conserva quantita e motivazione compilate.
4. Due sessioni tentano di restituire contemporaneamente l'ultima unita: una sola riesce e il residuo finale e zero.
5. Lo staff inserisce note diverse su due resi, approva uno e rifiuta l'altro senza contaminazione tra le note.
6. Dopo la ricezione registra due rimborsi parziali e verifica che ogni movimento compaia soltanto nel ledger del reso corretto.
7. Il sistema impedisce ricezione prematura, rimborso eccessivo e nuova richiesta oltre il residuo.

Criteri di esito:

- nessuna quantita, nota o transazione appare sul prodotto o reso sbagliato;
- nessuna richiesta concorrente supera la quantita ordinata;
- ricezione e rimborso restano distinguibili e verificabili;
- l'operatore recupera da errori senza reinserire la bozza;
- anomalie e tempi vengono registrati e trasformati in correzioni o backlog approvato.

Evidenze da registrare dopo l'esecuzione: identificativo valutatore, data, build, ordini e resi sintetici, request ID, audit, movimenti stock, transazioni rimborso e decisione finale `OPERATOR_VALIDATED` oppure `REJECTED`.

## Scheda WB-005 - Ordini fornitore

### Identificazione

- Workflow: creazione, invio, ricezione amministrativa parziale e annullo del residuo di un ordine fornitore.
- Step roadmap: 4.1.
- Responsabile decisionale: proprietario del progetto per gli invarianti di approvvigionamento, stock e storico.
- Data benchmark: 2026-08-27.
- Stato: `IMPLEMENTED`.

### 1. Problema operativo

Un dipendente autorizzato deve ordinare prodotti da un fornitore censito e conoscere in ogni momento cosa e stato ordinato, inviato, ricevuto o annullato senza affidarsi a note esterne o carichi manuali privi di origine.

Gli errori da prevenire sono:

- ordine associato a un fornitore tramite nome ambiguo invece che tramite ID stabile;
- modifica retroattiva di descrizione, codice, quantita o prezzo gia inviati;
- ricezione oltre la quantita residua oppure duplicata dopo retry;
- stato completo quando esistono ancora righe da ricevere;
- annullo che cancella anche quantita gia ricevute o nasconde lo storico;
- creazione prematura di fatture passive, debiti o movimenti contabili;
- ricezione amministrativa scollegata dall'ordine sorgente.

### 2. Comportamento osservato in due o piu gestionali maturi

#### Odoo Purchase

Odoo distingue richiesta di quotazione e ordine confermato, conserva fornitore, prodotti e data prevista e collega la ricezione al purchase order. Una ricezione parziale mantiene esplicito il residuo tramite backorder; la fatturazione fornitore resta un passaggio distinto. Decisione: `ADAPT` per separare bozza e invio, data prevista e ricezione collegata; `DEFER` per RFQ, backorder separati, email e fatture passive.

Fonti ufficiali consultate il 2026-08-27:

- [Requests for quotation - Odoo 19](https://www.odoo.com/documentation/19.0/applications/inventory_and_mrp/purchase/manage_deals/rfq.html)
- [Purchase lead times - Odoo 19](https://www.odoo.com/documentation/19.0/applications/inventory_and_mrp/inventory/warehouses_storage/replenishment/lead_times.html)

#### ERPNext

ERPNext richiede fornitore e prodotti prima dell'ordine, espone percentuale ricevuta e collega il Purchase Receipt al Purchase Order. Il documento di ricezione puo chiudere un residuo non piu atteso preservando quanto gia ricevuto; fattura e pagamento sono documenti successivi distinti. Decisione: `ADOPT` per identita fornitore, quantita ricevute cumulative e chiusura motivata del residuo; `DEFER` per Purchase Invoice, condizioni commerciali estese e contabilita.

Fonti ufficiali consultate il 2026-08-27:

- [Purchase Order - ERPNext](https://docs.frappe.io/erpnext/purchase-order)
- [Purchase Receipt - ERPNext](https://docs.frappe.io/erpnext/purchase-receipt)

#### Microsoft Dynamics 365 Business Central

Business Central usa l'ordine di acquisto quando sono necessarie ricezioni parziali e mantiene per ogni riga quantita ordinata e ricevuta. La quantita da ricevere e un comando esplicito e non puo eccedere l'ordine senza una policy dedicata. Decisione: `ADOPT` per ricezioni parziali per riga e limite sul residuo; `REJECT` nello scope corrente per over-receipt e posting contabile.

Fonte ufficiale consultata il 2026-08-27:

- [Record purchases - Business Central](https://learn.microsoft.com/en-us/dynamics365/business-central/purchasing-how-record-purchases)

### 3. Regola di dominio da preservare

- Solo account con permesso dedicato agli acquisti possono creare, inviare, ricevere o annullare ordini fornitore.
- Il fornitore deve essere un `BusinessPartner` di tipo `SUPPLIER` selezionato tramite ID stabile; codice e nome vengono salvati come snapshot.
- Ogni riga conserva prodotto tramite ID stabile e snapshot di codice, nome, prezzo concordato e quantita ordinata.
- La quantita ricevuta e cumulativa, non negativa e mai superiore alla quantita ordinata; ogni comando agisce sul residuo autorevole lato server.
- `DRAFT` non rappresenta un impegno inviato. Solo `DRAFT` puo passare a `SENT`.
- Una ricezione e consentita soltanto da `SENT` o `PARTIALLY_RECEIVED`; tutte le righe complete portano a `RECEIVED`.
- L'annullo richiede una causale, preserva le quantita gia ricevute e impedisce nuove ricezioni sul residuo.
- Invio, ricezione e annullo sono idempotenti per intento e serializzati sullo stesso ordine.
- Ogni mutazione sensibile e autorizzata dal backend e produce audit con attore, ordine, stato e causale.
- Lo Step 4.1 registra la ricezione amministrativa collegata all'ordine ma non modifica ancora il ledger fisico: il movimento stock e la valorizzazione del costo appartengono allo Step 4.2.

### 4. Adattamento al monolite attuale

Decisione generale: `ADAPT`.

Il cambiamento minimo dello Step 4.1 deve:

1. introdurre il modulo backend `purchase` con aggregate ordine, righe, stati, repository, service e controller;
2. aggiungere una migrazione Flyway additiva per ordine fornitore, righe, quantità ricevute, versionamento e vincoli;
3. generare un codice ordine acquisto stabile e mantenere supplier ID e snapshot descrittivi separati;
4. esporre lista paginata, dettaglio e comandi create, send, receive e cancel con capability calcolate dal backend;
5. usare idempotency key per tutte le mutazioni e lock pessimista per ricezione e annullo concorrenti;
6. introdurre un permesso acquisti assegnato a super admin, admin e dipendente, mai al cliente;
7. aggiungere una vista SPA dedicata con elenco, dettaglio, righe e azioni coerenti con lo stato;
8. conservare le bozze frontend su errore e distinguere esito comando da refresh;
9. produrre audit per creazione, invio, ricezione e annullo;
10. coprire H2, PostgreSQL, autorizzazioni, concorrenza, idempotenza, frontend e build nello stesso step.

Compatibilita: lo schema e additivo. Nessuna tabella esistente viene reinterpretata e nessun movimento stock o valore prodotto viene creato dallo Step 4.1.

### 5. Cosa non implementare

- Nessuna copia di layout, testi, icone o flussi proprietari dei prodotti analizzati.
- Nessuna fattura passiva, scadenza fornitore, pagamento, prima nota o contabilita generale.
- Nessun aggiornamento di stock fisico, costo medio, ultimo costo o margine prima dello Step 4.2.
- Nessun over-receipt, tolleranza automatica, sostituzione prodotto, reso fornitore o RMA.
- Nessun multi-magazzino, drop shipment, RFQ, approvazione multilivello o invio email reale.
- Nessuna modifica distruttiva di righe o snapshot dopo l'invio.
- Nessun affidamento esclusivo al frontend per stato, quantita residua, fornitore o permessi.

### 6. Test di accettazione con un operatore reale

Stato: `PENDING_OPERATOR_EXECUTION`. L'implementazione automatica completata non equivale a prova umana.

Profilo richiesto: addetto acquisti o persona diversa dallo sviluppatore che simuli approvvigionamento e ricezione. Ambiente: build candidata isolata con fornitori, prodotti e ordini sintetici.

Dati iniziali:

- un fornitore valido e un partner cliente non selezionabile come fornitore;
- due prodotti attivi con prezzi concordati e stock invariato durante lo Step 4.1;
- account dipendente autorizzato e account cliente;
- due sessioni staff concorrenti;
- rete simulata con un errore recuperabile.

Scenari obbligatori:

1. L'operatore crea una bozza con fornitore, data prevista, due righe e prezzi concordati.
2. Invia l'ordine e verifica che fornitore e snapshot delle righe non siano riscritti da modifiche successive alle anagrafiche.
3. Registra una ricezione parziale su entrambe le righe e comprende residuo e prossima azione senza calcoli esterni.
4. Registra la seconda ricezione e ottiene `RECEIVED` soltanto quando tutte le quantita sono complete.
5. Su un secondo ordine riceve una parte e annulla il residuo con causale, preservando quantita e storico ricevuti.
6. Due sessioni tentano contemporaneamente di ricevere l'ultima unita: una sola operazione puo applicare la quantita.
7. Un retry con la stessa chiave non duplica la ricezione; un cliente riceve sempre `403`.
8. Lo stock fisico non cambia nello Step 4.1 e la UI dichiara che la movimentazione verra registrata nel passaggio di ricezione merce dello Step 4.2.

Criteri di esito:

- nessuna quantita supera l'ordinato e nessuna ricezione viene duplicata;
- fornitore, righe, stato, ricevuto e residuo sono comprensibili senza fogli esterni;
- annullo e ricezioni gia avvenute restano distinguibili e auditabili;
- nessun movimento stock, debito o documento fiscale viene creato prematuramente;
- errori e retry non fanno perdere la bozza o creare ordini duplicati;
- anomalie e tempi vengono registrati e trasformati in correzioni o backlog approvato.

Evidenze da registrare dopo l'esecuzione: identificativo valutatore, data, build, supplier order ID, request ID, audit, righe e quantita prima/dopo, esito delle richieste concorrenti e decisione finale `OPERATOR_VALIDATED` oppure `REJECTED`.

## Scheda WB-006 - Ricezione merce e valorizzazione gestionale

### Identificazione

- Workflow: ricezione fisica collegata all'ordine fornitore, aggiornamento del ledger e valorizzazione gestionale del prodotto.
- Step roadmap: 4.2.
- Responsabile decisionale: proprietario del progetto per gli invarianti di stock e costo.
- Data benchmark: 2026-08-28.
- Stato: `IMPLEMENTED`.

### 1. Problema operativo

La ricezione deve aumentare la giacenza una sola volta, conservare il costo effettivo della merce e rendere verificabile la differenza rispetto a quantita e prezzo concordati. Lo storico preesistente privo di evidenza di costo non deve essere valorizzato con importi inventati.

Gli errori da prevenire sono:

- carico manuale scollegato dall'ordine e dalla singola ricezione;
- retry che incrementa due volte stock o costo;
- media aritmetica dei prezzi invece della media ponderata sulle quantita;
- sovrascrittura del prezzo concordato con il costo effettivo ricevuto;
- valorizzazione retroattiva di giacenze storiche prive di costo attendibile;
- KPI di margine calcolati su quantita non coperte da un costo noto;
- arrotondamenti diversi tra dettaglio, ledger, dashboard e report.

### 2. Comportamento osservato in gestionali maturi

#### Odoo Inventory

Odoo collega il movimento di ricezione all'ordine di acquisto e valorizza l'entrata usando quantita ricevuta e prezzo unitario di acquisto. La valorizzazione inventariale resta distinta dalla registrazione contabile. Decisione: `ADOPT` per collegamento ordine-ricezione-movimento e valore della ricezione; `DEFER` per scritture contabili e fatture passive.

Fonti ufficiali consultate il 2026-08-28:

- [How inventory operations affect valuation - Odoo 19](https://www.odoo.com/documentation/19.0/applications/inventory_and_mrp/inventory/inventory_valuation/operations_valuation.html)
- [Inventory valuation cheat sheet - Odoo 19](https://www.odoo.com/documentation/19.0/applications/inventory_and_mrp/inventory/inventory_valuation/cheat_sheet.html)

#### ERPNext

ERPNext separa Purchase Receipt e Purchase Invoice e documenta la media mobile come rapporto tra valore della giacenza conosciuta e quantita complessiva dopo l'entrata. Decisione: `ADOPT` per costo medio ponderato mobile e documento fisico separato dalla fattura; `DEFER` per ledger contabile, batch e seriali.

Fonti ufficiali consultate il 2026-08-28:

- [Purchase Receipt - ERPNext](https://docs.frappe.io/erpnext/purchase-receipt)
- [FIFO and Moving Average - ERPNext](https://docs.frappe.io/erpnext/fifo-and-moving-average)

#### Microsoft Dynamics 365 Business Central

Business Central usa una media ponderata configurabile per periodo, prodotto, variante e ubicazione. Decisione: `ADAPT` il principio della ponderazione; `DEFER` periodicita, varianti e ubicazioni perche lo scope corrente e a singolo magazzino.

Fonte ufficiale consultata il 2026-08-28:

- [Design details: average cost - Business Central](https://learn.microsoft.com/en-us/dynamics365/business-central/design-details-average-cost)

### 3. Regola di dominio da preservare

- Una riga di ricezione produce esattamente un movimento autorevole di tipo `PURCHASE_RECEIPT` e origine `SUPPLIER_ORDER_RECEIPT`.
- Ordine, ricezione, riga ricevuta e movimento restano collegati tramite ID stabili e vincolo univoco database.
- Il prezzo concordato dell'ordine e immutabile; il costo effettivo della ricezione e uno snapshot separato, non negativo e con quattro decimali.
- `ultimo costo` e il costo effettivo dell'ultima ricezione applicata.
- `costo medio` usa la media ponderata mobile sulle sole quantita con costo noto: `(valore noto precedente + quantita ricevuta x costo effettivo) / (quantita nota precedente + quantita ricevuta)`.
- `quantita valorizzata` non supera mai la giacenza fisica. Le giacenze storiche o manuali senza evidenza restano esplicitamente non valorizzate.
- Uno scarico riduce anche la quantita valorizzata fino al suo esaurimento; un carico manuale o un reso cliente senza costo documentato non inventa valore.
- Il primo ricevimento di un prodotto nuovo puo costituire il baseline autorevole soltanto se la giacenza precedente e zero.
- Dashboard e report espongono separatamente valore potenziale di vendita, valore gestionale a costo e copertura del costo.
- Il margine potenziale viene mostrato soltanto sulla quota valorizzata e non viene chiamato utile, ricavo o margine contabile.

Regola di arrotondamento: costi unitari e costo medio a quattro decimali con `HALF_UP`; valori aggregati e KPI monetari a due decimali con `HALF_UP`.

### 4. Adattamento al monolite attuale

Decisione generale: `ADAPT`.

Il cambiamento minimo dello Step 4.2 deve:

1. aggiungere con Flyway costo effettivo alle righe ricevute, riferimenti sorgente e costo ai movimenti, ultimo costo, costo medio e quantita valorizzata ai prodotti;
2. estendere la ricezione con costo effettivo opzionale compatibile, inizializzato dal prezzo concordato quando non specificato;
3. applicare ricezione amministrativa, carico fisico e costo nella stessa transazione;
4. rendere il posting idempotente anche tramite vincolo univoco sulla riga di ricezione;
5. esporre prezzo concordato, costo effettivo, scostamento unitario, totale ricevuto e movimento collegato nello storico;
6. mostrare ultimo costo, media mobile, valore noto e copertura in catalogo, dashboard e report riservati allo staff;
7. preservare projection cliente senza informazioni di costo;
8. coprire ricezioni parziali multiple, retry, concorrenza, arrotondamento, baseline e storico senza costo.

Compatibilita: la migrazione e additiva. I prodotti storici partono con `quantita valorizzata = 0` e costi nulli; nessun valore economico viene inferito dai prezzi di vendita.

### 5. Cosa non implementare

- Nessuna fattura passiva, debito fornitore, pagamento, IVA acquisti o scrittura contabile.
- Nessuna dichiarazione di conformita fiscale o contabile della valorizzazione.
- Nessun FIFO/LIFO, costo standard, costo per lotto, seriale, variante, sede o magazzino multiplo.
- Nessun reso a fornitore nello Step 4.2; il modello deve soltanto evitare di impedirne l'evoluzione futura.
- Nessuna valorizzazione automatica dei saldi migrati o dei carichi manuali privi di documento sorgente.
- Nessun margine su quantita prive di copertura costo e nessuna stima presentata come dato consuntivo.

### 6. Test di accettazione con un operatore reale

Stato: `PENDING_OPERATOR_EXECUTION`. Implementazione e test automatici non sostituiscono una prova umana.

Profilo richiesto: addetto acquisti o magazzino diverso dallo sviluppatore. Ambiente: build candidata isolata con dati sintetici.

Scenari obbligatori:

1. Riceve parzialmente una riga a un costo diverso dal concordato e vede stock, scostamento e primo costo medio.
2. Riceve il residuo a un secondo costo e verifica manualmente la media ponderata e l'ultimo costo.
3. Ripete la stessa richiesta e verifica che ricezione, stock e valore non raddoppino.
4. Riceve un prodotto nuovo senza saldo iniziale e ottiene un ledger riconciliato con baseline esplicito.
5. Consulta un prodotto storico con stock ma senza costo e vede copertura parziale o assente, mai un costo presunto.
6. Esegue uno scarico e verifica che quantita valorizzata e valore noto non superino la giacenza residua.
7. Confronta ordine, storico ricezioni, movimento magazzino, dashboard e report ottenendo gli stessi importi arrotondati.

Criteri di esito:

- ogni ricezione produce un solo incremento fisico e un solo movimento sorgente;
- media e ultimo costo rispettano la regola documentata;
- scostamenti prezzo e quantita sono comprensibili senza fogli esterni;
- la copertura costo distingue dati noti e mancanti;
- nessun dato viene presentato come contabilita o conformita fiscale;
- anomalie e tempi vengono registrati e trasformati in correzioni o backlog approvato.

Evidenze da registrare dopo l'esecuzione: identificativo valutatore, data, build, ordine e ricezioni sintetiche, request ID, movimenti, costi prima/dopo, calcolo indipendente, report e decisione `OPERATOR_VALIDATED` oppure `REJECTED`.

## WB-007 - Inventario fisico con operativita continua e approvazione separata

### Obiettivo

Permettere a un operatore di contare prodotti reali mentre il magazzino continua a ricevere, vendere ed evadere merce, senza trasformare automaticamente una differenza osservata in una rettifica. Ogni variazione deve restare spiegabile tramite conteggio, approvazione, movimento e audit.

### Fonti ufficiali consultate

- Odoo 17, `Inventory adjustments`: il conteggio viene registrato separatamente dalla sua applicazione; la documentazione segnala esplicitamente che movimenti successivi possono avvenire prima dell'applicazione e rende consultabile la cronologia dei movimenti. Fonte: https://www.odoo.com/documentation/17.0/applications/inventory_and_mrp/inventory/warehouses_storage/inventory_management/count_products.html
- ERPNext, `Stock Reconciliation`: il sistema recupera la quantita corrente e consente di impostare la quantita effettiva tramite un documento di riconciliazione. Fonte: https://docs.frappe.io/erpnext/stock-reconciliation
- Microsoft Dynamics 365 Business Central, `Count inventory using documents`: ordine di inventario e registrazioni fisiche sono separati; completamento e posting costituiscono passaggi distinti. Fonte: https://learn.microsoft.com/en-us/dynamics365/business-central/inventory-how-count-inventory-with-documents

Fonti verificate il 1 settembre 2026. Le scelte seguenti sono un'interpretazione progettuale conservativa adatta al perimetro monomagazzino del gestionale, non una riproduzione dei prodotti citati.

### Confronto e decisioni

| Tema | Evidenza benchmark | Decisione del gestionale |
|---|---|---|
| Documento di conteggio | I sistemi maturi separano preparazione, conteggio e posting | Sessione `OPEN`, invio `SUBMITTED`, decisione `APPROVED` o `CANCELED` |
| Operativita durante il conteggio | Odoo contempla movimenti tra conteggio e applicazione | Nessun freeze globale; la differenza osservata viene applicata alla giacenza corrente sotto lock |
| Quantita teorica | Viene confrontata con la quantita realmente contata | Snapshot all'apertura e fotografia autorevole anche al momento del conteggio |
| Governance | Posting distinto dalla raccolta del dato | Il submitter non puo approvare; permesso dedicato al responsabile |
| Rettifica | La riconciliazione produce un movimento verificabile | Un solo movimento per differenza non nulla, collegato uno-a-uno a sessione e riga |
| Storico | Lo storico deve restare consultabile | Snapshot descrittivi, attori, motivazioni, quantita prima/dopo, delta compensato e audit |
| Riserve | La disponibilita impegnata non deve diventare impossibile | Approvazione bloccata se la nuova giacenza scende sotto il riservato |

### Workflow scelto

1. L'operatore apre una sessione con motivo e ambito esplicito.
2. Il backend acquisisce i prodotti in ordine stabile, verifica saldo iniziale e assenza di altre sessioni attive e salva lo snapshot teorico.
3. L'operatore registra il contato; il backend fotografa nuovamente teorico, riservato e versione e calcola la differenza.
4. Vendite, carichi, scarichi e ricezioni possono continuare normalmente.
5. Quando tutte le righe sono contate, l'operatore invia la sessione.
6. Un account diverso con `APPROVE_INVENTORY_COUNTS` esamina le differenze e inserisce la motivazione.
7. In approvazione il backend blocca ogni prodotto, applica la differenza originaria alla giacenza corrente, rispetta le riserve e registra il movimento autorevole.
8. Sessione e righe vengono chiuse; un retry con la medesima chiave restituisce lo stesso esito senza duplicare movimenti.

### Invarianti e casi di errore

- Lo stesso prodotto non puo comparire due volte nella sessione, neppure con casing differente.
- Lo stesso prodotto non puo appartenere a due sessioni attive.
- Una sessione incompleta non puo essere inviata.
- Una sessione inviata non puo essere modificata.
- Il submitter non puo approvare la propria sessione.
- Una sessione approvata non puo essere approvata di nuovo.
- Un delta negativo non puo violare lo stock riservato.
- Una differenza zero non genera un movimento artificiale.
- Un errore in una riga annulla l'intera approvazione transazionale.
- Le rettifiche dirette fuori dal workflow sono rifiutate.

### Test automatici richiesti

- movimento di carico tra conteggio e approvazione compensato correttamente;
- doppia approvazione e auto-approvazione rifiutate;
- rettifica sotto riservato rifiutata senza effetti parziali;
- sessione attiva duplicata e codici duplicati case-insensitive rifiutati;
- permessi distinti tra dipendente, admin e super admin;
- un solo movimento fisico collegato alla riga;
- migrazione V35 su storico popolato senza sessioni o rettifiche inventate;
- interfaccia con creazione, conteggio, submit, approvazione, annullo ed errori preservati.

### Prova guidata con operatore

Stato: `PENDING_OPERATOR_EXECUTION`.

1. Aprire una sessione su almeno tre prodotti: uno senza differenza, uno con eccedenza e uno con ammanco.
2. Registrare i conteggi e verificare che teorico, riservato e differenza siano comprensibili.
3. Prima dell'invio registrare un carico o un'evasione su uno dei prodotti.
4. Inviare la sessione con un account dipendente e verificare che non possa approvarla.
5. Approvare con un admin differente e confrontare manualmente giacenza finale, differenza e movimenti intermedi.
6. Ripetere il comando di approvazione e verificare che non venga creato un secondo movimento.
7. Provare un ammanco incompatibile con una riserva e verificare il blocco senza effetti parziali.

Criteri di esito:

- ogni differenza e comprensibile e riconducibile a una riga contata;
- l'operativita intercorsa non viene persa o sovrascritta;
- approvatore e submitter sono account distinti;
- ledger, prodotto, sessione e audit espongono lo stesso risultato;
- nessuna rettifica puo essere applicata senza motivazione e autorizzazione;
- errori e retry non producono sessioni o movimenti duplicati.

Evidenze da registrare: identificativo valutatore, data, build, sessione, prodotti sintetici, snapshot, conteggi, movimenti intercorsi, approvatore, request ID, movimenti risultanti, calcolo indipendente e decisione `OPERATOR_VALIDATED` oppure `REJECTED`.

## Registro schede future

| Workflow | Step | Scheda richiesta | Stato |
|---|---|---|---|
| Vendita assistita | 3.1 | WB-001 | IMPLEMENTED |
| Dashboard cliente | 3.2 | WB-002 | IMPLEMENTED |
| Lifecycle account | 3.3 | WB-003 | IMPLEMENTED |
| Resi multi-riga | 3.8 | WB-004 | IMPLEMENTED |
| Ordini fornitore | 4.1 | WB-005 | IMPLEMENTED |
| Ricezione merce | 4.2 | WB-006 | IMPLEMENTED |
| Inventario fisico | 4.3 | WB-007 | IMPLEMENTED |
| Riordino | 4.4 | Da creare prima dello step | PENDING |
| Importazione prodotti | 4.5 | Da creare prima dello step | PENDING |
