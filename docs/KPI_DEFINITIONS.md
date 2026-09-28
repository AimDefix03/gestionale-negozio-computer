# Definizioni KPI e contratto temporale

## Scopo

Queste definizioni sono il contratto operativo della dashboard e dei report. Gli importi descrivono lo stato applicativo registrato dal gestionale e non sostituiscono contabilita, bilancio, dichiarazioni fiscali o fatturazione elettronica.

## KPI ordini

| Campo API | Etichetta UI | Definizione |
| --- | --- | --- |
| `totalOrders` | Ordini totali | Numero di ordini in qualsiasi stato. |
| `draftOrders` | Ordini in bozza | Numero di ordini con stato `DRAFT`. |
| `confirmedOrders` | Ordini confermati | Numero di ordini con stato `CONFIRMED`, non ancora evasi. |
| `fulfilledOrders` | Ordini evasi | Numero di ordini con stato `FULFILLED`. |
| `canceledOrders` | Ordini annullati | Numero di ordini con stato `CANCELED`. |
| `draftOrderValue` | Valore bozze | Somma dei totali snapshot degli ordini `DRAFT`. Non e un ricavo e non e un incasso. |
| `confirmedOrderValue` | Valore confermato | Somma dei totali snapshot degli ordini `CONFIRMED`. Non implica pagamento o evasione. |
| `fulfilledOrderValue` | Valore evaso | Somma dei totali snapshot degli ordini `FULFILLED`. Non coincide necessariamente con l'incassato. |
| `grossCollected` | Incassato lordo | Somma degli importi incassati registrati nel ledger dei pagamenti, prima di rimborsi e storni. |
| `refunded` | Rimborsato o stornato | Somma dei rimborsi e degli storni registrati nel ledger dei pagamenti. |
| `netCollected` | Incassato netto | `grossCollected - refunded`. Non viene chiamato fatturato o utile. |

Gli ordini `CANCELED` non contribuiscono ai valori di bozza, confermato o evaso. Un pagamento `UNRECONCILED` non produce importi economici presunti: richiede evidenze e riconciliazione manuale.

## KPI catalogo e magazzino

| Campo API | Etichetta UI | Definizione |
| --- | --- | --- |
| `products` | Prodotti | Numero di elementi del catalogo operativo. |
| `lowStock` | Scorte basse | Prodotti attivi con disponibilita vendibile maggiore di zero e non superiore alla soglia operativa di tre unita. |
| `outOfStock` | Esauriti | Prodotti con disponibilita vendibile pari a zero. |
| `potentialRetailStockValue` | Valore potenziale vendita | Somma, per ogni prodotto, di `quantita fisica x prezzo unitario di vendita scontato corrente`. |
| `knownInventoryCostValue` | Valore noto a costo | Somma, per ogni prodotto, di `costo medio ponderato x quantita valorizzata`. Esclude le unita prive di costo documentato. |
| `costedUnits` | Unita valorizzate | Quantita fisiche coperte da un costo di ricezione noto. |
| `uncostedUnits` | Unita senza costo | Quantita fisiche prive di un costo di ricezione attendibile. |
| `costCoveragePercentage` | Copertura costo | `unita valorizzate / unita fisiche x 100`; vale zero quando non esistono unita fisiche. |
| `potentialGrossMarginOnCostedStock` | Margine potenziale | Somma di `(prezzo di vendita scontato corrente - costo medio ponderato) x quantita valorizzata`, limitata alla quota con costo noto. |

`potentialRetailStockValue` non e valore contabile dell'inventario, costo di acquisto, margine, utile, ricavo o previsione certa di vendita. `knownInventoryCostValue` e `potentialGrossMarginOnCostedStock` sono indicatori gestionali basati sulle sole quantita valorizzate: non rappresentano inventario contabile, costo del venduto, utile o margine consuntivo. Il costo medio e mobile e ponderato sulle ricezioni fornitore registrate; costo unitario e media usano quattro decimali, mentre gli aggregati monetari usano due decimali, sempre con arrotondamento `HALF_UP`.

Per il singolo prodotto, `lastPurchaseCost` conserva il costo effettivo dell'ultima ricezione, `averagePurchaseCost` la media mobile, `costedQuantity` la quantita valorizzata e `uncostedQuantity` la parte storica o movimentata senza evidenza di costo. Un prodotto storico non viene valorizzato usando il prezzo di vendita. Le informazioni di costo sono riservate allo staff e non fanno parte della projection cliente.

## Contratto temporale

- Il clock applicativo e UTC.
- I timestamp storici persistiti come `LocalDateTime` sono interpretati esclusivamente come UTC.
- Le risposte API espongono gli eventi come `OffsetDateTime`: gli eventi operativi generali usano offset UTC, i documenti usano il fuso salvato nello snapshot aziendale.
- `timeZone` usa un identificatore IANA, per esempio `Europe/Rome`, e viene validato dal backend.
- L'anno documentale viene calcolato dall'istante corrente nel fuso aziendale, non dall'anno UTC.
- Il cambio di fuso e bloccato dopo l'emissione del primo documento, per preservare numerazione e storico.
- I documenti legacy precedenti alla migrazione V32 mantengono lo snapshot `UTC`; i nuovi documenti salvano il fuso configurato.
- I passaggi tra ora solare e ora legale sono delegati alle regole IANA; non vengono codificati offset fissi.

Esempio: `2026-12-31T23:30:00Z` corrisponde a `2027-01-01T00:30:00+01:00` in `Europe/Rome`. Un documento creato in quell'istante appartiene all'esercizio 2027.

## Configurazione documentale

Prima di generare un documento sono obbligatori:

- ragione sociale;
- almeno uno tra codice fiscale e partita IVA;
- indirizzo;
- CAP;
- citta;
- provincia;
- codice paese;
- fuso orario aziendale.

L'assenza di uno o piu campi disabilita la capability di emissione e il backend rifiuta comunque il comando. Ogni documento mantiene il disclaimer `DOCUMENTO SIMULATO - NON VALIDO AI FINI FISCALI`.

## Verifiche automatiche

Le definizioni sono protette da test su:

- matrice `DRAFT`, `CONFIRMED`, `FULFILLED` e `CANCELED`;
- incasso, rimborso/storno e netto;
- Capodanno nel fuso `Europe/Rome`;
- passaggi DST di `Europe/Rome`;
- configurazione aziendale incompleta;
- immutabilita del fuso dopo il primo documento;
- nomenclatura e separazione dei KPI nel frontend.
- ricezioni parziali a costi differenti e media ponderata;
- arrotondamento a quattro e due decimali;
- stock storico senza costo, copertura parziale e assenza di inferenze dal prezzo di vendita;
- esclusione dei costi dalla projection cliente.
