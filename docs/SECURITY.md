# Security

## Stato attuale

Il backend web usa Spring Boot 4.1 e Spring Security 7.1 con filtro custom su header `X-Session-Token`. Le password sono hashate con BCrypt tramite `PasswordEncoder`.

Sono presenti:

- ruoli applicativi;
- permessi granulari;
- permessi ordine separati per creazione, conferma, evasione, annullamento, incasso, rimborso, richiesta e gestione resi;
- permessi dedicati per lettura e gestione anagrafiche clienti/fornitori;
- lockout dopo tentativi login falliti persistito su database;
- rate limiting indipendente di login e registrazione pubblica per indirizzo IP sul reverse proxy Nginx, configurabile per frequenza e burst;
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
- subject di sessione basato sull'ID stabile dell'account, con username conservato soltanto come snapshot non autorizzativo;
- versione credenziali persistita su account e sessione, con revoca globale dopo cambio password o ruolo;
- foreign key delle sessioni verso l'account con cancellazione a cascata come ultima protezione referenziale; il workflow operativo usa disabilitazione e vieta il riuso dello username;
- lock pessimista sulla riga di sessione per rendere logout e rotazione prevalenti rispetto a touch concorrenti;
- rinnovo autenticato con rotazione atomica del token e revoca immediata del token precedente;
- aggiornamento controllato dell'ultima attivita senza una scrittura database a ogni richiesta;
- risposte login e rinnovo protette da `Cache-Control: no-store` e `Pragma: no-cache`;
- scadenze sessione e lockout login calcolati tramite clock applicativo UTC centralizzato;
- logout con revoca della sessione;
- cleanup programmato di sessioni obsolete e lock login scaduti;
- bootstrap controllato del primo super admin solo con configurazione esplicita;
- blocco delle credenziali locali note nel bootstrap di ogni profilo, senza password in chiaro nel codice produttivo;
- registrazione pubblica priva di selezione ruolo e forzata lato backend a `CUSTOMER`;
- rifiuto con `422 VALIDATION_FAILED` di qualsiasi ruolo inviato alla registrazione pubblica;
- audit delle registrazioni pubbliche con attore non privilegiato `SELF_SERVICE`;
- provenienza account persistita e classificata tra self-service, provisioning amministrativo, bootstrap e origine storica non dimostrabile;
- quarantena fail-closed degli account operativi storici self-service o non verificati, con audit critico preservato prima della revoca delle sessioni;
- report di revisione riservato al super admin con evidenze di sessione e conteggi audit per magazzino, ordini, pagamenti, resi e documenti;
- riabilitazione manuale degli account operativi con ri-autenticazione super admin, senza riattivare token gia revocati;
- creazione di dipendenti e amministratori confinata al flusso amministrativo autenticato e ri-autenticato;
- creazione admin consentita solo al super admin;
- configurazione aziendale e numerazioni modificabili solo dal super admin tramite permesso `MANAGE_COMPANY_SETTINGS`;
- report vendite/magazzino protetti dal permesso `VIEW_REPORTS`, assegnato ai ruoli operativi e non ai clienti;
- export report auditati nella categoria `REPORT`, senza contenuti sensibili nel log;
- CSV protetti dalla Formula Injection neutralizzando celle che iniziano con caratteri interpretabili come formula;
- download con `Cache-Control: no-store`, `Content-Disposition: attachment` e `X-Content-Type-Options: nosniff`;
- aggiornamento configurazione protetto da versione ottimistica e registrato in audit;
- blocco dell'autodisabilitazione e delle modifiche amministrative sull'account corrente;
- richiesta di ri-autenticazione per creare o cancellare account;
- audit per operazioni sensibili con `requestId`, origine richiesta e tipo entita;
- endpoint di monitoraggio protetto da permesso `VIEW_AUDIT`;
- raccolta in memoria degli errori API interni recenti con `requestId`;
- timestamp degli errori API e degli eventi audit generati dal clock applicativo UTC;
- correlation ID `X-Request-Id` propagato tra frontend, API, log backend e audit;
- idempotenza sulle operazioni critiche di ordini, documenti e movimenti magazzino tramite `Idempotency-Key`;
- saldo iniziale, rettifiche e riconciliazione inventario protetti dal permesso backend `MANAGE_INVENTORY`;
- nessun endpoint prodotto puo variare la giacenza fisica; attore e ruolo dei movimenti derivano dalla sessione;
- template `.env.example` senza segreti reali e regole Git per non tracciare `.env` locali;
- resolver fail-closed per valori protetti o file secret, con rifiuto di configurazioni mancanti, ambigue, vuote o multilinea;
- password dello stack E2E montate in sola lettura e assenti dai valori della configurazione Docker;
- immagini PostgreSQL, backend e frontend con utente non-root esplicito e UID runtime verificato;
- ruoli PostgreSQL separati: owner `NOLOGIN`, migrator Flyway, runtime DML, backup read-only e restore dedicato;
- runtime privo di `CREATE`, `ALTER`, `DROP`, ownership e privilegi amministrativi, verificato su PostgreSQL reale;
- porta PostgreSQL prod-like vincolata a loopback e da rimuovere nell'orchestratore di produzione;
- filesystem root dei container read-only, `no-new-privileges` e tutte le capability Linux eliminate;
- scrittura confinata ai tmpfs operativi e al solo volume dati PostgreSQL, senza mount scrivibili per backend e frontend;
- runner Docker con project name casuale, doppia label project/run ID, rifiuto collisioni e cleanup limitato agli ID di ownership verificata;
- workflow di sicurezza con Gitleaks sulla storia Git, dependency review, audit npm, OWASP Dependency-Check backend e CodeQL;
- gate SCA backend fail-closed con report HTML/JSON e blocco da CVSS 7;
- Dependabot configurato per Maven, npm e GitHub Actions;
- test automatici di autorizzazione sui moduli prodotti, anagrafiche, magazzino, ordini, documenti, account e audit;
- endpoint e DTO cliente separati dai contratti operativi: lo stock esatto, le riserve, il valore potenziale di vendita, i KPI economici globali e i movimenti non vengono serializzati per `CUSTOMER`;
- disponibilita cliente ridotta server-side ai livelli `AVAILABLE`, `LIMITED` e `UNAVAILABLE`, senza limiti numerici o ordinamenti che permettano di inferire la giacenza;
- dashboard cliente vincolata all'ID account stabile della sessione e limitata ai propri ordini; gli endpoint operativi di prodotto e dashboard restituiscono `403` al cliente;
- test dedicati sulle transizioni ordine consentite o vietate per ruolo;
- ownership degli ordini basata su `customer_account_id` e `partner_id`, mai su username, nome cliente o display name;
- link account-anagrafica esplicito, univoco e limitato ad account `CUSTOMER`, con gestione riservata a chi possiede entrambi i permessi amministrativi;
- ordini storici non riconciliabili marcati `UNRESOLVED` e inaccessibili ai clienti;
- nessun profilo applicativo predefinito e bootstrap super admin disattivato per default in tutti gli ambienti;
- H2 confinato allo scope test e nessuna eccezione `/h2-console` nella security chain distribuita.

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
- OWASP Dependency-Check fornisce il gate CVE backend iniziale; policy licenze, SBOM firmata e SCA commerciale restano nello Step 5.1.
- I payload operativi di magazzino, ordini e documenti non ricevono piu `actor` e `role`: il backend li deriva dalla sessione autenticata.
- I documenti conservano uno snapshot dell'azienda emittente e dell'aliquota IVA; i dati storici non dipendono dalle modifiche successive alla configurazione.
- Prefissi e lunghezza dei progressivi non possono essere modificati dopo il primo documento dell'esercizio corrente.
- Gli export sono sincroni e materializzati in memoria, ma il backend rifiuta dataset superiori a 10.000 righe e periodi vendite oltre cinque anni.
- Il lifecycle account e non distruttivo: disabilitazione, cambio password, reset amministrativo, revoca esplicita e cambio ruolo invalidano le sessioni; riabilitazione e username riservato preservano storico e ownership.
- La suite PostgreSQL autorevole estesa resta pianificata nello Step 2.9; lo Step 1.3 include comunque test PostgreSQL 16 mirati sulle race tra revoca, rotazione e aggiornamento attivita.
- Il ruolo bootstrap conserva poteri amministrativi necessari all'inizializzazione; deve restare confinato al container PostgreSQL e alle procedure di emergenza. L'accesso al socket Docker o all'host database puo aggirare la separazione applicativa e richiede controlli infrastrutturali.
- Gli ordini `UNRESOLVED` richiedono riconciliazione amministrativa futura; non vengono collegati automaticamente per somiglianza testuale e rimangono esclusi dalle viste cliente.
- Un `SIGKILL` puo lasciare risorse temporanee del runner; il recupero e consentito soltanto tramite `scripts/ci/cleanup-docker-run.sh` con project name e run ID presenti nella diagnostica.

## Regole operative

- Non fidarsi mai del ruolo ricevuto dal client.
- Le operazioni applicative devono usare sempre attore e ruolo della sessione backend.
- Le autorizzazioni cliente sugli ordini devono usare esclusivamente l'ID account della sessione; i campi snapshot non sono subject di sicurezza.
- Non loggare password, token o segreti.
- Non persistere il token browser in storage accessibili tra sessioni.
- Una futura migrazione a cookie HttpOnly deve includere nello stesso rilascio TLS, attributi cookie sicuri e protezione CSRF.
- Ogni azione distruttiva deve essere autorizzata e auditata.
- Gli endpoint amministrativi devono avere test di autorizzazione.
- La registrazione pubblica deve creare esclusivamente account `CUSTOMER` e rifiutare ogni ruolo fornito dal client.
- Le configurazioni demo devono rimanere confinate a `dev`.
- Il profilo `prod` non deve avviare seed demo o credenziali predefinite.
- In produzione, un database vuoto deve partire solo con bootstrap super admin esplicito e password forte.
- Il backend non deve essere pubblicato direttamente su interfacce esterne: il login deve attraversare il reverse proxy protetto.
- Le soglie di login e registrazione devono essere calibrate separatamente per traffico, NAT e numero di repliche; non devono essere disabilitate per risolvere falsi positivi.
- Ogni errore segnalato dal frontend deve riportare un codice richiesta consultabile nei log backend.
- Ogni nuova variabile sensibile deve essere documentata in `docs/CONFIGURATION.md` e non deve comparire con valori reali negli esempi.
- Ogni secret deve usare un solo canale tra valore protetto e file `_FILE`; la rotazione deve seguire `docs/SECRET_ROTATION.md`.
- Ogni modifica a Dockerfile o Compose deve mantenere verde `scripts/security/verify-container-hardening.sh`; non aggiungere mount scrivibili o capability senza motivazione e revisione esplicite.
- I runner non devono usare `docker compose down -v`; ogni cleanup deve verificare entrambe le label di ownership prima di rimuovere risorse inventariate.
- La configurazione aziendale iniziale deve restare priva di dati identificativi inventati e deve essere completata dal super admin prima dell'uso documentale.

## Prossimi controlli consigliati

1. Integrazione con il secret manager del provider e identita workload a privilegi minimi.
2. Rate limiting distribuito o gateway condiviso quando vengono introdotte piu repliche Nginx.
3. Policy licenze, SBOM firmata e SCA professionale oltre il gate CVE OWASP iniziale.
4. HSTS sul dominio reale dopo attivazione e verifica TLS.
5. Alerting automatico su rate limit, lockout ripetuti, errori API e operazioni amministrative critiche.
