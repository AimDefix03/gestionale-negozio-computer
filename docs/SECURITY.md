# Security

## Stato attuale

Il backend web usa Spring Security con filtro custom su header `X-Session-Token`. Le password sono hashate con BCrypt tramite `PasswordEncoder`.

Sono presenti:

- ruoli applicativi;
- permessi granulari;
- permessi ordine separati per creazione, conferma, evasione, annullamento, incasso, rimborso, richiesta e gestione resi;
- permessi dedicati per lettura e gestione anagrafiche clienti/fornitori;
- lockout dopo tentativi login falliti persistito su database;
- rate limiting del login per indirizzo IP sul reverse proxy Nginx, configurabile per frequenza e burst;
- risposta infrastrutturale `429 RATE_LIMIT_EXCEEDED` con `Retry-After`, `no-store`, `nosniff` e request ID;
- log Nginx con stato del limitatore e correlation ID, verificati nello smoke test prod-like;
- Content Security Policy Nginx in enforcement senza `unsafe-inline` o `unsafe-eval`, limitata a script, stili, connessioni e risorse same-origin;
- protezione da framing e plugin tramite `frame-ancestors 'none'`, `X-Frame-Options: DENY` e `object-src 'none'`;
- header browser uniformi per isolamento origine, referrer, MIME sniffing e funzionalita sensibili, applicati anche alle risposte API proxy;
- shell HTML non memorizzabile e asset Vite versionati con cache immutabile annuale;
- verifica automatica di CSP, header e cache tramite script HTTP e smoke test Chromium senza violazioni runtime;
- porta host del backend prod-like vincolata a loopback per impedire il bypass remoto del reverse proxy;
- Actuator separato sulla porta management interna in produzione, con esposizione limitata a `health` e `prometheus`, dettagli health nascosti e probe liveness/readiness distinte;
- endpoint Actuator generici o sensibili bloccati dalla security chain e verifica automatica dell'assenza di esposizione sulla porta API;
- metriche Prometheus accessibili solo dal management plane interno e prive di tag con username, token o request ID;
- log backend JSON con correlation ID e metadati HTTP limitati, senza query string, body, password o token;
- rotte inesistenti tradotte in `404 RESOURCE_NOT_FOUND`, senza stack trace o falsa risposta `500` al client;
- sessione con scadenza assoluta a 45 minuti e timeout di inattivita a 30 minuti, entrambi configurabili;
- sessioni persistite su database con token salvati solo come hash SHA-256;
- rinnovo autenticato con rotazione atomica del token e revoca immediata del token precedente;
- aggiornamento controllato dell'ultima attivita senza una scrittura database a ogni richiesta;
- risposte login e rinnovo protette da `Cache-Control: no-store` e `Pragma: no-cache`;
- scadenze sessione e lockout login calcolati tramite clock applicativo UTC centralizzato;
- logout con revoca della sessione;
- cleanup programmato di sessioni obsolete e lock login scaduti;
- bootstrap controllato del primo super admin solo con configurazione esplicita;
- blocco delle credenziali locali di default nel bootstrap produzione;
- blocco registrazione pubblica di admin e super admin;
- creazione admin consentita solo al super admin;
- configurazione aziendale e numerazioni modificabili solo dal super admin tramite permesso `MANAGE_COMPANY_SETTINGS`;
- report vendite/magazzino protetti dal permesso `VIEW_REPORTS`, assegnato ai ruoli operativi e non ai clienti;
- export report auditati nella categoria `REPORT`, senza contenuti sensibili nel log;
- CSV protetti dalla Formula Injection neutralizzando celle che iniziano con caratteri interpretabili come formula;
- download con `Cache-Control: no-store`, `Content-Disposition: attachment` e `X-Content-Type-Options: nosniff`;
- aggiornamento configurazione protetto da versione ottimistica e registrato in audit;
- blocco autocancellazione account corrente;
- richiesta di ri-autenticazione per creare o cancellare account;
- audit per operazioni sensibili con `requestId`, origine richiesta e tipo entita;
- endpoint di monitoraggio protetto da permesso `VIEW_AUDIT`;
- raccolta in memoria degli errori API interni recenti con `requestId`;
- timestamp degli errori API e degli eventi audit generati dal clock applicativo UTC;
- correlation ID `X-Request-Id` propagato tra frontend, API, log backend e audit;
- idempotenza sulle operazioni critiche di ordini, documenti e movimenti magazzino tramite `Idempotency-Key`;
- template `.env.example` senza segreti reali e regole Git per non tracciare `.env` locali;
- resolver fail-closed per valori protetti o file secret, con rifiuto di configurazioni mancanti, ambigue, vuote o multilinea;
- password dello stack E2E montate in sola lettura e assenti dai valori della configurazione Docker;
- immagini PostgreSQL, backend e frontend con utente non-root esplicito e UID runtime verificato;
- filesystem root dei container read-only, `no-new-privileges` e tutte le capability Linux eliminate;
- scrittura confinata ai tmpfs operativi e al solo volume dati PostgreSQL, senza mount scrivibili per backend e frontend;
- workflow di sicurezza con Gitleaks sulla storia Git, dependency review, audit npm, inventory Maven e CodeQL;
- Dependabot configurato per Maven, npm e GitHub Actions;
- test automatici di autorizzazione sui moduli prodotti, anagrafiche, magazzino, ordini, documenti, account e audit;
- test dedicati sulle transizioni ordine consentite o vietate per ruolo;
- profilo `prod` separato con seed super admin disattivato.

## Rischi attuali

- H2 console disabilitata nella configurazione web corrente.
- Lo stack Compose supporta file secret e una procedura di rotazione; l'integrazione con il secret manager remoto scelto resta responsabilita del deployment reale.
- Token gestito dal frontend esclusivamente in memoria JS e inviato tramite header custom, mai in `localStorage` o `sessionStorage`.
- CSRF disabilitato intenzionalmente perche il deployment corrente non usa cookie di autenticazione ambientali; la decisione e documentata in `docs/DECISIONS/0004-browser-session-token-and-csrf.md`.
- La CSP riduce la superficie XSS ma non neutralizza codice same-origin compromesso, dipendenze malevole o vulnerabilita DOM future; ogni estensione della policy deve essere revisionata.
- HSTS non e abilitato nello stack HTTP locale: deve essere introdotto solo sul dominio reale dopo avere verificato terminazione TLS e assenza di traffico HTTP necessario.
- Il rate limiting Nginx e locale alla singola istanza e non condivide lo stato tra repliche.
- Dietro load balancer o reverse proxy aggiuntivi, l'IP reale deve essere accettato esclusivamente da proxy fidati; la configurazione prod-like non interpreta arbitrariamente `X-Forwarded-For`.
- Utenti dietro lo stesso NAT condividono il limite per IP e possono richiedere un tuning delle soglie.
- Il processo applicativo deve ricevere la credenziale risolta in memoria; file, audit, cifratura e accesso al secret manager devono essere protetti dall'infrastruttura.
- Il hardening Compose limita l'impatto di una compromissione ma non sostituisce patching delle immagini, isolamento del runtime, network policy e policy admission dell'orchestratore.
- La porta management resta raggiungibile dalla rete interna dei container; in un orchestratore reale deve essere limitata ai sistemi di probe e monitoraggio tramite network policy.
- Il backend ha inventory Maven e dependency review su PR, ma non ancora OWASP Dependency-Check o SCA professionale dedicata.
- I payload operativi di magazzino, ordini e documenti non ricevono piu `actor` e `role`: il backend li deriva dalla sessione autenticata.
- I documenti conservano uno snapshot dell'azienda emittente e dell'aliquota IVA; i dati storici non dipendono dalle modifiche successive alla configurazione.
- Prefissi e lunghezza dei progressivi non possono essere modificati dopo il primo documento dell'esercizio corrente.
- Gli export sono sincroni e materializzati in memoria, ma il backend rifiuta dataset superiori a 10.000 righe e periodi vendite oltre cinque anni.

## Regole operative

- Non fidarsi mai del ruolo ricevuto dal client.
- Le operazioni applicative devono usare sempre attore e ruolo della sessione backend.
- Non loggare password, token o segreti.
- Non persistere il token browser in storage accessibili tra sessioni.
- Una futura migrazione a cookie HttpOnly deve includere nello stesso rilascio TLS, attributi cookie sicuri e protezione CSRF.
- Ogni azione distruttiva deve essere autorizzata e auditata.
- Gli endpoint amministrativi devono avere test di autorizzazione.
- La registrazione pubblica non deve creare ruoli privilegiati.
- Le configurazioni demo devono rimanere confinate a `dev`.
- Il profilo `prod` non deve avviare seed demo o credenziali predefinite.
- In produzione, un database vuoto deve partire solo con bootstrap super admin esplicito e password forte.
- Il backend non deve essere pubblicato direttamente su interfacce esterne: il login deve attraversare il reverse proxy protetto.
- Le soglie login devono essere calibrate per traffico, NAT e numero di repliche; non devono essere disabilitate per risolvere falsi positivi.
- Ogni errore segnalato dal frontend deve riportare un codice richiesta consultabile nei log backend.
- Ogni nuova variabile sensibile deve essere documentata in `docs/CONFIGURATION.md` e non deve comparire con valori reali negli esempi.
- Ogni secret deve usare un solo canale tra valore protetto e file `_FILE`; la rotazione deve seguire `docs/SECRET_ROTATION.md`.
- Ogni modifica a Dockerfile o Compose deve mantenere verde `scripts/security/verify-container-hardening.sh`; non aggiungere mount scrivibili o capability senza motivazione e revisione esplicite.
- La configurazione aziendale iniziale deve restare priva di dati identificativi inventati e deve essere completata dal super admin prima dell'uso documentale.

## Prossimi controlli consigliati

1. Integrazione con il secret manager del provider e identita workload a privilegi minimi.
2. Rate limiting distribuito o gateway condiviso quando vengono introdotte piu repliche Nginx.
3. Policy licenze e SCA backend con database CVE dedicato.
4. HSTS sul dominio reale dopo attivazione e verifica TLS.
5. Alerting automatico su rate limit, lockout ripetuti, errori API e operazioni amministrative critiche.
