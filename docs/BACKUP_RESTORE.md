# Backup e restore PostgreSQL

## Obiettivo

Il gestionale usa backup PostgreSQL in formato custom, verificabili con `pg_restore`. La procedura protegge il ciclo completo:

- creazione atomica senza sovrascrittura;
- checksum SHA-256 obbligatorio;
- lock contro esecuzioni sovrapposte;
- retention per eta e numero di copie;
- controllo della freschezza;
- restore distruttivo solo dopo la validazione dell'archivio;
- restore drill isolato e ricorrente.

Questa procedura non sostituisce storage off-site, cifratura, immutabilita, controllo accessi o una strategia di disaster recovery dell'infrastruttura.

## Obiettivi operativi

- RPO operativo: 24 ore con backup giornaliero.
- Soglia di freschezza: 26 ore, configurabile con `BACKUP_MAX_AGE_HOURS`.
- Soglia del restore drill: 900 secondi, configurabile con `RESTORE_DRILL_MAX_SECONDS`.

La soglia del drill misura il ripristino nell'ambiente in cui viene eseguito. Non costituisce un RTO contrattuale e deve essere ricalibrata con volumi e infrastruttura reali.

## Configurazione

Creare `/etc/gestionale/backup.env` o un file equivalente non tracciato:

```dotenv
POSTGRES_DB=gestionale
GESTIONALE_DB_USERNAME=gestionale_app
GESTIONALE_DB_PASSWORD_SECRET_FILE=/etc/gestionale/secrets/database-password
BACKUP_DIR=/var/backups/gestionale
BACKUP_RETENTION_DAYS=14
BACKUP_RETENTION_COUNT=30
BACKUP_MAX_AGE_HOURS=26
RESTORE_DRILL_MAX_SECONDS=900
```

La password deve arrivare da un solo canale tra valore diretto, file oppure file secret. In produzione e raccomandato il file secret.

## Creazione manuale

```bash
ENV_FILE=/etc/gestionale/backup.env scripts/db/backup.sh
```

Ogni esecuzione produce una coppia non sovrascrivibile:

```text
gestionale_gestionale_20260724T021500Z.dump
gestionale_gestionale_20260724T021500Z.dump.sha256
```

L'archivio temporaneo viene validato con `pg_restore --list` e reso visibile solo al completamento. Il backup esce con codice `75` se un'altra esecuzione detiene il lock.

## Verifica di un archivio

```bash
scripts/db/verify-backup.sh /var/backups/gestionale/nome-backup.dump
```

Il checksum e obbligatorio. `ALLOW_UNVERIFIED_BACKUP=yes` esiste soltanto per un recupero legacy eccezionale, autorizzato e documentato.

## Controllo freschezza

```bash
ENV_FILE=/etc/gestionale/backup.env scripts/db/check-backup-freshness.sh
```

Lo script fallisce se non trova backup o se l'ultimo archivio supera `BACKUP_MAX_AGE_HOURS`.

## Restore manuale

Il restore ricrea il database configurato e quindi richiede conferma esplicita:

```bash
CONFIRM_RESTORE=yes \
ENV_FILE=/etc/gestionale/backup.env \
scripts/db/restore.sh /var/backups/gestionale/nome-backup.dump
```

Prima di eliminare il database lo script verifica checksum e struttura dell'archivio. Per il restore operativo:

1. dichiarare una finestra di manutenzione;
2. verificare identita di database, host e archivio;
3. copiare archivio e checksum su storage di lavoro protetto;
4. eseguire la verifica;
5. fermare i servizi applicativi;
6. eseguire il restore;
7. controllare migrazioni, dati critici e health check;
8. riaprire il traffico;
9. registrare durata, esito e operatore.

## Test automatici

Lifecycle senza database:

```bash
scripts/db/test-backup-lifecycle.sh
scripts/db/verify-backup-schedule.sh
```

Backup e restore sintetico su PostgreSQL isolato:

```bash
scripts/db/verify-backup-restore.sh
```

Restore drill dell'ultimo backup reale su uno stack PostgreSQL isolato:

```bash
ENV_FILE=/etc/gestionale/backup.env scripts/db/restore-drill-latest.sh
```

Il drill reale non modifica il database operativo. Verifica le tabelle critiche e l'allineamento alla versione Flyway piu recente disponibile nel codice.

## Scheduling systemd

Le unita disponibili sono:

- `gestionale-backup.timer`: ogni giorno alle 02:15, con ritardo casuale massimo di 15 minuti;
- `gestionale-restore-drill.timer`: ogni domenica alle 03:30, con ritardo casuale massimo di 30 minuti.

Entrambe usano `Persistent=true`, quindi systemd recupera un'esecuzione saltata durante uno spegnimento.

Installazione indicativa:

```bash
sudo install -m 0644 deploy/systemd/gestionale-backup.service /etc/systemd/system/
sudo install -m 0644 deploy/systemd/gestionale-backup.timer /etc/systemd/system/
sudo install -m 0644 deploy/systemd/gestionale-restore-drill.service /etc/systemd/system/
sudo install -m 0644 deploy/systemd/gestionale-restore-drill.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now gestionale-backup.timer
sudo systemctl enable --now gestionale-restore-drill.timer
```

Verifica:

```bash
systemctl list-timers 'gestionale-*'
journalctl -u gestionale-backup.service
journalctl -u gestionale-restore-drill.service
```

L'utente `gestionale-backup` deve poter leggere il file di configurazione e scrivere solo nella directory backup. Le unita di esempio usano il gruppo `docker`: l'accesso al socket Docker equivale di fatto a privilegi elevati sull'host. In Kubernetes o cloud usare CronJob o scheduled task native, identita dedicate e connessione diretta a PostgreSQL senza montare il socket Docker.

## Retention e storage

I valori predefiniti conservano 14 giorni e al massimo 30 copie. Le due regole sono applicate insieme e operano soltanto sui file con prefisso controllato.

Un backup sullo stesso host non protegge dalla perdita del nodo. In un ambiente reale:

- replicare gli archivi su storage off-site;
- cifrare in transito e a riposo;
- abilitare versioning o immutabilita;
- separare le credenziali di scrittura e restore;
- monitorare backup mancanti, checksum e capacita storage;
- provare periodicamente il ripristino da una copia off-site.

## Incidenti comuni

Lock presente: controllare `.gestionale_<database>_backup.lock/owner` e verificare che il processo non sia attivo prima di rimuovere manualmente il lock.

Checksum non valido: non procedere al restore; recuperare un'altra copia e aprire un incidente.

Backup non recente: verificare timer, spazio disco, credenziali, connettivita PostgreSQL e log systemd.

Drill oltre soglia: misurare dimensione archivio, throughput storage e tempi PostgreSQL; aggiornare il piano di capacita, non soltanto la soglia.

## Protezione dati

I backup possono contenere account, clienti, ordini e dati operativi. Non devono essere inseriti in Git, allegati a ticket non protetti o condivisi come file dimostrativi.
