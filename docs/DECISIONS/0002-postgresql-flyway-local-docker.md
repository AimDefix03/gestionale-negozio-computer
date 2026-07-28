# ADR 0002 - PostgreSQL, Flyway and local Docker database

## Stato

Accettata.

## Contesto

Il backend web usava H2 in memoria con Hibernate `ddl-auto: update`. Questa configurazione e comoda nelle prime prove ma non permette di governare l'evoluzione dello schema, validare migrazioni o simulare in modo credibile un ambiente operativo.

## Decisione

La web app usa:

- PostgreSQL 16 per sviluppo locale tramite Docker Compose;
- Flyway per migrazioni versionate;
- H2 solo nei test automatici;
- Hibernate `ddl-auto: validate`.

## Alternative valutate

### Restare su H2

Pro: semplice e veloce.

Contro: poco credibile, dati volatili, differenze rispetto a database reale.

### PostgreSQL senza Flyway

Pro: database reale.

Contro: schema non versionato e difficile da aggiornare.

### PostgreSQL con Flyway

Pro: schema reale, versionato e testabile.

Contro: richiede disciplina sulle migrazioni.

## Conseguenze

- Il backend di sviluppo richiede PostgreSQL avviato.
- I test restano autonomi con H2 e Flyway.
- Le modifiche future al modello dati devono passare da nuove migrazioni.
- `ddl-auto: update` non deve essere reintrodotto come meccanismo di evoluzione schema.
