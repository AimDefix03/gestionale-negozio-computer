# AGENTS.md

Regole permanenti per lavorare su questo repository.

## Fonte primaria

Il codice reale e lo stato Git hanno priorita su qualsiasi descrizione esterna. Prima di ogni modifica vanno verificati struttura, build, configurazione, test e stato delle modifiche non committate.

## Stato del progetto

Il repository contiene due linee:

- applicazione desktop legacy Java Swing nella root;
- migrazione web in `web/` con backend Spring Boot e frontend React.

La direzione raccomandata e web-first con monolite modulare. La parte Swing resta utile come riferimento funzionale finche la web app non copre i flussi principali.

## Protocollo di lavoro

- Procedere sempre a step piccoli, approvati e verificabili.
- Non implementare piu moduli grandi nello stesso step.
- Non modificare file non pertinenti allo step approvato.
- Non effettuare commit, push, merge, reset o cancellazioni distruttive senza autorizzazione esplicita.
- Non dichiarare conformita fiscale, legale, GDPR o produttiva senza verifica professionale.
- Non lasciare TODO, placeholder o integrazioni simulate presentate come complete.

## Qualita tecnica

- Backend: usare DTO pubblici, servizi transazionali, validazione input, errori coerenti e autorizzazioni lato server.
- Frontend: ogni vista deve gestire caricamento, vuoto, errore, successo, permessi e sessione scaduta.
- Database: ogni evoluzione strutturale futura deve passare da migrazioni versionate.
- Sicurezza: il frontend puo nascondere azioni, ma il backend deve sempre far rispettare permessi e regole.

## Comandi di verifica

Build web dalla root:

```bash
mvn verify
```

Backend web:

```bash
cd web/backend
mvn test
```

Frontend web:

```bash
cd web/frontend
npm run build
```

Swing legacy:

```bash
mvn -f pom-legacy.xml test
```

## Documentazione

Ogni step che cambia architettura, modello dati, API, sicurezza o deployment deve aggiornare la documentazione in `docs/`.
