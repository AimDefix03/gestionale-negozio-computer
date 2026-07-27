# ADR 0001 - Web-first modular monolith

## Stato

Accettata come direzione raccomandata per la roadmap.

## Contesto

Il repository contiene una applicazione Swing legacy e una nuova applicazione web con backend Spring Boot e frontend React. La parte Swing dimostra pattern OOP e flussi operativi, ma il prodotto desiderato richiede interfaccia moderna, API, database e possibilita futura di deployment.

## Decisione

La direzione architetturale e una web app basata su monolite modulare:

- backend Spring Boot;
- frontend React + TypeScript;
- PostgreSQL come database target;
- Flyway per migrazioni;
- Docker Compose per sviluppo locale;
- moduli separati per dominio.

La parte Swing non viene cancellata subito. Rimane come legacy/reference finche i flussi web non sono completi.

## Alternative valutate

### Continuare solo con Swing

Pro: meno migrazione iniziale, sfrutta il codice esistente.

Contro: meno credibile come gestionale moderno, difficile da distribuire come prodotto web, UX limitata.

### Microservizi

Pro: isolamento forte dei moduli.

Contro: complessita prematura, costi operativi alti, dominio non ancora stabilizzato.

### Monolite modulare web-first

Pro: professionale, testabile, evolutivo, adatto a portfolio e prodotto MVP.

Contro: richiede disciplina nei confini dei moduli.

## Conseguenze

- La web app diventa sorgente principale del prodotto futuro.
- Il database deve diventare persistente e versionato.
- Le API devono stabilizzarsi con DTO, errori, paginazione e autorizzazioni.
- Il frontend deve essere refactorizzato in componenti e pagine.
- Ogni step deve mantenere compatibilita o documentare breaking change.
