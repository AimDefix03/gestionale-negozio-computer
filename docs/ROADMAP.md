# Roadmap

Stato verificato il 2026-09-05.

## Fonte e stato reale

`MASTER_SOURCE_AGENTE_AI.md` definisce ordine, dipendenze, test, criteri di accettazione e stop condition. Questa roadmap ne espone la classificazione operativa senza sostituirlo.

La precedente roadmap documentava molte capacita gia implementate, ma non dimostrava la chiusura delle criticita emerse dal nuovo audit. Quelle implementazioni restano evidenze tecniche utili, non prove di prontezza produttiva.

Stato corrente:

- prototipo avanzato senza P0 aperti;
- maturita indicativa del 95-97% nel repository reale dopo lo Step 4.3;
- 32 step completati, sincronizzati e verificati nel repository reale su 48 (66,7%);
- gli step MVP approvati sono ammessi uno alla volta secondo `PRODUCT_SCOPE.md`;
- i Gate A, B e C sono soddisfatti tecnicamente nel repository reale.

## Classificazione del backlog

| Classe | Step | Scopo | Gate |
|---|---|---|---|
| Stabilizzazione | 0.3, 1.1-1.6, 2.1-2.9 | Fixture realistiche, chiusura P0, identita stabile, runner sicuri, framework aggiornato, migration correttive, invarianti di stock e denaro, PostgreSQL autorevole | Gate 0, A e B |
| MVP | 3.0-3.10 | Workflow operativi coerenti, vendita assistita, viste cliente, lifecycle account, frontend affidabile, KPI corretti, accessibilita e gate E2E | Gate C |
| Dopo MVP | 4.1-4.8, 5.1-5.6, 6.1-6.5 e Fase 7 | Funzioni commerciali estese, beta controllata, hardening produttivo, compliance, packaging, supportabilita e integrazioni | Gate D, E e F |

“Dopo MVP” non significa facoltativo per la produzione. Gli step 4.7, 4.8 e gli interventi della Fase 5 restano necessari prima di un rilascio reale anche quando risolvono finding P1.

## Stabilizzazione

Durante questa classe sono accettati esclusivamente P0/P1, test e correzioni necessarie. Gli interventi P2 vengono eseguiti solo quando sono una dipendenza esplicita per fixture, PostgreSQL, integrita o gate.

### Preparazione

- 0.1 Baseline nel repository reale: completato.
- 0.2 Freeze di scope e nuove feature: completato.
- 0.3 Fixture anonimizzate e dati di test realistici: completato.

### Chiusura P0

- 1.1 Registrazione pubblica limitata a `CUSTOMER`: completato.
- 1.2 Censimento account e revoca delle sessioni potenzialmente emesse: completato.
- 1.3 Subject di sessione stabile: completato.
- 1.4 Ownership degli ordini basata su ID: completato.
- 1.5 Runner Docker isolati e non distruttivi: completato.
- 1.6 Spring Security aggiornato e avvio fail-closed: completato.

### Integrita dati e processi

- 2.1 Migration correttive per V15 e V17: completato.
- 2.2 Identita e codici business canonici: completato.
- 2.3 Ledger magazzino autorevole: completato.
- 2.4 Idempotenza atomica e concorrente: completato.
- 2.5 Separazione tra esito comando e refresh frontend: completato.
- 2.6 Annullamento ordine con reversal: completato.
- 2.7 Riconciliazione finanziaria: completato.
- 2.8 Ruoli PostgreSQL least privilege e porta chiusa: completato.
- 2.9 Suite PostgreSQL obbligatoria: completato.

## MVP

- 3.0 Benchmark dei workflow reali: completato; template e prima scheda in `WORKFLOW_BENCHMARKS.md`.
- 3.1 Vendita assistita da personale: completato tecnicamente; test automatici verdi, prova operatore reale ancora da eseguire.
- 3.2 Projection e dashboard separate per cliente: completato.
- 3.3 Lifecycle account completo: completato.
- 3.4 Query state e sessione frontend centralizzati: completato.
- 3.5 Decomposizione di `App.tsx` per vertical slice: completato.
- 3.6 Capability e aggregazioni server-side: completato.
- 3.7 KPI, timezone e configurazione azienda corretti: completato.
- 3.8 Resi multi-riga e feedback operativo: completato e sincronizzato.
- 3.9 Accessibilita e preservazione delle bozze: completato, sincronizzato e verificato nel repository reale.
- 3.10 Gate E2E dell'MVP: completato, sincronizzato e verificato nel repository reale.

Il Gate C richiede workflow core completi, autorizzazioni coerenti, dati corretti e test E2E ripetibili. Lo Step 3.10 soddisfa tecnicamente questi criteri su PostgreSQL e browser reali. La prova guidata con operatore resta `PENDING_OPERATOR_EXECUTION` e il Gate C non equivale ancora a produzione.

## Dopo MVP

### Beta controllata

- 4.1 Ordini fornitore: completato, sincronizzato e verificato nel repository reale; prova operatore ancora pendente.
- 4.2 Ricezione fisica e costo: completato, sincronizzato e verificato nel repository reale.
- 4.3 Inventario fisico e rettifiche approvate: completato, sincronizzato e verificato nel repository reale.
- 4.4-4.6 Riordino, import e performance.
- 4.7 Backup off-site e restore parallelo.
- 4.8 Restart, consegna alert e log centralizzati.

### Produzione per scope definito

- 5.1 Supply-chain security.
- 5.2 Artifact immutabile, promozione e rollback.
- 5.3 Security hardening e penetration test.
- 5.4 Test di carico, concorrenza, failure e disaster recovery.
- 5.5 Privacy e retention.
- 5.6 Decisione vincolante sul perimetro pagamenti.

### Commercializzazione supportabile

- 6.1 Packaging, onboarding e configurazione guidata.
- 6.2 Supportabilita e policy di upgrade.
- 6.3 Permessi granulari e approvazioni.
- 6.4 Documenti fiscali tramite provider e consulenza professionale.
- 6.5 Integrazioni e funzioni vendibili.

La Fase 7 contiene soltanto evoluzioni future guidate da domanda commerciale. Multi-magazzino, multi-azienda e multi-tenant non entrano nel prodotto senza un nuovo threat model e un nuovo scope approvato.

## Regola di avanzamento

Ogni step viene eseguito singolarmente, nel suo ordine, dopo autorizzazione. Non si avanza se test, criteri di accettazione, dipendenze o stop condition non sono soddisfatti. Lo stato puntuale e registrato in `IMPLEMENTATION_PROGRESS.md`.
