# API Contract

## Timestamp

- Il backend interpreta come UTC i timestamp storici persistiti senza offset.
- Gli eventi nelle risposte API sono stringhe ISO 8601 con offset esplicito.
- Gli eventi operativi generali usano UTC; i documenti espongono l'offset del fuso salvato nello snapshot aziendale.
- L'anno documentale e calcolato dall'istante corrente nel fuso IANA configurato per l'azienda.
- Le definizioni complete dei KPI e del contratto temporale sono in `docs/KPI_DEFINITIONS.md`.

## Paginazione

Le liste operative principali restituiscono una risposta paginata:

```json
{
  "content": [],
  "page": 0,
  "size": 25,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

Endpoint paginati:

| Endpoint | Filtri supportati |
| --- | --- |
| `GET /api/products` | `page`, `size`, `q`, `category`, `brand`, `productType`, `stock`, `sort` |
| `GET /api/customer/catalog` | `page`, `size`, `q`, `category`, `brand`, `productType`, `stock`, `sort`; `stock` usa i soli livelli commerciali e gli ordinamenti ammessi sono nome e prezzo |
| `GET /api/partners` | `page`, `size`, `q`, `type`, `active` |
| `GET /api/inventory/movements` | `page`, `size`, `q`, `type`, `productCode` |
| `GET /api/orders` | `page`, `size`, `q`, `customer` |
| `GET /api/orders/customer/{customer}` | `page`, `size`, `q`; per `CUSTOMER` il path e solo compatibilita e viene ignorato |
| `GET /api/documents` | `page`, `size`, `q`, `type` |
| `GET /api/accounts` | `page`, `size`, `q`, `role`, `enabled` |
| `GET /api/audit` | `page`, `size`, `q`, `category`, `severity`; `q` cerca anche `requestId`, origine richiesta e tipo entita |

Regole:

- `page` parte da `0`.
- `size` viene normalizzato e limitato dal backend.
- Il frontend puo usare `content` per i dati visibili e `totalElements`/`totalPages` per i controlli di pagina.
- Il contenuto di una pagina non e una fonte completa per autorizzazioni, capability o aggregazioni: tali decisioni arrivano da endpoint di dettaglio o DTO server-side dedicati.
- I clienti vedono solo i propri ordini anche se inviano un filtro `customer` diverso.
- Per un cliente autenticato, liste, dettaglio e transizioni usano sempre `accountId` della sessione; username, nome cliente e parametro path non autorizzano accessi.

## Dashboard

- `GET /api/dashboard` richiede il permesso operativo `VIEW_REPORTS`.
- La risposta operativa distingue prodotti, valore potenziale di vendita, scorte, ordini per stato, incassato lordo, rimborsato/stornato e incassato netto, oltre a ordini e movimenti recenti.
- `potentialRetailStockValue` e quantita fisica per prezzo di vendita scontato corrente: non e una valorizzazione contabile dell'inventario.
- `orders` separa conteggi e valori di `DRAFT`, `CONFIRMED`, `FULFILLED` e `CANCELED`; `grossCollected`, `refunded` e `netCollected` derivano dal ledger finanziario.
- I movimenti magazzino recenti sono inclusi solo per ruoli con permesso `MANAGE_INVENTORY`.
- `GET /api/customer/dashboard` e riservato a `CUSTOMER`, usa l'ID account stabile della sessione e restituisce soltanto conteggi per stato e riepiloghi recenti dei propri ordini.
- La dashboard cliente non contiene KPI globali, ricavi aziendali, dati inventariali, movimenti di magazzino o ordini di altri account.
- Le dashboard non espongono token, password o dati di sicurezza sensibili.
- Il frontend operativo usa esclusivamente questa aggregazione e non calcola KPI o attivita recenti dalle prime righe delle liste paginate.

## Sessioni

- `POST /api/accounts/login` accetta soltanto `username` e `password`; ruolo e permessi sono derivati dall'account autorevole lato server.
- Il login restituisce un token da inviare con header `X-Session-Token`.
- Il backend conserva solo l'hash del token nella tabella `auth_sessions`.
- Ogni sessione identifica l'account tramite il suo ID database stabile; lo username e conservato soltanto come snapshot diagnostico e non viene usato come subject autorizzativo.
- La sessione conserva la versione delle credenziali dell'account. Cambio password o ruolo incrementano tale versione e revocano tutte le sessioni emesse in precedenza.
- Disabilitare un account incrementa la versione credenziali e revoca tutte le sessioni; lo username resta riservato e non puo essere riutilizzato.
- Logout, rinnovo e aggiornamento dell'ultima attivita serializzano l'accesso alla riga di sessione: una revoca concorrente prevale sempre sul touch.
- La durata assoluta standard della sessione e 45 minuti e il timeout di inattivita standard e 30 minuti.
- `POST /api/accounts/session/renew` richiede sessione valida e password dell'account autenticato; la risposta ha lo stesso formato del login.
- Il rinnovo ruota il token: il token precedente viene revocato nella stessa transazione e non puo essere riutilizzato.
- Se il token e gia scaduto o inattivo, il rinnovo autenticato viene rifiutato e il client deve eseguire un nuovo login.
- Login e rinnovo restituiscono `Cache-Control: no-store` e `Pragma: no-cache`.
- Il logout revoca la sessione salvando `revoked_at`.
- Se l'account e disabilitato, non esiste oppure la sessione e scaduta/revocata, gli endpoint protetti restituiscono `AUTH_UNAUTHORIZED` senza rivelare lo stato dell'account.
- Dopo disabilitazione, modifica password, reset amministrativo o cambio ruolo, tutti i token precedenti restituiscono `AUTH_UNAUTHORIZED`; e necessario un nuovo login dopo l'eventuale riabilitazione.
- Se un account operativo storico e in attesa di revisione, ogni sua sessione viene revocata e gli endpoint protetti restituiscono `AUTH_UNAUTHORIZED`; la revoca viene confermata anche quando la richiesta termina con `401`.
- Il token non deve comparire in log, errori o dettagli diagnostici.
- Il frontend conserva il token solo in memoria e non usa cookie di autenticazione, `localStorage` o `sessionStorage`.
- CSRF resta disabilitato per questo contratto basato su header non ambientale; un passaggio a cookie HttpOnly richiede una migrazione atomica con protezione CSRF.
- I tentativi login falliti vengono persistiti in `login_attempts`.
- Dopo 5 tentativi falliti per la stessa identita canonica, il login viene bloccato temporaneamente per 5 minuti; il lockout non disabilita permanentemente l'account.
- Un job programmato elimina sessioni obsolete, lock scaduti e tentativi falliti non piu rilevanti.

## Password policy

- La robustezza password viene controllata lato backend, non solo dal frontend.
- La policy e centralizzata e viene applicata a registrazione pubblica, creazione account amministrativa, cambio password personale e reset amministrativo.
- Una password valida deve avere almeno 8 caratteri, maiuscole, minuscole, numeri e simboli.
- La password non deve essere troppo simile allo username.
- `POST /api/accounts/password-strength` fornisce una valutazione indicativa, ma non sostituisce il controllo bloccante in creazione account.
- Se la password non rispetta la policy, il backend risponde `400` con codice `REQUEST_INVALID` e messaggio operativo senza esporre dati sensibili.

## Account

- Lo username visualizzato viene ripulito dagli spazi esterni; l'identita tecnica e `lower(trim(username))` ed e univoca tramite vincolo database.
- Login, ricerca e creazione interpretano casing e spazi esterni in modo coerente; una collisione canonica restituisce `409 RESOURCE_CONFLICT`.
- `GET /api/accounts` richiede `MANAGE_ACCOUNTS` e restituisce una risposta paginata ordinata per username crescente.
- Il filtro `q` cerca nello username.
- Il filtro `role` accetta `SUPER_ADMIN`, `ADMIN`, `EMPLOYEE` e `CUSTOMER`.
- Il filtro `enabled` distingue account abilitati e disabilitati.
- `POST /api/accounts/register` accetta esclusivamente `username` e `password` e crea sempre un account `CUSTOMER`.
- Se il payload di registrazione pubblica contiene `role`, la richiesta viene rifiutata con `422 VALIDATION_FAILED`, anche quando il valore richiesto e `CUSTOMER`.
- Le registrazioni pubbliche sono auditabili con attore `SELF_SERVICE`; password e altri dati sensibili non entrano nell'audit.
- Ogni account conserva una provenienza tra `SELF_SERVICE`, `ADMIN_PROVISIONED`, `BOOTSTRAP` e `UNKNOWN`.
- `GET /api/accounts/security-review` e riservato al super admin e restituisce gli account self-service cliente e gli account operativi, con classificazione, stato di verifica, sessioni e conteggi audit sensibili.
- `POST /api/accounts/security-review/{username}/verify` e riservato al super admin, richiede `X-Reauth-Password` e abilita un account operativo soltanto dopo revisione manuale.
- La verifica non riattiva sessioni precedenti: l'account deve effettuare un nuovo login.
- Gli account `EMPLOYEE` e `ADMIN` possono essere creati soltanto tramite `POST /api/accounts`, con sessione amministrativa e ri-autenticazione.
- La creazione di account admin e riservata al super admin.
- `POST /api/accounts/me/password` richiede password corrente e nuova password valida; al successo revoca tutte le sessioni, inclusa quella corrente.
- `POST /api/accounts/{username}/disable` e `/enable` richiedono una motivazione e ri-autenticazione amministrativa tramite `X-Reauth-Password`.
- `POST /api/accounts/{username}/password-reset`, `/sessions/revoke` e `/role` sono operazioni amministrative sensibili, richiedono motivazione e ri-autenticazione e revocano le sessioni quando applicabile.
- Nessun endpoint operativo elimina fisicamente account; i vecchi endpoint `DELETE` sono mantenuti per compatibilita ma applicano una disabilitazione auditata.
- Un amministratore non puo disabilitare o gestire il proprio account attraverso le operazioni amministrative.
- Il super admin non puo essere disabilitato o modificato dal pannello operativo.
- Soltanto il super admin puo gestire stato, credenziali o ruolo di un account `ADMIN`.
- Riabilitare un account non ripristina le sessioni precedenti e non aggira l'eventuale revisione dell'accesso operativo.
- La migrazione `V31` conserva gli account esistenti come abilitati e introduce stato lifecycle, metadati di disabilitazione e relativo vincolo di coerenza.
- La migrazione `V20` revoca intenzionalmente tutte le sessioni legacy durante il passaggio dal subject username al subject account ID.

## Idempotenza

Le operazioni critiche possono ricevere header `Idempotency-Key`.

Endpoint coperti:

- `POST /api/orders`
- `POST /api/orders/{code}/confirm`
- `POST /api/orders/{code}/fulfill`
- `POST /api/orders/{code}/cancel`
- `POST /api/inventory/movements`
- `POST /api/inventory/initial-balance`
- `POST /api/inventory/counts`
- `POST /api/inventory/counts/{id}/submit`
- `POST /api/inventory/counts/{id}/approve`
- `POST /api/inventory/counts/{id}/cancel`
- `POST /api/orders/{code}/payments/receipts`
- `POST /api/orders/{code}/payments/reconciliation`
- `POST /api/orders/{code}/returns`
- `POST /api/orders/{code}/returns/{returnCode}/approve`
- `POST /api/orders/{code}/returns/{returnCode}/reject`
- `POST /api/orders/{code}/returns/{returnCode}/receive`
- `POST /api/orders/{code}/returns/{returnCode}/refund`
- `POST /api/documents/invoice`
- `POST /api/documents/credit-note`

Regole:

- la chiave e scoped per ID stabile dell'account autenticato e operazione;
- il fingerprint include account, endpoint/operazione e payload canonico; una chiave non puo identificare due intenti diversi;
- il claim viene acquisito atomicamente dal database e usa gli stati `IN_PROGRESS`, `COMPLETED` e `FAILED_RETRYABLE`;
- mutazione business e risposta persistita vengono confermate nella stessa transazione;
- se la stessa richiesta completata viene ripetuta con la stessa chiave, il backend restituisce la risposta gia prodotta senza rieseguire la mutazione;
- se la stessa chiave viene riutilizzata con dati diversi, il backend risponde `409` con codice `IDEMPOTENCY_CONFLICT`;
- se la stessa operazione e ancora in corso oltre l'attesa breve, il backend risponde `409 IDEMPOTENCY_IN_PROGRESS` con `Retry-After`; il client deve riprovare con la stessa chiave;
- dopo timeout, errore di rete o risposta persa il client mantiene la chiave; ne genera una nuova soltanto dopo un esito definitivo o quando cambia il payload;
- un claim fallito in modo ripetibile puo essere riacquisito senza duplicare una mutazione gia completata;
- i record scaduti vengono rimossi da un cleanup programmato; lease e retention predefiniti sono rispettivamente 5 minuti e 24 ore;
- se l'header manca, l'endpoint continua a comportarsi come una normale richiesta non idempotente;
- la chiave non deve contenere dati sensibili.

## Audit

Gli eventi audit espongono anche:

- `requestId`: codice richiesta associato all'operazione;
- `source`: metodo e path HTTP, oppure `SYSTEM` per eventi non legati a una richiesta;
- `entityType`: tipo logico dell'entita coinvolta, ad esempio `PRODUCT`, `USER_ACCOUNT`, `CUSTOMER_ORDER`, `STOCK_MOVEMENT`.

Le modifiche sensibili devono registrare un riepilogo operativo chiaro senza includere password, token o dati non necessari.

## Codici operativi

- Gli ordini usano codici `ORD-0001`, `ORD-0002`, ecc. generati da sequenza database `order_code_seq`.
- Le fatture simulate usano codici `FS-0001`, `FS-0002`, ecc. generati da `invoice_code_seq`.
- Le note credito simulate usano codici `NC-0001`, `NC-0002`, ecc. generati da `credit_note_code_seq`.
- Il backend non usa il conteggio righe per generare nuovi codici.
- In caso di dati legacy con codici gia presenti, il backend salta eventuali collisioni e richiede un nuovo valore di sequenza.

## Magazzino

- `POST /api/inventory/initial-balance` registra l'unico saldo iniziale ammesso per un prodotto e richiede `MANAGE_INVENTORY`.
- `POST /api/inventory/adjustments` resta disponibile per compatibilita ma rifiuta le rettifiche dirette con `409`: una differenza deve passare dal workflow di inventario fisico.
- `POST /api/inventory/movements` mantiene i comandi espliciti `LOAD` e `UNLOAD` per compatibilita operativa.
- `GET /api/inventory/counts` e `GET /api/inventory/counts/{id}` restituiscono sessioni e righe di conteggio secondo capability calcolate dal backend.
- `POST /api/inventory/counts` apre una sessione sull'intero catalogo o su codici espliciti e richiede `MANAGE_INVENTORY`.
- `PUT /api/inventory/counts/{sessionId}/items/{itemId}` registra o aggiorna il conteggio mentre la sessione e aperta.
- `POST /api/inventory/counts/{id}/submit` congela i conteggi completi e li invia al responsabile.
- `POST /api/inventory/counts/{id}/approve` richiede `APPROVE_INVENTORY_COUNTS`, una causale e un approvatore diverso dal submitter.
- `POST /api/inventory/counts/{id}/cancel` chiude senza rettificare e richiede una causale.
- Vendite, evasione, resi e ricezioni restano operative durante il conteggio; l'approvazione applica la differenza osservata alla giacenza corrente sotto lock.
- Le rettifiche approvate espongono giacenza prima/dopo, riservato, movimento compensato e ID del movimento autorevole.
- `GET /api/inventory/reconciliation` confronta ledger autorevole, catena dei movimenti e proiezione di giacenza; richiede `MANAGE_INVENTORY`.
- Ogni variazione fisica scrive un movimento nella stessa transazione e ne espone `productId`, `deltaQuantity`, `origin`, `authoritative` e `baseline`.
- Prima di qualsiasi variazione deve esistere un saldo iniziale autorevole.
- Lo scarico non puo rendere negativa la giacenza.
- Lo scarico manuale non puo scendere sotto la quantita gia riservata.
- Le risposte prodotto espongono `quantity`, `reservedQuantity` e `availableQuantity`.
- I filtri stock lavorano sulla disponibilita vendibile, non sulla sola giacenza fisica.
- `GET /api/inventory/low-stock` restituisce prodotti con disponibilita vendibile maggiore di zero e minore o uguale alla soglia operativa di 3 unita.
- I prodotti esauriti non sono conteggiati come scorte basse: vanno trattati come stato separato.
- Se due operazioni modificano lo stesso prodotto in concorrenza, il backend restituisce `409` con codice `RESOURCE_CONFLICT`.
- Il movimento conserva sempre snapshot di codice prodotto, nome prodotto, quantita precedente e nuova quantita.
- Il report usa gli stati `BALANCED`, `MISSING_INITIAL_BALANCE`, `UNVERIFIED_INITIAL_BALANCE`, `CHAIN_BROKEN` e `LEDGER_DRIFT`.
- Le baseline create da `V24` per dati esistenti sono autorevoli per continuita operativa ma restano non verificate fino a riconciliazione basata su evidenze.

## Prodotti

- `GET /api/products`, `GET /api/products/{code}` e `GET /api/products/lookup` richiedono `MANAGE_INVENTORY` e sono endpoint operativi riservati al personale.
- Le risposte operative espongono giacenza fisica, quantita riservata, disponibilita vendibile e stato disattivato; non devono essere usate nel percorso cliente.
- `GET /api/products/{code}/detail` restituisce il prodotto con `capabilities`, movimenti recenti mirati e ordini recenti che lo contengono, indipendentemente dalla pagina globale correntemente visualizzata.
- Le capability prodotto sono `canEdit`, `canChangeCode`, `canDelete`, `canDiscontinue` e `canMoveStock`; il backend le deriva da permessi, uso negli ordini, giacenza, riserve e stato disattivato.
- `GET /api/customer/catalog` e riservato a `CUSTOMER` con `VIEW_CATALOG` e restituisce una projection commerciale dedicata con `code`, `name`, `description`, `category`, `brand`, `productType`, `usageContext`, `price`, `discount`, `discountedPrice`, `availability` e `availabilityLabel`.
- La projection cliente non contiene ID interni, `quantity`, `reservedQuantity`, `availableQuantity`, `discontinued` o valore potenziale di vendita. I prodotti disattivati sono esclusi prima della serializzazione.
- `availability` e calcolata nel backend sulla disponibilita vendibile: `AVAILABLE` oltre 3 unita, `LIMITED` da 1 a 3 e `UNAVAILABLE` a 0. Il client non riceve un limite numerico dal quale inferire lo stock.
- `POST /api/products`, `PUT /api/products/{code}`, `DELETE /api/products/{code}`, `POST /api/products/bulk-delete` e `POST /api/products/{code}/discontinue` richiedono `MANAGE_PRODUCTS`.
- I payload di creazione e modifica prodotto non contengono `quantity`; eventuali campi aggiuntivi inviati non possono modificare la giacenza.
- Un nuovo prodotto nasce sempre con giacenza zero e riceve il saldo tramite il modulo inventario.
- Le risposte prodotto espongono anche `discontinued`.
- Il codice prodotto puo essere modificato solo finche il prodotto non compare in righe ordine.
- Un prodotto non puo essere eliminato se ha giacenza fisica o stock riservato da ordini confermati.
- Un prodotto non puo essere eliminato se compare in ordini, anche non confermati; in quel caso va disattivato.
- Un prodotto disattivato resta leggibile nello storico e nel catalogo, ma non puo essere acquistato in nuovi ordini.
- Gli ordini gia confermati con un prodotto successivamente disattivato restano evadibili, preservando storico e stock riservato.
- Il database rifiuta prodotti con codice, nome, descrizione, brand o tipo prodotto vuoti.
- Il codice prodotto conserva il valore visuale ripulito e usa `lower(trim(code))` per lookup e unicita; collisioni sequenziali o concorrenti restituiscono `409 RESOURCE_CONFLICT`.
- Il database rifiuta quantita negative, prezzo negativo e sconto fuori dall'intervallo 0-100.

## Ordini

- `GET /api/orders/{code}/detail` restituisce ordine, capability di transizione e capability documentali senza dipendere dai filtri o dalla pagina di elenco.
- Le capability ordine sono `canConfirm`, `canFulfill`, `canCancel`, `canRecordReceipt` e `canRequestReturn`; ogni reso espone separatamente `canApprove`, `canReject`, `canReceive` e `canRefund`.
- Le capability documentali indicano almeno se l'ordine puo generare una fattura simulata o una nota credito simulata e considerano tutti i documenti gia collegati all'ordine.
- `POST /api/orders` crea un ordine in stato `DRAFT` e non scarica il magazzino.
- Il nuovo contratto esplicito usa `customerType`: `SELF_SERVICE`, `REGISTERED` oppure `WALK_IN`.
- Una vendita assistita `REGISTERED` richiede `customerPartnerId`, usa esclusivamente un'anagrafica cliente attiva e salva l'ID stabile oltre agli snapshot di codice e nome.
- Una vendita assistita `WALK_IN` richiede `walkInCustomerName`, non crea anagrafiche fittizie e non associa account o partner.
- Un account `CUSTOMER` puo usare soltanto `SELF_SERVICE`: eventuali ID partner, codici cliente o nominativi occasionali vengono rifiutati con `403`.
- `customer` e `customerCode` restano accettati senza `customerType` per compatibilita con client precedenti, ma non sono il contratto raccomandato per nuove integrazioni.
- `paymentMethod` accetta i codici stabili `CARD`, `BANK_TRANSFER` e `CASH`; le etichette storiche `Carta`, `Bonifico` e `Contanti` restano accettate in ingresso per compatibilita.
- Ogni ordine crea esattamente un pagamento strutturato in stato `PENDING`, con importo richiesto uguale al totale ordine, importo pagato iniziale pari a zero e valuta `EUR`.
- La risposta ordine mantiene `paymentMethod` come etichetta snapshot compatibile ed espone inoltre `payment` con metodo, stato, importo richiesto, incassato, rimborsato, netto, residuo, rimborsabile, valuta, timestamp e ledger `transactions`.
- Ogni riga ordine espone `returnedOrReservedQuantity` e `returnableQuantity`; il backend calcola il residuo sottraendo dalle quantita ordinate tutti i resi che impegnano ancora quantita, senza affidarsi allo stato locale del client.
- Il metodo `OTHER` e riservato alla migrazione di eventuali valori storici non riconosciuti e non puo essere selezionato per un nuovo ordine.
- Gli utenti operativi devono usare `customerPartnerId` per collegare stabilmente la vendita a un cliente censito; `customerCode` resta solo compatibilita legacy.
- Se `customerCode` e valido, il backend salva `partnerId` e usa il nome/ragione sociale dell'anagrafica come snapshot cliente.
- Se l'anagrafica e collegata esplicitamente a un account `CUSTOMER`, l'ordine conserva anche `customerAccountId`; nessun collegamento viene inferito dal nome.
- Gli ordini creati direttamente da un cliente usano sempre `customerAccountId` della sessione e l'eventuale anagrafica verificata associata.
- I clienti non possono forzare `customerCode` arbitrari: il backend continua a usare l'identita stabile della sessione.
- La risposta espone `customerAccountId`, `partnerId`, `customerType`, `customerTypeLabel`, `ownershipStatus` e `ownershipStatusLabel`; `customer` e `customerCode` restano snapshot descrittivi.
- Ogni riga congela codice, nome, descrizione, quantita, prezzo unitario e totale al momento della creazione; gli aggiornamenti successivi del catalogo non alterano l'ordine.
- Gli stati ownership ammessi sono `ACCOUNT`, `PARTNER` e `UNRESOLVED`.
- Lo storico viene migrato a `PARTNER` soltanto quando il codice cliente identifica deterministicamente una singola anagrafica. Gli altri ordini restano `UNRESOLVED` e non sono accessibili ai clienti.
- Eliminazione account o anagrafica non trasferisce ownership a soggetti omonimi; le foreign key vengono scollegate e lo snapshot storico resta disponibile agli utenti operativi autorizzati.
- `POST /api/orders/{code}/confirm` porta l'ordine da `DRAFT` a `CONFIRMED` e riserva lo stock.
- `POST /api/orders/{code}/fulfill` porta l'ordine da `CONFIRMED` a `FULFILLED`, scarica la giacenza fisica e libera la riserva.
- `POST /api/orders/{code}/cancel` annulla ordini in `DRAFT` o `CONFIRMED` e accetta un body con `reason` obbligatoria e `reference` opzionale soltanto in assenza di incassi.
- Se esiste un incasso netto, `reference` e obbligatorio e l'annullamento registra una transazione immutabile `REVERSAL` per l'intero netto, collegata all'ordine; il pagamento passa a `REFUNDED` e il netto diventa zero.
- Un ordine confermato libera lo stock riservato nella stessa transazione di ordine, pagamento, reversal e audit. Un errore in qualsiasi punto annulla ogni effetto.
- Il comando supporta `Idempotency-Key`: replay con chiave e payload uguali restituisce lo stesso risultato senza un secondo reversal; chiave riusata con payload diverso produce conflitto.
- Due operatori concorrenti non possono creare due annullamenti: il backend serializza la transizione e il database ammette al massimo un reversal per ordine.
- La risposta ordine espone `cancellationReference`, `cancellationReason`, `canceledAt`, `canceledBy` e `canceledByRole`; ogni transazione espone l'eventuale `cancellationOrderId`.
- `POST /api/orders/{code}/payments/receipts` registra un incasso totale o parziale su un ordine confermato o evaso e richiede `RECORD_PAYMENTS`.
- Ogni incasso produce un record immutabile `RECEIPT`; l'importo non puo superare il saldo residuo.
- `POST /api/orders/{code}/returns` apre una richiesta di reso su un ordine evaso e richiede `REQUEST_RETURNS`; il body accetta una o piu righe prodotto con quantita positiva e il cliente puo operare solo sui propri ordini.
- La somma dei resi gia attivi e della nuova richiesta non puo superare la quantita ordinata. Il controllo viene ripetuto dal backend nella transazione e serializzato sullo stesso ordine, quindi la UI non costituisce una barriera di concorrenza.
- `POST /api/orders/{code}/returns/{returnCode}/approve` e `/reject` richiedono `MANAGE_RETURNS`.
- `POST /api/orders/{code}/returns/{returnCode}/receive` reintegra il magazzino solo dopo approvazione e registra movimenti di tipo `RETURN`.
- `POST /api/orders/{code}/returns/{returnCode}/refund` richiede `REFUND_PAYMENTS` e `MANAGE_RETURNS`, accetta solo resi ricevuti e crea un movimento immutabile `REFUND`.
- Ogni reso nella risposta espone `id` stabile e `refundTransactions`. Il ledger include soltanto transazioni collegate a tale ID; il codice reso viene usato come compatibilita esclusivamente per record storici che non dispongono del collegamento ID.
- Le operazioni finanziarie e di reso supportano `Idempotency-Key` e restituiscono `409 RESOURCE_CONFLICT` per transizioni incompatibili.
- La creazione richiede `CREATE_ORDERS`, la conferma `CONFIRM_ORDERS`, l'evasione `FULFILL_ORDERS` e l'annullamento `CANCEL_ORDERS`.
- I clienti possono operare solo sui propri ordini e possono annullare solo bozze.
- La fattura simulata puo essere generata solo per ordini in stato `FULFILLED`.
- Il database rifiuta ordini con codice, cliente o metodo pagamento snapshot vuoti, totali negativi e righe ordine con quantita non positiva o importi negativi.
- `order_payments` impone una relazione univoca uno-a-uno con l'ordine, metodo e stato controllati, valuta ISO di tre caratteri, importi non negativi, incassato non superiore al richiesto e rimborsato non superiore all'incassato.
- `payment_transactions`, `order_returns` e `order_return_items` applicano vincoli su codici, importi, quantita, stati e relazioni; i saldi aggregati non sostituiscono il ledger.

## Riconciliazione finanziaria

- `GET /api/financial-reconciliation` richiede `VIEW_REPORTS` e non modifica dati.
- La risposta espone data del controllo, esito complessivo, numero di pagamenti e resi analizzati, totale anomalie, conteggi per tipo e righe di dettaglio.
- Le anomalie distinguono drift degli importi, stati incompatibili, cronologie impossibili, collegamenti non validi e pagamenti storici ancora `UNRECONCILED`.
- `payment_transactions` e la fonte autorevole; `paidAmount`, `refundedAmount` e l'importo rimborsato del reso sono proiezioni derivate.
- Ogni `REFUND` espone `returnId` oltre allo snapshot `returnCode`; ogni `RECONCILIATION` espone `reconciliationPaymentId`.
- Il report non corregge automaticamente le anomalie e non trasforma assenza di evidenza in un valore finanziario.

## Documenti simulati

- `POST /api/documents/invoice` genera una fattura simulata solo da ordine evaso.
- `POST /api/documents/credit-note` genera una nota credito simulata solo se esiste gia la fattura simulata dell'ordine.
- `GET /api/documents` restituisce una risposta paginata ordinata per data creazione decrescente.
- Ogni documento espone `capabilities.canCreateCreditNote`, calcolata considerando globalmente i documenti dell'ordine collegato e non il solo filtro o la sola pagina richiesta.
- Il filtro `q` cerca codice documento, ordine collegato, cliente snapshot, citta e operatore.
- Il filtro `type` accetta `SIMULATED_INVOICE` e `SIMULATED_CREDIT_NOTE`.
- I codici seguono `PREFISSO-ANNO-PROGRESSIVO`; anno, progressivo e prefisso sono esposti anche come campi separati.
- Ogni documento conserva lo snapshot dell'azienda emittente e l'aliquota applicata al momento della creazione.
- Ogni documento conserva anche `companySnapshotTimeZone`; `createdAt` e restituito con l'offset di tale fuso.
- La numerazione usa l'anno dell'istante corrente nel fuso aziendale. Il disclaimer resta `DOCUMENTO SIMULATO - NON VALIDO AI FINI FISCALI`.
- La generazione viene rifiutata se ragione sociale, identificativo fiscale, indirizzo, CAP, citta, provincia, paese o fuso orario non sono completi.
- Il database impone una sola fattura simulata e una sola nota credito simulata per ordine tramite vincolo univoco su ordine collegato e tipo documento.
- Una seconda fattura o nota credito per lo stesso ordine restituisce `409` con codice `RESOURCE_CONFLICT`.
- Se la stessa richiesta viene ripetuta con la stessa `Idempotency-Key`, viene restituita la risposta gia prodotta; se la stessa chiave viene riutilizzata con dati diversi, viene restituito `409` con codice `IDEMPOTENCY_CONFLICT`.
- Il vincolo database protegge anche richieste concorrenti che superano il controllo applicativo.
- Ogni documento conserva uno snapshot cliente con codice, nome, codice fiscale, partita IVA, email, telefono, indirizzo e citta.
- Se l'ordine e collegato a un cliente in anagrafica, lo snapshot viene copiato dall'anagrafica al momento della generazione della fattura simulata.
- Se l'ordine non ha un cliente strutturato, lo snapshot conserva almeno il cliente testuale dell'ordine.
- La nota credito simulata copia lo snapshot dalla fattura simulata originale, non dall'anagrafica aggiornata.
- La nota credito copia anche snapshot emittente, aliquota e importi della fattura originale.
- Il campo `customer` resta valorizzato per compatibilita e corrisponde al nome cliente snapshot.
- Il database rifiuta documenti simulati con importi negativi, aliquota IVA fuori dall'intervallo 0-1, campi operativi obbligatori vuoti e righe documento con quantita non positiva o importi negativi.

## Configurazione aziendale

- `GET /api/company-settings` restituisce dati aziendali, `timeZone`, aliquota predefinita, prefissi, lunghezza progressivo, versione, stato configurato e `missingDocumentFields`.
- `PUT /api/company-settings` aggiorna la configurazione e richiede il permesso `MANAGE_COMPANY_SETTINGS`.
- Il payload di aggiornamento deve includere la `version` letta; una versione obsoleta restituisce `409 RESOURCE_CONFLICT`.
- Solo il super admin possiede il permesso nella matrice attuale.
- L'aliquota usa rappresentazione decimale tra `0` e `1`: ad esempio `0.22` rappresenta il 22%.
- I prefissi sono alfanumerici, distinti e lunghi da 1 a 8 caratteri; il progressivo usa da 3 a 8 cifre.
- Dopo il primo documento dell'esercizio non e possibile cambiare prefissi o lunghezza progressivo.
- Dopo il primo documento non e possibile cambiare il fuso aziendale.
- `timeZone` deve essere un identificatore IANA valido, per esempio `Europe/Rome`.
- I dati identificativi sono inizialmente vuoti e devono essere compilati con valori reali.
- L'endpoint configura documenti simulati e non certifica conformita fiscale o fatturazione elettronica.

## Report operativi

Tutti gli endpoint report richiedono una sessione valida e il permesso `VIEW_REPORTS`.

### Vendite

- `GET /api/reports/sales` restituisce il report in JSON.
- `GET /api/reports/sales/export` scarica lo stesso dataset in un formato file.
- Filtri: `from` e `to` in formato `YYYY-MM-DD`; `status` accetta `DRAFT`, `CONFIRMED`, `FULFILLED` o `CANCELED`.
- Se il periodo non viene specificato, il backend usa il primo gennaio dell'anno corrente e la data odierna.
- Le date del periodo sono giorni civili nel fuso IANA aziendale e vengono convertite in istanti UTC solo per interrogare gli eventi persistiti; questo include correttamente cambi d'ora legale e solare.
- La data iniziale non puo essere successiva alla data finale e il periodo non puo superare cinque anni.
- La risposta distingue `orderValue`, `paidAmount`, `refundedAmount`, `netCollectedAmount`, `outstandingAmount` e `averageOrderValue`; nessuno di questi valori viene presentato automaticamente come fatturato fiscale.
- `orders` contiene le righe di dettaglio e `topProducts` i primi dieci prodotti per quantita ordinata.

### Magazzino

- `GET /api/reports/inventory` restituisce lo snapshot in JSON.
- `GET /api/reports/inventory/export` scarica lo stesso dataset in un formato file.
- Filtri: `q`, `category` (`HARDWARE` o `SOFTWARE`), `stock` (`ALL`, `AVAILABLE`, `LOW` o `OUT`) e `discontinued` (`true` o `false`).
- La risposta distingue unita fisiche, riservate e disponibili, valore potenziale di vendita al prezzo scontato corrente, scorte basse, esauriti e disattivati.
- `potentialRetailStockValue` e `potentialRetailValue` non rappresentano costo, valore contabile, margine o utile.
- Lo snapshot rappresenta lo stato corrente del catalogo e non e uno storico di valorizzazione contabile.

### Export

- Il parametro `format` accetta `CSV`, `XLSX` e `PDF`; il valore predefinito e `CSV`.
- CSV usa UTF-8 con BOM, separatore `;`, escaping dei campi e neutralizzazione della Formula Injection.
- XLSX e un workbook Excel reale con celle numeriche e temporali tipizzate.
- PDF e un report gestionale interno in formato landscape e non un documento fiscale.
- Ogni download usa `Content-Disposition: attachment`, `Cache-Control: no-store` e `X-Content-Type-Options: nosniff`.
- Gli export riusciti generano eventi audit `EXPORT_SALES_REPORT` o `EXPORT_INVENTORY_REPORT` nella categoria `REPORT`.
- Il backend accetta al massimo 10.000 righe per report; oltre il limite restituisce `400 REQUEST_INVALID` e richiede filtri piu restrittivi.

## Anagrafiche clienti e fornitori

- `GET /api/partners` richiede `VIEW_PARTNERS`.
- `POST /api/partners` richiede `MANAGE_PARTNERS`.
- `PUT /api/partners/{code}` richiede `MANAGE_PARTNERS`.
- `DELETE /api/partners/{code}` richiede `MANAGE_PARTNERS` e disattiva il soggetto senza rimuoverlo fisicamente.
- `PUT /api/partners/{code}/account-link` richiede sia `MANAGE_ACCOUNTS` sia `MANAGE_PARTNERS` e associa l'anagrafica a un account `CUSTOMER` tramite `accountId`.
- `DELETE /api/partners/{code}/account-link` richiede gli stessi permessi e rimuove l'associazione senza modificare lo storico degli ordini.
- I tipi ammessi sono `CUSTOMER` e `SUPPLIER`.
- Il codice anagrafica e univoco e stabile.
- Il codice anagrafica conserva il valore visuale ripulito e usa `lower(trim(code))` per lookup e unicita; collisioni sequenziali o concorrenti restituiscono `409 RESOURCE_CONFLICT`.
- Un account puo essere collegato a una sola anagrafica cliente e un'anagrafica a un solo account; fornitori e ruoli operativi non sono collegabili.
- `linkedAccountId` nella risposta espone l'associazione stabile oppure `null` quando non configurata.
- La ricerca `q` filtra codice, nome/ragione sociale, email, telefono, citta e dati fiscali.
- Il database rifiuta anagrafiche con codice o nome/ragione sociale vuoti.

## Ordini fornitore

- `GET /api/purchase-orders` richiede `VIEW_PURCHASE_ORDERS` e restituisce una pagina filtrabile per `q`, `status` e `supplierId`.
- `GET /api/purchase-orders/{code}` richiede `VIEW_PURCHASE_ORDERS` e restituisce dettaglio, righe, ricezioni, residui e capability operative.
- `POST /api/purchase-orders` richiede `MANAGE_PURCHASE_ORDERS`, `Idempotency-Key` e crea esclusivamente una bozza.
- `POST /api/purchase-orders/{code}/send` richiede `MANAGE_PURCHASE_ORDERS`, `Idempotency-Key` e porta una bozza a `SENT`.
- `POST /api/purchase-orders/{code}/receipts` richiede `MANAGE_PURCHASE_ORDERS`, `Idempotency-Key`, causale e quantita positive per `lineId`.
- `POST /api/purchase-orders/{code}/cancel` richiede `MANAGE_PURCHASE_ORDERS`, `Idempotency-Key` e una causale non vuota.
- Gli stati pubblici sono `DRAFT`, `SENT`, `PARTIALLY_RECEIVED`, `RECEIVED` e `CANCELED`.
- Il fornitore e selezionato tramite ID stabile e deve essere un partner `SUPPLIER` attivo. ID, codice e nome sono conservati separatamente; codice e nome sono snapshot.
- Ogni riga conserva ID prodotto e snapshot di codice, nome, prezzo concordato, quantita e data prevista. Lo stesso prodotto puo comparire una sola volta nella bozza.
- Una ricezione e amministrativa, cumulativa e non puo superare il residuo autorevole. Richieste concorrenti sullo stesso ordine vengono serializzate.
- `CANCELED` preserva quantita e ricezioni gia registrate e chiude soltanto il residuo; `RECEIVED` e raggiunto solo quando tutte le righe sono complete.
- Le capability `canSend`, `canReceive` e `canCancel` vengono calcolate dal backend usando permesso e stato persistito.
- Le mutazioni producono audit nella categoria `PURCHASE` e rispettano il contratto idempotente condiviso.
- Lo Step 4.1 non produce movimenti di magazzino, variazioni di giacenza, valorizzazioni costo, fatture passive o pagamenti.

## Errori

Tutti gli errori HTTP della web app devono seguire questo formato:

```json
{
  "timestamp": "2026-07-05T20:49:00.000Z",
  "status": 422,
  "code": "VALIDATION_FAILED",
  "message": "Dati non validi.",
  "details": ["code: must not be blank"],
  "path": "/api/products",
  "requestId": "req-123"
}
```

## Campi

- `timestamp`: istante server in cui l'errore viene prodotto.
- `status`: status HTTP numerico.
- `code`: codice applicativo stabile, usabile da frontend e test.
- `message`: messaggio leggibile.
- `details`: lista di dettagli tecnici o di validazione.
- `path`: endpoint richiesto.
- `requestId`: identificativo richiesta. Se il client invia `X-Request-Id`, viene riutilizzato; altrimenti viene generato dal backend.

## Codici attuali

| Codice | Significato |
| --- | --- |
| `AUTH_UNAUTHORIZED` | Sessione mancante, non valida o scaduta. |
| `AUTH_FORBIDDEN` | Utente autenticato ma senza permesso sufficiente. |
| `RATE_LIMIT_EXCEEDED` | Troppe richieste login provenienti dallo stesso IP; il client deve rispettare `Retry-After`. |
| `REQUEST_INVALID` | Regola applicativa violata o richiesta non accettabile. |
| `REQUEST_MALFORMED` | JSON o body non leggibile. |
| `VALIDATION_FAILED` | Validazione bean fallita. |
| `RESOURCE_CONFLICT` | Dato in conflitto con lo stato corrente, vincolo database violato o modifica concorrente; il client deve ricaricare e riprovare. |
| `IDEMPOTENCY_CONFLICT` | Chiave idempotente riutilizzata con account, operazione o payload diversi. |
| `IDEMPOTENCY_IN_PROGRESS` | Intento idempotente ancora in corso; rispettare `Retry-After` e riprovare con la stessa chiave. |
| `INTERNAL_ERROR` | Errore inatteso lato server. |

## Regole

- Il frontend non deve dipendere solo dal testo del messaggio.
- I test API devono verificare almeno `status`, `code`, `path` e presenza di `requestId`.
- I dettagli non devono contenere password, token o segreti.
- Gli errori interni non devono esporre stack trace o nomi classe al client.
- La risposta `429 RATE_LIMIT_EXCEEDED` e generata dal reverse proxy e include `Retry-After`, `Cache-Control: no-store`, `X-Content-Type-Options: nosniff` e `X-Request-Id`.

## Tracciabilita richieste

- Ogni richiesta puo inviare `X-Request-Id`.
- Se il client non invia `X-Request-Id`, il backend genera un identificativo UUID.
- Ogni risposta HTTP include `X-Request-Id`.
- Gli errori API includono lo stesso valore nel campo `requestId`.
- Il backend registra nei log `requestId`, metodo, path, status HTTP e durata.
- Il frontend genera un `X-Request-Id` per ogni chiamata e lo mostra nei messaggi di errore quando disponibile.
