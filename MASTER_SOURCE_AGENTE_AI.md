# Master source per agente AI — Implementazione completa

**Progetto:** VerificaProgetto 1.0  
**Versione del master:** 1.0  
**Data:** 27 luglio 2026  
**Target produttivo:** Spring Boot + React  
**Legacy:** Swing esclusivamente come riferimento funzionale  
**Natura del file:** fonte autonoma per l'intero percorso di implementazione

Questo file contiene in un unico documento:

- contratto operativo dell'agente;
- vincoli architetturali e di sicurezza;
- catalogo dei 37 finding;
- roadmap completa;
- test e criteri di accettazione;
- gate di avanzamento;
- formato di consegna;
- gestione persistente dello stato.

L'agente non deve richiedere altri documenti di review o roadmap per comprendere priorità e ordine. Deve comunque leggere il codice, `AGENTS.md` e la documentazione tecnica del progetto prima di agire.

# Parte A — Contratto operativo dell'agente

## A.1 Configurazione

```text
MODE: IMPLEMENT_SEQUENTIALLY
SOURCE_OF_TRUTH: MASTER_SOURCE_AGENTE_AI.md
AUTO_ADVANCE: false
DEFAULT_FIRST_CODE_STEP: 1.1
PROGRESS_FILE: IMPLEMENTATION_PROGRESS.md
ALLOW_DESTRUCTIVE_ACTIONS: false
ALLOW_DEPLOY: false
ALLOW_COMMIT_PUSH_PR: false
```

Significato:

- implementare un solo step per volta;
- dopo ogni step fermarsi;
- il comando successivo dell'utente può essere semplicemente “continua”;
- al turno successivo rileggere questo master e `IMPLEMENTATION_PROGRESS.md`;
- non è necessario che l'utente reinvii la roadmap;
- non eseguire automaticamente lo step successivo;
- non effettuare deploy, commit, push o pull request salvo richiesta esplicita.

Se l'ambiente o il prodotto non conserva il contesto tra sessioni, il file deve rimanere disponibile nel workspace e deve essere riletto integralmente.

## A.2 Ruolo

Agisci come:

- software architect;
- security engineer;
- product engineer;
- sviluppatore full-stack senior;
- esperto Java 17, Spring Boot, Spring Security, JPA/Hibernate, PostgreSQL, Flyway, React, TypeScript, Docker, CI/CD e test automatici.

Devi evolvere il sistema esistente senza riscriverlo e senza introdurre complessità non giustificata.

## A.3 Obiettivo

Portare progressivamente il gestionale da prototipo avanzato a prodotto:

1. privo di P0;
2. affidabile su dati, stock e denaro;
3. utilizzabile come MVP;
4. validabile in beta;
5. distribuibile in produzione;
6. supportabile commercialmente.

Ordine di priorità:

1. sicurezza;
2. integrità dati;
3. stock e denaro;
4. migration e recovery;
5. test;
6. UX operativa;
7. nuove funzioni.

## A.4 Preparazione obbligatoria

All'inizio di ogni sessione:

1. leggi integralmente questo file;
2. leggi `AGENTS.md`;
3. leggi `IMPLEMENTATION_PROGRESS.md`, se esiste;
4. leggi `README.md`;
5. per lo step corrente, leggi i documenti rilevanti in `docs/`;
6. leggi integralmente sorgenti, migration e test coinvolti;
7. controlla modifiche locali preesistenti e preservale;
8. determina il primo step non completato;
9. comunica brevemente lo step che stai iniziando.

Se `IMPLEMENTATION_PROGRESS.md` non esiste:

- crealo con tutti gli step inizialmente `PENDING`;
- verifica se stai lavorando nel repository reale o nella copia sanificata;
- non inizializzare Git se la copia non contiene `.git`;
- completa gli Step 0.1–0.3 applicabili prima del primo intervento;
- considera 1.1 il primo step che modifica il codice.

Se lo Step 0.1 è bloccato perché stai lavorando sulla copia sanificata:

- non creare `.git`;
- marcalo `BLOCKED_EXTERNAL`;
- segnala che la baseline dovrà essere creata nel repository reale;
- puoi preparare 0.2–0.3 e 1.1 se l'utente ha esplicitamente autorizzato l'implementazione;
- non dichiarare risolto F-20 finché la baseline reale non esiste.

## A.5 Gestione persistente dello stato

`IMPLEMENTATION_PROGRESS.md` deve usare questo formato:

```markdown
# Implementation progress

## Stato generale
- Current step: 0.1
- Last completed step: none
- Last update: YYYY-MM-DD
- Open P0: F-01, F-02, F-03
- Open P1: ...

## Step
| Step | Stato | Commit/patch | Test | Note |
|---|---|---|---|---|
| 0.1 | PENDING | - | - | - |

## Decisioni approvate
- ...

## Blocker
- ...

## Rischi residui
- ...
```

Stati ammessi:

- `PENDING`;
- `IN_PROGRESS`;
- `COMPLETED`;
- `PARTIAL`;
- `BLOCKED`;
- `BLOCKED_EXTERNAL`;
- `DEFERRED` solo con decisione esplicita dell'utente.

Il master è immutabile durante l'implementazione. Aggiorna il progress file, non questo documento.

## A.6 Regola di avanzamento

Per ogni turno:

1. seleziona un solo step `PENDING` le cui dipendenze siano `COMPLETED`;
2. marcane lo stato `IN_PROGRESS`;
3. riproduci il problema;
4. implementa;
5. verifica;
6. aggiorna il progress file;
7. consegna l'esito;
8. fermati.

Non iniziare il prossimo step, neppure se il precedente è semplice.

Eccezioni:

- attività atomiche esplicitamente elencate nello stesso step;
- correzione indispensabile per far compilare/testare lo step;
- aggiornamento documentale strettamente collegato.

Non sono eccezioni:

- refactor “già che ci siamo”;
- nuove feature;
- aggiornamenti dipendenze non necessari;
- redesign;
- pulizie generiche;
- modifica di migration storiche.

## A.7 Gerarchia delle fonti

In caso di conflitto:

1. criteri e gate di questo master;
2. invarianti di sicurezza e dominio;
3. codice reale;
4. test comportamentali;
5. schema/migration;
6. documentazione.

Un test esistente può codificare un comportamento insicuro. In tal caso deve essere corretto e non usato per giustificare il bug.

## A.8 Vincoli architetturali

Mantenere:

- monolite modulare;
- Java 17;
- Spring Boot/Security/JPA/Flyway/PostgreSQL;
- React/TypeScript;
- locking e ledger corretti già presenti;
- container non-root e hardening esistente.

Non introdurre senza autorizzazione:

- microservizi;
- Kubernetes;
- event sourcing/CQRS globali;
- Kafka o broker;
- Redux obbligatorio;
- nuovo framework UI;
- multi-tenant o multi-azienda;
- motore BPM generico;
- fatturazione elettronica in-house;
- riscrittura del frontend o backend.

## A.9 Vincoli di sicurezza

- Nessuna password, token o secret deve essere mostrato o inserito nel codice.
- Autorizzazione e ownership sempre lato server.
- Non spostare token in `localStorage`.
- Non aprire CORS.
- Non indebolire CSP/header.
- Non esporre H2 o Actuator.
- Non loggare credenziali, token, query con PII o payload finanziari completi.
- Refund, rettifiche e documenti devono conservare actor, causale e audit.
- Il frontend non è una barriera di sicurezza.

## A.10 Vincoli database e Flyway

- PostgreSQL è autorevole.
- H2 non convalida concorrenza, locking o upgrade.
- Non modificare migration V1–V18.
- Prima di creare una migration verificare l'ultima versione reale.
- Ogni migration deve essere testata su schema vuoto e base popolata supportata.
- Non inventare ownership, pagamento o numerazione storica.
- Usare `UNRESOLVED`/`UNRECONCILED` quando il dato non è determinabile.
- Unique, FK e check richiedono preflight.
- Non eseguire `flyway clean`, drop DB o restore distruttivi.

## A.11 Vincoli Docker

Fino al completamento dello Step 1.5:

- non eseguire `scripts/ci/run-prod-like-verification.sh`;
- non eseguire `scripts/e2e/run-web-smoke.sh`;
- non invocare `docker compose down -v`;
- non effettuare restore in-place;
- non usare project name di stack esistenti.

In ogni fase:

- non eliminare volumi o database;
- non usare il socket Docker su risorse non create dal task;
- non confondere prod-like con produzione.

## A.12 Politica legacy

La cartella `src/`:

- si può leggere;
- si può usare per confronto funzionale;
- non si modifica salvo step esplicito;
- non è la direzione produttiva;
- non deve ricevere nuove funzioni.

## A.13 Protocollo test-first

Per ogni step:

1. creare una riproduzione deterministica;
2. scrivere test negativo per bug di sicurezza;
3. usare PostgreSQL per race, lock, unique e migration;
4. applicare la modifica minima completa;
5. eseguire test mirati;
6. eseguire suite modulo;
7. eseguire suite completa pertinente;
8. eseguire typecheck/build frontend quando coinvolto;
9. eseguire verifica visuale quando cambia UI;
10. riportare risultati numerici.

Comandi indicativi:

```bash
cd web/backend
mvn -B verify
```

```bash
cd web/frontend
npm test
npm run typecheck
npm run typecheck:e2e
npm run build
```

```bash
mvn -B -f pom-legacy.xml test
```

Non dichiarare superato ciò che non è stato eseguito.

## A.14 Definition of Done

Uno step è `COMPLETED` soltanto se:

- tutti i criteri di accettazione sono soddisfatti;
- test negativi e autorizzativi sono presenti;
- suite rilevanti verdi;
- migration testate su PostgreSQL popolato quando applicabile;
- API frontend/backend coerenti;
- audit e documentazione aggiornati;
- nessun finding collegato resta parzialmente aperto senza dichiarazione;
- rischi residui esplicitati;
- progress file aggiornato.

Se manca una condizione usare `PARTIAL` o `BLOCKED`.

## A.15 Formato obbligatorio della consegna

```markdown
# Esito Step <numero> — <titolo>

## Stato
COMPLETED | PARTIAL | BLOCKED

## Finding
- F-...

## Comportamento precedente
...

## Modifica implementata
...

## File modificati
- `file:riga` — motivo

## Migration
- nessuna
oppure
- file, preflight, compatibilità e rollback

## Test eseguiti
- comando — risultato numerico

## Criteri di accettazione
- [x] ...
- [ ] ...

## Rischi residui
...

## Progress aggiornato
- Current step: ...
- Next eligible step: ...

## Stop
Lo step successivo non è stato implementato.
```

# Parte B — Catalogo dei finding

| ID | Severità | Titolo |
|---|---:|---|
| F-01 | P0 | Autoregistrazione anonima come dipendente operativo |
| F-02 | P0 | Ownership cliente basata su stringa e takeover degli ordini omonimi |
| F-03 | P0 | Runner prod-like in grado di eliminare volumi di uno stack reale |
| F-04 | P1 | Spring Security affetto da CVE critica e assenza gate SCA backend |
| F-05 | P1 | Variazione stock fuori dal ledger magazzino |
| F-06 | P1 | Race logout/touch può ripristinare una sessione revocata |
| F-07 | P1 | Token di account eliminato può autenticare un account ricreato |
| F-08 | P1 | Ordine incassato ma non evaso non annullabile con storno |
| F-09 | P1 | V17 può bloccare la numerazione su database popolato |
| F-10 | P1 | V15 trasforma lo storico in insoluti fittizi |
| F-11 | P1 | Unique DB case-sensitive incompatibile con lookup ignore-case |
| F-12 | P1 | PostgreSQL pubblicato e runtime eccessivamente privilegiato |
| F-13 | P1 | Retry duplicati dopo comando riuscito e refresh fallito |
| F-14 | P1 | Il cliente riceve dati interni dell'inventario |
| F-15 | P1 | Il personale non può creare ordini cliente |
| F-16 | P1 | Avvio senza profilo usa dev e bootstrap prevedibile |
| F-17 | P1 | Restore distruttivo e non failure-atomic |
| F-18 | P1 | Backup solo locale |
| F-19 | P1 | Crash e alert senza recovery/notifica |
| F-20 | P1 | Baseline web/CI non tracciata nello snapshot |
| F-20A | P1 | KPI economici includono bozze e valore inventario errato |
| F-21 | P2 | Migration testate soltanto su H2 o PostgreSQL vuoto |
| F-22 | P2 | Integrità finanziaria duplicata senza riconciliazione |
| F-23 | P2 | Errori, 401 e race nelle query frontend |
| F-24 | P2 | Liste paginate usate come verità completa |
| F-25 | P2 | Frontend monolitico e modular test incompleto |
| F-26 | P2 | Accessibilità, feedback e bozze insufficienti |
| F-27 | P2 | Contratto temporale ambiguo e anno documentale UTC |
| F-28 | P2 | Concorrenza debole su login attempt e partner |
| F-29 | P2 | Documenti simulati incompleti e scollegati |
| F-30 | P2 | Retention assente e query PII nei log |
| F-31 | P2 | Test non coprono PostgreSQL/failure/workflow critici |
| F-31A | P2 | Lifecycle account, input e audit self-service incompleti |
| F-31B | P2 | Idempotenza concorrente non deterministica |
| F-32 | P3 | Login richiede il ruolo |
| F-33 | P3 | H2 console autorizzata nel filtro |
| F-34 | P3 | Download export poco difensivo |

# Parte C — Roadmap completa e criteri di esecuzione

## 1. Obiettivo e regole di esecuzione

Questa roadmap trasforma i 37 finding della review in una sequenza implementabile. L'obiettivo non è aggiungere il maggior numero possibile di funzioni, ma arrivare progressivamente a:

1. applicazione senza P0;
2. dati, stock e denaro coerenti;
3. MVP utilizzabile da personale e clienti;
4. beta controllata;
5. produzione supportabile;
6. prodotto commercializzabile.

Regole vincolanti:

- non iniziare nuove funzioni commerciali finché i P0 non sono chiusi;
- non modificare le migration Flyway già distribuite: aggiungere migration correttive;
- ogni step deve essere piccolo, revisionabile e reversibile;
- nessun refactor “big bang” di backend o frontend;
- PostgreSQL, non H2, deve validare concorrenza, vincoli e upgrade;
- una funzione è completata solo quando possiede test negativi e criteri di accettazione;
- niente microservizi, Kubernetes, multi-tenant o event sourcing globale;
- documenti fiscali e GDPR restano non dichiarabili come conformi senza validazione professionale;
- la baseline Git va creata nel repository originario, non inizializzando il pacchetto sanificato.

## 2. Assunzioni di pianificazione

Target iniziale:

- singola azienda;
- singolo magazzino;
- negozio/attività commerciale di dimensione piccola o media;
- pagamenti inizialmente registrati manualmente;
- documenti ancora non fiscali/simulati;
- monolite modulare Spring Boot e SPA React.

Le stime sono espresse in **giorni-persona di effort**:

- **XS:** meno di 1 giorno;
- **S:** 1–3 giorni;
- **M:** 4–8 giorni;
- **L:** 9–15 giorni;
- **XL:** oltre 15 giorni, da suddividere ulteriormente.

Con due sviluppatori senior full-stack e supporto QA/DevOps parziale, la produzione controllata resta realisticamente un percorso di circa 6–9 mesi. Con una sola persona il calendario può più che raddoppiare.

## 3. Definition of Done comune

Ogni step deve rispettare tutti i punti applicabili:

- test scritto prima o insieme alla correzione;
- casi positivi, negativi e di autorizzazione;
- test PostgreSQL quando coinvolge DB, locking, idempotenza o migration;
- nessun segreto o valore ambiente inserito nel codice;
- API e documentazione aggiornate;
- audit e metriche aggiornati per operazioni sensibili;
- compatibilità con dati esistenti dimostrata;
- rollback applicativo o procedura di recovery documentata;
- nessun finding precedente riaperto;
- pull request focalizzata, senza refactor non collegati;
- criterio di accettazione verificato in ambiente pulito.

## 4. Quadro delle milestone

| Milestone | Stato atteso | Completamento indicativo |
|---|---|---:|
| Situazione attuale | Prototipo avanzato con P0 | 35–40% |
| Gate A | P0 chiusi e contenimento completato | 45% |
| Gate B | Invarianti dati/stock/denaro affidabili | 50–55% |
| Gate C | MVP operativo | 60–65% |
| Gate D | Beta controllata | 70% |
| Gate E | Produzione per scope definito | 78–82% |
| Gate F | Commercializzazione supportabile | circa 85% |

Le percentuali non sostituiscono i gate: un singolo P0 aperto impedisce comunque produzione.

# Fase 0 — Preparazione e controllo del cambiamento

Durata indicativa: 3–6 giorni-persona.

## Step 0.1 — Creare la baseline nel repository reale

**Finding:** F-20.  
**Obiettivo:** rendere la versione web e i workflow realmente versionati e riproducibili.

**Attività:**

1. eseguire secret scan sull'albero reale;
2. verificare quali file siano intenzionali;
3. creare un commit baseline nel repository originario;
4. attivare branch protection;
5. rendere obbligatori test backend, frontend e migration;
6. inserire commit SHA negli artifact.

**File/moduli:** `.github/`, `web/`, `scripts/`, `deploy/`, `docs/`.

**Dipendenze:** accesso al repository reale.  
**Rischio:** versionare file locali o segreti; deve precedere il commit una revisione manuale.  
**Test:** clone pulito, build/test completi e configurazione Compose valida.  
**Accettazione:** un nuovo sviluppatore riproduce il progetto da clone senza file esterni non documentati.  
**Effort:** M.

## Step 0.2 — Congelare scope e nuove feature

**Obiettivo:** evitare che correzioni critiche competano con nuove funzioni.

**Attività:**

1. dichiarare temporaneamente bloccate nuove feature;
2. accettare solo P0/P1, test e correzioni necessarie;
3. definire il target MVP: azienda singola, magazzino singolo, pagamenti manuali;
4. nominare un responsabile per decisioni su stock, pagamenti e storico.

**Dipendenze:** nessuna.  
**Rischio:** pressione a inserire funzioni commerciali prima delle fondamenta.  
**Test:** non applicabile; verificare il backlog.  
**Accettazione:** backlog corrente riclassificato in “stabilizzazione”, “MVP”, “dopo MVP”.  
**Effort:** S.

## Step 0.3 — Salvare evidenze e predisporre dati di test realistici

**Obiettivo:** poter verificare migration e riconciliazioni senza usare dati personali reali.

**Attività:**

1. creare fixture anonimizzate per versioni V14, V16 e V18;
2. includere ordini per ogni stato, pagamenti, resi e documenti;
3. includere collisioni case-insensitive e record storici ambigui;
4. definire volumi small/medium/large;
5. conservare checksum delle fixture.

**File/moduli:** test backend, migration Flyway, script DB.  
**Dipendenze:** Step 0.1.  
**Rischio:** fixture troppo pulite e non rappresentative.  
**Test:** caricamento fixture su PostgreSQL pulito.  
**Accettazione:** ogni scenario P0/P1 relativo ai dati è riproducibile localmente.  
**Effort:** M.

**Gate 0:** baseline riproducibile, scope congelato e fixture disponibili.

# Fase 1 — Chiusura immediata dei P0

Durata indicativa: 15–25 giorni-persona.

## Step 1.1 — Impedire la registrazione pubblica di ruoli operativi

**Finding:** F-01.  
**Obiettivo:** un anonimo non può diventare dipendente.

**Attività:**

1. scrivere test che provino il rifiuto di `EMPLOYEE`, `ADMIN` e `SUPER_ADMIN`;
2. rimuovere `role` dal DTO pubblico oppure ignorarlo e forzare `CUSTOMER`;
3. rimuovere Dipendente dalla schermata Registrazione;
4. mantenere la creazione staff soltanto nel flusso amministrativo;
5. applicare rate limit dedicato a `/api/accounts/register`;
6. aggiornare audit con actor `SELF_SERVICE`, non `Sistema`.

**File principali:**

- `web/backend/src/main/java/it/giovannidefilippo/gestionale/user/UserRequests.java`;
- `UserService.java`;
- `UserController.java`;
- `web/frontend/src/pages/AuthPage.tsx`;
- `web/frontend/nginx.conf`;
- test autorizzativi.

**Dipendenze:** Gate 0.  
**Rischio:** interrompere un onboarding interno impropriamente basato sulla registrazione pubblica.  
**Test:** matrice anonima per tutti i ruoli, rate limit e E2E di registrazione cliente.  
**Accettazione:** nessuna richiesta anonima può ottenere permessi operativi.  
**Effort:** S.

## Step 1.2 — Censire account e revocare sessioni potenzialmente emesse

**Finding:** F-01, F-07.  
**Obiettivo:** contenere il rischio già creato prima della correzione.

**Attività:**

1. estrarre un report degli account creati tramite self-service;
2. separare CUSTOMER da EMPLOYEE sospetti;
3. revocare le sessioni degli account operativi non verificati;
4. controllare audit su stock, ordini, pagamenti, resi e documenti;
5. conservare le evidenze prima di eventuali disabilitazioni.

**Dipendenze:** Step 1.1.  
**Rischio:** disabilitare un account legittimo; prevedere revisione manuale.  
**Test:** token revocati restituiscono sempre 401.  
**Accettazione:** non rimangono account operativi self-service non verificati.  
**Effort:** S, esclusa l'analisi manuale.

## Step 1.3 — Rendere stabile il subject di sessione

**Finding:** F-06, F-07.  
**Obiettivo:** una sessione deve riferirsi a un account immutabile.

**Attività:**

1. adottare l'ID già presente in `UserAccount` come subject stabile;
2. salvare una FK `account_id` nella sessione, non usare lo username come subject;
3. aggiungere `credential_version` o equivalente;
4. revocare tutte le sessioni su delete/disable/password/role change;
5. rendere “revoke wins” rispetto al touch concorrente;
6. impedire che un token sopravviva a delete→recreate.
7. definire esplicitamente la cancellazione FK delle sessioni finché il delete fisico non viene sostituito dal disable nello Step 3.3.

**File/moduli:** `user/AuthSession`, `AuthSessionService`, repository, account e nuova migration.  
**Dipendenze:** Step 0.3.  
**Rischio:** invalidazione intenzionale di tutte le sessioni esistenti.  
**Test:** concurrency PostgreSQL logout/touch e rotate/touch; delete→recreate; cambio ruolo/password.  
**Accettazione:** ogni token precedente a una mutazione credenziale restituisce 401 e non può essere riattivato.  
**Effort:** M.

## Step 1.4 — Sostituire l'ownership testuale degli ordini

**Finding:** F-02.  
**Obiettivo:** un cliente vede e modifica soltanto ordini collegati al proprio account ID.

**Attività:**

1. aggiungere `customer_account_id` e/o `partner_id` a `customer_orders`;
2. mantenere nome cliente solo come snapshot descrittivo;
3. creare associazione verificata account-partner;
4. modificare list, detail e transizioni per usare ID;
5. migrare gli ordini deterministici;
6. mettere gli ambigui in stato `UNRESOLVED`, senza auto-link per nome;
7. impedire al cliente l'accesso agli ordini unresolved.

**File/moduli:** `order`, `partner`, `user`, migration Flyway e client frontend.  
**Dipendenze:** Step 1.3.  
**Rischio:** storico non riconciliabile automaticamente.  
**Test:** omonimie, rename, account non collegato, partner duplicabili per display name, tutte le transizioni cliente.  
**Accettazione:** cambiare username o display name non cambia ownership; nessuna collisione testuale consente accesso.  
**Effort:** L.

## Step 1.5 — Rendere sicuri i runner Docker

**Finding:** F-03.  
**Obiettivo:** nessun test può eliminare volumi non creati dalla propria run.

**Attività:**

1. generare project name casuale con prefisso fisso;
2. rifiutare project name già esistente;
3. etichettare ogni risorsa con run ID;
4. inventariare le risorse create;
5. rimuovere solo quelle risorse;
6. eliminare il `down -v` preliminare;
7. rendere sicuri anche SIGINT/SIGTERM e crash.

**File:** `scripts/ci/run-prod-like-verification.sh`, `scripts/e2e/run-web-smoke.sh`, relativi test/documenti.  
**Dipendenze:** nessuna. Può procedere in parallelo agli Step 1.3–1.4.  
**Rischio:** risorse temporanee residue; è preferibile alla perdita di dati altrui.  
**Test:** stack sentinella con dato persistente, collisione nome, interruzione in ogni fase.  
**Accettazione:** il runner non invoca mai cleanup su risorse prive della propria label/run ID.  
**Effort:** S.

## Step 1.6 — Aggiornare Spring Security e rendere l'avvio fail-closed

**Finding:** F-04, F-16, F-33.  
**Obiettivo:** rimuovere la CVE applicabile e impedire avvii operativi con profilo dev.

**Attività:**

1. aggiornare a una linea Spring Boot OSS supportata;
2. verificare Spring Security non affetto da CVE-2026-22732;
3. eliminare `dev` come profilo predefinito;
4. bootstrap super-admin disabilitato per default;
5. rifiutare valori locali noti indipendentemente dal nome profilo;
6. confinare H2 console a un profilo test/local non distribuibile;
7. aggiungere un gate SCA backend iniziale.

**File:** `web/backend/pom.xml`, `application*.yml`, `SecurityConfig`, `SeedAdminInitializer`, CI.  
**Dipendenze:** Step 0.1.  
**Rischio:** regressioni Spring/Modulith/Flyway.  
**Test:** suite completa, header 2xx/4xx/5xx, jar senza profilo, prod senza secret e scan dipendenze.  
**Accettazione:** avvio senza configurazione termina; nessuna versione affetta nota resta senza waiver motivato.  
**Effort:** M.

**Gate A — Sicurezza minima**

Si può uscire dalla Fase 1 solo se:

- tutti i P0 sono chiusi;
- account/sessioni self-service operativi sono stati verificati;
- ordini autorizzati tramite ID stabile;
- runner incapace di toccare stack preesistenti;
- CVE critica risolta;
- profilo dev non può avviarsi accidentalmente.

# Fase 2 — Invarianti di dati, stock e denaro

Durata indicativa: 35–55 giorni-persona.

## Step 2.1 — Correggere V15 e V17 con nuove migration

**Finding:** F-09, F-10, F-21.  
**Obiettivo:** upgrade sicuro di database già popolati.

**Attività:**

1. non alterare V15/V17 esistenti;
2. aggiungere migration che inizializza i contatori a `max+1`;
3. introdurre stato `UNRECONCILED` per pagamenti storici non verificabili;
4. aggiungere preflight collisioni;
5. creare una procedura di riconciliazione manuale;
6. misurare lock e durata su fixture medium/large.

**Dipendenze:** Step 0.3.  
**Rischio:** storico privo di una fonte affidabile.  
**Test:** upgrade V14/V16/V18→latest su PostgreSQL; primo documento successivo; emissione concorrente.  
**Accettazione:** nessuna collisione di numerazione e nessun ordine storico presentato come insoluto senza evidenza.  
**Effort:** L.

## Step 2.2 — Rendere canonici username e codici business

**Finding:** F-11.  
**Obiettivo:** unicità coerente tra applicazione e database.

**Attività:**

1. definire forma canonical di username, product code e partner code;
2. preflight e bonifica collisioni;
3. aggiungere colonne canonical o unique index funzionali;
4. conservare separatamente il valore di presentazione;
5. tradurre i conflict DB in 409/errore dominio uniforme.

**Dipendenze:** Step 2.1 e stable account ID.  
**Rischio:** collisioni esistenti richiedono decisione manuale.  
**Test:** inserimenti sequenziali/concorrenziali con casing e spazi diversi.  
**Accettazione:** il database, non un pre-check, garantisce l'unicità canonica.  
**Effort:** M.

## Step 2.3 — Rendere autorevole il ledger di magazzino

**Finding:** F-05.  
**Obiettivo:** ogni variazione stock deve avere un movimento.

**Attività:**

1. separare DTO prodotto e DTO inventario;
2. rimuovere `quantity` dall'update anagrafica;
3. creare comando “saldo iniziale”;
4. creare rettifica positiva/negativa con causale;
5. proteggere stock riservato;
6. aggiungere report di riconciliazione;
7. identificare e classificare anomalie storiche.

**File/moduli:** product backend/frontend, inventory, migration e report.  
**Dipendenze:** Step 2.1.  
**Rischio:** saldo iniziale storico non ricostruibile.  
**Test:** `saldo iniziale + movimenti = giacenza`; concorrenza con riserve; rettifica sotto riservato rifiutata.  
**Accettazione:** nessun endpoint prodotto può variare quantità direttamente.  
**Effort:** L.

## Step 2.4 — Rendere atomica e concorrente l'idempotenza

**Finding:** F-13, F-31B.  
**Obiettivo:** retry e doppio click non duplicano mutazioni.

**Attività:**

1. definire la key per “intento utente”;
2. aggiungere hash di actor, endpoint e payload;
3. claim atomico della key;
4. stati `IN_PROGRESS/COMPLETED/FAILED_RETRYABLE`;
5. replay della response completata;
6. stessa key sul retry ambiguo;
7. nuova key solo dopo esito confermato o payload modificato;
8. introdurre TTL e cleanup.

**File/moduli:** idempotency backend, client HTTP/order, command handler frontend.  
**Dipendenze:** stable account ID.  
**Rischio:** crash tra mutazione e memorizzazione response.  
**Test:** due thread PostgreSQL, doppio click, timeout, response persa, stesso key con payload diverso.  
**Accettazione:** una sola mutazione e risposta deterministica per ogni intento.  
**Effort:** M.

## Step 2.5 — Separare esito del comando e refresh frontend

**Finding:** F-13.  
**Obiettivo:** la UI non deve indurre l'utente a ripetere un comando già eseguito.

**Attività:**

1. usare immediatamente la response del comando;
2. mostrare outcome distinto “salvato”/“refresh fallito”;
3. invalidare solo le query interessate;
4. non cancellare form/carrello finché l'esito è ambiguo;
5. disabilitare doppio submit per singola operazione.

**Dipendenze:** Step 2.4.  
**Rischio:** dati temporaneamente stale.  
**Test:** POST 200 + GET 500; timeout POST; retry; doppio click.  
**Accettazione:** l'utente sa sempre se la mutazione è stata registrata.  
**Effort:** M.

## Step 2.6 — Implementare annullamento con reversal

**Finding:** F-08, F-22.  
**Obiettivo:** annullare correttamente un ordine incassato prima dell'evasione.

**Attività:**

1. definire transizioni e stati economici;
2. registrare reversal/rimborso nel ledger;
3. rilasciare stock riservato;
4. rendere il comando idempotente;
5. conservare causale, actor e riferimento;
6. gestire fallimento dello storno senza stato impossibile.

**File/moduli:** order, payment, inventory, audit e UI operazioni ordine.  
**Dipendenze:** Step 2.3–2.5.  
**Rischio:** eventuale PSP asincrono; nell'MVP limitarsi al ledger manuale.  
**Test:** acconto, saldo, doppio annullo, errore a metà, due operatori concorrenti.  
**Accettazione:** ordine, pagamento e stock restano coerenti in ogni failure point.  
**Effort:** L.

## Step 2.7 — Aggiungere riconciliazione finanziaria

**Finding:** F-22.  
**Obiettivo:** rilevare divergenze tra ledger, saldi materializzati, resi e rimborsi.

**Attività:**

1. definire una sola fonte autorevole;
2. collegare refund a return ID;
3. creare query/job di riconciliazione;
4. esporre metriche mismatch;
5. impedire stati/timestamp incompatibili;
6. documentare procedura di correzione.

**Dipendenze:** Step 2.6.  
**Rischio:** trigger DB eccessivamente complessi; preferire invariant service + reconciliation.  
**Test:** SQL negativi, failure injection e refund concorrenti.  
**Accettazione:** ogni mismatch viene impedito o rilevato automaticamente.  
**Effort:** M.

## Step 2.8 — Separare ruoli PostgreSQL e chiudere la porta

**Finding:** F-12.  
**Obiettivo:** least privilege sul database.

**Attività:**

1. creare owner, migrator, runtime, backup e restore;
2. togliere DDL al runtime;
3. eseguire Flyway con identità dedicata;
4. rimuovere la pubblicazione DB o bindarla a loopback;
5. ruotare le credenziali;
6. aggiornare script di backup/restore.

**Dipendenze:** Step 2.1.  
**Rischio:** privilegi insufficienti interrompono startup o recovery.  
**Test:** backend funziona ma non può `CREATE/DROP/ALTER`; restore solo con ruolo dedicato.  
**Accettazione:** una compromissione applicativa non concede ownership/DDL.  
**Effort:** M.

## Step 2.9 — Introdurre la suite PostgreSQL obbligatoria

**Finding:** F-06, F-11, F-21, F-28, F-31.  
**Obiettivo:** rendere permanenti le garanzie introdotte nella Fase 2.

**Attività:**

1. Testcontainers PostgreSQL;
2. suite upgrade con fixture versionate;
3. concurrency test sessioni, idempotenza, stock e unique;
4. autorizzazione negativa per ruolo;
5. test H2 mantenuti solo come fast feedback dove utili;
6. job CI separato fast/nightly se necessario.

**Dipendenze:** Step 0.1 e tutti gli Step 2.x.  
**Rischio:** CI più lenta; non eliminare i test necessari per velocità.  
**Test:** la suite stessa.  
**Accettazione:** nessuna correzione P0/P1 dipendente dal DB è protetta soltanto da H2.  
**Effort:** L.

**Gate B — Affidabilità del dominio**

Per uscire dalla Fase 2:

- upgrade popolati riusciti;
- sessioni e unique concorrenti protetti;
- stock variabile solo tramite ledger;
- annullamento con incasso funzionante;
- idempotenza deterministica;
- mismatch finanziari rilevati;
- runtime DB senza DDL.

# Fase 3 — Completamento MVP operativo

Durata indicativa: 40–60 giorni-persona.

## Step 3.0 — Benchmark dei workflow, non copia dei prodotti

**Obiettivo:** usare gestionali maturi come riferimento prima di costruire nuovi flussi.

Per ogni nuovo flusso creare una scheda con:

1. problema operativo;
2. comportamento osservato in due o più gestionali maturi;
3. regola di dominio da preservare;
4. adattamento al monolite attuale;
5. cosa non implementare;
6. test di accettazione con un operatore reale.

Riferimenti indicativi:

- Odoo/ERPNext per ordine–acquisto–magazzino;
- Business Central per documenti registrati, numerazioni e storni;
- gestionali italiani per ergonomia e flussi PMI.

Non copiare codice, testi, asset o grafica proprietaria.

**Effort:** S iniziale, poi parte della definizione di ogni feature.

## Step 3.1 — Implementare vendita assistita da personale

**Finding:** F-15.  
**Obiettivo:** dipendente/admin crea un ordine per cliente censito o occasionale.

**Attività:**

1. separare modalità Gestione catalogo e Vendita;
2. rendere il carrello disponibile a `CREATE_ORDERS`;
3. selezionare partner tramite ID stabile;
4. supportare cliente occasionale secondo regola esplicita;
5. congelare prezzo e descrizione sulla riga;
6. audit dell'operatore;
7. impedire al cliente self-service di scegliere un altro cliente.

**Dipendenze:** Step 1.4, 2.3 e 2.4.  
**Rischio:** sconti/listini non definiti; usare il prezzo catalogo per l'MVP.  
**Test:** ordine staff per partner e walk-in; cliente solo per sé; stock insufficiente e doppio submit.  
**Accettazione:** un operatore completa una vendita senza workaround.  
**Effort:** L.

## Step 3.2 — Creare projection e dashboard separate per cliente

**Finding:** F-14.  
**Obiettivo:** non inviare dati interni di inventario al cliente.

**Attività:**

1. DTO catalogo cliente senza fisico/riservato/valore;
2. disponibilità commerciale minima;
3. dashboard cliente basata solo sui propri ordini;
4. response-shape autorizzata lato server;
5. test che verifichino assenza dei campi.

**Dipendenze:** Step 1.4.  
**Rischio:** disponibilità esatta può rivelare stock; scegliere una policy.  
**Test:** contract test per ogni ruolo.  
**Accettazione:** i campi interni non attraversano la rete verso CUSTOMER.  
**Effort:** M.

## Step 3.3 — Completare il lifecycle account

**Finding:** F-28, F-31A, F-32.  
**Obiettivo:** gestire personale e clienti senza cancellazioni distruttive.

**Attività:**

1. disable/enable;
2. change password con re-auth;
3. reset amministrativo controllato;
4. revoke all sessions;
5. eliminare il selettore ruolo dal login;
6. lockout atomico con protezione anti-DoS;
7. mantenere storico account/audit.

**Dipendenze:** Step 1.3 e 2.2.  
**Rischio:** recovery pubblico senza canale verificato; rinviarlo se necessario.  
**Test:** tutti i token precedenti, cambio ruolo, disable, login senza ruolo, concorrenza login attempt.  
**Accettazione:** uscita dipendente e compromissione credenziali sono gestibili senza delete fisico.  
**Effort:** L.

## Step 3.4 — Centralizzare query state e scadenza sessione frontend

**Finding:** F-23.  
**Obiettivo:** loading, errori, 401 e race uniformi.

**Attività:**

1. session provider;
2. hook per risorse paginate;
3. `loading/error/data/refreshing`;
4. gestione centrale 401;
5. AbortController o request sequence;
6. debounce filtri;
7. niente retry automatico di mutazioni ambigue.

**Dipendenze:** Step 2.5.  
**Rischio:** refactor trasversale; iniziare da un solo dominio.  
**Test:** 401/403/500/offline e response fuori ordine per ogni vista migrata.  
**Accettazione:** nessuna promise rejection non gestita e latest-request-wins.  
**Effort:** L.

## Step 3.5 — Decomporre `App.tsx` per vertical slice

**Finding:** F-25.  
**Obiettivo:** ridurre accoppiamento senza riscrittura.

Ordine consigliato:

1. sessione;
2. prodotti/catalogo;
3. ordini/pagamenti/resi;
4. partner;
5. documenti/report;
6. account/audit.

Per ogni slice estrarre:

- hook/query state;
- command handler;
- stato form;
- test;
- componenti di pagina.

**Dipendenze:** Step 3.4.  
**Rischio:** big-bang refactor; vietare PR che migrano più domini contemporaneamente.  
**Test:** comportamento invariato prima/dopo, più race/error test.  
**Accettazione:** nessun singolo componente orchestra l'intero workspace.  
**Effort:** XL, suddiviso in 6 step M.

## Step 3.6 — Spostare capability e aggregazioni sul server

**Finding:** F-24.  
**Obiettivo:** non dedurre azioni da liste paginate parziali.

**Attività:**

1. endpoint detail prodotto/ordine;
2. capability esplicite (`canCancel`, `canCredit`, ecc.);
3. query mirate per storico;
4. dashboard senza fallback da otto righe;
5. server resta autorità delle transizioni.

**Dipendenze:** Step 3.4–3.5.  
**Rischio:** duplicare la logica capability; centralizzarla nei service dominio.  
**Test:** nota credito/movimento fuori pagina e filtri esclusivi.  
**Accettazione:** paginazione e filtri non cambiano le azioni proposte.  
**Effort:** M.

## Step 3.7 — Correggere KPI, timezone e configurazione azienda

**Finding:** F-20A, F-27, F-29.  
**Obiettivo:** dashboard e documenti non devono presentare dati economicamente falsi.

**Attività:**

1. separare bozza, confermato, evaso, incassato, rimborsato e netto;
2. non chiamare valore inventario il prezzo vendita×quantità;
3. introdurre timezone aziendale;
4. usare `Instant/OffsetDateTime` per eventi;
5. rendere obbligatori i dati aziendali prima del documento;
6. mantenere il disclaimer “simulato”.

**Dipendenze:** Step 2.1, 2.7.  
**Rischio:** definizioni economiche/fiscali non approvate.  
**Test:** matrice stati, Capodanno Europe/Rome, DST, configurazione incompleta.  
**Accettazione:** ogni KPI ha definizione documentata e testata.  
**Effort:** L.

## Step 3.8 — Completare resi multi-riga e feedback operativo

**Finding:** F-26 e gap frontend resi.  
**Obiettivo:** rendere utilizzabile il workflow resi.

**Attività:**

1. builder multi-riga;
2. quantità residua restituibile;
3. note per return ID;
4. outcome tipizzato success/error/warning;
5. reset form solo dopo successo;
6. dettaglio ledger e rimborso.

**Dipendenze:** Step 2.6–2.7 e 3.4.  
**Rischio:** doppio reso concorrente; backend resta autorità.  
**Test:** due resi, più prodotti, quantità cumulativa e refund parziale.  
**Accettazione:** nessuna nota o quantità può riferirsi al reso sbagliato.  
**Effort:** M.

## Step 3.9 — Rendere il workspace accessibile e preservare le bozze

**Finding:** F-26, F-34.  
**Obiettivo:** UX professionale senza redesign cosmetico fine a sé stesso.

**Attività:**

1. `thead/th/caption` e label;
2. focus trap/Escape/restore focus;
3. contrasto conforme;
4. draft store per vista/entità;
5. dirty guard su cambio/chiusura tab;
6. touch target mobile;
7. parser download difensivo e test WebKit.

**Dipendenze:** Step 3.5.  
**Rischio:** regressioni visuali.  
**Test:** axe, tastiera, screen-reader smoke, viewport 390/768/1440, Chromium/Firefox/WebKit.  
**Accettazione:** nessuna violazione critica automatica; workflow principali completabili da tastiera.  
**Effort:** L.

## Step 3.10 — Gate E2E dell'MVP

**Finding:** F-31.  
**Obiettivo:** validare verticalmente i flussi principali.

Scenari obbligatori:

1. super-admin crea dipendente;
2. dipendente crea cliente e vendita;
3. carico/rettifica→ordine→riserva→evasione;
4. acconto→annullo→storno;
5. reso multi-riga→ricezione→rimborso;
6. sessione scaduta durante query;
7. autorizzazione cliente negativa;
8. upgrade DB popolato.

**Dipendenze:** tutti gli Step 3.x.  
**Rischio:** E2E fragili; usare dati effimeri e selettori semantici.  
**Accettazione:** scenari verdi su PostgreSQL e Chromium; almeno auth/accessibilità anche su WebKit/Firefox.  
**Effort:** L.

**Gate C — MVP**

L'MVP è raggiunto soltanto quando:

- un dipendente autorizzato gestisce vendita, stock e annullo senza workaround;
- il cliente vede solo dati propri;
- account e sessioni hanno lifecycle governato;
- UI distingue sempre command e refresh;
- KPI non includono bozze come ricavi;
- workflow critici E2E e PostgreSQL sono verdi.

# Fase 4 — Beta controllata e funzioni commerciali essenziali

Durata indicativa: 50–80 giorni-persona.

## Step 4.1 — Modellare ordini fornitore

**Obiettivo:** coprire approvvigionamento reale.

Stati minimi:

- `DRAFT`;
- `SENT`;
- `PARTIALLY_RECEIVED`;
- `RECEIVED`;
- `CANCELED`.

**Attività:** supplier ID, righe snapshot, quantità ordinata/ricevuta, date previste, causale e audit.  
**Dipendenze:** Gate C e benchmark Step 3.0.  
**Rischio:** introdurre prematuramente fatture passive; non fanno parte del primo step.  
**Test:** ricezione parziale, annullo residuo, concorrenza e idempotenza.  
**Accettazione:** un acquisto non richiede un carico manuale scollegato.  
**Effort:** XL, da dividere tra backend, frontend e test.

## Step 4.2 — Ricezione merce e costo

**Obiettivo:** collegare ordine fornitore, movimento e costo prodotto.

**Attività:**

1. ricezione crea movimenti stock;
2. storico quantità ricevute;
3. ultimo costo e/o costo medio con regola esplicita;
4. differenza prezzo/quantità;
5. KPI margine solo dopo disponibilità del costo.

**Dipendenze:** Step 4.1 e ledger Step 2.3.  
**Rischio:** scelta contabile del costo; richiede validazione funzionale.  
**Test:** ricezioni parziali multiple, reso a fornitore futuro, arrotondamenti.  
**Accettazione:** valore/margine non deriva più dal solo prezzo di vendita.  
**Effort:** L.

## Step 4.3 — Inventario fisico e rettifiche approvate

**Obiettivo:** governare conteggi e differenze.

**Attività:**

1. sessione inventario;
2. snapshot teorico;
3. conteggio operatore;
4. differenza;
5. approvazione;
6. movimento di rettifica;
7. report e audit.

**Dipendenze:** Step 2.3.  
**Rischio:** inventario aperto mentre avvengono vendite; definire freeze o compensazione.  
**Test:** stock concorrente, doppia approvazione, rettifica sotto riservato.  
**Accettazione:** ogni differenza è approvata e tracciata.  
**Effort:** L.

## Step 4.4 — Scorta minima e proposte di riordino

**Obiettivo:** generare valore operativo senza AI prematura.

**Attività:** scorta minima, quantità obiettivo, lead time opzionale, proposta spiegabile e conversione in ordine fornitore.  
**Dipendenze:** Step 4.1–4.3.  
**Rischio:** suggerimenti errati con dati incompleti; mostrare sempre la formula.  
**Test:** stock disponibile/riservato, ordini già aperti e lead time.  
**Accettazione:** nessun ordine viene inviato automaticamente.  
**Effort:** M.

## Step 4.5 — Importazione con staging

**Obiettivo:** migrare prodotti e partner senza corrompere dati.

**Attività:**

1. template CSV;
2. upload in staging;
3. validazione riga per riga;
4. dry-run;
5. riepilogo errori;
6. commit atomico o batch recuperabile;
7. idempotenza import.

**Dipendenze:** canonical identity Step 2.2.  
**Rischio:** import usato per aggirare invarianti.  
**Test:** duplicati, casing, righe invalide, file grande e retry.  
**Accettazione:** nessuna riga invalida entra nelle tabelle di dominio.  
**Effort:** L.

## Step 4.6 — Performance su volumi target

**Finding:** F-24 e gap indici/lookup.  
**Obiettivo:** definire e rispettare budget misurabili.

**Attività:**

1. concordare volumi target;
2. dataset sintetici 100k/1M dove sensato;
3. `EXPLAIN ANALYZE`;
4. indici dimostrati;
5. lookup server-side;
6. paginazione/virtualizzazione report;
7. budget p95 e memoria browser.

**Dipendenze:** MVP stabile.  
**Rischio:** ottimizzazione prematura; nessun indice senza piano misurato.  
**Test:** load e browser profiling.  
**Accettazione:** budget p95 approvati per ricerca, ordini e report.  
**Effort:** M.

## Step 4.7 — Backup off-site e restore parallelo

**Finding:** F-17, F-18.  
**Obiettivo:** recuperare il servizio senza distruggere il DB originale.

**Attività:**

1. copia off-site cifrata;
2. versioning/immutabilità;
3. credenziali recovery separate;
4. restore su DB/cluster nuovo;
5. test semantico e avvio applicativo;
6. cutover controllato;
7. monitor freshness indipendente;
8. sostituire lock directory con `flock`.

**Dipendenze:** Step 2.8.  
**Rischio:** chiavi di cifratura o account recovery indisponibili.  
**Test:** perdita host, archivio corrotto, failure a metà restore, rotazione chiavi.  
**Accettazione:** DB originale rimane disponibile finché il clone non supera gli smoke test.  
**Effort:** L.

## Step 4.8 — Restart, alert delivery e log centralizzati

**Finding:** F-19, F-30.  
**Obiettivo:** rilevare e recuperare guasti operativi.

**Attività:**

1. restart/backoff;
2. limiti CPU/memoria/PID;
3. Alertmanager o equivalente;
4. destinatario ed escalation;
5. log centralizzati;
6. non loggare query string/PII;
7. runbook per alert.

**Dipendenze:** ambiente beta.  
**Rischio:** crash loop e alert fatigue.  
**Test:** kill, OOM, DB down, backup stale e marker sensibile nei log.  
**Accettazione:** ogni alert critico arriva a un destinatario e possiede un runbook.  
**Effort:** M.

**Gate D — Beta controllata**

Condizioni:

- gruppo utenti limitato e supportato;
- acquisti e inventario senza correzioni manuali fuori sistema;
- backup remoto e restore dimostrato;
- alert consegnati;
- performance entro target;
- dati beta separati dalla produzione futura;
- nessun P0/P1 aperto.

# Fase 5 — Hardening produttivo

Durata indicativa: 35–55 giorni-persona.

## Step 5.1 — Supply-chain security

**Finding:** F-04, F-20, F-31.  
**Attività:** SCA backend/frontend, image scan, SBOM, secret scan, CodeQL, action/base image pin a digest, processo waiver con owner e scadenza.  
**Dipendenze:** baseline Git e suite PostgreSQL.  
**Test:** dipendenza vulnerabile fixture deve bloccare la pipeline.  
**Accettazione:** nessun artifact è rilasciabile senza scansioni e provenienza.  
**Effort:** M.

## Step 5.2 — Artifact immutabile, promozione e rollback

**Obiettivo:** costruire una volta e promuovere lo stesso digest.

**Attività:** registry, firma, provenance, staging, approval, deploy per digest, migration expand/contract e rollback drill.  
**Dipendenze:** Step 5.1.  
**Rischio:** rollback binario incompatibile con migration distruttive.  
**Test:** staging→produzione simulata e rollback dello stesso artifact.  
**Accettazione:** ogni release è riconducibile a commit, SBOM, test e migration.  
**Effort:** L.

## Step 5.3 — Security hardening e pentest

**Obiettivo:** validazione indipendente dell'applicazione.

**Attività:**

1. threat model aggiornato;
2. matrice autorizzativa completa;
3. step-up per rimborsi/rettifiche/documenti sopra soglia;
4. brute-force e rate limiting multilivello;
5. DAST;
6. pentest esterno;
7. remediation e retest.

**Dipendenze:** feature freeze candidato produzione.  
**Rischio:** trovare problemi tardivi; prevedere buffer.  
**Accettazione:** nessun finding critico/alto aperto senza decisione formale.  
**Effort:** L più costo esterno.

## Step 5.4 — Load, concorrenza, failure e DR

**Obiettivo:** provare SLO, RPO e RTO.

**Attività:** load test, race test, kill/latency DB, saturazione, restore, alert, rollback e runbook exercise.  
**Dipendenze:** Step 4.7–4.8 e 5.2.  
**Rischio:** test distruttivi; ambiente isolato con sentinella obbligatorio.  
**Accettazione:** SLO p95/error rate e RPO/RTO approvati e misurati.  
**Effort:** L.

## Step 5.5 — Privacy e retention

**Finding:** F-30, F-31A.  
**Obiettivo:** governare lifecycle di account, audit, log, idempotenza e backup.

**Attività:** data inventory, finalità, retention, legal hold, cleanup, export/correzione/cancellazione dove applicabili, revisione professionale.  
**Dipendenze:** stable IDs e policy aziendale.  
**Rischio:** cancellare dati soggetti a obblighi di conservazione.  
**Test:** dry-run, legal hold, restore dopo cleanup e access control.  
**Accettazione:** policy approvata e tradotta in job verificabili.  
**Effort:** L più consulenza.

## Step 5.6 — Decidere il perimetro pagamenti

**Obiettivo:** evitare una falsa integrazione.

Opzione A — MVP manuale:

- il sistema registra incassi comunicati dall'operatore;
- UI e documenti lo dichiarano;
- nessuna affermazione di esecuzione del pagamento.

Opzione B — PSP reale:

- adapter provider;
- autorizzazione/capture;
- webhook firmati;
- idempotenza;
- stati pending/failure;
- reconciliation;
- refund asincrono.

**Dipendenze:** Step 2.6–2.7.  
**Rischio:** callback duplicate e divergenza provider/ledger.  
**Accettazione:** il perimetro scelto è documentato e testato end-to-end.  
**Effort:** S per formalizzare manuale; XL per PSP.

**Gate E — Produzione**

Condizioni:

- nessun P0/P1;
- pentest chiuso;
- artifact firmato e rollback provato;
- restore remoto e DR drill riusciti;
- alert/on-call attivi;
- SLO, RPO e RTO approvati;
- policy privacy/retention approvata;
- perimetro pagamenti dichiarato;
- documenti ancora chiaramente simulati se non validati.

# Fase 6 — Commercializzazione

Durata variabile: 40–80+ giorni-persona.

## Step 6.1 — Packaging, onboarding e configurazione guidata

**Obiettivo:** installazione ripetibile senza interventi sul codice.

**Attività:** wizard azienda, ambiente, ruoli iniziali, import, health, diagnostica redatta, aggiornamenti e rollback.  
**Accettazione:** un ambiente nuovo viene installato seguendo solo documentazione pubblicabile.  
**Effort:** L.

## Step 6.2 — Supportabilità e policy di upgrade

**Attività:** versioning, changelog, support bundle senza PII, compatibilità DB, finestra manutenzione, policy deprecazioni e matrice versioni supportate.  
**Accettazione:** ogni release ha install, upgrade e rollback testati.  
**Effort:** L.

## Step 6.3 — Permessi granulari e approvazioni

**Obiettivo:** superare il ruolo Dipendente onnipotente senza costruire un motore RBAC eccessivo.

Template iniziali:

- vendite;
- magazzino;
- responsabile;
- amministrazione account;
- sola lettura/report.

Approvazioni per:

- rettifiche sopra soglia;
- rimborsi sopra soglia;
- sconti sopra soglia;
- emissione/storno documenti.

**Dipendenze:** uso reale dei ruoli in beta.  
**Accettazione:** matrice automatica e dual control per le operazioni selezionate.  
**Effort:** L.

## Step 6.4 — Documenti fiscali tramite provider/consulenza

**Obiettivo:** trasformare la simulazione solo dopo validazione.

**Attività:** requisiti professionali, provider esterno, sandbox, invio/ricezione esiti, conservazione, retry e riconciliazione.  
**Rischio:** dichiarare conformità senza sign-off.  
**Accettazione:** validazione fiscale/legale indipendente e test del provider.  
**Effort:** XL.

## Step 6.5 — Integrazioni e funzioni vendibili

Ordine consigliato:

1. import/export guidato;
2. notifiche operative;
3. ricerca globale;
4. barcode;
5. e-commerce/corrieri;
6. contabilità/provider fiscali;
7. API/webhook versionati.

Ogni integrazione deve avere retry, idempotenza, audit e gestione errori.

**Effort:** XL, una integrazione per release.

**Gate F — Prodotto commercializzabile**

- installazione e upgrade ripetibili;
- supporto e diagnostica;
- scope contrattuale coerente;
- permessi adeguati ai profili reali;
- eventuali integrazioni fiscali validate;
- roadmap e SLA sostenibili;
- nessuna dipendenza da correzioni manuali del database.

# Fase 7 — Evoluzioni future, non bloccanti

Implementare solo in presenza di domanda commerciale:

1. multi-magazzino e trasferimenti;
2. lotti, seriali e barcode avanzato;
3. garanzie e servizi extra come righe servizio versionate;
4. portale fornitore/cliente;
5. automazioni e suggerimenti avanzati;
6. multi-azienda;
7. multi-tenant solo dopo un threat model dedicato.

Non usare queste evoluzioni per ritardare i gate precedenti.

# Appendice A — Sequenza consigliata delle prime quattro iterazioni

## Iterazione 1 — Contenimento, 1–2 settimane

1. Step 0.1–0.3;
2. Step 1.1;
3. Step 1.2;
4. Step 1.5;
5. test regressione P0.

**Output:** nessuna nuova registrazione staff e runner sicuri.

## Iterazione 2 — Identità, 2–3 settimane

1. Step 1.3;
2. Step 1.4;
3. fixture e migration;
4. authorization test PostgreSQL.

**Output:** sessioni e ordini legati a ID stabili.

## Iterazione 3 — Framework e dati, 2–3 settimane

1. Step 1.6;
2. Step 2.1;
3. Step 2.2;
4. primi gate PostgreSQL.

**Output:** avvio fail-closed, CVE risolta e upgrade popolati ripetibili.

## Iterazione 4 — Stock e idempotenza, 3–4 settimane

1. Step 2.3;
2. Step 2.4;
3. Step 2.5;
4. Step 2.9 relativo;
5. riconciliazione iniziale.

**Output:** stock autorevole e retry frontend sicuri.

Non iniziare vendita assistita prima di aver completato identità, stock e idempotenza.

# Appendice B — Tracciabilità finding → step

| Finding | Step principali |
|---|---|
| F-01 | 1.1, 1.2 |
| F-02 | 1.4 |
| F-03 | 1.5 |
| F-04 | 1.6, 5.1 |
| F-05 | 2.3 |
| F-06 | 1.3, 2.9 |
| F-07 | 1.2, 1.3 |
| F-08 | 2.6 |
| F-09 | 2.1 |
| F-10 | 2.1 |
| F-11 | 2.2 |
| F-12 | 2.8 |
| F-13 | 2.4, 2.5 |
| F-14 | 3.2 |
| F-15 | 3.1 |
| F-16 | 1.6 |
| F-17 | 4.7 |
| F-18 | 4.7 |
| F-19 | 4.8 |
| F-20 | 0.1 |
| F-20A | 3.7, 4.2 |
| F-21 | 0.3, 2.1, 2.9 |
| F-22 | 2.6, 2.7 |
| F-23 | 3.4 |
| F-24 | 3.6, 4.6 |
| F-25 | 3.5 |
| F-26 | 3.8, 3.9 |
| F-27 | 3.7 |
| F-28 | 2.2, 3.3 |
| F-29 | 3.7, 6.4 |
| F-30 | 2.4, 4.8, 5.5 |
| F-31 | 2.9, 3.10 |
| F-31A | 1.1, 3.3, 5.5 |
| F-31B | 2.4 |
| F-32 | 3.3 |
| F-33 | 1.6 |
| F-34 | 3.9 |

# Appendice C — Decisioni da prendere prima dell'implementazione

Non bloccano la consegna della roadmap, ma devono essere decise nel relativo step:

1. la registrazione cliente resta pubblica o avviene solo su invito?
2. come associare un account cliente a un partner esistente?
3. come classificare ordini storici senza ownership certa?
4. quale stato usare per pagamenti storici non riconciliati?
5. quale politica applicare al riuso username?
6. quale formula definisce costo e valore inventario?
7. il cliente vede “disponibile/non disponibile” o una quantità?
8. chi può approvare rettifiche, rimborsi e sconti?
9. quale RPO/RTO è economicamente sostenibile?
10. pagamenti manuali o integrazione PSP?
11. i documenti restano simulati fino a quale milestone?
12. quali volumi e browser devono essere supportati?

# Appendice D — Primo punto di partenza

Il primo lavoro da implementare è **Step 1.1 — impedire la registrazione pubblica di ruoli operativi**, preceduto soltanto dalla baseline e dai test di regressione minimi della Fase 0.

La prima release tecnica deve contenere esclusivamente:

1. registrazione pubblica limitata a CUSTOMER;
2. rimozione del ruolo Dipendente dalla UI pubblica;
3. test autorizzativi negativi;
4. rate limit della registrazione;
5. audit self-service corretto;
6. procedura di censimento e revoca.

Non includere nella stessa release refactor di `App.tsx`, nuove funzioni di magazzino o redesign grafico.
