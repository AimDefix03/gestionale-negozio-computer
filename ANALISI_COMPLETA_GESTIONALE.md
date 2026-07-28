# Analisi completa gestionale

Data analisi: 2026-07-08
Repository analizzato: `/Users/aim_defix/Desktop/GestionaleNegozioComputer`
Modalita: sola lettura funzionale, con esecuzione di build/test/lint/analisi non distruttive. Nessuna modifica a codice sorgente, configurazioni, dipendenze dichiarate o database.
Nota segreti: eventuali credenziali o token presenti nei file sono stati trattati come sensibili e non vengono riportati in chiaro.

Legenda verifiche:

| Stato            | Significato                                                                 |
| ---------------- | --------------------------------------------------------------------------- |
| confermato       | Evidenza diretta da codice, configurazione o comando eseguito.              |
| probabile        | Indizio tecnico forte, ma non riprodotto con test specifico o carico reale. |
| da verificare    | Serve ambiente esterno, dato reale, decisione funzionale o test aggiuntivo. |
| non verificabile | Non controllabile nell'ambiente disponibile o senza accesso esterno.        |

## 1. Executive summary

Il repository contiene due linee applicative reali:

* una applicazione desktop legacy Java Swing nella root, file-based, utile come riferimento funzionale ma non adatta a produzione;
* una migrazione web in `web/`, con backend Spring Boot, frontend React e PostgreSQL gestito tramite Flyway.

La linea web e la direzione architetturale migliore del progetto. Ha gia diversi elementi sani: DTO pubblici, validazioni Bean Validation, servizi transazionali, security server-side, sessioni con token hashato lato database, audit, migrazioni versionate, Dockerfile, workflow CI e test backend significativi. Non e pero pronta per produzione o vendita: mancano ancora vincoli e regole critiche, flussi commerciali completi, test end-to-end, hardening operativo, gestione tenant/azienda, configurazione fiscale reale, report/export, backup operativo e documentazione utente.

Verdetto unico: **MVP incompleto**.

Rischi principali emersi:

| Priorita | Sintesi                                                                                                                                                                   |
| -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| P1       | I prodotti possono essere rinominati o cancellati anche se usati da ordini confermati o stock riservato, con rischio di ordini bloccati in evasione.                      |
| P1       | La policy password non e applicata nella creazione/registrazione account: e valutata da endpoint dedicato ma non bloccante.                                               |
| P1       | La generazione di fatture/note credito simulate usa un controllo applicativo senza vincolo univoco database per ordine/tipo, quindi e esposta a duplicati in concorrenza. |
| P1       | Il comando backend richiesto da `AGENTS.md` (`mvn test`) fallisce nell'ambiente locale Java 23 senza configurazione aggiuntiva del Java agent Mockito/ByteBuddy o JDK 17. |
| P2       | Alcune liste e dashboard caricano dataset completi senza endpoint aggregati o paginazione coerente.                                                                       |
| P2       | Frontend e API client sono monolitici e non hanno test automatici.                                                                                                        |
| P2       | Deployment, sicurezza perimetrale, osservabilita, backup, GDPR e commercializzazione sono ancora incompleti.                                                              |

Punteggi sintetici:

| Area                 | Punteggio |
| -------------------- | --------: |
| Architettura         |      7/10 |
| Codice               |      7/10 |
| Database             |      6/10 |
| Sicurezza            |      6/10 |
| Funzionalita         |      5/10 |
| Frontend             |      5/10 |
| Test                 |      7/10 |
| Prestazioni          |      5/10 |
| Deployment           |      5/10 |
| Documentazione       |      6/10 |
| Manutenibilita       |      6/10 |
| Personalizzabilita   |      3/10 |
| Maturita commerciale |      3/10 |

## 2. Descrizione reale del gestionale

Descrizione confermata:

* gestionale per negozio informatico, con catalogo prodotti, carrello/ordini, magazzino, utenti/ruoli, audit, partner commerciali e documenti fiscali simulati;
* root Swing: applicazione desktop con persistenza su file serializzati;
* `web/backend`: API REST Spring Boot con PostgreSQL/H2 test, Flyway, JPA, security custom basata su token di sessione;
* `web/frontend`: SPA React/Vite con dashboard, catalogo, carrello, ordini, magazzino, partner, documenti, account, audit e stato sistema.

Descrizione da verificare:

* utilizzo reale da parte di utenti finali;
* dimensioni dataset previste;
* regole fiscali effettive;
* requisiti multi-azienda, multi-sede e multi-magazzino;
* target commerciale: installazione singola, SaaS, white-label o verticalizzazione per singolo cliente.

Non verificabile in questa analisi:

* stato Git storico, perche `git status` ha restituito `fatal: not a git repository`;
* vulnerabilita aggiornate da registry npm, perche l'ambiente non ha accesso di rete a `registry.npmjs.org`;
* comportamento Docker end-to-end, perche non e stato avviato lo stack.

## 3. Stack tecnologico

| Area             | Evidenza                                                                                                                 | Stato      |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------ | ---------- |
| Java legacy root | `pom.xml`, `src/main/java`, `src/test/java`; Java release 17, FlatLaf 3.5.4, JUnit 5.11.4, Maven Shade                   | confermato |
| Backend web      | `web/backend/pom.xml`; Spring Boot 3.4.5, Java 17, JPA, Validation, Web, Security, Actuator, Flyway, PostgreSQL, H2 test | confermato |
| Frontend web     | `web/frontend/package.json`; React 19.2.7, TypeScript 6.0.3, Vite 8.1.3, plugin React 6.0.3, Node >=22                   | confermato |
| Database         | PostgreSQL runtime, H2 test, Flyway migrations `V1`-`V11`                                                                | confermato |
| Build            | Maven root, Maven backend, npm/Vite frontend                                                                             | confermato |
| Docker           | `docker-compose.yml`, `docker-compose.prod-like.yml`, backend Dockerfile, frontend Dockerfile, nginx config              | confermato |
| CI/CD            | `.github/workflows/ci.yml`, `security.yml`, Dependabot                                                                   | confermato |
| Runtime locale   | Java 23.0.1, Maven 3.9.16, Node 23.10.0, npm 10.9.2                                                                      | confermato |
| Java 17 locale   | richiesto dal progetto, ma non disponibile tra le versioni locali rilevate                                               | confermato |

Dipendenze rilevanti backend:

* Hibernate 6.6.13.Final;
* Spring Security 6.4.5;
* Flyway 10.20.1;
* PostgreSQL JDBC 42.7.5;
* H2 2.3.232;
* Mockito 5.14.2 e ByteBuddy Agent 1.15.11 per i test.

Configurazioni ambiente:

| File                                              | Contenuto principale                                                                                                       | Stato      |
| ------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- | ---------- |
| `web/backend/src/main/resources/application.yml`  | profilo default `dev`, JPA validate, Flyway enabled, open-in-view false, actuator health/info                              | confermato |
| `application-dev.yml`                             | datasource PostgreSQL con fallback locali, bootstrap super-admin abilitabile da env, segreti sanitizzati in questa analisi | confermato |
| `application-prod.yml`                            | datasource da variabili ambiente, bootstrap disabilitato di default, Flyway clean disabled                                 | confermato |
| `web/backend/src/test/resources/application.yml`  | H2 in PostgreSQL mode, Flyway enabled, bootstrap test                                                                      | confermato |
| `.env.docker.example`, `web/backend/.env.example` | placeholder per deploy                                                                                                     | confermato |

Segreti:

* confermato: non sono stati trovati file `.env` reali;
* confermato: sono presenti credenziali locali/test/demo in configurazioni e documentazione, trattate come valori di sviluppo e non riportate;
* da verificare: secret scanning professionale su tutta la storia Git non possibile per assenza repository Git locale.

## 4. Mappa del repository

Struttura confermata:

```text
.
├── AGENTS.md
├── README.md
├── pom.xml
├── docker-compose.yml
├── docker-compose.prod-like.yml
├── .github/
│   ├── dependabot.yml
│   └── workflows/
├── docs/
├── scripts/
│   └── db/
├── src/
│   ├── main/java/        # Java Swing legacy
│   └── test/java/
└── web/
    ├── backend/
    │   ├── Dockerfile
    │   ├── pom.xml
    │   └── src/
    └── frontend/
        ├── Dockerfile
        ├── package.json
        ├── nginx.conf
        └── src/
```

Artefatti generati durante le verifiche:

* `target/`;
* `web/backend/target/`;
* `web/frontend/node_modules/`;
* `web/frontend/dist/`.

Questi sono output di build/install, non modifiche manuali al codice sorgente.

## 5. Architettura

Architettura confermata:

* legacy Swing: UI Swing, servizi applicativi, repository file-based;
* web backend: monolite modulare Spring Boot organizzato per dominio (`product`, `order`, `inventory`, `document`, `partner`, `user`, `audit`, `idempotency`, `security`, `system`, `common`);
* web frontend: SPA React con componenti e API client TypeScript;
* database relazionale con migrazioni Flyway versionate.

Valutazione:

* confermato: il backend web segue una separazione ragionevole controller/service/repository/entity/DTO;
* confermato: i controller non espongono direttamente le entity principali, ma response DTO/record;
* confermato: i servizi critici sono transazionali;
* confermato: la security e server-side e non solo frontend;
* probabile: l'architettura e estendibile come monolite modulare, ma i moduli non sono ancora isolati tramite boundary forti o package checks;
* confermato: il frontend ha troppo stato e orchestrazione in `App.tsx`;
* confermato: il client API e concentrato in `web/frontend/src/api/products.ts`, nonostante gestisca anche ordini, utenti, partner, documenti, audit e sistema.

Accoppiamenti rilevanti:

* `OrderService` dipende da `ProductService`, `InventoryService`, `BusinessPartnerService` e `AuditService`;
* `FiscalDocumentService` dipende da `OrderService`, `BusinessPartnerService`, `AuditService` e repository documenti;
* `InventoryService` dipende da `ProductService`;
* frontend dashboard dipende da quasi tutti i moduli tramite snapshot completo.

## 6. Moduli e stato di completamento

| Modulo                              | Stato     | Backend                                                        | Frontend                            | Database                                           | Test                  | Problemi                                                                             |
| ----------------------------------- | --------- | -------------------------------------------------------------- | ----------------------------------- | -------------------------------------------------- | --------------------- | ------------------------------------------------------------------------------------ |
| Catalogo prodotti web               | parziale  | controller, service, repository, DTO presenti                  | form, tabella, filtri, paginazione  | tabella `products`, versioning, stock riservato    | presenti backend      | identita prodotto modificabile/cancellabile con ordini aperti; vincoli DB incompleti |
| Magazzino web                       | parziale  | movimenti, stock adjustment, low-stock                         | form movimenti, lista, indicatori   | `stock_movements`, campi stock prodotto            | presenti backend      | low-stock non paginato; vincoli DB su quantita incompleti                            |
| Ordini web                          | parziale  | lifecycle draft/confirmed/fulfilled/cancelled                  | carrello e azioni ruolo             | `customer_orders`, `order_items`, sequence         | presenti backend      | pagamenti semplificati, no partial/refund, dipendenza da codice prodotto mutabile    |
| Documenti fiscali simulati          | prototipo | fattura e nota credito simulate                                | generazione e lista                 | `fiscal_documents`, `fiscal_document_lines`        | presenti backend      | non fiscale reale; rischio duplicato concorrente; lista non paginata                 |
| Partner commerciali                 | parziale  | CRUD logico con deactivate                                     | form e lista                        | `business_partners`                                | presenti backend      | validazione fiscale/email debole; assenza FK con ordini                              |
| Utenti, ruoli, sessioni             | parziale  | login/logout/register/account, token hashato, lockout username | login, session modal, account admin | `user_accounts`, `auth_sessions`, `login_attempts` | presenti backend      | password strength non bloccante; no rate limiting perimetrale                        |
| Audit                               | parziale  | registrazione eventi e ricerca                                 | vista audit e status                | `audit_events`                                     | presenti backend      | no retention/export/alerting; dati potenzialmente sensibili da governare             |
| Dashboard frontend                  | prototipo | usa endpoint generici                                          | KPI e viste aggregate lato client   | nessuna tabella dedicata                           | non presenti frontend | carica dataset completi invece di endpoint aggregati                                 |
| Sistema/status                      | parziale  | endpoint stato protetto da `VIEW_AUDIT`                        | widget status                       | query database/app                                 | presenti backend      | non sostituisce metriche/observability                                               |
| Docker e deploy                     | parziale  | backend container                                              | frontend nginx                      | postgres compose                                   | CI configurata        | mancano TLS, secrets manager, backup schedulato, observability                       |
| Legacy Swing                        | prototipo | servizi Java locali                                            | Swing                               | file serializzati                                  | 41 test root passano  | password in chiaro, file serialization, no transazioni                               |
| Report/export PDF/CSV/Excel         | mancante  | non emerso nel web                                             | non emerso nel web                  | non emerso                                         | non emerso            | necessario per produzione/vendita                                                    |
| Multi-azienda/tenant/sedi/magazzini | mancante  | non emerso                                                     | non emerso                          | non emerso                                         | non emerso            | necessario per vendita estesa/SaaS                                                   |

## 7. Flussi principali

### Catalogo prodotti

Stato: parziale.

Flusso confermato:

1. Frontend: `ProductForm`, `ProductTable`, chiamate in `web/frontend/src/api/products.ts`.
2. API: `ProductController` su `/api/products`.
3. Validazione: `ProductRequest` e validazione dominio in entity/service.
4. Service: `ProductService`.
5. Repository: `ProductRepository`.
6. Entity/DB: `Product`, tabella `products`.
7. Error handling: `GlobalExceptionHandler`.
8. Autorizzazione: `VIEW_CATALOG` per lettura, `MANAGE_PRODUCTS` per mutazioni.
9. Test: presenti lato backend.

Problema centrale confermato: `ProductService.update` cambia il codice prodotto (`web/backend/src/main/java/it/giovannidefilippo/gestionale/product/ProductService.java:68-84`) e `ProductService.delete` elimina il record (`:87-96`) senza bloccare prodotti con stock riservato o ordini aperti. Gli ordini conservano `productCode` come stringa; in evasione `OrderService.fulfill` usa quel codice per scalare lo stock (`web/backend/src/main/java/it/giovannidefilippo/gestionale/order/OrderService.java:105-113`). Se il prodotto e stato rinominato o cancellato, l'ordine puo restare confermato ma non evadibile.

### Magazzino

Stato: parziale.

Flusso confermato:

1. Frontend: form movimento e lista.
2. API: `InventoryController`.
3. Validazione: `StockMovementRequest`.
4. Service: `InventoryService`.
5. Regole: blocco stock negativo e blocco scarico sotto stock riservato in `ProductService.adjustStock`.
6. Repository/DB: `StockMovementRepository`, `products`, `stock_movements`.
7. Autorizzazione: `MANAGE_INVENTORY`.
8. Test: presenti lato backend.

Problemi:

* confermato: `InventoryService.lowStockProducts` carica tutti i prodotti e filtra in memoria (`web/backend/src/main/java/it/giovannidefilippo/gestionale/inventory/InventoryService.java:49-52`);
* probabile: per dataset grandi serve query paginata/indicizzata o endpoint aggregato.

### Ordini

Stato: parziale.

Flusso confermato:

1. Frontend: carrello e azioni ordine in `App.tsx`.
2. API: `OrderController`.
3. Validazione: `OrderRequests`.
4. Service: `OrderService`.
5. Regole: draft, confirm, fulfill, cancel; pessimistic lock per transizione.
6. Repository/DB: `CustomerOrderRepository`, `CustomerOrder`, `OrderItem`.
7. Error handling: handler globale.
8. Autorizzazione: ruoli/permessi e filtro ownership per customer.
9. Test: presenti lato backend.

Punti forti confermati:

* `CustomerOrderRepository.findByCodeForUpdate` usa `PESSIMISTIC_WRITE` nelle transizioni (`web/backend/src/main/java/it/giovannidefilippo/gestionale/order/CustomerOrderRepository.java:17-19`);
* `OrderService.confirm` riserva stock prima di confermare (`OrderService.java:94-102`);
* `OrderService.cancel` rilascia stock se l'ordine era confermato (`OrderService.java:116-129`).

Limiti confermati:

* metodo pagamento e una stringa; nel frontend il checkout usa un valore fisso (`web/frontend/src/App.tsx`, flusso checkout);
* assenti pagamenti parziali, stato pagamento, rimborsi, acconti, scadenze, canali pagamento.

### Documenti fiscali simulati

Stato: prototipo.

Flusso confermato:

1. Frontend: vista documenti e azioni di generazione.
2. API: `FiscalDocumentController`.
3. Validazione: request record.
4. Service: `FiscalDocumentService`.
5. Regole: fattura solo dopo ordine evaso; nota credito solo dopo fattura.
6. Repository/DB: `FiscalDocumentRepository`, `FiscalDocument`, `FiscalDocumentLine`.
7. Autorizzazione: `MANAGE_DOCUMENTS`.
8. Test: presenti lato backend.

Limite chiave confermato:

* il servizio verifica duplicati con una query applicativa (`FiscalDocumentService.java:54-55` e `:67-68`), ma non e emerso un vincolo univoco DB su `(related_order_code, type)`. In concorrenza due richieste possono superare entrambe il controllo se non usano la stessa `Idempotency-Key`.

### Account e sessioni

Stato: parziale.

Flusso confermato:

1. Frontend: login/register/account management.
2. API: `UserController`.
3. Validazione: `UserRequests` con `@NotBlank`/`@NotNull`.
4. Service: `UserService`, `AuthSessionService`.
5. Regole: BCrypt, token random, hash token in DB, sessione 45 minuti, lockout username.
6. Repository/DB: `user_accounts`, `auth_sessions`, `login_attempts`.
7. Autorizzazione: permessi per account management.
8. Test: presenti lato backend.

Problema confermato:

* `UserRequests.RegisterRequest` e `CreateAccountRequest` richiedono solo password non vuota (`web/backend/src/main/java/it/giovannidefilippo/gestionale/user/UserRequests.java:13-16`);
* `UserService.createAccount` hasha e salva la password senza applicare la valutazione di robustezza (`web/backend/src/main/java/it/giovannidefilippo/gestionale/user/UserService.java:63-76`).

## 8. Database

Database confermato:

* PostgreSQL runtime;
* H2 in modalita PostgreSQL per test;
* migrazioni Flyway versionate `V1`-`V11`.

Migrazioni principali:

| Migrazione | Contenuto                                                                        | Stato      |
| ---------- | -------------------------------------------------------------------------------- | ---------- |
| V1         | prodotti, utenti, ordini, movimenti stock, documenti fiscali, audit, indici base | confermato |
| V2         | sessioni auth con hash token                                                     | confermato |
| V3         | contesto audit                                                                   | confermato |
| V4         | tentativi login                                                                  | confermato |
| V5         | versione prodotto per optimistic locking                                         | confermato |
| V6         | sequence codici ordine/fattura/nota credito                                      | confermato |
| V7         | stato ordine e data cambio stato                                                 | confermato |
| V8         | partner commerciali e `customer_code` su ordini                                  | confermato |
| V9         | `reserved_quantity` con check `reserved >= 0` e `reserved <= quantity`           | confermato |
| V10        | snapshot cliente su documenti                                                    | confermato |
| V11        | record idempotenza con unique actor/operation/key                                | confermato |

Valutazione:

* confermato: uso corretto di `BigDecimal` nel web per importi monetari;
* confermato: `Product` ha `@Version` per optimistic locking (`web/backend/src/main/java/it/giovannidefilippo/gestionale/product/Product.java:23-25`);
* confermato: transizioni ordine con pessimistic lock;
* confermato: `open-in-view` disabilitato in configurazione;
* confermato: molte relazioni usano snapshot stringa invece di FK, utile per storico ma debole per integrita operativa;
* confermato: `CustomerOrder.items` e `FiscalDocument.lines` sono `fetch = FetchType.EAGER` (`CustomerOrder.java:41-42`, `FiscalDocument.java:74-75`);
* probabile: su pagine grandi possono emergere query pesanti o N+1/logiche di caricamento eccessivo;
* confermato: mancano check constraint diffusi su prezzo, sconto, quantita riga ordine, quantita movimento e campi fiscali;
* confermato: date applicative miste tra `Instant` e `LocalDateTime`, con possibile ambiguita timezone in produzione.

Casi in cui un errore puo lasciare dati incoerenti:

| Caso                                                | Verifica   | Evidenza                                                                 | Effetto                                               |
| --------------------------------------------------- | ---------- | ------------------------------------------------------------------------ | ----------------------------------------------------- |
| Prodotto cancellato/rinominato dopo conferma ordine | confermato | `ProductService.delete/update`; `OrderService.fulfill` usa `productCode` | ordine confermato non evadibile                       |
| Doppia fattura simulata concorrente                 | probabile  | controllo duplicato applicativo senza unique DB su ordine/tipo           | documenti duplicati                                   |
| Legacy Swing: movimento stock salvato prima ordine  | confermato | `src/main/java/service/OrderService.java:73-86`                          | movimento senza ordine se salvataggio ordine fallisce |
| Dati inseriti direttamente a DB fuori applicazione  | probabile  | vincoli DB incompleti                                                    | prezzi/quantita/sconti invalidi                       |

## 9. Regole di business

Regole confermate presenti:

* duplicato codice prodotto bloccato lato backend;
* stock negativo bloccato lato backend;
* stock riservato gestito per ordini confermati;
* transizioni ordine draft/confirmed/fulfilled/cancelled;
* customer limitato ai propri ordini;
* fattura simulata solo su ordine evaso;
* nota credito simulata solo dopo fattura simulata;
* audit per operazioni critiche;
* idempotenza su mutazioni principali quando la chiave e inviata.

Regole mancanti o incomplete:

| Regola                                                                              | Verifica             | Impatto                                  |
| ----------------------------------------------------------------------------------- | -------------------- | ---------------------------------------- |
| Blocco modifica/cancellazione prodotto se referenziato da ordini aperti o riservato | confermato           | rischio ordine bloccato                  |
| Password strength bloccante                                                         | confermato           | account deboli                           |
| Duplicato documento ordine/tipo a livello DB                                        | confermato/probabile | duplicati in concorrenza                 |
| Pagamenti parziali, stato pagamento, rimborso                                       | confermato           | flusso commerciale incompleto            |
| IVA e fiscalita configurabili                                                       | confermato           | non vendibile come gestionale fiscale    |
| Resi merce e rimborsi                                                               | confermato           | flusso post-vendita incompleto           |
| Numerazioni configurabili per azienda/anno/sezionale                                | confermato           | requisito produzione/vendita             |
| Permessi personalizzabili                                                           | confermato           | ruoli hard-coded, poca personalizzazione |
| Multi-sede/multi-magazzino                                                          | confermato           | stock non adatto a negozi multi-location |
| Annullamento/modifica documenti confermati con audit legale                         | da verificare        | compliance non attestabile               |

## 10. Sicurezza

Punti forti confermati:

* Spring Security attivo;
* CSRF disabilitato in modo coerente con API stateless a token header, ma da riesaminare se si passa a cookie;
* permessi lato backend per endpoint (`web/backend/src/main/java/it/giovannidefilippo/gestionale/security/SecurityConfig.java:43-76`);
* password web hashate con BCrypt;
* token sessione random salvato come hash (`web/backend/src/main/java/it/giovannidefilippo/gestionale/user/AuthSessionService.java:34-40`);
* sessione revocabile/scadibile (`AuthSessionService.java:43-67`);
* lockout username lato applicativo (`AuthSessionService.java:19-21`);
* error handler non espone stack trace nei casi generali.

Rischi principali:

| Rischio                                | Gravita        | Verifica          | Scenario realistico                                                               | Mitigazione consigliata                                               |
| -------------------------------------- | -------------- | ----------------- | --------------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| Password deboli accettate              | alta           | confermato        | utente registra password banale; attacco credential stuffing o brute force mirato | applicare policy in `UserService` e test negativi                     |
| Token in memoria JS                    | media          | confermato        | XSS futuro legge il token da memoria modulo frontend                              | CSP, hardening input/output, valutare cookie HttpOnly SameSite + CSRF |
| CSRF disabilitato                      | media          | confermato        | se si passasse a cookie senza revisione, mutazioni cross-site possibili           | mantenere header token o introdurre CSRF token con cookie             |
| Assenza rate limiting perimetrale      | media          | confermato        | attacco distribuito aggira lockout per username o satura login                    | reverse proxy/API gateway con limit IP/user, alert                    |
| IDOR ordini customer                   | basso/medio    | mitigato in parte | customer prova codice ordine altrui                                               | test presenti/da estendere, mantenere filtro ownership in service     |
| Endpoint actuator health/info pubblici | basso          | confermato        | fingerprinting servizi                                                            | ok per health, limitare info in produzione                            |
| H2 console permessa in matcher         | basso/medio    | confermato        | se abilitata accidentalmente fuori dev, accesso console                           | condizionare per profilo dev/test                                     |
| Legacy password in chiaro              | alta ma legacy | confermato        | uso reale Swing espone credenziali su file                                        | non usare legacy in produzione, migrare/hashare                       |
| Java serialization legacy              | alta ma legacy | confermato        | file `.dat` non fidati possono causare deserializzazione pericolosa               | non leggere file non fidati, sostituire formato                       |
| Segreti in repo                        | medio          | da verificare     | valori locali/demo confusi con segreti reali                                      | secret scanning, rotazione se valori reali sono mai stati usati       |

Non sono state trovate evidenze dirette di SQL injection nei repository JPA analizzati. Non sono stati rilevati upload file, command execution, SSRF o path traversal nel web backend. Questi punti restano da verificare se verranno aggiunte integrazioni, import/export o upload.

## 11. API

Tabella endpoint:

| Metodo | Endpoint                          | Funzione               | Ruoli              | Validazione                         | Stato                                      |
| ------ | --------------------------------- | ---------------------- | ------------------ | ----------------------------------- | ------------------------------------------ |
| POST   | `/api/accounts/login`             | login e sessione       | pubblico           | `LoginRequest`                      | parziale                                   |
| POST   | `/api/accounts/logout`            | revoca sessione        | autenticato        | token sessione                      | parziale                                   |
| POST   | `/api/accounts/register`          | registrazione pubblica | pubblico           | `RegisterRequest`                   | parziale, password debole                  |
| POST   | `/api/accounts/password-strength` | valutazione password   | pubblico           | `PasswordRequest`                   | completo come valutazione, non enforcement |
| GET    | `/api/accounts`                   | lista account          | `MANAGE_ACCOUNTS`  | nessuna paginazione                 | parziale                                   |
| POST   | `/api/accounts`                   | crea account           | `MANAGE_ACCOUNTS`  | `CreateAccountRequest`              | parziale                                   |
| DELETE | `/api/accounts/{username}`        | elimina account        | `MANAGE_ACCOUNTS`  | reauth/frontend, service guards     | parziale                                   |
| POST   | `/api/accounts/bulk-delete`       | elimina multipli       | `MANAGE_ACCOUNTS`  | lista usernames                     | parziale                                   |
| GET    | `/api/products`                   | ricerca prodotti       | `VIEW_CATALOG`     | query/page/size normalizzate        | parziale                                   |
| GET    | `/api/products/{code}`            | dettaglio prodotto     | `VIEW_CATALOG`     | path                                | parziale                                   |
| POST   | `/api/products`                   | crea prodotto          | `MANAGE_PRODUCTS`  | `ProductRequest`                    | parziale                                   |
| PUT    | `/api/products/{code}`            | aggiorna prodotto      | `MANAGE_PRODUCTS`  | `ProductRequest`                    | parziale, identita mutabile                |
| DELETE | `/api/products/{code}`            | elimina prodotto       | `MANAGE_PRODUCTS`  | path                                | parziale, delete rischiosa                 |
| POST   | `/api/products/bulk-delete`       | elimina multipli       | `MANAGE_PRODUCTS`  | lista codici                        | parziale                                   |
| GET    | `/api/inventory/movements`        | lista movimenti        | `MANAGE_INVENTORY` | query/page/size                     | parziale                                   |
| POST   | `/api/inventory/movements`        | movimento stock        | `MANAGE_INVENTORY` | `StockMovementRequest`, idempotenza | parziale                                   |
| GET    | `/api/inventory/low-stock`        | prodotti sotto soglia  | `MANAGE_INVENTORY` | nessuna paginazione                 | parziale                                   |
| GET    | `/api/orders`                     | lista ordini           | `VIEW_ORDERS`      | query/page/size                     | parziale                                   |
| GET    | `/api/orders/customer/{customer}` | ordini per cliente     | `VIEW_ORDERS`      | path                                | parziale                                   |
| POST   | `/api/orders`                     | crea ordine            | `CREATE_ORDERS`    | `CreateOrderRequest`, idempotenza   | parziale                                   |
| POST   | `/api/orders/{code}/confirm`      | conferma ordine        | `CONFIRM_ORDERS`   | path, stato                         | parziale                                   |
| POST   | `/api/orders/{code}/fulfill`      | evade ordine           | `FULFILL_ORDERS`   | path, stato                         | parziale                                   |
| POST   | `/api/orders/{code}/cancel`       | annulla ordine         | `CANCEL_ORDERS`    | path, stato                         | parziale                                   |
| GET    | `/api/documents`                  | lista documenti        | `MANAGE_DOCUMENTS` | nessuna paginazione                 | parziale                                   |
| POST   | `/api/documents/invoice`          | fattura simulata       | `MANAGE_DOCUMENTS` | request, idempotenza                | prototipo                                  |
| POST   | `/api/documents/credit-note`      | nota credito simulata  | `MANAGE_DOCUMENTS` | request, idempotenza                | prototipo                                  |
| GET    | `/api/partners`                   | ricerca partner        | `VIEW_PARTNERS`    | query/page/size                     | parziale                                   |
| GET    | `/api/partners/{code}`            | dettaglio partner      | `VIEW_PARTNERS`    | path                                | parziale                                   |
| POST   | `/api/partners`                   | crea partner           | `MANAGE_PARTNERS`  | `BusinessPartnerRequest`            | parziale                                   |
| PUT    | `/api/partners/{code}`            | aggiorna partner       | `MANAGE_PARTNERS`  | `BusinessPartnerRequest`            | parziale                                   |
| DELETE | `/api/partners/{code}`            | disattiva partner      | `MANAGE_PARTNERS`  | path                                | parziale                                   |
| GET    | `/api/audit`                      | ricerca audit          | `VIEW_AUDIT`       | query/page/size                     | parziale                                   |
| GET    | `/api/system/status`              | stato sistema          | `VIEW_AUDIT`       | nessuna                             | parziale                                   |
| GET    | `/actuator/health`                | health                 | pubblico           | n/a                                 | parziale produzione                        |
| GET    | `/actuator/info`                  | info                   | pubblico           | n/a                                 | parziale produzione                        |

Valutazione API:

* confermato: URI coerenti e status code gestiti tramite handler;
* confermato: errori strutturati e request id;
* confermato: alcune mutazioni supportano idempotenza;
* confermato: paginazione su prodotti, partner, ordini, movimenti, audit;
* confermato: documenti e account non paginati;
* da verificare: OpenAPI/Swagger non emerso come contratto generato;
* da verificare: versionamento API non presente.

## 12. Frontend e usabilita

Stato confermato:

* SPA React in `web/frontend`;
* routing/vista gestiti dentro `App.tsx`;
* session token in memoria modulo API (`web/frontend/src/api/products.ts:272-305`);
* header `X-Session-Token` e `X-Request-Id` in ogni request autenticata (`products.ts:502-508`);
* gestione 401 con `SessionExpiredError` (`products.ts:516-529`);
* gestione di loading/error/message presente a livello applicativo;
* conferme per azioni distruttive presenti in vari flussi;
* role gating nel menu e nelle azioni;
* dashboard e snapshot iniziale caricano prodotti, partner, ordini, movimenti, documenti, account, audit e status (`web/frontend/src/App.tsx:235-255`).

Limiti:

* `App.tsx` e molto grande e concentra routing, stato, autorizzazioni, orchestrazione e viste;
* `products.ts` e un API client multi-dominio nonostante il nome;
* non sono presenti test frontend automatici;
* `body` ha `min-width: 1180px` (`web/frontend/src/styles.css:29-32`), quindi il responsive mobile e limitato;
* accessibilita parziale: presenti form e label/aria in alcuni punti, ma mancano verifiche automatiche e pattern completi per tabelle/stati;
* i documenti non hanno export PDF/CSV, dettaglio completo o flusso fiscale reale;
* dashboard calcolata lato client da dataset completi.

Funzioni backend non pienamente raggiunte dal frontend:

* gestione avanzata/idempotenza visibile all'utente;
* audit export/retention;
* configurazioni operative;
* health/readiness oltre widget semplice.

Funzioni frontend non supportate da backend completo:

* esperienza commerciale/dashboards piu ricche basate su calcoli lato client senza endpoint aggregati;
* checkout con metodo pagamento fisso/non strutturato.

## 13. Qualita del codice

Punti positivi:

* backend modulare per package dominio;
* DTO e mapper usati per API;
* servizi transazionali;
* error handling centralizzato;
* audit e request context;
* test backend significativi;
* uso di `BigDecimal` nel web;
* migrazioni versionate.

Criticita:

| Area                     | Verifica   | Evidenza                                                                                                                                            |
| ------------------------ | ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| File troppo grandi       | confermato | `src/main/java/ui/modern/DashboardPanel.java` circa 1357 righe; `web/frontend/src/App.tsx` circa 1267; `web/frontend/src/api/products.ts` circa 579 |
| Nomi incoerenti frontend | confermato | `products.ts` contiene API per molti domini                                                                                                         |
| Legacy non production    | confermato | password in chiaro e serializzazione Java                                                                                                           |
| Magic business constants | confermato | IVA fissa 22%, soglia low-stock fissa, durata sessione/lockout hard-coded                                                                           |
| Duplicazioni concettuali | probabile  | validazioni sparse tra frontend, DTO, entity e service                                                                                              |
| Documentazione stale     | confermato | alcuni documenti dichiarano Dockerfile mancanti o verifiche non riprodotte                                                                          |

TODO/FIXME:

* non sono emersi TODO/FIXME critici nel codice applicativo;
* confermato un placeholder legacy `PLACEHOLDER_METADATA = "Da definire"` in root `ProductService`, legato alla normalizzazione di vecchi dati.

## 14. Test e comandi eseguiti

Comandi eseguiti:

| Comando                                                                    | Esito                    | Note                                                                                   |
| -------------------------------------------------------------------------- | ------------------------ | -------------------------------------------------------------------------------------- |
| `mvn test` in root                                                         | successo                 | 41 test, 0 fallimenti                                                                  |
| `mvn test` in `web/backend`                                                | fallito                  | 54 test avviati, 52 errori infrastrutturali Mockito/ByteBuddy sotto Java 23            |
| `MAVEN_OPTS="-Djdk.attach.allowAttachSelf=true" mvn test` in `web/backend` | fallito                  | stesso problema Mockito/ByteBuddy                                                      |
| `mvn -DargLine=-javaagent:...byte-buddy-agent... test` in `web/backend`    | successo                 | 54 test, 0 fallimenti                                                                  |
| `npm run build` in `web/frontend` prima di install                         | fallito                  | `tsc` non disponibile, `node_modules` assente                                          |
| `npm ci` in `web/frontend`                                                 | successo                 | installate dipendenze dichiarate in lockfile                                           |
| `npm run build` in `web/frontend` dopo install                             | successo                 | TypeScript + Vite build OK                                                             |
| `npm run audit` in `web/frontend`                                          | fallito/non verificabile | DNS/network verso registry npm non disponibile; log npm non scrivibile in home sandbox |
| `npm ls --depth=0`                                                         | successo                 | dipendenze top-level confermate                                                        |

Copertura test:

* confermato: test root legacy presenti e passanti;
* confermato: test backend presenti e passanti se eseguiti con Java agent o ambiente compatibile;
* confermato: test frontend assenti;
* non verificabile: coverage percentuale;
* da verificare: test di concorrenza reali, test E2E browser, test Docker prod-like in locale, performance test.

Problema di riproducibilita:

* le istruzioni repository indicano `cd web/backend && mvn test`, ma nell'ambiente locale con Java 23 il comando fallisce senza configurazione aggiuntiva. Serve allineare toolchain o build test.

## 15. Prestazioni

Rischi confermati/probabili:

| Rischio                           | Verifica   | Evidenza                                                    | Impatto                               |
| --------------------------------- | ---------- | ----------------------------------------------------------- | ------------------------------------- |
| Dashboard carica dataset completi | confermato | `App.tsx:235-255`, `fetchAllPages` in `products.ts:558-568` | lentezza con dataset grandi           |
| Endpoint documenti non paginato   | confermato | API lista documenti restituisce lista                       | memoria/tempo su molti documenti      |
| Endpoint account non paginato     | confermato | API lista account restituisce lista                         | gestione inefficiente su molte utenze |
| Low-stock in memoria              | confermato | `InventoryService.java:49-52`                               | query non scalabile                   |
| EAGER su righe ordine/documento   | confermato | `CustomerOrder.java:41-42`, `FiscalDocument.java:74-75`     | query pesanti/N+1 probabile           |
| Nessuna cache applicativa         | confermato | non emersa                                                  | ok per MVP, da valutare               |
| Nessun test carico/concorrenza    | confermato | non emersi                                                  | rischi non quantificati               |

## 16. Docker e deployment

Confermato:

* `docker-compose.yml` per PostgreSQL dev con volume e healthcheck;
* `docker-compose.prod-like.yml` con postgres, backend e frontend;
* backend Dockerfile multi-stage Maven + runtime Java 17, utente non-root;
* frontend Dockerfile Node build + nginx runtime;
* nginx proxy `/api/` verso backend e fallback SPA;
* healthcheck presenti in compose prod-like;
* documentazione Docker/backup presente.

Limiti:

* non e stato eseguito lo stack Docker in questa analisi;
* frontend nginx non evidenzia hardening utente non-root nel Dockerfile;
* non sono presenti TLS/reverse proxy production-ready;
* secrets manager assente;
* backup/restore non schedulati come operazione di produzione;
* logging/metriche/tracing centralizzati assenti;
* readiness/liveness Kubernetes non presenti;
* rollback e release strategy non definiti;
* registry immagini e versionamento immagini non definiti.

## 17. Documentazione

Documentazione presente:

* `README.md`;
* `AGENTS.md`;
* `docs/ARCHITECTURE.md`;
* `docs/API_CONTRACT.md`;
* `docs/SECURITY.md`;
* `docs/TESTING.md`;
* `docs/DEPLOYMENT.md`;
* `docs/DOCKER.md`;
* `docs/BACKUP_RESTORE.md`;
* `docs/RISK_REGISTER.md`;
* ulteriori note operative in `docs/`.

Valutazione:

* confermato: la documentazione e ampia e utile;
* confermato: `docs/API_CONTRACT.md` rispecchia in buona parte le API;
* confermato: `docs/SECURITY.md` riconosce diversi rischi reali;
* confermato: alcune parti sono stale o sovrastimano lo stato, per esempio test/audit Docker e rischio Dockerfile mancanti;
* da verificare: manuale utente finale, onboarding cliente, runbook incidenti, privacy/GDPR, SLA assistenza.

## 18. Funzionalita mancanti

Necessarie per produzione:

* vincoli database aggiuntivi;
* blocco modifiche prodotto rischiose;
* password policy effettiva;
* paginazione coerente e dashboard aggregata;
* backup/restore operativo e testato;
* logging/metriche/alerting;
* hardening deployment e segreti;
* test E2E minimi;
* documentazione operativa aggiornata.

Necessarie per vendita:

* configurazione aziendale;
* numerazioni configurabili;
* IVA e impostazioni fiscali reali;
* PDF/CSV/Excel;
* report vendite/magazzino;
* ruoli personalizzabili;
* sedi e magazzini;
* audit log consultabile/esportabile;
* import/export dati;
* backup self-service o procedura assistita;
* onboarding e manuale utente;
* diagnostica e assistenza.

Competitive:

* dashboard avanzate;
* API pubbliche/integrate;
* notifiche;
* integrazioni e-commerce/fatturazione/pagamenti;
* white-label/branding cliente;
* multi-tenant isolato.

Opzionali:

* automazioni marketing;
* analisi predittiva;
* integrazioni marketplace;
* mobile app dedicata.

## 19. Valutazione commerciale

Il gestionale non dovrebbe essere venduto o distribuito come prodotto production-ready nello stato attuale.

Classificazione:

| Area commerciale          | Stato                                         |
| ------------------------- | --------------------------------------------- |
| Uso interno dimostrativo  | possibile con cautela                         |
| MVP operativo controllato | possibile dopo correzione P1 e runbook minimo |
| Produzione reale          | non pronto                                    |
| SaaS multi-cliente        | non pronto                                    |
| Vendita commerciale       | non pronto                                    |
| Conformita fiscale/GDPR   | non attestabile                               |

Motivazione:

* le funzioni core esistono, ma sono ancora incomplete per un negozio reale;
* i documenti sono dichiaratamente simulati;
* mancano configurazioni aziendali/fiscali;
* mancano flussi reali di pagamento, reso, rimborso, scadenze;
* mancano garanzie operative e di sicurezza sufficienti;
* personalizzazione cliente e multi-tenant sono assenti.

## 20. Punti di forza

* direzione web-first corretta;
* monolite modulare adatto allo stadio del progetto;
* Flyway gia adottato;
* security server-side con ruoli/permessi;
* audit e idempotenza gia introdotti;
* test backend non banali;
* Docker e CI gia avviati;
* documentazione piu ampia della media dei prototipi;
* separazione DTO/entity nel backend web;
* uso di BigDecimal per valori monetari nel web;
* root Swing utile come archivio funzionale durante la migrazione.

## 21. Limiti dell'analisi

* lo stato Git non e verificabile per assenza repository Git locale;
* non e stato eseguito Docker;
* non sono stati eseguiti test E2E browser;
* non sono stati eseguiti test di carico/concorrenza;
* non e stato possibile completare `npm audit` per rete bloccata;
* non e stata fatta verifica professionale fiscale, legale o GDPR;
* non sono stati analizzati dati reali di produzione;
* non e stato verificato un deploy reale dietro HTTPS/reverse proxy;
* non sono stati mostrati segreti o password in chiaro.

## Registro problemi

| ID    | Priorita | Verifica         | Area                | Problema                                                                   | Evidenza                                                                 | Impatto                                             | Soluzione                                                                                                                     | Costo |
| ----- | -------- | ---------------- | ------------------- | -------------------------------------------------------------------------- | ------------------------------------------------------------------------ | --------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------- | ----- |
| R-001 | P1       | confermato       | Prodotti/ordini     | Prodotto cancellabile o rinominabile con ordini confermati/stock riservato | `ProductService.java:68-96`, `OrderService.java:105-113`                 | Ordini confermati possono diventare non evadibili   | Rendere immutabile il codice prodotto dopo uso, bloccare delete con riserve/ordini aperti, preferire soft delete/discontinued | M     |
| R-002 | P1       | confermato       | Sicurezza account   | Password deboli accettate in registrazione/creazione                       | `UserRequests.java:13-16`, `UserService.java:63-76`                      | Compromissione account piu probabile                | Applicare `PasswordStrengthService` in `UserService`, policy configurabile, test negativi                                     | S     |
| R-003 | P1       | probabile        | Documenti/DB        | Duplicato fattura/nota credito in concorrenza                              | `FiscalDocumentService.java:54-57`, `:67-70`; assenza unique ordine/tipo | Documenti duplicati e dati incoerenti               | Migrazione unique `(related_order_code,type)`, gestione conflict 409, test concorrenza                                        | S/M   |
| R-004 | P1       | confermato       | Build/test          | `mvn test` backend fallisce su Java 23 senza Java agent                    | comando eseguito, errore Mockito/ByteBuddy                               | Verifica locale non riproducibile secondo AGENTS    | Usare JDK 17/toolchain/Maven wrapper o configurare `argLine` javaagent per test                                               | S     |
| R-005 | P2       | confermato       | Database            | Vincoli DB incompleti per prezzi, sconti e quantita                        | migrazioni V1/V9                                                         | Bug o import manuali possono scrivere dati invalidi | Aggiungere CHECK constraint e test migration                                                                                  | M     |
| R-006 | P2       | confermato       | Prestazioni         | Dashboard carica dataset completi                                          | `App.tsx:235-255`, `products.ts:558-568`                                 | Lentezza e memoria alta su dati reali               | Endpoint aggregati dashboard e caricamento lazy                                                                               | M     |
| R-007 | P2       | confermato       | API                 | Lista documenti non paginata                                               | endpoint `/api/documents`                                                | Scalabilita limitata                                | Introdurre PageResponse, filtri e ordinamento                                                                                 | S     |
| R-008 | P2       | confermato       | API                 | Lista account non paginata                                                 | endpoint `/api/accounts`                                                 | Scalabilita e UX limitate                           | Paginazione e ricerca account                                                                                                 | S     |
| R-009 | P2       | confermato       | Prestazioni DB      | EAGER su righe ordine/documento                                            | `CustomerOrder.java:41-42`, `FiscalDocument.java:74-75`                  | Query pesanti/N+1 probabili                         | LAZY + fetch join/entity graph/proiezioni                                                                                     | M     |
| R-010 | P2       | confermato       | Frontend            | `App.tsx` troppo grande                                                    | circa 1267 righe                                                         | Manutenzione difficile, regressioni UI              | Split per pagine, hook e state modules                                                                                        | L     |
| R-011 | P2       | confermato       | Frontend/API        | API client monolitico e nome fuorviante                                    | `web/frontend/src/api/products.ts` circa 579 righe                       | Bassa coesione                                      | Separare client per dominio                                                                                                   | M     |
| R-012 | P2       | confermato       | QA                  | Test frontend assenti                                                      | `package.json` senza script test                                         | Regressioni UI non intercettate                     | Vitest/RTL + Playwright smoke E2E                                                                                             | M     |
| R-013 | P2       | non verificabile | Security deps       | `npm audit` non completato                                                 | rete bloccata verso registry npm                                         | Vulnerabilita dipendenze non escluse                | Eseguire audit in CI/rete e salvare risultato                                                                                 | S     |
| R-014 | P2       | confermato       | Sicurezza           | Nessun rate limiting perimetrale                                           | configurazioni deployment                                                | Brute force/distributed abuse                       | Nginx/API gateway rate limits e alert                                                                                         | S/M   |
| R-015 | P2       | confermato       | Sicurezza frontend  | Token in memoria JS                                                        | `products.ts:272-305`                                                    | XSS futuro puo rubare sessione                      | CSP, hardening, valutare cookie HttpOnly + CSRF                                                                               | M     |
| R-016 | P2       | confermato       | Partner             | Validazione email/VAT/tax debole                                           | `BusinessPartnerRequest.java:7-18`                                       | Dati anagrafici sporchi/duplicati                   | `@Email`, pattern/config paese, unique dove necessario                                                                        | S/M   |
| R-017 | P2       | confermato       | Business            | Pagamenti semplificati                                                     | request con `paymentMethod`, frontend valore fisso                       | Non gestisce acconti/saldi/rimborsi                 | Modello pagamento e stato pagamento                                                                                           | M     |
| R-018 | P2       | confermato       | Commerciale         | Multi-azienda/tenant assente                                               | schema e entity senza company/tenant                                     | Non adatto a SaaS/multi-cliente                     | Modello tenant/company e isolamento permessi/dati                                                                             | XL    |
| R-019 | P2       | confermato       | Deployment          | Produzione non completa                                                    | docs/deploy/docker                                                       | Mancano TLS, segreti, osservabilita, rollback       | Runbook e infrastruttura production-ready                                                                                     | L/XL  |
| R-020 | P2       | confermato       | Documentazione      | Documentazione stale/overclaim                                             | `docs/TESTING.md`, `docs/DEPLOYMENT.md`, `docs/RISK_REGISTER.md`         | Decisioni basate su stato non reale                 | Aggiornare docs dopo verifica attuale                                                                                         | S     |
| R-021 | P2       | confermato       | Database            | Riferimenti ordine/prodotto/partner come stringhe senza FK                 | entity/migrazioni                                                        | Integrita referenziale debole                       | Dichiarare snapshot strategy o introdurre FK dove serve                                                                       | M     |
| R-022 | P2       | confermato       | Date/time           | Uso misto `LocalDateTime` e `Instant`                                      | entity ordini/documenti/audit/sessioni                                   | Ambiguita timezone/audit                            | Standardizzare su `Instant`/`OffsetDateTime`, migration                                                                       | M     |
| R-023 | P2       | confermato       | Frontend responsive | Layout impone `min-width:1180px`                                           | `styles.css:29-32`                                                       | Mobile/tablet limitati                              | Revisione responsive e test viewport                                                                                          | M     |
| R-024 | P2       | confermato       | Documenti           | Documenti dichiaratamente simulati                                         | `FiscalDocumentService` disclaimer e nomi                                | Non vendibile come gestione fiscale                 | Integrazione fiscale/PDF/numerazioni/configurazioni                                                                           | L/XL  |
| R-025 | P2       | confermato       | Backup              | Backup non operativo/schedulato                                            | docs/scripts presenti, non deploy                                        | Rischio dati in produzione                          | Procedura schedulata, restore test periodico                                                                                  | M     |
| R-026 | P3       | confermato       | Magazzino           | Low-stock calcolato in memoria                                             | `InventoryService.java:49-52`                                            | Inefficiente su cataloghi grandi                    | Query repository con soglia e paginazione                                                                                     | S     |
| R-027 | P3       | confermato       | Config              | Costanti business hard-coded                                               | VAT, low-stock, session duration                                         | Scarsa personalizzazione                            | Configurazioni per azienda/ambiente                                                                                           | M     |
| R-028 | P3       | confermato       | Legacy              | Password Swing in chiaro                                                   | `src/main/java/service/AuthService.java:43-76`                           | Non adatto a produzione legacy                      | Non usare legacy o migrare hashing                                                                                            | S/M   |
| R-029 | P3       | confermato       | Legacy              | Serializzazione Java file-based                                            | `src/main/java/utils/FileManager.java:7-24`                              | Rischio sicurezza/corruzione file                   | Formato sicuro o migrazione DB                                                                                                | M     |
| R-030 | P3       | confermato       | Legacy              | Stock movement salvato prima ordine                                        | `src/main/java/service/OrderService.java:73-86`                          | Incoerenza file se errore intermedio                | Considerare legacy read-only o transazione/migrazione web                                                                     | M     |
| R-031 | P3       | confermato       | CI                  | Test Docker/security non riprodotti localmente                             | solo workflow/documenti                                                  | Falsa fiducia se CI non eseguita                    | Verificare CI reale e badge/stato                                                                                             | S     |
| R-032 | P4       | confermato       | Repository          | Stato Git non disponibile                                                  | `git status` fallito                                                     | Impossibile distinguere modifiche non committate    | Ripristinare/fornire clone Git o indicare snapshot                                                                            | XS    |

## Punteggi

| Area                 | Punteggio | Motivazione                                                                               |
| -------------------- | --------: | ----------------------------------------------------------------------------------------- |
| Architettura         |         7 | Monolite modulare sensato, ma boundary e frontend ancora deboli.                          |
| Codice               |         7 | Backend pulito, legacy e frontend monolitico abbassano il voto.                           |
| Database             |         6 | Flyway e locking presenti, ma vincoli/integrita e referenze da rafforzare.                |
| Sicurezza            |         6 | Buona base server-side, ma password policy, rate limit, token model e hardening mancanti. |
| Funzionalita         |         5 | Core presente ma flussi reali commerciali/fiscali incompleti.                             |
| Frontend             |         5 | Usabile, ma monolitico, poco testato e responsive limitato.                               |
| Test                 |         7 | Backend/root ben avviati, frontend/E2E/concorrenza assenti.                               |
| Prestazioni          |         5 | Paginazione parziale, ma snapshot completi e EAGER rischiosi.                             |
| Deployment           |         5 | Docker/CI presenti, produzione ancora non pronta.                                         |
| Documentazione       |         6 | Ampia, ma alcune parti stale.                                                             |
| Manutenibilita       |         6 | Buon backend, frontend e legacy pesano.                                                   |
| Personalizzabilita   |         3 | Ruoli, fiscalita, azienda, sedi e branding non configurabili.                             |
| Maturita commerciale |         3 | Non vendibile senza completamento funzionale e operativo.                                 |

Verdetto: **MVP incompleto**.

## Piano di intervento

### 1. Emergenze

| Attivita                                                   | Priorita | Dipendenze                | Componenti                     | Risultato atteso                   | Criterio di accettazione                                                                         | Costo |
| ---------------------------------------------------------- | -------- | ------------------------- | ------------------------------ | ---------------------------------- | ------------------------------------------------------------------------------------------------ | ----- |
| Bloccare modifica/cancellazione prodotti usati o riservati | P1       | nessuna                   | backend product/order/db       | ordini confermati sempre evadibili | test: delete/update prodotto con ordine confermato ritorna errore; fulfill continua a funzionare | M     |
| Applicare password policy nel service                      | P1       | definizione policy minima | backend user/security/frontend | account deboli rifiutati           | test negativi register/create; messaggio UI coerente                                             | S     |
| Rendere univoci i documenti per ordine/tipo                | P1       | migration                 | backend document/db            | niente fatture/note duplicate      | unique DB + test concorrenza/base                                                                | S/M   |
| Stabilizzare comando backend test                          | P1       | scelta JDK/toolchain      | build/backend                  | `mvn test` riproducibile           | comando AGENTS passa in ambiente standard                                                        | S     |

### 2. Stabilizzazione

| Attivita                                    | Priorita | Dipendenze           | Componenti        | Risultato atteso          | Criterio di accettazione                 | Costo |
| ------------------------------------------- | -------- | -------------------- | ----------------- | ------------------------- | ---------------------------------------- | ----- |
| Aggiungere vincoli DB su importi e quantita | P2       | pulizia dati         | db/backend        | integrita anche fuori app | migration validata e test repository     | M     |
| Paginare documenti e account                | P2       | API contract         | backend/frontend  | liste scalabili           | endpoint PageResponse e UI con controlli | M     |
| Sostituire low-stock in memoria             | P3       | nessuna              | backend inventory | query efficiente          | repository query paginata/test           | S     |
| Aggiornare documentazione stale             | P2       | verifiche completate | docs              | docs allineate            | nessun documento dichiara stato falso    | S     |

### 3. Miglioramento architetturale

| Attivita                        | Priorita | Dipendenze        | Componenti          | Risultato atteso           | Criterio di accettazione                               | Costo |
| ------------------------------- | -------- | ----------------- | ------------------- | -------------------------- | ------------------------------------------------------ | ----- |
| Spezzare `App.tsx` per domini   | P2       | test minimi UI    | frontend            | UI manutenibile            | pagine/hook per catalogo, ordini, magazzino, account   | L     |
| Separare API client per dominio | P2       | nessuna           | frontend            | coesione e naming corretti | file `products`, `orders`, `accounts`, ecc.            | M     |
| Rivedere EAGER/queries          | P2       | test integrazione | backend/db          | query prevedibili          | test lista ordini/documenti senza regressioni          | M     |
| Standardizzare date/time        | P2       | migration         | backend/db/frontend | timestamp coerenti         | uso coerente `Instant`/timezone e test serializzazione | M     |

### 4. Completezza funzionale

| Attivita                         | Priorita | Dipendenze            | Componenti          | Risultato atteso                   | Criterio di accettazione               | Costo |
| -------------------------------- | -------- | --------------------- | ------------------- | ---------------------------------- | -------------------------------------- | ----- |
| Modellare pagamenti              | P2       | requisiti funzionali  | backend/frontend/db | pagamento strutturato              | stati pagamento, acconti/saldi testati | M/L   |
| Gestire resi/rimborsi            | P2       | pagamenti/documenti   | backend/frontend/db | post-vendita completo              | flusso reso con stock e audit          | L     |
| Configurazione azienda/fiscalita | P2       | requisiti commerciali | backend/frontend/db | dati aziendali e IVA configurabili | UI config + migrazioni + test          | L     |
| Export PDF/CSV/Excel             | P2       | definizione template  | backend/frontend    | output operativo                   | download documenti/report validato     | M/L   |

### 5. Test

| Attivita                            | Priorita | Dipendenze                | Componenti | Risultato atteso                        | Criterio di accettazione             | Costo |
| ----------------------------------- | -------- | ------------------------- | ---------- | --------------------------------------- | ------------------------------------ | ----- |
| Introdurre Vitest/RTL               | P2       | refactor leggero frontend | frontend   | test componenti/form                    | build + test frontend in CI          | M     |
| E2E smoke Playwright                | P2       | ambiente test stabile     | full stack | flussi login-prodotto-ordine verificati | pipeline verde su smoke              | M     |
| Test concorrenza su documenti/stock | P2       | vincoli/locking           | backend/db | race intercettate                       | test riproducono conflitti e passano | M     |
| Coverage/report qualità             | P3       | CI                        | build/CI   | visibilita qualità                      | report pubblicato in CI              | S     |

### 6. Produzione

| Attivita                  | Priorita | Dipendenze        | Componenti    | Risultato atteso    | Criterio di accettazione                              | Costo |
| ------------------------- | -------- | ----------------- | ------------- | ------------------- | ----------------------------------------------------- | ----- |
| Hardening deployment      | P1       | stack target      | docker/infra  | deploy sicuro       | TLS, reverse proxy, secrets, non-root, env documented | L     |
| Backup/restore schedulato | P1       | ambiente target   | db/infra      | recuperabilita dati | restore testato periodicamente                        | M     |
| Observability             | P2       | deployment        | backend/infra | diagnosi incidenti  | metriche, log centralizzati, alert base               | M     |
| Runbook operativo         | P2       | deployment deciso | docs/ops      | gestione incidenti  | procedure deploy/rollback/backup incidenti            | M     |

### 7. Commercializzazione

| Attivita                    | Priorita   | Dipendenze        | Componenti          | Risultato atteso           | Criterio di accettazione                         | Costo |
| --------------------------- | ---------- | ----------------- | ------------------- | -------------------------- | ------------------------------------------------ | ----- |
| Definire packaging prodotto | P2         | produzione minima | prodotto/docs       | offerta chiara             | installazione riproducibile e manuale            | M     |
| Ruoli personalizzabili      | P2         | modello permessi  | backend/frontend/db | adattamento cliente        | CRUD ruoli/permessi testato                      | L     |
| Multi-sede/magazzino        | P2         | requisiti cliente | backend/frontend/db | stock reale multi-location | flussi trasferimento/giacenza per sede           | XL    |
| GDPR/privacy workflow       | P1 vendita | consulenza legale | backend/docs/ops    | compliance verificabile    | DPIA/policy/processi approvati da professionista | L     |

## Prossime 20 attivita

1. Ripristinare o chiarire lo stato Git del repository/snapshot.
2. Decidere toolchain standard Java 17 e rendere `web/backend mvn test` riproducibile.
3. Aggiungere test che riproduce update codice prodotto con ordine confermato.
4. Bloccare modifica codice prodotto quando esistono ordini aperti o stock riservato.
5. Aggiungere test che riproduce cancellazione prodotto con stock riservato.
6. Bloccare delete fisico e introdurre dismissione/soft delete prodotto.
7. Applicare password policy in `UserService`.
8. Aggiornare frontend account/register per mostrare errori password bloccanti.
9. Aggiungere unique DB su documento fiscale per ordine/tipo.
10. Gestire conflict documenti con errore API coerente.
11. Aggiungere test concorrenza base per documenti.
12. Aggiungere check constraint DB su prodotti, movimenti e righe ordine.
13. Paginare `/api/documents`.
14. Paginare `/api/accounts`.
15. Sostituire low-stock in memoria con query paginata.
16. Creare endpoint aggregato dashboard.
17. Aggiornare `App.tsx` per usare dashboard aggregata e caricamenti lazy.
18. Introdurre test frontend minimi per login, catalogo e ordine.
19. Aggiornare `docs/TESTING.md`, `docs/DEPLOYMENT.md` e `docs/RISK_REGISTER.md`.
20. Eseguire pipeline completa in ambiente con rete e Docker, includendo audit dipendenze e prod-like compose.
