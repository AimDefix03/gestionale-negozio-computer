# Verifica prod-like ricorrente

## Scopo

La verifica prod-like controlla in modo ripetibile che immagini, migrazioni, runtime, sicurezza, osservabilita, flussi browser e procedure di recovery continuino a funzionare insieme.

Non esegue deploy e non usa dati di produzione. Ogni esecuzione crea credenziali effimere, un progetto Docker isolato e un database PostgreSQL dedicato.

## Esecuzione automatica

La workflow `.github/workflows/ci.yml` esegue la verifica:

- a ogni push su `main`;
- per ogni pull request verso `main`;
- su avvio manuale;
- ogni lunedi alle 03:23 UTC.

La schedulazione GitHub Actions usa sempre il contenuto del branch predefinito. Il job ha un timeout di 60 minuti e il quality gate fallisce se la verifica prod-like non termina correttamente.

## Controlli

Lo script `scripts/ci/run-prod-like-verification.sh` esegue:

1. validazione della configurazione Compose e Prometheus;
2. build delle immagini locali con aggiornamento esplicito delle immagini base;
3. avvio di PostgreSQL, backend Spring Boot e frontend Nginx;
4. readiness, liveness e isolamento della porta Actuator;
5. verifica di tutte le migrazioni Flyway versionate sul database reale;
6. controllo dei secret montati, utenti non-root, filesystem read-only e privilegi minimi;
7. CSP, header browser, cache e rate limiting del login;
8. raccolta metriche, log JSON, regole alert e hardening Prometheus;
9. typecheck e smoke test Playwright sul flusso applicativo reale;
10. lifecycle, scheduling, backup e restore PostgreSQL;
11. acquisizione diagnostica;
12. rimozione di container, reti, volumi e secret effimeri;
13. verifica esplicita dell'assenza di risorse Docker residue.

## Esecuzione locale

Prerequisiti:

- Docker Desktop o Docker Engine con Compose;
- Node.js 22 e npm;
- OpenSSL;
- curl;
- porte locali `55433`, `55434`, `55435`, `28080`, `28081` e `29091` disponibili.

Comando:

```bash
scripts/ci/run-prod-like-verification.sh
```

Su una macchina CI Linux che deve installare anche le dipendenze di sistema di Chromium:

```bash
PLAYWRIGHT_INSTALL_WITH_DEPS=true scripts/ci/run-prod-like-verification.sh
```

Le porte possono essere personalizzate con:

- `PRODLIKE_POSTGRES_PORT`;
- `PRODLIKE_BACKEND_PORT`;
- `PRODLIKE_FRONTEND_PORT`;
- `PRODLIKE_PROMETHEUS_PORT`.
- `PRODLIKE_BACKUP_VERIFY_POSTGRES_PORT`;
- `PRODLIKE_RESTORE_DRILL_POSTGRES_PORT`.

Le sei porte devono essere numeriche e distinte. Il runner interrompe l'esecuzione prima di creare risorse Docker se rileva una collisione.

Il nome progetto puo essere reso deterministico con `PRODLIKE_RUN_ID` oppure sostituito con `PRODLIKE_PROJECT_NAME`.

## Diagnostica

Lo script salva sempre:

- metadati dell'esecuzione;
- stato dei servizi Compose;
- log completi senza colori;
- metadati delle immagini;
- report e risultati Playwright, quando presenti;
- esito del cleanup.

La directory predefinita e temporanea. Per conservarla in un percorso noto:

```bash
PRODLIKE_DIAGNOSTICS_DIR=/tmp/gestionale-prodlike-diagnostics \
  scripts/ci/run-prod-like-verification.sh
```

GitHub Actions pubblica la directory come artefatto `prod-like-diagnostics-<run>-<attempt>` anche quando il job fallisce.

I file diagnostici non includono i valori delle password effimere. I log applicativi non devono contenere password, token o payload sensibili.

## Isolamento e cleanup

Il runner usa:

- nomi container derivati dal progetto Compose isolato;
- database e utente dedicati;
- password casuali montate come secret read-only;
- porte distinte dagli smoke test E2E e dal restore drill;
- trap di cleanup attivo su uscita normale, errore e interruzione.

Il controllo `scripts/ci/verify-prod-like-cleanup.sh` fallisce se, dopo `docker compose down -v --remove-orphans`, rimangono container, volumi o reti con la label del progetto.

## Gestione dei fallimenti

In caso di errore:

1. aprire l'artefatto diagnostico;
2. controllare `compose-ps.txt` e `compose.log`;
3. verificare `playwright-report` e `test-results` per errori browser;
4. leggere `cleanup.log` per distinguere un errore applicativo da un residuo infrastrutturale;
5. riprodurre localmente con lo stesso commit;
6. non rilanciare o ignorare il quality gate senza una causa identificata.

Errori temporanei del registry immagini o indisponibilita GitHub Actions possono richiedere un nuovo tentativo, ma non devono essere classificati come successo applicativo.
