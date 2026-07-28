# API Contract

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
| `GET /api/partners` | `page`, `size`, `q`, `type`, `active` |
| `GET /api/inventory/movements` | `page`, `size`, `q`, `type`, `productCode` |
| `GET /api/orders` | `page`, `size`, `q`, `customer` |
| `GET /api/orders/customer/{customer}` | `page`, `size`, `q` |
| `GET /api/documents` | `page`, `size`, `q`, `type` |
| `GET /api/accounts` | `page`, `size`, `q`, `role` |
| `GET /api/audit` | `page`, `size`, `q`, `category`, `severity`; `q` cerca anche `requestId`, origine richiesta e tipo entita |

Regole:

- `page` parte da `0`.
- `size` viene normalizzato e limitato dal backend.
- Il frontend puo usare `content` per i dati visibili e `totalElements`/`totalPages` per i controlli di pagina.
- I clienti vedono solo i propri ordini anche se inviano un filtro `customer` diverso.

## Dashboard

- `GET /api/dashboard` richiede una sessione valida.
- La risposta contiene statistiche aggregate su prodotti, valore inventario, scorte basse, esauriti, ordini, ricavi, ultimi ordini e movimenti recenti.
- Per utenti cliente, `orders`, `revenue` e `recentOrders` sono limitati all'account autenticato.
- I movimenti magazzino recenti sono inclusi solo per ruoli con permesso `MANAGE_INVENTORY`.
- La dashboard non espone token, password o dati di sicurezza sensibili.

## Sessioni

- Il login restituisce un token da inviare con header `X-Session-Token`.
- Il backend conserva solo l'hash del token nella tabella `auth_sessions`.
- La durata assoluta standard della sessione e 45 minuti e il timeout di inattivita standard e 30 minuti.
- `POST /api/accounts/session/renew` richiede sessione valida e password dell'account autenticato; la risposta ha lo stesso formato del login.
- Il rinnovo ruota il token: il token precedente viene revocato nella stessa transazione e non puo essere riutilizzato.
- Se il token e gia scaduto o inattivo, il rinnovo autenticato viene rifiutato e il client deve eseguire un nuovo login.
- Login e rinnovo restituiscono `Cache-Control: no-store` e `Pragma: no-cache`.
- Il logout revoca la sessione salvando `revoked_at`.
- Se l'account non esiste piu o la sessione e scaduta/revocata, gli endpoint protetti restituiscono `AUTH_UNAUTHORIZED`.
- Il token non deve comparire in log, errori o dettagli diagnostici.
- Il frontend conserva il token solo in memoria e non usa cookie di autenticazione, `localStorage` o `sessionStorage`.
- CSRF resta disabilitato per questo contratto basato su header non ambientale; un passaggio a cookie HttpOnly richiede una migrazione atomica con protezione CSRF.
- I tentativi login falliti vengono persistiti in `login_attempts`.
- Dopo 5 tentativi falliti, l'account viene bloccato temporaneamente per 5 minuti.
- Un job programmato elimina sessioni obsolete, lock scaduti e tentativi falliti non piu rilevanti.

## Password policy

- La robustezza password viene controllata lato backend, non solo dal frontend.
- La policy e centralizzata e viene applicata a registrazione pubblica, creazione account amministrativa e futuri cambi password.
- Una password valida deve avere almeno 8 caratteri, maiuscole, minuscole, numeri e simboli.
- La password non deve essere troppo simile allo username.
- `POST /api/accounts/password-strength` fornisce una valutazione indicativa, ma non sostituisce il controllo bloccante in creazione account.
- Se la password non rispetta la policy, il backend risponde `400` con codice `REQUEST_INVALID` e messaggio operativo senza esporre dati sensibili.

## Account

- `GET /api/accounts` richiede `MANAGE_ACCOUNTS` e restituisce una risposta paginata ordinata per username crescente.
- Il filtro `q` cerca nello username.
- Il filtro `role` accetta `SUPER_ADMIN`, `ADMIN`, `EMPLOYEE` e `CUSTOMER`.
- La registrazione pubblica non consente ruoli amministrativi.
- La creazione di account admin e riservata al super admin.
- Un amministratore non puo eliminare il proprio account durante la sessione attiva.
- Il super admin non puo essere eliminato dal pannello operativo.
- Eliminazione e creazione account richiedono conferma password tramite `X-Reauth-Password`.

## Idempotenza

Le operazioni critiche possono ricevere header `Idempotency-Key`.

Endpoint coperti:

- `POST /api/orders`
- `POST /api/orders/{code}/confirm`
- `POST /api/orders/{code}/fulfill`
- `POST /api/orders/{code}/cancel`
- `POST /api/inventory/movements`
- `POST /api/documents/invoice`
- `POST /api/documents/credit-note`

Regole:

- la chiave e scoped per utente autenticato e operazione;
- se la stessa richiesta viene ripetuta con la stessa chiave, il backend restituisce la risposta gia prodotta;
- se la stessa chiave viene riutilizzata con dati diversi, il backend risponde `409` con codice `IDEMPOTENCY_CONFLICT`;
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

- Carichi e scarichi passano da una singola operazione backend di aggiustamento stock.
- Lo scarico non puo rendere negativa la giacenza.
- Lo scarico manuale non puo scendere sotto la quantita gia riservata.
- Le risposte prodotto espongono `quantity`, `reservedQuantity` e `availableQuantity`.
- I filtri stock lavorano sulla disponibilita vendibile, non sulla sola giacenza fisica.
- `GET /api/inventory/low-stock` restituisce prodotti con disponibilita vendibile maggiore di zero e minore o uguale alla soglia operativa di 3 unita.
- I prodotti esauriti non sono conteggiati come scorte basse: vanno trattati come stato separato.
- Se due operazioni modificano lo stesso prodotto in concorrenza, il backend restituisce `409` con codice `RESOURCE_CONFLICT`.
- Il movimento conserva sempre snapshot di codice prodotto, nome prodotto, quantita precedente e nuova quantita.

## Prodotti

- `GET /api/products` e `GET /api/products/{code}` richiedono `VIEW_CATALOG`.
- `GET /api/products/lookup` richiede `VIEW_CATALOG` e restituisce dati leggeri per filtri e select: codice, nome, brand, tipo prodotto, stato disattivato e disponibilita vendibile.
- `POST /api/products`, `PUT /api/products/{code}`, `DELETE /api/products/{code}`, `POST /api/products/bulk-delete` e `POST /api/products/{code}/discontinue` richiedono `MANAGE_PRODUCTS`.
- Le risposte prodotto espongono anche `discontinued`.
- Il codice prodotto puo essere modificato solo finche il prodotto non compare in righe ordine.
- Un prodotto non puo essere eliminato se ha stock riservato da ordini confermati.
- Un prodotto non puo essere eliminato se compare in ordini, anche non confermati; in quel caso va disattivato.
- Un prodotto disattivato resta leggibile nello storico e nel catalogo, ma non puo essere acquistato in nuovi ordini.
- Gli ordini gia confermati con un prodotto successivamente disattivato restano evadibili, preservando storico e stock riservato.
- Il database rifiuta prodotti con codice, nome, descrizione, brand o tipo prodotto vuoti.
- Il database rifiuta quantita negative, prezzo negativo e sconto fuori dall'intervallo 0-100.

## Ordini

- `POST /api/orders` crea un ordine in stato `DRAFT` e non scarica il magazzino.
- `paymentMethod` accetta i codici stabili `CARD`, `BANK_TRANSFER` e `CASH`; le etichette storiche `Carta`, `Bonifico` e `Contanti` restano accettate in ingresso per compatibilita.
- Ogni ordine crea esattamente un pagamento strutturato in stato `PENDING`, con importo richiesto uguale al totale ordine, importo pagato iniziale pari a zero e valuta `EUR`.
- La risposta ordine mantiene `paymentMethod` come etichetta snapshot compatibile ed espone inoltre `payment` con metodo, stato, importo richiesto, incassato, rimborsato, netto, residuo, rimborsabile, valuta, timestamp e ledger `transactions`.
- Il metodo `OTHER` e riservato alla migrazione di eventuali valori storici non riconosciuti e non puo essere selezionato per un nuovo ordine.
- Gli utenti operativi possono inviare `customerCode` per collegare l'ordine a un cliente registrato in anagrafica.
- Se `customerCode` e valido, il backend usa il nome/ragione sociale dell'anagrafica come cliente dell'ordine.
- I clienti non possono forzare `customerCode` arbitrari: il backend continua a usare l'identita della sessione.
- `POST /api/orders/{code}/confirm` porta l'ordine da `DRAFT` a `CONFIRMED` e riserva lo stock.
- `POST /api/orders/{code}/fulfill` porta l'ordine da `CONFIRMED` a `FULFILLED`, scarica la giacenza fisica e libera la riserva.
- `POST /api/orders/{code}/cancel` annulla ordini in `DRAFT` o `CONFIRMED`; se l'ordine era confermato, libera lo stock riservato. Un ordine con incassi non puo essere annullato direttamente.
- `POST /api/orders/{code}/payments/receipts` registra un incasso totale o parziale su un ordine confermato o evaso e richiede `RECORD_PAYMENTS`.
- Ogni incasso produce un record immutabile `RECEIPT`; l'importo non puo superare il saldo residuo.
- `POST /api/orders/{code}/returns` apre una richiesta di reso su un ordine evaso e richiede `REQUEST_RETURNS`; il cliente puo operare solo sui propri ordini.
- `POST /api/orders/{code}/returns/{returnCode}/approve` e `/reject` richiedono `MANAGE_RETURNS`.
- `POST /api/orders/{code}/returns/{returnCode}/receive` reintegra il magazzino solo dopo approvazione e registra movimenti di tipo `RETURN`.
- `POST /api/orders/{code}/returns/{returnCode}/refund` richiede `REFUND_PAYMENTS` e `MANAGE_RETURNS`, accetta solo resi ricevuti e crea un movimento immutabile `REFUND`.
- Le operazioni finanziarie e di reso supportano `Idempotency-Key` e restituiscono `409 RESOURCE_CONFLICT` per transizioni incompatibili.
- La creazione richiede `CREATE_ORDERS`, la conferma `CONFIRM_ORDERS`, l'evasione `FULFILL_ORDERS` e l'annullamento `CANCEL_ORDERS`.
- I clienti possono operare solo sui propri ordini e possono annullare solo bozze.
- La fattura simulata puo essere generata solo per ordini in stato `FULFILLED`.
- Il database rifiuta ordini con codice, cliente o metodo pagamento snapshot vuoti, totali negativi e righe ordine con quantita non positiva o importi negativi.
- `order_payments` impone una relazione univoca uno-a-uno con l'ordine, metodo e stato controllati, valuta ISO di tre caratteri, importi non negativi, incassato non superiore al richiesto e rimborsato non superiore all'incassato.
- `payment_transactions`, `order_returns` e `order_return_items` applicano vincoli su codici, importi, quantita, stati e relazioni; i saldi aggregati non sostituiscono il ledger.

## Documenti simulati

- `POST /api/documents/invoice` genera una fattura simulata solo da ordine evaso.
- `POST /api/documents/credit-note` genera una nota credito simulata solo se esiste gia la fattura simulata dell'ordine.
- `GET /api/documents` restituisce una risposta paginata ordinata per data creazione decrescente.
- Il filtro `q` cerca codice documento, ordine collegato, cliente snapshot, citta e operatore.
- Il filtro `type` accetta `SIMULATED_INVOICE` e `SIMULATED_CREDIT_NOTE`.
- I codici seguono `PREFISSO-ANNO-PROGRESSIVO`; anno, progressivo e prefisso sono esposti anche come campi separati.
- Ogni documento conserva lo snapshot dell'azienda emittente e l'aliquota applicata al momento della creazione.
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

- `GET /api/company-settings` restituisce dati aziendali, aliquota predefinita, prefissi, lunghezza progressivo, versione e stato configurato.
- `PUT /api/company-settings` aggiorna la configurazione e richiede il permesso `MANAGE_COMPANY_SETTINGS`.
- Il payload di aggiornamento deve includere la `version` letta; una versione obsoleta restituisce `409 RESOURCE_CONFLICT`.
- Solo il super admin possiede il permesso nella matrice attuale.
- L'aliquota usa rappresentazione decimale tra `0` e `1`: ad esempio `0.22` rappresenta il 22%.
- I prefissi sono alfanumerici, distinti e lunghi da 1 a 8 caratteri; il progressivo usa da 3 a 8 cifre.
- Dopo il primo documento dell'esercizio non e possibile cambiare prefissi o lunghezza progressivo.
- I dati identificativi sono inizialmente vuoti e devono essere compilati con valori reali.
- L'endpoint configura documenti simulati e non certifica conformita fiscale o fatturazione elettronica.

## Report operativi

Tutti gli endpoint report richiedono una sessione valida e il permesso `VIEW_REPORTS`.

### Vendite

- `GET /api/reports/sales` restituisce il report in JSON.
- `GET /api/reports/sales/export` scarica lo stesso dataset in un formato file.
- Filtri: `from` e `to` in formato `YYYY-MM-DD`; `status` accetta `DRAFT`, `CONFIRMED`, `FULFILLED` o `CANCELED`.
- Se il periodo non viene specificato, il backend usa il primo gennaio dell'anno corrente e la data odierna.
- La data iniziale non puo essere successiva alla data finale e il periodo non puo superare cinque anni.
- La risposta distingue `orderValue`, `paidAmount`, `refundedAmount`, `netCollectedAmount`, `outstandingAmount` e `averageOrderValue`; nessuno di questi valori viene presentato automaticamente come fatturato fiscale.
- `orders` contiene le righe di dettaglio e `topProducts` i primi dieci prodotti per quantita ordinata.

### Magazzino

- `GET /api/reports/inventory` restituisce lo snapshot in JSON.
- `GET /api/reports/inventory/export` scarica lo stesso dataset in un formato file.
- Filtri: `q`, `category` (`HARDWARE` o `SOFTWARE`), `stock` (`ALL`, `AVAILABLE`, `LOW` o `OUT`) e `discontinued` (`true` o `false`).
- La risposta distingue unita fisiche, riservate e disponibili, valore inventario al prezzo scontato corrente, scorte basse, esauriti e disattivati.
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
- I tipi ammessi sono `CUSTOMER` e `SUPPLIER`.
- Il codice anagrafica e univoco e stabile.
- La ricerca `q` filtra codice, nome/ragione sociale, email, telefono, citta e dati fiscali.
- Il database rifiuta anagrafiche con codice o nome/ragione sociale vuoti.

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
| `IDEMPOTENCY_CONFLICT` | Chiave idempotente riutilizzata con dati diversi o operazione identica gia in corso. |
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
