# Secret Management And Rotation

## Scopo

Questa procedura riguarda le credenziali infrastrutturali usate dal gestionale. Non sostituisce le policy del provider cloud, del database gestito o del secret manager scelto.

## Modalita supportate

Il backend e PostgreSQL accettano ogni segreto in uno solo dei due modi:

- valore iniettato direttamente dall'infrastruttura tramite la variabile standard;
- percorso di un file montato in sola lettura tramite la variabile con suffisso `_FILE`.

Se valore e percorso sono entrambi presenti, se un segreto obbligatorio manca, oppure se il file e illeggibile, vuoto o multilinea, il container termina con codice `78`. Il resolver non stampa valore o percorso del segreto.

Nello stack Compose il percorso raccomandato usa:

- `docker-compose.secrets.yml` per le credenziali PostgreSQL separate di bootstrap, migrator, runtime, backup e restore;
- `docker-compose.bootstrap-secret.yml` solo durante il bootstrap iniziale;
- le cinque variabili `GESTIONALE_DB_*_PASSWORD_SECRET_FILE` come file sorgente locali delle password database;
- `GESTIONALE_BOOTSTRAP_SUPER_ADMIN_PASSWORD_SECRET_FILE` come file sorgente temporaneo del bootstrap.

I file sorgente devono trovarsi fuori dal controllo versione, preferibilmente fuori dal repository, e non essere inclusi in backup applicativi.

## Avvio con file secret

Preparare una directory locale non versionata:

```bash
mkdir -p secrets
chmod 0700 secrets
printf '%s' 'password-bootstrap-generata' > secrets/database-bootstrap-password
printf '%s' 'password-migrator-generata' > secrets/database-migrator-password
printf '%s' 'password-runtime-generata' > secrets/database-runtime-password
printf '%s' 'password-backup-generata' > secrets/database-backup-password
printf '%s' 'password-restore-generata' > secrets/database-restore-password
chmod 0444 secrets/database-*-password
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

## Provisioning dei ruoli database

Su un database esistente, predisporre owner, migrator, runtime, backup e restore con una sessione amministrativa e conferma esplicita:

```bash
CONFIRM_DATABASE_ROLE_PROVISIONING=yes \
ENV_FILE=/etc/gestionale/database-roles.env \
scripts/db/provision-database-roles.sh
```

Se gli oggetti sono posseduti da una precedente utenza applicativa, configurare `GESTIONALE_DB_LEGACY_OWNER_USERNAME` dopo avere verificato il ruolo corretto. Lo script riassegna l'ownership senza cancellare dati e applica i grant minimi.

## Rotazione password database

1. generare cinque nuove password indipendenti nel secret manager;
2. fermare temporaneamente backend, backup e restore drill oppure pianificare un rolling restart controllato;
3. predisporre un file ambiente protetto con le nuove password o i relativi percorsi `_FILE`;
4. eseguire la rotazione con conferma esplicita:

```bash
CONFIRM_DATABASE_ROLE_ROTATION=yes \
ENV_FILE=/etc/gestionale/database-roles-next.env \
scripts/db/rotate-database-role-passwords.sh
```

5. aggiornare i cinque secret montati nei workload;
6. riavviare PostgreSQL solo se richiesto dall'infrastruttura, poi backend e job operativi;
7. attendere readiness `UP` e verificare Flyway, login, lettura catalogo, una transazione controllata, backup e restore drill;
8. verificare che le vecchie password siano rifiutate e chiudere le connessioni residue nella finestra approvata;
9. registrare versione, data, operatore ed esito senza conservare i valori.

La prova isolata `scripts/db/verify-database-least-privilege.sh` ruota tutte le credenziali e verifica che quelle precedenti non consentano nuove connessioni. La rotazione in-place puo interrompere nuove connessioni durante il riciclo del pool e va sempre gestita come change operativo.

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
