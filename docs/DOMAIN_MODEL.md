# Domain Model

## Modello attuale backend web

### UserAccount

Rappresenta un account applicativo con username, hash password BCrypt e ruolo.

Regole account:

- le password vengono salvate solo come hash BCrypt;
- la creazione account applica una policy forte lato backend;
- la password non puo essere vuota, troppo corta, debole o troppo simile allo username;
- la registrazione pubblica non puo creare ruoli amministrativi;
- solo il super admin puo creare altri admin.

Ruoli attuali:

- `SUPER_ADMIN`
- `ADMIN`
- `EMPLOYEE`
- `CUSTOMER`

Permessi attuali:

- `VIEW_CATALOG`
- `MANAGE_PRODUCTS`
- `MANAGE_INVENTORY`
- `APPROVE_INVENTORY_COUNTS`
- `VIEW_ORDERS`
- `CREATE_ORDERS`
- `CONFIRM_ORDERS`
- `FULFILL_ORDERS`
- `CANCEL_ORDERS`
- `MANAGE_DOCUMENTS`
- `MANAGE_ACCOUNTS`
- `VIEW_PARTNERS`
- `MANAGE_PARTNERS`
- `VIEW_AUDIT`
- `VIEW_REPORTS`

### Product

Rappresenta un prodotto di catalogo.

Campi principali:

- codice univoco;
- nome;
- descrizione;
- categoria `HARDWARE` o `SOFTWARE`;
- brand;
- tipo prodotto;
- utilizzo opzionale;
- quantita fisica come proiezione transazionale del ledger di magazzino;
- quantita riservata;
- disponibilita vendibile calcolata come quantita meno riservato;
- prezzo;
- sconto;
- stato `discontinued` per disattivare la vendita senza perdere storico;
- versione tecnica per rilevare aggiornamenti concorrenti sullo stock.

Regole attuali:

- codice, nome, descrizione, brand e tipo prodotto devono essere non vuoti anche a livello database;
- l'API anagrafica non accetta quantita e crea ogni prodotto con giacenza zero;
- saldo iniziale e variazioni fisiche sono consentiti soltanto tramite comandi del modulo inventario;
- quantita, prezzo e sconto hanno vincoli database coerenti con le regole applicative;
- il codice prodotto resta modificabile solo finche il prodotto non compare in ordini;
- la cancellazione fisica e consentita solo per prodotti senza ordini collegati, stock riservato o giacenza fisica;
- un prodotto con ordini o riserve va disattivato, non eliminato;
- un prodotto disattivato non puo essere acquistato in nuovi ordini;
- un prodotto disattivato resta consultabile nello storico e gli ordini gia confermati restano evadibili.

### StockMovement

Rappresenta il ledger autorevole delle variazioni fisiche di magazzino. Ogni record collega il prodotto tramite ID stabile, conserva gli snapshot descrittivi, una variazione firmata, la giacenza precedente e nuova, l'origine e lo stato di autorevolezza.

Regole attuali:

- ogni prodotto riceve un solo saldo iniziale autorevole prima di qualsiasi altra variazione;
- i tipi autorevoli comprendono `INITIAL_BALANCE`, `LOAD`, `UNLOAD`, `FULFILLMENT`, `RETURN`, `PURCHASE_RECEIPT`, `PHYSICAL_INVENTORY_INCREASE` e `PHYSICAL_INVENTORY_DECREASE`;
- l'origine distingue saldo manuale, rettifica, movimento manuale, evasione ordine, reso e baseline di migrazione;
- la somma delle variazioni firmate autorevoli deve coincidere con la giacenza fisica proiettata sul prodotto;
- variazione firmata, giacenza precedente e giacenza nuova sono protette da vincoli database;
- lo scarico non puo portare la quantita sotto zero;
- lo scarico manuale non puo consumare quantita gia riservate da ordini confermati;
- ogni variazione fisica passa da un comando inventariale e produce il movimento nella stessa transazione;
- aggiornamenti concorrenti sullo stesso prodotto vengono intercettati tramite optimistic locking.

La migrazione `V24` classifica i movimenti precedenti come osservazioni legacy non autorevoli e crea una baseline di migrazione per ogni prodotto esistente. Queste baseline sono dichiarate non verificate: il report di riconciliazione le distingue dai saldi iniziali ricostruiti da evidenze operative.

Il report di riconciliazione espone `BALANCED`, `MISSING_INITIAL_BALANCE`, `UNVERIFIED_INITIAL_BALANCE`, `CHAIN_BROKEN` e `LEDGER_DRIFT`. Non modifica automaticamente dati storici e non trasforma una baseline di migrazione in evidenza verificata.

### PhysicalInventorySession e PhysicalInventoryItem

Rappresentano un inventario fisico governato. La sessione conserva motivo, stato e attori; ogni riga conserva snapshot teorico, conteggio, differenza e risultato dell'approvazione.

Regole attuali:

- una sessione nasce `OPEN`, viene `SUBMITTED` soltanto quando tutte le righe sono contate e termina `APPROVED` o `CANCELED`;
- un prodotto puo appartenere a una sola sessione attiva, anche quando il codice viene scritto con maiuscole diverse;
- il conteggio fotografa giacenza, riservato e versione del prodotto senza bloccare vendite o ricezioni successive;
- la differenza resta quella osservata al conteggio; in approvazione viene applicata alla giacenza corrente, compensando i movimenti avvenuti nel frattempo;
- chi invia il conteggio non puo approvarlo; l'approvazione richiede `APPROVE_INVENTORY_COUNTS`;
- una differenza non puo ridurre la giacenza sotto la quantita riservata;
- ogni differenza non nulla approvata crea un solo movimento collegato a sessione e riga; una differenza zero conserva comunque l'evidenza di approvazione;
- la rettifica diretta e disabilitata: le correzioni inventariali passano dal workflow conteggio, invio e approvazione;
- cancellazione e approvazione chiudono le righe, rendendo nuovamente conteggiabile il prodotto in una sessione futura.

### CustomerOrder, OrderItem, OrderPayment, PaymentTransaction e OrderReturn

Rappresentano un ordine con righe prodotto, totale calcolato e un pagamento strutturato associato in modo uno-a-uno.

Regole attuali:

- le righe ordine devono avere quantita positiva e importi non negativi;
- ogni riga ordine conserva snapshot immutabili di codice, nome, descrizione e prezzo catalogo applicato al momento della creazione;
- codice ordine, cliente, metodo pagamento snapshot e totale hanno vincoli database coerenti con il dominio;
- il metodo pagamento non e piu una stringa libera: i nuovi ordini ammettono `CARD`, `BANK_TRANSFER` e `CASH`;
- il pagamento conserva stato, importo richiesto, incassato, rimborsato, netto, residuo, valuta e timestamp;
- alla creazione il pagamento nasce `PENDING`, con importo richiesto uguale al totale ordine e importo pagato pari a zero;
- il vincolo univoco su `order_payments.order_id` garantisce un solo pagamento strutturato per ordine in questa fase;
- i valori storici non riconosciuti vengono migrati come `OTHER` conservando il dettaglio originale, ma `OTHER` non e selezionabile per nuovi ordini;
- `customerType` distingue `SELF_SERVICE`, `REGISTERED`, `WALK_IN` e lo storico `LEGACY_UNRESOLVED` privo di evidenza sufficiente;
- una vendita assistita usa l'ID stabile dell'anagrafica per un cliente censito oppure un nominativo esplicito senza partner fittizio per un cliente occasionale;
- se viene selezionato un cliente registrato, l'ordine conserva partner ID, codice cliente e nome/ragione sociale come riferimento operativo;
- il cliente self-service non puo selezionare partner o identita diverse dal proprio account autenticato;
- la migrazione `V30` non deduce il canale degli ordini storici: li classifica conservativamente come `LEGACY_UNRESOLVED`;
- alla creazione l'ordine nasce in bozza e non scarica il magazzino;
- la conferma ordine riserva lo stock e non modifica la giacenza fisica;
- l'evasione scarica fisicamente il magazzino, libera la riserva e rende l'ordine pronto per la generazione della fattura simulata;
- l'annullamento di una bozza non modifica lo stock;
- l'annullamento di un ordine confermato libera lo stock riservato;
- l'annullamento ordine porta a `CANCELED` un pagamento ancora pendente;
- l'annullamento di un ordine con incasso netto richiede un riferimento contabile, crea un solo `REVERSAL` per l'intero netto e porta il pagamento a `REFUNDED`;
- ordine, pagamento, reversal, rilascio delle riserve e audit appartengono alla stessa transazione e vengono ripristinati insieme in caso di errore;
- causale, riferimento, timestamp, actor e ruolo dell'annullamento restano nello storico dell'ordine;
- i pagamenti storici `UNRECONCILED` non sono annullabili finche una persona autorizzata non completa la riconciliazione;
- evasione e pagamento restano stati distinti: l'evasione non dichiara automaticamente un incasso;
- il codice ordine viene generato da sequenza database dedicata.
- ogni incasso, rimborso, reversal o riconciliazione genera un `PaymentTransaction` immutabile con codice, tipo, importo, causale, riferimento, operatore e timestamp;
- il ledger `PaymentTransaction` e la fonte autorevole degli importi; i saldi di pagamento e reso sono proiezioni sincronizzate nella stessa transazione;
- gli incassi possono essere parziali ma non possono superare il totale richiesto;
- i rimborsi sono collegati tramite ID a un reso ricevuto e non possono superare ne l'incassato netto ne il valore residuo del reso;
- un movimento `REFUND` richiede un reso, un `REVERSAL` richiede un ordine annullato e un `RECONCILIATION` identifica un solo pagamento riconciliato;
- cronologia di pagamento, movimento e reso e protetta da vincoli database;
- il reso segue `REQUESTED`, `APPROVED`, `RECEIVED`, eventuale `PARTIALLY_REFUNDED` e `REFUNDED`, oppure termina in `REJECTED`;
- una richiesta di reso puo contenere piu righe dello stesso ordine e ogni quantita deve essere positiva;
- per ogni riga ordine il backend calcola quantita gia restituita o impegnata e residuo restituibile considerando tutti i resi che riservano quantita;
- la creazione del reso acquisisce un lock sull'ordine e rivalida il residuo nella transazione, impedendo che richieste concorrenti restituiscano due volte la stessa unita;
- ogni reso ha un ID stabile usato per note, selezione operativa e associazione delle transazioni di rimborso; il codice rimane uno snapshot leggibile;
- la ricezione del reso reintegra le quantita con un movimento magazzino dedicato e impedisce doppi resi sulla stessa quantita acquistata.

Pagamenti parziali, incassi, rimborsi e resi sono attivi tramite transazioni applicative, permessi dedicati, audit e idempotenza.

Il report finanziario confronta periodicamente le proiezioni con il ledger e segnala drift, stati, collegamenti o timestamp incompatibili. Il controllo e read-only: preserva le evidenze e richiede una correzione manuale revisionata quando rileva un'anomalia.

### CompanySettings e DocumentNumberCounter

`CompanySettings` rappresenta la configurazione aziendale singleton del gestionale. Non contiene dati aziendali inventati: la migrazione inizializza vuoti i campi identificativi e mantiene soltanto valori tecnici espliciti per IVA e prefissi.

Regole attuali:

- aggiornamento consentito solo con `MANAGE_COMPANY_SETTINGS`, assegnato al super admin;
- optimistic locking tramite `version` e risposta `409` su modifica obsoleta;
- ragione sociale e almeno uno tra codice fiscale e partita IVA determinano lo stato configurato;
- aliquota predefinita compresa tra 0 e 1, con massimo quattro decimali;
- prefissi fattura e nota credito distinti e alfanumerici;
- lunghezza progressivo tra 3 e 8 cifre;
- prefissi e lunghezza non cambiano dopo il primo documento dell'esercizio;
- ogni aggiornamento e auditato.

`DocumentNumberCounter` alloca progressivi distinti per tipo documento ed esercizio. L'allocazione avviene nella stessa transazione del documento e usa un lock pessimista sulla configurazione prima del contatore, garantendo un ordine di lock stabile anche con piu istanze backend.

### FiscalDocument e FiscalDocumentLine

Rappresentano fattura simulata e nota credito simulata. Non sono documenti fiscali reali.

Regole attuali:

- una fattura simulata per ordine;
- fattura simulata solo dopo evasione ordine;
- nota credito simulata solo dopo fattura simulata;
- una nota credito simulata per ordine;
- il database impone l'unicita della coppia ordine collegato e tipo documento;
- importi, aliquota IVA e righe documento sono protetti da vincoli database;
- codice nel formato `PREFISSO-ANNO-PROGRESSIVO`, con progressivo univoco per tipo ed esercizio;
- aliquota predefinita letta dalla configurazione aziendale al momento della fattura;
- snapshot emittente: ragione sociale, identificativi fiscali, contatti e indirizzo;
- i documenti salvano snapshot cliente: codice, nome/ragione sociale, codice fiscale, partita IVA, email, telefono, indirizzo e citta;
- la nota credito copia dalla fattura originale snapshot emittente, snapshot cliente, aliquota e importi;
- i dati storici non cambiano quando configurazione aziendale o anagrafica vengono aggiornate.

### AuditEvent

Registra attore, ruolo, azione, target, dettagli, categoria, severita, `requestId`, origine richiesta e tipo entita coinvolta.

### Reporting read model

I report non introducono nuove entita persistenti. Sono proiezioni di sola lettura costruite dai dati di ordini, pagamenti, righe ordine e prodotti.

Regole attuali:

- il report vendite distingue valore ordini, incassato, rimborsato, netto incassato e residuo;
- il periodo predefinito va dall'inizio dell'anno corrente alla data odierna e non puo superare cinque anni;
- il report magazzino distingue giacenza fisica, riservata e disponibile e valorizza lo stock al prezzo scontato corrente;
- ogni dataset e limitato a 10.000 righe e deve essere ristretto tramite filtri oltre tale soglia;
- gli export sono generati su richiesta e non vengono salvati nel database;
- i report sono strumenti gestionali interni e non costituiscono rendicontazione fiscale certificata.

### LoginAttempt

Registra i tentativi login falliti normalizzati per username, il conteggio, il periodo del primo/ultimo tentativo e l'eventuale blocco temporaneo.

### AuthSession

Rappresenta una sessione browser persistita senza conservare il token in chiaro.

Regole attuali:

- il database conserva l'hash SHA-256 del token, username, ruolo, creazione, scadenza assoluta, ultima attivita e revoca;
- il token in chiaro viene restituito soltanto a login o rinnovo e resta nella memoria del frontend;
- il rinnovo acquisisce un lock sulla sessione, revoca il token corrente e ne emette uno nuovo nella stessa transazione;
- una sessione scade al raggiungimento del limite assoluto o del timeout di inattivita;
- l'ultima attivita viene aggiornata a intervalli configurabili per limitare le scritture;
- token scaduti, inattivi, revocati o associati ad account rimossi non sono accettati.

### IdempotencyRecord

Registra le richieste critiche ricevute con `Idempotency-Key`.

Regole attuali:

- la chiave e valida per utente autenticato e operazione;
- la richiesta viene confrontata tramite hash del payload operativo;
- una richiesta ripetuta con stessi dati restituisce la risposta gia salvata;
- una chiave riusata con dati diversi genera conflitto applicativo;
- le risposte salvate riguardano operazioni concluse correttamente.

### BusinessPartner

Rappresenta un soggetto commerciale dell'anagrafica.

Tipi attuali:

- `CUSTOMER`
- `SUPPLIER`

Campi principali:

- codice univoco;
- tipo soggetto;
- nome o ragione sociale;
- codice fiscale;
- partita IVA;
- email;
- telefono;
- indirizzo;
- citta;
- note;
- stato attivo o disattivato.

Regole attuali:

- codice e nome/ragione sociale sono obbligatori anche a livello database;
- i campi fiscali e di contatto possono restare vuoti quando non disponibili;
- la disattivazione preserva lo storico operativo.

## Ordine fornitore

`SupplierOrder` rappresenta l'impegno di approvvigionamento e possiede:

- codice business stabile;
- ID fornitore con snapshot di codice e nome;
- stato `DRAFT`, `SENT`, `PARTIALLY_RECEIVED`, `RECEIVED` o `CANCELED`;
- data prevista, note, versione concorrente e metadati di creazione, invio e annullo;
- una o piu `SupplierOrderItem` con ID prodotto e snapshot di codice, nome, prezzo, quantita ordinata, quantita ricevuta e data prevista;
- zero o piu `SupplierOrderReceipt`, ciascuna con codice stabile, causale, attore, timestamp e righe ricevute.

Regole attuali:

- solo una bozza puo essere inviata;
- una ricezione e ammessa solo su ordine inviato o parzialmente ricevuto;
- la quantita ricevuta e cumulativa e non supera mai l'ordinato;
- lo stato diventa `RECEIVED` soltanto quando ogni riga ha residuo zero;
- l'annullo richiede una causale, preserva quanto ricevuto e impedisce ulteriori ricezioni;
- invio, ricezione e annullo acquisiscono un lock sull'ordine e sono idempotenti per intento;
- un prodotto referenziato da un ordine fornitore non puo essere rinominato o eliminato;
- ogni riga ricevuta conserva costo concordato, costo effettivo, scostamento, totale e movimento inventariale collegato;
- la ricezione applica ordine, carico fisico e costo nella stessa transazione e un retry non puo duplicare il movimento;
- ultimo costo e costo medio ponderato mobile sono aggiornati soltanto sulle quantita con costo noto;
- lo stock storico privo di evidenza resta non valorizzato e non eredita il prezzo di vendita.

## Relazioni attuali

- `CustomerOrder` contiene piu `OrderItem`.
- `FiscalDocument` contiene piu `FiscalDocumentLine`.
- `StockMovement` collega il prodotto tramite ID, conserva codice e nome come snapshot e registra tutte le variazioni fisiche, incluse evasione e ricezione reso.
- `CustomerOrder` puo puntare a un `BusinessPartner` di tipo cliente tramite `customerCode`.
- `AuditEvent` e indipendente e conserva dati testuali dell'evento insieme al contesto di richiesta.
- `LoginAttempt` e indipendente dagli account per poter tracciare anche username non validi.
- `IdempotencyRecord` e indipendente dalle entita operative e conserva chiave, attore, operazione, hash richiesta e risposta salvata.
- `BusinessPartner` e usato come anagrafica operativa per clienti e fornitori.
- `SupplierOrder` contiene piu `SupplierOrderItem` e piu `SupplierOrderReceipt`; una ricezione contiene piu `SupplierOrderReceiptItem`, ognuno collegabile a un solo `StockMovement` di tipo `PURCHASE_RECEIPT`.
- `PhysicalInventorySession` contiene piu `PhysicalInventoryItem`; una differenza approvata non nulla collega la riga a un solo `StockMovement` fisico.
- `SupplierOrder` collega il fornitore e le righe tramite ID stabili ma conserva snapshot descrittivi separati per proteggere lo storico.
- `FiscalDocument` conserva uno snapshot di `CompanySettings`; non mantiene una relazione viva verso la configurazione.
- `DocumentNumberCounter` e identificato da tipo documento ed esercizio.

## Invarianti attuali

- Ogni modifica della giacenza fisica produce un movimento autorevole nella stessa transazione.
- La somma dei delta autorevoli coincide con la quantita fisica proiettata sul prodotto.
- L'anagrafica prodotto non puo modificare direttamente la quantita.
- Nessuna rettifica puo portare la giacenza sotto lo stock riservato.
- Un ordine attivo non puo avere metadati di annullamento e un ordine `CANCELED` deve avere causale, timestamp, actor e ruolo.
- Ogni ordine puo avere al massimo un reversal di annullamento, obbligatoriamente riferito all'ordine e dotato di riferimento contabile.
- Un ordine annullato non puo conservare un incasso netto positivo ne una quantita riservata.
- Una riga ordine fornitore non puo avere quantita ricevuta negativa o superiore alla quantita ordinata.
- Un ordine fornitore ricevuto non puo avere residui; un ordine annullato deve avere causale e metadati di annullo.
- Una sessione di inventario non puo essere approvata dal medesimo account che l'ha inviata.
- Una rettifica di inventario approvata applica la differenza osservata alla giacenza corrente e non puo violare le riserve.

## Invarianti da rafforzare

- Il prezzo di una riga ordine deve rimanere snapshot del momento di acquisto.
- I documenti simulati non devono essere presentati come fiscalmente validi.
- Un account deve rispettare la password policy backend al momento della creazione.
- Un ordine non puo avere piu di una fattura simulata o piu di una nota credito simulata.
- Nessun account viene eliminato fisicamente dal workflow operativo; la disabilitazione preserva storico, ownership e username riservato.
- Un admin non puo disabilitare o modificare il proprio account tramite operazioni amministrative.
- Solo il super admin puo creare o gestire account admin.
- La numerazione documento deve restare univoca e monotona per tipo ed esercizio anche in concorrenza.

## Gap verso prodotto professionale

- La numerazione applicativa annuale non equivale a numerazione fiscale certificata e non integra il Sistema di Interscambio.
- E disponibile una sola aliquota IVA predefinita per documento; non sono ancora modellate righe multi-aliquota, esenzioni o regimi fiscali.
- Mancano ubicazioni e magazzini multipli; il modello target separa prodotto, magazzino e saldo in `MULTI_TENANCY_ARCHITECTURE.md`.
- Mancano tenant/aziende; ADR 0005 definisce l'isolamento target, ma l'implementazione non e ancora iniziata.
- Mancano richieste privacy, policy retention, legal hold, fatture elettroniche, ricevute SdI ed evidenze di conservazione; ADR 0006 ne separa i confini senza dichiararne l'implementazione.
- Mancano customer account, installation registry, onboarding saga, brand profile, entitlement e release fleet; ADR 0007 ne definisce i confini senza dichiararne l'implementazione.
- Mancano migrazioni e vincoli database espliciti per le prossime entita operative.
- Gli export sono sincroni e in memoria; dataset superiori al limite richiederanno un futuro processo asincrono o streaming.
