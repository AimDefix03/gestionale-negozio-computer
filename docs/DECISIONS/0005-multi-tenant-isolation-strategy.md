# ADR 0005 - Strategia di isolamento multi-tenant

## Stato

Accettata come architettura target. L'implementazione non e ancora iniziata.

## Contesto

Il gestionale usa oggi un solo spazio dati:

- `CompanySettings` e un singleton;
- account, prodotti, anagrafiche, ordini, documenti e audit sono globali;
- la giacenza e memorizzata direttamente sul prodotto;
- sessioni e autorizzazioni non contengono un tenant;
- i codici business sono univoci sull'intera installazione.

Questa struttura e coerente con un'installazione singola, ma non puo garantire isolamento tra clienti SaaS o gestire piu aziende, sedi e magazzini nello stesso contratto.

## Decisione

Il prodotto adottera un modello ibrido:

- schema PostgreSQL condiviso con `tenant_id` per il servizio SaaS standard;
- PostgreSQL Row-Level Security come difesa aggiuntiva all'isolamento applicativo;
- database o deployment dedicato per clienti con requisiti di isolamento superiori;
- stesso monolite modulare e stesso modello logico in entrambe le modalita;
- nessuno schema PostgreSQL creato dinamicamente per tenant.

`Tenant`, `LegalEntity`, `Site` e `Warehouse` sono concetti distinti:

- il tenant e il confine contrattuale e di sicurezza;
- la legal entity rappresenta un'azienda giuridica;
- la sede rappresenta una localita operativa;
- il magazzino possiede le giacenze.

L'identita utente resta globale. L'accesso a un tenant passa da una membership esplicita. La sessione contiene il tenant selezionato e non accetta `tenant_id` dai payload operativi come fonte autorevole.

L'isolamento viene applicato su piu livelli:

1. contesto tenant derivato dalla sessione autenticata;
2. servizi e repository tenant-aware;
3. chiavi e foreign key composte con `tenant_id`;
4. policy PostgreSQL con `ENABLE ROW LEVEL SECURITY` e `FORCE ROW LEVEL SECURITY`;
5. test automatici negativi tra tenant.

## Alternative valutate

### Database per ogni tenant

Pro:

- isolamento dati molto forte;
- backup, restore e cancellazione confinati;
- personalizzazioni infrastrutturali per cliente.

Contro:

- provisioning, migrazioni e osservabilita piu costosi;
- pool di connessioni e manutenzione moltiplicati;
- inefficiente come unica modalita per molti tenant piccoli.

Decisione: disponibile come livello dedicato, non come modello unico.

### Schema PostgreSQL per ogni tenant

Pro:

- separazione nominale dentro lo stesso database.

Contro:

- migrazioni Flyway replicate per schema;
- ricerca, pooling e troubleshooting piu complessi;
- rischio di drift tra schemi;
- vantaggi di isolamento inferiori a un database dedicato.

Decisione: non adottato.

### Schema condiviso con solo filtri applicativi

Pro:

- implementazione iniziale piu semplice.

Contro:

- una query dimenticata puo esporre dati di un altro tenant;
- script, report e query native aggirano facilmente il filtro.

Decisione: insufficiente senza vincoli database e RLS.

### Schema condiviso con isolamento multilivello

Pro:

- costo operativo sostenibile;
- query e migrazioni uniformi;
- isolamento verificabile a piu livelli;
- percorso compatibile con il monolite modulare.

Contro:

- migrazione ampia;
- disciplina obbligatoria su transazioni, repository e contesto;
- test di isolamento non negoziabili.

Decisione: modello predefinito.

## Conseguenze

- tutte le entita operative dovranno appartenere a un tenant;
- le unicita globali dei codici business diventeranno unicita per tenant;
- `CompanySettings` non sara piu un singleton globale;
- stock e riserve verranno spostati dal prodotto a saldi per magazzino;
- sessioni, idempotenza, audit, metriche e job asincroni dovranno conservare il tenant;
- il ruolo applicativo PostgreSQL non dovra essere proprietario delle tabelle e dovra essere soggetto a RLS;
- le migrazioni verranno eseguite con un ruolo separato;
- una richiesta priva di tenant valido dovra fallire senza accedere ai dati;
- la modalita single-tenant restera supportata tramite un tenant predefinito, non tramite rami di codice differenti.

## Condizioni prima dell'implementazione

- approvare il modello commerciale tra installazione dedicata e SaaS pooled;
- completare la proposta GDPR e retention dello Step 34;
- definire onboarding e provisioning dello Step 35;
- disporre di test PostgreSQL reali per RLS e foreign key composte;
- inventariare ogni tabella, query nativa, export, job e metrica che tratta dati tenant-owned;
- pianificare migrazioni expand/contract e rollback provati su una copia dati.

