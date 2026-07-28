# Release Checklist

## Scopo

Questa checklist definisce i controlli minimi prima di pubblicare una versione del gestionale web in un ambiente condiviso o dimostrativo serio.

Non certifica idoneita a produzione reale, conformita fiscale, legale o GDPR.

## Codice

- Repository senza modifiche inattese.
- `web/` e `docs/` tracciati correttamente nel commit.
- Nessun file `.env` reale tracciato.
- Nessun file `.dat`, `target`, `dist`, `node_modules` o artefatto locale tracciato.
- Nessun endpoint nuovo senza autorizzazione valutata.
- Workflow CI completata con successo su branch o pull request.
- Workflow sicurezza completata con successo quando applicabile.
- Stack Docker prod-like avviato almeno una volta con PostgreSQL reale.
- Runner `scripts/ci/run-prod-like-verification.sh` completato senza risorse Docker residue.
- Tutte le migrazioni Flyway del repository risultano applicate sul database prod-like.
- Artefatto diagnostico prod-like disponibile per l'ultima esecuzione CI.

## Backend

- `mvn test` completato con successo in `web/backend`.
- CI backend completata con `mvn -B verify`.
- Migrazioni Flyway validate.
- Profilo `prod` con `ddl-auto: validate`.
- H2 console disabilitata.
- Bootstrap super admin disabilitato dopo il primo avvio.
- Password locali di default non usate in ambienti condivisi.
- Errori API nel formato standard.
- Endpoint critici coperti da audit, permessi e idempotenza dove previsto.
- Endpoint report protetti da `VIEW_REPORTS`, limite righe e audit export.
- Configurazione aziendale completata con dati reali e verificata dal super admin.
- Prefissi documentali, padding e aliquota IVA validati prima del primo documento dell'esercizio.

## Frontend

- `npm ci` completato con successo in `web/frontend`.
- `npm test` completato con successo in `web/frontend`.
- `npm run typecheck:e2e` completato con successo in `web/frontend`.
- `npm run build` completato con successo in `web/frontend`.
- CI frontend completata con `npm ci`, `npm test` e `npm run build`.
- Nessun errore TypeScript.
- Nessun riferimento visibile a credenziali demo in UI.
- Flussi principali verificati manualmente: login, catalogo, ordini, magazzino, documenti, account.
- Smoke test Playwright completati sullo stack prod-like per autenticazione, catalogo e ciclo ordine.
- Report vendite e magazzino verificati su notebook e tablet landscape senza overflow della pagina.
- CSP verificata in enforcement senza violazioni browser, `unsafe-inline` o `unsafe-eval`.
- Shell HTML verificata `no-store` e asset versionati verificati con cache `immutable`.

## Database

- PostgreSQL raggiungibile.
- Readiness backend interna `/actuator/health/readiness` in stato `UP`.
- Liveness backend interna `/actuator/health/liveness` in stato `UP`.
- Nessun endpoint Actuator utilizzabile dalla porta API pubblica.
- Porta management non pubblicata dall'ingress o dal Service applicativo.
- `/actuator/prometheus` raggiungibile soltanto dal sistema di monitoraggio autorizzato.
- Configurazione Prometheus validata con `scripts/observability/verify-prometheus-config.sh`.
- Log JSON, metriche e regole alert verificati con `scripts/observability/verify-runtime-observability.sh`.
- Container Prometheus verificato con `scripts/observability/verify-prometheus-hardening.sh`.
- Canale Alertmanager, escalation e reperibilita provati nell'ambiente reale.
- Collettore log centralizzato, retention e access control verificati nell'ambiente reale.
- Health check frontend `/health` raggiungibile.
- Endpoint `/api/system/status` raggiungibile da super admin e non raggiungibile da ruoli non autorizzati.
- Backup giornaliero attivo e visibile in `systemctl list-timers`.
- Ultimo backup entro la soglia verificato con `scripts/db/check-backup-freshness.sh`.
- Archivio e checksum verificati con `scripts/db/verify-backup.sh`.
- Restore sintetico verificato con `scripts/db/verify-backup-restore.sh`.
- Ultimo restore drill reale completato con versione Flyway allineata e durata registrata.
- Retention, storage off-site, cifratura e alert configurati per l'ambiente.
- Utente database applicativo non superuser.
- Password database non condivisa nel repository.
- Migrazioni applicate nell'ordine corretto.
- Migrazione `V17` applicata e vincoli sui progressivi documentali verificati.

## Sicurezza

- Variabili sensibili gestite fuori dal repository.
- Gitleaks completato sulla storia Git senza rilevazioni aperte.
- Resolver file secret verificato con `scripts/security/test-resolve-file-secrets.sh`.
- Container verificati con password dirette vuote e mount secret read-only.
- `scripts/security/verify-container-hardening.sh` completato con processi non-root, filesystem read-only, `no-new-privileges` e capability eliminate.
- Backend e frontend senza mount persistenti scrivibili; PostgreSQL con il solo volume dati scrivibile.
- Procedura di rotazione e rollback provata nell'ambiente di staging.
- Security workflow completata senza vulnerabilita bloccanti.
- Dependency review GitHub superata sulle pull request con modifiche alle dipendenze.
- Nessuna credenziale demo o segreto reale introdotto nel frontend o nei template.
- Super admin iniziale creato con password forte.
- Sessioni e tentativi login persistiti.
- Logout verificato.
- Durata assoluta, timeout inattivita e intervallo touch configurati con valori positivi e coerenti.
- Rinnovo sessione verificato con rotazione token, revoca immediata e rifiuto del replay del token precedente.
- Login e rinnovo verificati con `Cache-Control: no-store` e `Pragma: no-cache`.
- Rate limiting login verificato con `scripts/security/verify-login-rate-limit.sh` e log Nginx `limit_req=REJECTED`.
- Risposta `429` verificata con `Retry-After`, request ID, `no-store` e `nosniff`.
- `scripts/security/verify-browser-security.sh` completato con successo sul dominio di rilascio.
- Framing, plugin, MIME sniffing e funzionalita browser non necessarie bloccati dagli header Nginx.
- Porta backend non pubblicata su interfacce esterne e accesso API instradato tramite reverse proxy.
- IP reale configurato solo da proxy fidati e soglie calibrate se l'ambiente usa NAT o load balancer.
- Accesso ad account e audit riservato ai ruoli corretti.
- Accesso alla configurazione aziendale negato a tutti i ruoli diversi dal super admin.
- Accesso ai report negato ai clienti e download CSV protetto da Formula Injection.

## Documentazione

- `docs/CONFIGURATION.md` aggiornato se cambiano variabili.
- `docs/CI_CD.md` aggiornato se cambiano controlli automatici, versioni runtime o workflow.
- `docs/DOCKER.md` aggiornato se cambiano Dockerfile, compose, porte o procedura bootstrap.
- `docs/BACKUP_RESTORE.md` aggiornato se cambiano procedure, script o policy dati.
- `docs/OBSERVABILITY.md` aggiornato se cambiano log, metriche, alert o runbook.
- `docs/SECURITY_SCANS.md` aggiornato se cambiano scanner, soglie o policy.
- `docs/SECURITY.md` aggiornato se cambiano controlli o rischi.
- `docs/SECRET_ROTATION.md` aggiornato se cambiano secret, mount o procedure operative.
- `docs/ROADMAP.md` aggiornato con stato e prossimo step.
- `docs/CHANGELOG.md` aggiornato con le modifiche rilevanti.
- `docs/USER_MANUAL.md` coerente con ruoli, permessi e flussi correnti.
- `README.md` e `web/README.md` coerenti con l'avvio reale.

## Prove manuali minime

1. Avvio backend con database pulito e bootstrap abilitato.
2. Login super admin.
3. Riavvio backend con bootstrap disabilitato.
4. Creazione prodotto.
5. Movimento magazzino.
6. Creazione ordine.
7. Conferma ed evasione ordine.
8. Incasso parziale e saldo ordine con verifica ledger.
9. Richiesta, approvazione e ricezione reso con verifica giacenza.
10. Rimborso reso con verifica saldo e audit.
11. Generazione fattura simulata.
12. Verifica snapshot azienda/cliente/IVA e progressivo del documento simulato.
13. Creazione account non privilegiato.
14. Verifica accesso negato a modulo non autorizzato.
15. Verifica schermata Monitoraggio con database, sessioni, audit ed errori recenti.
16. Verifica filtri del report vendite e coerenza tra valore ordini, incassato, rimborsato e netto.
17. Verifica snapshot magazzino e download CSV, Excel e PDF.
18. Verifica blocco temporaneo del login per IP in ambiente di test e successivo ripristino dopo la finestra configurata.

## Rischi da dichiarare se non completati

- Assenza replica off-site, cifratura o immutabilita dei backup.
- Restore drill reale non recente o oltre la soglia operativa.
- Assenza deploy automatico.
- Assenza registry immagini.
- Assenza rate limiting condiviso tra repliche o configurazione IP reale affidabile quando il deploy usa piu proxy.
- Secret manager remoto non ancora integrato o rotazione non provata sul provider scelto.
- Assenza scansione CVE backend dedicata oltre dependency review e inventory Maven.
- Assenza HSTS finche il dominio reale non e servito e validato esclusivamente tramite HTTPS.
- Assenza integrazione fiscale reale.
- Supporto di una sola aliquota IVA predefinita per documento, senza gestione multi-aliquota o esenzioni.
