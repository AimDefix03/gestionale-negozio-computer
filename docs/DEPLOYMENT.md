# Deployment

## Stato attuale

Il progetto supporta sviluppo locale, test automatici e uno stack `prod-like` containerizzato. Include backup giornaliero e restore drill schedulati, ma non e ancora pronto per produzione reale senza storage off-site, HTTPS, osservabilita e validazione completa della checklist release.

## Sviluppo locale web

Avviare PostgreSQL:

```bash
cp .env.docker.example .env.docker
docker compose --env-file .env.docker up -d --build postgres
```

Prima dell'avvio sostituire tutti i placeholder in `.env.docker` con credenziali locali indipendenti. Il file non va versionato.

Avviare backend:

```bash
cd web/backend
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

Avviare frontend:

```bash
cd web/frontend
npm run dev
```

## Configurazione database sviluppo

Configurazione minima del backend:

- `GESTIONALE_DB_URL=jdbc:postgresql://localhost:5432/gestionale`
- `GESTIONALE_DB_OWNER_USERNAME=gestionale_owner`
- `GESTIONALE_DB_RUNTIME_USERNAME=gestionale_runtime`
- `GESTIONALE_DB_RUNTIME_PASSWORD=<password-runtime-locale>`
- `GESTIONALE_DB_MIGRATOR_USERNAME=gestionale_migrator`
- `GESTIONALE_DB_MIGRATOR_PASSWORD=<password-migrator-locale>`

Non esistono password predefinite. Le credenziali locali devono essere diverse da quelle di ambienti condivisi o produzione.

La configurazione completa delle variabili ambiente e descritta in `docs/CONFIGURATION.md`.

Il template backend e disponibile in `web/backend/.env.example`.

## Manutenzione sicurezza

Parametri configurabili:

- `gestionale.security.cleanup-delay-ms`: intervallo tra esecuzioni del cleanup.
- `gestionale.security.cleanup-initial-delay-ms`: ritardo iniziale del cleanup.
- `gestionale.security.session-duration-minutes`: durata assoluta della sessione.
- `gestionale.security.session-idle-timeout-minutes`: timeout di inattivita, non superiore alla durata assoluta.
- `gestionale.security.session-touch-interval-seconds`: intervallo minimo tra aggiornamenti dell'ultima attivita.
- `gestionale.security.session-retention-days`: giorni di conservazione per sessioni scadute o revocate.
- `gestionale.security.login-attempt-retention-hours`: ore di conservazione dei tentativi login non bloccati.

## Bootstrap super admin

Il backend crea il primo super admin solo se il database non contiene account e il bootstrap e abilitato esplicitamente. Il bootstrap e disabilitato per default in tutti i profili e non esistono credenziali precompilate o alias legacy.

In `prod`, se il database e vuoto e il bootstrap non e configurato, l'applicazione si ferma con errore. Primo avvio produzione:

```bash
SPRING_PROFILES_ACTIVE=prod \
GESTIONALE_DB_URL=jdbc:postgresql://host:5432/gestionale \
GESTIONALE_DB_OWNER_USERNAME=gestionale_owner \
GESTIONALE_DB_RUNTIME_USERNAME=gestionale_runtime \
GESTIONALE_DB_RUNTIME_PASSWORD=*** \
GESTIONALE_DB_MIGRATOR_USERNAME=gestionale_migrator \
GESTIONALE_DB_MIGRATOR_PASSWORD=*** \
GESTIONALE_BOOTSTRAP_SUPER_ADMIN_ENABLED=true \
GESTIONALE_BOOTSTRAP_SUPER_ADMIN_USERNAME=nome_admin_sicuro \
GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD='<secret-unico-fornito-in-modo-protetto>' \
mvn spring-boot:run
```

Dopo la creazione del primo super admin, riavviare senza `GESTIONALE_BOOTSTRAP_SUPER_ADMIN_ENABLED=true`.

Regole produzione:

- non usare username o password locali note: il backend le rifiuta in ogni profilo;
- password super admin minima: 12 caratteri, maiuscole, minuscole, numeri e simboli;
- il bootstrap non aggiorna o ricrea account se esiste gia almeno un account;
- la creazione di altri admin passa dal pannello operativo ed e consentita solo al super admin.

Un JAR avviato senza profilo e configurazione oppure con profilo `prod` privo di segreti deve terminare prima di raggiungere lo stato pronto. Il contratto si verifica con `scripts/security/verify-backend-fail-closed.sh`.

## Test

I test backend usano H2 in memoria con Flyway attivo e profilo `test`.

```bash
cd web/backend
mvn test
```

## Produzione

Avvio backend con profilo produzione:

```bash
SPRING_PROFILES_ACTIVE=prod \
GESTIONALE_DB_URL=jdbc:postgresql://host:5432/gestionale \
GESTIONALE_DB_OWNER_USERNAME=gestionale_owner \
GESTIONALE_DB_RUNTIME_USERNAME=gestionale_runtime \
GESTIONALE_DB_RUNTIME_PASSWORD=*** \
GESTIONALE_DB_MIGRATOR_USERNAME=gestionale_migrator \
GESTIONALE_DB_MIGRATOR_PASSWORD=*** \
mvn spring-boot:run
```

Nel profilo `prod`:

- il bootstrap del super admin e disattivato di default;
- H2 console resta disabilitata;
- Hibernate usa `ddl-auto: validate`;
- Flyway resta attivo;
- Hibernate usa il ruolo runtime privo di DDL e Flyway usa il ruolo migrator separato con `SET ROLE` verso l'owner `NOLOGIN`;
- le credenziali database devono arrivare da variabili protette dell'infrastruttura o file secret materializzati dal secret manager;
- Actuator ascolta sulla porta management interna `9090`, configurabile con `GESTIONALE_MANAGEMENT_PORT`;
- l'esposizione Actuator include `health` e `prometheus`, i dettagli health sono nascosti e le probe sono separate in liveness e readiness;
- la readiness include il database, mentre la liveness misura il processo applicativo senza usare PostgreSQL come dipendenza.
- i log console sono JSON strutturati con servizio, ambiente e correlation ID;

Lo stack prod-like esegue PostgreSQL, Java e Nginx con utenti non-root espliciti. I filesystem root sono read-only, tutte le capability Linux vengono eliminate e `no-new-privileges` impedisce acquisizioni successive. Le sole aree scrivibili sono tmpfs effimeri e il volume dati PostgreSQL. La verifica obbligatoria e:

```bash
scripts/security/verify-container-hardening.sh \
  gestionale-prodlike-postgres \
  gestionale-prodlike-backend \
  gestionale-prodlike-frontend
```

La porta management non deve essere pubblicata dal Service, ingress o reverse proxy destinato agli utenti. L'orchestratore deve usare:

```text
/actuator/health/liveness
/actuator/health/readiness
```

La prima decide quando riavviare il processo; la seconda decide quando rimuovere o reinserire l'istanza dal traffico. Una perdita temporanea del database non deve provocare un ciclo di riavvii applicativi.

Il sistema di monitoraggio e l'unico consumer previsto di `/actuator/prometheus`. Network policy e security group devono consentire il traffico verso la porta management soltanto da orchestratore e monitoraggio. Configurazione, metriche, alert e runbook sono descritti in `docs/OBSERVABILITY.md`.

## Hardening browser

L'immagine frontend applica tramite Nginx una CSP in enforcement che consente script, stili, connessioni, font e risorse soltanto dalla stessa origine. Non sono ammessi `unsafe-inline` o `unsafe-eval`; framing, plugin e media sono bloccati. Nginx rimuove gli header browser eventualmente prodotti dal backend e applica una policy unica anche alle risposte API proxy.

La shell HTML usa `Cache-Control: no-store`; gli asset Vite con hash usano `public, max-age=31536000, immutable`. La configurazione e verificabile con:

```bash
scripts/security/verify-browser-security.sh https://dominio-gestionale.example
```

HSTS non deve essere aggiunto allo stack HTTP locale. Va attivato sul dominio reale soltanto dopo avere verificato HTTPS end-to-end, rinnovo certificati e redirect HTTP definitivo.

Prima di un rilascio reale servono ancora:

- installare e monitorare i timer descritti in `docs/BACKUP_RESTORE.md`;
- replicare i backup su storage off-site cifrato e immutabile e provare il restore dalla copia remota;
- HTTPS e configurazione dell'IP reale esclusivamente da proxy fidati;
- collettore centralizzato per log JSON con retention e access control;
- Alertmanager con destinatari e reperibilita verificati;
- rate limiting condiviso a livello gateway se vengono usate piu repliche;
- integrazione con il secret manager remoto scelto e verifica della procedura `docs/SECRET_ROTATION.md`;
- network policy che limiti la porta management ai soli sistemi di orchestrazione e monitoraggio;
- esecuzione completa di `docs/RELEASE_CHECKLIST.md`.
