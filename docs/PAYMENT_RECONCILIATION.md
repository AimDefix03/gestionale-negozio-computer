# Riconciliazione pagamenti storici

## Scopo

La migrazione `V22` corregge due assunzioni non dimostrabili introdotte dalle migrazioni storiche:

- un pagamento privo di movimenti non viene piu dichiarato automaticamente non pagato;
- un contatore documentale assente o arretrato riparte dal massimo storico della stessa tipologia e dello stesso esercizio, aumentato di uno.

Gli ordini senza evidenza finanziaria sufficiente ricevono lo stato `UNRECONCILED`. Finche lo stato rimane attivo, importo incassato, rimborsato e residuo non vengono esposti come valori noti, nuovi movimenti sono bloccati e l'ordine e escluso dai report finanziari.

## Preflight prima dell'upgrade

Eseguire il controllo sul database destinato all'upgrade dopo un backup verificato:

```sql
select lower(code), count(*)
from fiscal_documents
group by lower(code)
having count(*) > 1;

select type, fiscal_year, sequence_number, count(*)
from fiscal_documents
group by type, fiscal_year, sequence_number
having count(*) > 1;
```

Entrambe le query devono restituire zero righe. In presenza di collisioni fermare il rollout, conservare un export dei record coinvolti e definire la correzione con il responsabile amministrativo. Non rinumerare automaticamente documenti storici.

## Rollout

1. Eseguire backup PostgreSQL e restore drill.
2. Sospendere la creazione di ordini, incassi e documenti per la finestra di migrazione.
3. Eseguire il preflight e archiviare il risultato.
4. Avviare il backend che applica `V22` tramite Flyway.
5. Verificare che ogni contatore sia maggiore della sequenza storica massima.
6. Verificare il primo documento simulato in un ambiente non produttivo.
7. Censire i pagamenti `UNRECONCILED` e assegnare la revisione a un super admin.

Verifica contatori:

```sql
select counter.document_type, counter.fiscal_year, counter.next_value, history.maximum_sequence
from document_number_counters counter
join (
    select type, fiscal_year, max(sequence_number) as maximum_sequence
    from fiscal_documents
    group by type, fiscal_year
) history on history.type = counter.document_type and history.fiscal_year = counter.fiscal_year
where counter.next_value <= history.maximum_sequence;
```

La query deve restituire zero righe.

## Procedura manuale

La riconciliazione richiede:

- sessione `SUPER_ADMIN`;
- ri-autenticazione tramite `X-Reauth-Password`;
- `Idempotency-Key` univoca;
- importo verificato compreso tra zero e totale ordine;
- motivazione obbligatoria;
- riferimento all'evidenza, quando disponibile.

Endpoint:

```text
POST /api/orders/{orderCode}/payments/reconciliation
```

Payload:

```json
{
  "verifiedPaidAmount": 40.00,
  "reference": "ESTRATTO-2026-001",
  "reason": "Saldo verificato rispetto all'estratto conto archiviato"
}
```

L'operazione registra operatore, ruolo, data, riferimento e motivazione sul pagamento e genera un audit `CRITICAL`. Una riconciliazione gia completata non puo essere ripetuta tramite lo stesso endpoint. Se l'importo verificato e zero lo stato diventa `PENDING`; se e parziale diventa `PARTIALLY_PAID`; se coincide con il totale diventa `PAID`.

La riconciliazione certifica soltanto il dato inserito dall'operatore sulla base dell'evidenza disponibile. Non sostituisce controlli contabili, fiscali o legali professionali.

## Ledger finanziario autorevole

Da `V27` il ledger `payment_transactions` e la fonte autorevole per gli importi incassati e rimborsati. I campi aggregati di `order_payments` e `order_returns` sono proiezioni aggiornate nella stessa transazione applicativa e verificate dal report di riconciliazione.

Tipi di movimento ammessi:

- `RECEIPT`: incasso reale;
- `REFUND`: rimborso collegato tramite foreign key al reso che lo giustifica;
- `REVERSAL`: storno collegato all'ordine annullato;
- `RECONCILIATION`: evidenza iniziale verificata tramite la procedura manuale protetta.

Il testo `return_code` resta uno snapshot leggibile, ma non identifica piu autonomamente un reso. Un rimborso valido deve avere `return_id`; un movimento di altro tipo non puo averlo. Ogni pagamento storico riconciliato con importo positivo riceve un solo movimento `RECONCILIATION`, senza alterare l'importo verificato in precedenza.

## Report continuo

L'endpoint seguente richiede `VIEW_REPORTS`:

```text
GET /api/financial-reconciliation
```

Il report confronta ledger e proiezioni e classifica anomalie di importo, stato, collegamento e cronologia. Il job programmato esegue lo stesso controllo ogni 15 minuti per impostazione predefinita. Frequenza iniziale e frequenza successiva sono configurabili con durate ISO-8601:

```text
APP_FINANCIAL_RECONCILIATION_INITIAL_DELAY=PT15M
APP_FINANCIAL_RECONCILIATION_FIXED_DELAY=PT15M
```

Il controllo e intenzionalmente read-only: non modifica o completa dati finanziari in autonomia.

## Procedura di correzione

1. Sospendere le operazioni finanziarie sull'ordine coinvolto e conservare report, request ID ed evidenze esterne.
2. Verificare che backup e procedura di ripristino siano disponibili prima di intervenire sui dati.
3. Classificare l'anomalia come drift di proiezione, evidenza storica assente, collegamento errato o cronologia incompatibile.
4. Per un pagamento `UNRECONCILED`, usare esclusivamente l'endpoint manuale protetto e riportare riferimento e motivazione reali.
5. Se il ledger e corretto ma la proiezione e errata, applicare una correzione transazionale revisionata e tracciata; non modificare o cancellare movimenti esistenti.
6. Se un movimento del ledger e errato, progettare un movimento compensativo esplicito e auditato. Non correggere il record originario con SQL diretto.
7. Rieseguire il report, verificare che l'anomalia sia chiusa e archiviare le evidenze dell'intervento.

Il sistema non deduce pagamenti da ordini o documenti e non inventa importi mancanti. Le evidenze storiche non disponibili restano da riconciliare manualmente.

## Verifica automatica

```bash
scripts/db/verify-historical-fixtures.sh small upgrades
scripts/db/verify-historical-fixtures.sh medium upgrade-v18
scripts/db/verify-historical-fixtures.sh large upgrade-v18
scripts/db/verify-payment-numbering-postgres.sh
scripts/db/verify-financial-reconciliation-postgres.sh
```

Misure locali PostgreSQL 16 del 2026-08-04:

| Profilo | Ordini | Upgrade | Durata/lock V22 |
|---|---:|---|---:|
| small | 45 | V14 -> V22 | 163 ms |
| small | 45 | V16 -> V22 | 147 ms |
| small | 45 | V18 -> V22 | 131 ms |
| medium | 5.005 | V18 -> V22 | 165 ms |
| large | 50.005 | V18 -> V22 | 588 ms |

I tempi dipendono da hardware, carico, indici, dimensione reale e attivita concorrente. Devono essere rimisurati sull'ambiente candidato prima di ogni rollout.
