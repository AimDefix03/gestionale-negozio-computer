# Confini modulari

## Obiettivo

Il backend e un monolite modulare. I moduli sono organizzati per capacita aziendale e vengono distribuiti nello stesso processo Spring Boot e nello stesso database PostgreSQL.

Spring Modulith verifica automaticamente che il grafo dei moduli business non contenga cicli. La verifica viene eseguita da `ModularArchitectureTest` durante `mvn test` e `mvn verify`.

## Moduli business verificati

- `user`: account, ruoli, permessi e sessioni.
- `product`: catalogo e ciclo di vita del prodotto.
- `partner`: clienti e fornitori.
- `inventory`: movimenti e disponibilita di magazzino.
- `order`: ordini, righe, pagamenti, ledger finanziario e resi.
- `document`: documenti simulati e snapshot cliente.
- `company`: dati aziendali, IVA predefinita e regole di numerazione.
- `reporting`: report operativi e formati di esportazione.

## Dipendenze consentite attuali

```text
user
product
partner
inventory
└── product
order
├── product
├── partner
└── inventory
company
document
├── company
├── partner
└── order
reporting
├── order
└── product
```

Le frecce logiche procedono dai moduli orchestratori verso le capacita richieste. Nessun modulo di base deve dipendere da un modulo che gia lo utilizza.

Il controllo sull'utilizzo di un prodotto negli ordini passa attraverso `ProductOrderUsage`, porta dichiarata dal modulo `product` e implementata nel modulo `order`. In questo modo il catalogo non conosce repository o entita degli ordini.

Il controllo sull'esistenza di documenti nell'esercizio passa attraverso `DocumentNumberingUsage`, porta dichiarata dal modulo `company` e implementata nel modulo `document`. Il modulo documenti usa il servizio aziendale per ottenere in transazione configurazione bloccata, aliquota e prefissi, senza accedere al repository interno di `company`.

Il modulo `reporting` compone dati di vendita e inventario tramite `SalesReportingUsage` e `InventoryReportingUsage`. Le porte espongono read model immutabili; gli adapter restano nei moduli proprietari `order` e `product`, applicano i filtri lato database e non rendono pubblici repository o entita JPA.

## Package tecnici

I package seguenti sono temporaneamente esclusi dal modello dei moduli business:

- `common`
- `security`
- `audit`
- `idempotency`
- `dashboard`
- `system`

Sono infrastrutture condivise o moduli di composizione. L'esclusione e intenzionale e visibile nel test architetturale; non deve diventare un modo per aggirare i confini business.

## Regole

- Un nuovo dominio nasce come package diretto sotto il package principale.
- Un modulo non accede ai repository o alle entita interne di un altro modulo.
- Le dipendenze inverse usano porte definite dal modulo che esprime il bisogno.
- Controller e DTO non sostituiscono le regole del dominio.
- Gli eventi sono introdotti solo per conseguenze trasversali reali.
- Prima di aggiungere un microservizio deve esistere un motivo operativo misurabile.
- Ogni modifica ai confini deve aggiornare questo documento e il test architetturale.

## Evoluzione prevista

Pagamenti e resi restano nel modulo `order` perche condividono invarianti e lock transazionali con l'ordine. Un modulo `payment` separato avra senso solo con integrazione PSP, riconciliazione esterna o contabilita autonoma. I package tecnici saranno valutati progressivamente per distinguere API condivise e implementazioni interne senza una riorganizzazione massiva.
