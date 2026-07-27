# Configuration

## Obiettivo

Questo documento descrive le variabili di configurazione del gestionale web e distingue valori locali, test e produzione.

I file `.env` reali non devono essere versionati. Il repository deve contenere solo file `.env.example` o `.env.docker.example` con placeholder.

## Profili

| Profilo | Uso | Note |
| --- | --- | --- |
| `dev` | sviluppo locale | usa PostgreSQL locale e bootstrap super admin locale se il database e vuoto |
| `test` | test automatici | usa H2 in memoria e Flyway attivo |
| `prod` | produzione o staging serio | richiede variabili ambiente esplicite |

## Backend

File esempio:

- `web/backend/.env.example`

Variabili principali:

| Variabile | Obbligatoria in prod | Descrizione |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | si | profilo Spring, normalmente `prod` |
| `GESTIONALE_DB_URL` | si | JDBC URL del database PostgreSQL |
| `GESTIONALE_DB_USERNAME` | si | utente applicativo database |
| `GESTIONALE_DB_PASSWORD` | alternativa | password database iniettata direttamente dal secret manager |
| `GESTIONALE_DB_PASSWORD_FILE` | alternativa | file runtime contenente la password database; non usarlo insieme al valore diretto |
| `GESTIONALE_MANAGEMENT_PORT` | no | porta interna Actuator nel profilo `prod`, default `9090`; non pubblicarla verso Internet |
| `GESTIONALE_BOOTSTRAP_SUPER_ADMIN_ENABLED` | solo primo avvio | abilita creazione primo super admin se il database e vuoto |
| `GESTIONALE_BOOTSTRAP_SUPER_ADMIN_USERNAME` | solo primo avvio | username del primo super admin |
| `GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD` | alternativa, solo primo avvio | password forte iniettata direttamente per il primo super admin |
| `GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD_FILE` | alternativa, solo primo avvio | file runtime della password bootstrap; non usarlo insieme al valore diretto |
| `GESTIONALE_SECURITY_SESSION_DURATION_MINUTES` | no | durata assoluta della sessione, default 45 minuti |
| `GESTIONALE_SECURITY_SESSION_IDLE_TIMEOUT_MINUTES` | no | timeout di inattivita, default 30 minuti e non superiore alla durata assoluta |
| `GESTIONALE_SECURITY_SESSION_TOUCH_INTERVAL_SECONDS` | no | intervallo minimo tra aggiornamenti dell'ultima attivita, default 60 secondi |
| `GESTIONALE_SECURITY_CLEANUP_DELAY_MS` | no | intervallo cleanup sicurezza |
| `GESTIONALE_SECURITY_CLEANUP_INITIAL_DELAY_MS` | no | ritardo iniziale cleanup sicurezza |
| `GESTIONALE_SECURITY_SESSION_RETENTION_DAYS` | no | conservazione sessioni scadute o revocate |
| `GESTIONALE_SECURITY_LOGIN_ATTEMPT_RETENTION_HOURS` | no | conservazione tentativi login falliti |

## Bootstrap super admin

Il bootstrap crea il primo super admin solo quando:

- il database non contiene account;
- `GESTIONALE_BOOTSTRAP_SUPER_ADMIN_ENABLED=true`;
- username e password sono configurati.

In produzione sono vietate le credenziali locali di default, ad esempio `admin/RootSecure123!`.

Dopo il primo avvio, disabilitare il bootstrap e riavviare l'applicazione.

## Frontend

File esempio:

- `web/frontend/.env.example`

Al momento il frontend non richiede variabili obbligatorie. In sviluppo usa il proxy Vite verso `http://localhost:8080` per le chiamate `/api`.

## Docker prod-like

File esempio:

- `.env.docker.example`

Il file va copiato in `.env.docker`. I percorsi `GESTIONALE_DB_PASSWORD_SECRET_FILE` e, solo al primo avvio, `GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD_SECRET_FILE` devono puntare a file esterni al repository.

Variabili operative dei backup:

| Variabile | Default | Descrizione |
| --- | --- | --- |
| `BACKUP_RETENTION_DAYS` | `14` | eta massima delle copie gestite dallo script |
| `BACKUP_RETENTION_COUNT` | `30` | numero massimo di archivi conservati |
| `BACKUP_MAX_AGE_HOURS` | `26` | soglia di freschezza oltre la quale il controllo fallisce |
| `RESTORE_DRILL_MAX_SECONDS` | `900` | durata massima ammessa dal restore drill isolato |

Variabili di osservabilita:

| Variabile | Default | Descrizione |
| --- | --- | --- |
| `GESTIONALE_PROMETHEUS_PORT` | `9091` | porta host Prometheus vincolata a loopback |
| `GESTIONALE_LOG_MAX_SIZE` | `10m` | dimensione massima di un file log Docker |
| `GESTIONALE_LOG_MAX_FILES` | `5` | numero massimo di file log Docker ruotati |
| `GESTIONALE_METRICS_RETENTION` | `15d` | retention TSDB Prometheus locale |

La directory operativa si configura con `BACKUP_DIR` nel file dedicato `/etc/gestionale/backup.env`. Non deve trovarsi nel repository e deve essere scrivibile soltanto dall'identita che esegue i backup.

```bash
docker compose --env-file .env.docker \
  -f docker-compose.prod-like.yml \
  -f docker-compose.secrets.yml \
  up --build
```

Per il primo avvio su database vuoto e necessario abilitare temporaneamente `GESTIONALE_BOOTSTRAP_SUPER_ADMIN_ENABLED=true`, poi disabilitarlo dopo la creazione del super admin.

Variabili del reverse proxy:

| Variabile | Default | Descrizione |
| --- | --- | --- |
| `GESTIONALE_LOGIN_RATE_LIMIT` | `10r/m` | frequenza login accettata per IP da Nginx; usare la sintassi Nginx, ad esempio `10r/m` o `1r/s` |
| `GESTIONALE_LOGIN_RATE_BURST` | `10` | richieste aggiuntive ammesse nel burst prima della risposta `429` |
| `GESTIONALE_LOGIN_RATE_RETRY_AFTER_SECONDS` | `60` | valore in secondi comunicato al client tramite `Retry-After` |

Il limite Nginx per IP completa, ma non sostituisce, il lockout persistente per username del backend. In presenza di NAT condivisi o proxy aziendali le soglie devono essere verificate con traffico realistico. Dietro un load balancer, configurare l'IP reale solo per indirizzi proxy esplicitamente fidati.

## Regole sui segreti

- Non committare `.env`.
- Non committare `.env.docker`.
- Non committare password, token, chiavi API o credenziali database reali.
- Usare `.env.example` e `.env.docker.example` solo con placeholder.
- Non configurare contemporaneamente una variabile sensibile e la corrispondente variabile `_FILE`.
- In produzione usare variabili protette fornite dall'infrastruttura o file read-only materializzati da un secret manager.
- Rimuovere il secret bootstrap e il relativo override dopo il primo accesso verificato.
- Seguire `docs/SECRET_ROTATION.md` per rotazione, verifica e rollback.

## Controllo manuale prima del deploy

Prima di avviare in produzione verificare:

- profilo `prod` attivo;
- database PostgreSQL raggiungibile;
- `ddl-auto` non impostato a `update`;
- Flyway attivo;
- bootstrap super admin disattivato dopo il primo avvio;
- backend non esposto direttamente su interfacce esterne;
- porta management separata dalla porta API, non pubblicata sull'host e raggiungibile solo dalla rete operativa autorizzata;
- esposizione Actuator limitata a `health`, con dettagli disabilitati e probe liveness/readiness verificate;
- endpoint Prometheus raggiungibile solo dalla rete di monitoraggio e mai dalla porta API pubblica;
- log JSON, rotazione Docker, metriche e regole alert verificati come descritto in `docs/OBSERVABILITY.md`;
- soglie rate limit login configurate e verificate senza disabilitare il lockout backend;
- durata assoluta, timeout inattivita e intervallo aggiornamento sessione coerenti con le policy aziendali;
- nessun `.env` tracciato da Git;
- Gitleaks completato senza rilevazioni non gestite;
- password non presenti nella configurazione Docker e mount secret verificati read-only;
- test backend e build frontend completati.
