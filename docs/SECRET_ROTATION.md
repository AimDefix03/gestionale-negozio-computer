# Secret Management And Rotation

## Scopo

Questa procedura riguarda le credenziali infrastrutturali usate dal gestionale. Non sostituisce le policy del provider cloud, del database gestito o del secret manager scelto.

## Modalita supportate

Il backend e PostgreSQL accettano ogni segreto in uno solo dei due modi:

- valore iniettato direttamente dall'infrastruttura tramite la variabile standard;
- percorso di un file montato in sola lettura tramite la variabile con suffisso `_FILE`.

Se valore e percorso sono entrambi presenti, se un segreto obbligatorio manca, oppure se il file e illeggibile, vuoto o multilinea, il container termina con codice `78`. Il resolver non stampa valore o percorso del segreto.

Nello stack Compose il percorso raccomandato usa:

- `docker-compose.secrets.yml` per la password PostgreSQL;
- `docker-compose.bootstrap-secret.yml` solo durante il bootstrap iniziale;
- `GESTIONALE_DB_PASSWORD_SECRET_FILE` come file sorgente locale della password database;
- `GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD_SECRET_FILE` come file sorgente temporaneo del bootstrap.

I file sorgente devono trovarsi fuori dal controllo versione, preferibilmente fuori dal repository, e non essere inclusi in backup applicativi.

## Avvio con file secret

Preparare una directory locale non versionata:

```bash
mkdir -p secrets
chmod 0700 secrets
printf '%s' 'password-database-generata' > secrets/database-password
chmod 0444 secrets/database-password
```

La directory `0700` impedisce agli altri utenti host di attraversarla; il file `0444` permette ai diversi utenti non-root dei container Compose di leggerne il mount individuale, che Docker espone comunque in sola lettura. Su un orchestratore reale usare ownership e modalita fornite dal secret manager.

Configurare `.env.docker` con il percorso sorgente e avviare:

```bash
docker compose --env-file .env.docker \
  -f docker-compose.prod-like.yml \
  -f docker-compose.secrets.yml \
  up -d --build
```

Per un database vuoto aggiungere temporaneamente anche l'override bootstrap:

```bash
docker compose --env-file .env.docker \
  -f docker-compose.prod-like.yml \
  -f docker-compose.secrets.yml \
  -f docker-compose.bootstrap-secret.yml \
  up -d --build
```

Dopo la creazione e la verifica del primo super admin:

1. impostare `GESTIONALE_BOOTSTRAP_SUPER_ADMIN_ENABLED=false`;
2. riavviare senza `docker-compose.bootstrap-secret.yml`;
3. eliminare la versione bootstrap dal secret manager;
4. verificare che il relativo mount non compaia piu nel container backend;
5. conservare l'evento operativo nel sistema di change management.

## Rotazione password database

La procedura preferita usa una nuova identita database, quando il provider e il modello di ownership lo consentono:

1. creare una nuova credenziale nel secret manager senza revocare quella corrente;
2. creare o preparare un nuovo ruolo PostgreSQL con i privilegi minimi necessari a runtime e alle migrazioni previste;
3. verificare separatamente permessi su schema, tabelle, sequenze e Flyway;
4. distribuire la nuova versione del secret e aggiornare username e riferimento file del backend;
5. effettuare un rolling restart e attendere readiness `UP`;
6. verificare login, lettura catalogo e una transazione controllata;
7. monitorare errori di autenticazione e connessioni database;
8. revocare la vecchia credenziale solo dopo la finestra di osservazione;
9. chiudere le vecchie connessioni residue in una finestra controllata;
10. registrare versione, data, operatore, esito e rollback disponibile senza salvare il valore.

La rotazione in-place della password dello stesso ruolo puo interrompere nuove connessioni durante il riciclo del pool. Va pianificata come change con finestra di manutenzione o verificata sul provider specifico.

## Rollback

Finche la vecchia credenziale non e revocata:

1. ripristinare il riferimento alla precedente versione del secret;
2. ridistribuire il backend;
3. attendere readiness `UP`;
4. verificare il percorso critico;
5. analizzare l'errore prima di ripetere la rotazione.

Dopo la revoca, il rollback richiede una nuova credenziale: non riattivare segreti scaduti o compromessi.

## Secret manager reale

In staging e produzione il secret manager deve fornire:

- cifratura at rest e in transit;
- controllo accessi per identita del workload;
- versionamento e revoca;
- audit di letture e modifiche;
- rotazione automatica o procedura approvata;
- nessun valore nei manifest, nei log, nella CI o nell'immagine;
- disponibilita coerente con il piano di disaster recovery.

Lo stack Compose dimostra il contratto file-based, ma non sostituisce Vault, AWS Secrets Manager, Azure Key Vault, Google Secret Manager, Kubernetes Secrets protetti o servizi equivalenti.

## Verifiche

```bash
sh scripts/security/test-resolve-file-secrets.sh
sh scripts/security/verify-container-secrets.sh nome-container-postgres nome-container-backend
docker run --rm -v "$PWD:/repo" ghcr.io/gitleaks/gitleaks:v8.30.1 git /repo --redact --no-banner
```

La scansione Gitleaks deve essere eseguita anche sulla storia Git. Un segreto gia committato deve essere considerato compromesso anche dopo la rimozione dal file: va revocato e, se necessario, la storia va riscritta con una procedura coordinata.
