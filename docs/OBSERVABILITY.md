# Observability

## Obiettivo

Il profilo `prod` produce log JSON correlabili, espone metriche Prometheus su una porta di management separata e include regole di alert per disponibilita, errori, autenticazione, memoria e pool database.

L'osservabilita non modifica il contratto delle API e non espone metriche sulla porta pubblica.

## Avvio prod-like

Prometheus e un servizio opzionale del profilo Compose `observability`:

```bash
docker compose --env-file .env.docker \
  -f docker-compose.prod-like.yml \
  -f docker-compose.secrets.yml \
  --profile observability \
  up --build -d
```

Endpoint locali:

- frontend: `http://127.0.0.1:8081`;
- API backend: `http://127.0.0.1:8080`;
- Prometheus: `http://127.0.0.1:9091`;
- metriche backend: `http://backend:9090/actuator/prometheus`, raggiungibili solo dalla rete interna Compose.

La porta Prometheus e vincolata a loopback nello stack prod-like. In produzione reale non deve essere pubblicata senza autenticazione, TLS e controllo di rete.

## Log strutturati

Il profilo `prod` usa il formato JSON Logstash integrato in Spring Boot. Ogni richiesta completata genera un evento `HTTP request completed` con:

- `service`;
- `environment`;
- `requestId`;
- `http_method`;
- `http_path`;
- `http_status`;
- `duration_ms`.

Il filtro non registra query string, body, password, token o header di autorizzazione. Gli errori imprevisti mantengono lo stack trace nei log server, mentre il client riceve un errore generico con `requestId`.

Docker usa il driver `json-file` con rotazione configurabile:

- `GESTIONALE_LOG_MAX_SIZE`, default `10m`;
- `GESTIONALE_LOG_MAX_FILES`, default `5`.

La rotazione locale limita l'uso disco, ma non sostituisce un collettore centralizzato con retention, access control e ricerca.

## Metriche

Prometheus raccoglie le metriche ogni 15 secondi. Oltre alle metriche standard JVM, HTTP, datasource e Hikari, il backend espone:

| Metrica | Tag ammessi | Significato |
| --- | --- | --- |
| `gestionale_authentication_attempts_total` | `outcome` | Login riusciti, credenziali non valide e account bloccati |
| `gestionale_session_events_total` | `outcome` | Rinnovi riusciti o falliti e logout |
| `gestionale_api_errors_total` | `status`, `code` | Errori API gestiti e prodotti dalla security chain |

I tag sono enumerati e a cardinalita limitata. Non aggiungere username, token, request ID, URL dinamiche o dati cliente alle metriche.

La retention locale e configurata con `GESTIONALE_METRICS_RETENTION`, default `15d`.

## Alert

Le regole in `deploy/observability/alerts.yml` definiscono:

| Alert | Severita | Condizione |
| --- | --- | --- |
| `GestionaleBackendUnavailable` | critical | backend non raggiungibile per 2 minuti |
| `GestionaleElevatedServerErrors` | warning | frequenza 5xx superiore a 0,1/s per 5 minuti |
| `GestionaleRepeatedLoginFailures` | warning | oltre 20 login falliti o bloccati in 10 minuti |
| `GestionaleJvmHeapPressure` | warning | heap oltre 85% per 10 minuti |
| `GestionaleDatabasePoolSaturation` | warning | pool Hikari oltre 85% per 5 minuti |

Le soglie sono una baseline. Prima del rilascio vanno calibrate con carico e traffico realistici.

## Runbook

### Backend non disponibile

1. Verificare readiness e liveness.
2. Controllare i log JSON del backend usando l'intervallo temporale dell'alert.
3. Verificare PostgreSQL e le migrazioni.
4. Riavviare solo se la liveness e fallita; una sola readiness fallita richiede prima la diagnosi della dipendenza.

### Errori 5xx

1. Cercare gli eventi tramite `requestId`.
2. Raggruppare per codice errore e percorso.
3. Controllare deploy recenti, database e dipendenze.
4. Eseguire rollback se il tasso cresce dopo una release.

### Login anomali

1. Verificare lockout backend e rate limiting Nginx.
2. Correlare finestra temporale, indirizzo proxy affidabile e account coinvolti senza esportare credenziali.
3. Non sbloccare account senza verifica dell'identita.

### Memoria o pool database

1. Verificare andamento, non il solo valore istantaneo.
2. Controllare latenza HTTP, query lente e numero di istanze.
3. Acquisire diagnostica prima di un riavvio, se il servizio e ancora raggiungibile.

## Verifiche

Validazione statica:

```bash
scripts/observability/verify-prometheus-config.sh
```

Verifica runtime su stack avviato:

```bash
scripts/observability/verify-runtime-observability.sh \
  gestionale-prodlike-backend \
  http://127.0.0.1:9091 \
  9090
```

Hardening container Prometheus:

```bash
scripts/observability/verify-prometheus-hardening.sh \
  gestionale-prodlike-prometheus
```

Isolamento dalla porta API:

```bash
scripts/security/verify-actuator-exposure.sh \
  http://127.0.0.1:8080
```

## Limiti e responsabilita infrastrutturali

- Il repository non configura un destinatario Alertmanager: email, paging e reperibilita dipendono dall'ambiente.
- I log non sono ancora inviati a un archivio centralizzato.
- Freschezza e restore dei backup sono verificati dagli script e dai timer dedicati, ma l'alert remoto richiede il sistema di monitoraggio dell'infrastruttura.
- La porta management deve essere protetta da network policy nel deployment reale.
- Prometheus locale non sostituisce una piattaforma altamente disponibile con backup delle metriche.
