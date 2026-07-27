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
- quantita;
- quantita riservata;
- disponibilita vendibile calcolata come quantita meno riservato;
- prezzo;
- sconto;
- stato `discontinued` per disattivare la vendita senza perdere storico;
- versione tecnica per rilevare aggiornamenti concorrenti sullo stock.

Regole attuali:

- codice, nome, descrizione, brand e tipo prodotto devono essere non vuoti anche a livello database;
- quantita, prezzo e sconto hanno vincoli database coerenti con le regole applicative;
- il codice prodotto resta modificabile solo finche il prodotto non compare in ordini;
- la cancellazione fisica e consentita solo per prodotti senza ordini collegati e senza stock riservato;
- un prodotto con ordini o riserve va disattivato, non eliminato;
- un prodotto disattivato non puo essere acquistato in nuovi ordini;
- un prodotto disattivato resta consultabile nello storico e gli ordini gia confermati restano evadibili.

### StockMovement

Rappresenta un movimento di magazzino con tipo `LOAD` o `UNLOAD`, quantita precedente e nuova quantita.

Regole attuali:

- quantita movimento, giacenza precedente e giacenza nuova sono protette da vincoli database;
- lo scarico non puo portare la quantita sotto zero;
- lo scarico manuale non puo consumare quantita gia riservate da ordini confermati;
- ogni variazione passa da un'operazione centralizzata di aggiustamento stock;
- aggiornamenti concorrenti sullo stesso prodotto vengono intercettati tramite optimistic locking.

### CustomerOrder, OrderItem, OrderPayment, PaymentTransaction e OrderReturn

Rappresentano un ordine con righe prodotto, totale calcolato e un pagamento strutturato associato in modo uno-a-uno.

Regole attuali:

- le righe ordine devono avere quantita positiva e importi non negativi;
- codice ordine, cliente, metodo pagamento snapshot e totale hanno vincoli database coerenti con il dominio;
- il metodo pagamento non e piu una stringa libera: i nuovi ordini ammettono `CARD`, `BANK_TRANSFER` e `CASH`;
- il pagamento conserva stato, importo richiesto, incassato, rimborsato, netto, residuo, valuta e timestamp;
- alla creazione il pagamento nasce `PENDING`, con importo richiesto uguale al totale ordine e importo pagato pari a zero;
- il vincolo univoco su `order_payments.order_id` garantisce un solo pagamento strutturato per ordine in questa fase;
- i valori storici non riconosciuti vengono migrati come `OTHER` conservando il dettaglio originale, ma `OTHER` non e selezionabile per nuovi ordini;
- l'ordine puo essere collegato a un cliente dell'anagrafica tramite `customerCode`;
- se viene selezionato un cliente registrato, l'ordine conserva codice cliente e nome/ragione sociale come riferimento operativo;
- alla creazione l'ordine nasce in bozza e non scarica il magazzino;
- la conferma ordine riserva lo stock e non modifica la giacenza fisica;
- l'evasione scarica fisicamente il magazzino, libera la riserva e rende l'ordine pronto per la generazione della fattura simulata;
- l'annullamento di una bozza non modifica lo stock;
- l'annullamento di un ordine confermato libera lo stock riservato;
- l'annullamento ordine porta a `CANCELED` un pagamento ancora pendente;
- evasione e pagamento restano stati distinti: l'evasione non dichiara automaticamente un incasso;
- il codice ordine viene generato da sequenza database dedicata.
- ogni incasso o rimborso genera un `PaymentTransaction` immutabile con codice, tipo, importo, causale, riferimento, operatore e timestamp;
- gli incassi possono essere parziali ma non possono superare il totale richiesto;
- i rimborsi sono collegati a un reso ricevuto e non possono superare ne l'incassato netto ne il valore residuo del reso;
- il reso segue `REQUESTED`, `APPROVED`, `RECEIVED`, eventuale `PARTIALLY_REFUNDED` e `REFUNDED`, oppure termina in `REJECTED`;
- la ricezione del reso reintegra le quantita con un movimento magazzino dedicato e impedisce doppi resi sulla stessa quantita acquistata.

Pagamenti parziali, incassi, rimborsi e resi sono attivi tramite transazioni applicative, permessi dedicati, audit e idempotenza.

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

## Relazioni attuali

- `CustomerOrder` contiene piu `OrderItem`.
- `FiscalDocument` contiene piu `FiscalDocumentLine`.
- `StockMovement` conserva codice e nome prodotto come snapshot testuale e registra lo scarico fisico al momento dell'evasione.
- `CustomerOrder` puo puntare a un `BusinessPartner` di tipo cliente tramite `customerCode`.
- `AuditEvent` e indipendente e conserva dati testuali dell'evento insieme al contesto di richiesta.
- `LoginAttempt` e indipendente dagli account per poter tracciare anche username non validi.
- `IdempotencyRecord` e indipendente dalle entita operative e conserva chiave, attore, operazione, hash richiesta e risposta salvata.
- `BusinessPartner` e usato come anagrafica operativa per clienti e fornitori.
- `FiscalDocument` conserva uno snapshot di `CompanySettings`; non mantiene una relazione viva verso la configurazione.
- `DocumentNumberCounter` e identificato da tipo documento ed esercizio.

## Invarianti da rafforzare

- Ogni modifica stock deve produrre un movimento.
- Il prezzo di una riga ordine deve rimanere snapshot del momento di acquisto.
- I documenti simulati non devono essere presentati come fiscalmente validi.
- Un account deve rispettare la password policy backend al momento della creazione.
- Un ordine non puo avere piu di una fattura simulata o piu di una nota credito simulata.
- Un admin non deve potersi autocancellare.
- Solo super admin puo creare o rimuovere admin.
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
