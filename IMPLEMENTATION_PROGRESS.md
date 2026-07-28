# Implementation progress

## Stato generale

- Current step: none; awaiting authorization for Step 0.2
- Last completed step: 0.1
- Last update: 2026-07-27
- Open P0: F-01, F-02, F-03
- Open P1: F-04, F-05, F-06, F-07, F-08, F-09, F-10, F-11, F-12, F-13, F-14, F-15, F-16, F-17, F-18, F-19, F-20A
- Open P2: F-21, F-22, F-23, F-24, F-25, F-26, F-27, F-28, F-29, F-30, F-31, F-31A, F-31B
- Open P3: F-32, F-33, F-34

## Step

| Step | Stato | Commit/patch | Test | Note |
|---|---|---|---|---|
| 0.1 | COMPLETED | `9f9e23a` baseline; `945fcd0` CI; `62a8d14` security; `fa52524` gate finali | clone pulito: backend 154/154; frontend 32/32; legacy 41/41; CI e security remote verdi | PR #2 aperta in draft; `main` protetta con backend, frontend, migration e quality gate obbligatori |
| 0.2 | PENDING | - | - | Congelare scope e nuove feature |
| 0.3 | PENDING | - | - | Fixture anonimizzate e dati di test realistici |
| 1.1 | PENDING | - | - | Registrazione pubblica limitata a CUSTOMER |
| 1.2 | PENDING | - | - | Censimento account e revoca sessioni |
| 1.3 | PENDING | - | - | Subject di sessione stabile |
| 1.4 | PENDING | - | - | Ownership ordini basata su ID |
| 1.5 | PENDING | - | - | Runner Docker sicuri |
| 1.6 | PENDING | - | - | Spring Security aggiornato e avvio fail-closed |
| 2.1 | PENDING | - | - | Migration correttive V15 e V17 |
| 2.2 | PENDING | - | - | Identita canoniche e unique coerenti |
| 2.3 | PENDING | - | - | Ledger magazzino autorevole |
| 2.4 | PENDING | - | - | Idempotenza atomica e concorrente |
| 2.5 | PENDING | - | - | Separazione command outcome e refresh frontend |
| 2.6 | PENDING | - | - | Annullamento ordine con reversal |
| 2.7 | PENDING | - | - | Riconciliazione finanziaria |
| 2.8 | PENDING | - | - | Ruoli PostgreSQL least privilege |
| 2.9 | PENDING | - | - | Suite PostgreSQL obbligatoria |
| 3.0 | PENDING | - | - | Benchmark dei workflow |
| 3.1 | PENDING | - | - | Vendita assistita da personale |
| 3.2 | PENDING | - | - | Projection e dashboard cliente |
| 3.3 | PENDING | - | - | Lifecycle account completo |
| 3.4 | PENDING | - | - | Query state e sessione frontend centralizzati |
| 3.5 | PENDING | - | - | Decomposizione App.tsx per vertical slice |
| 3.6 | PENDING | - | - | Capability e aggregazioni server-side |
| 3.7 | PENDING | - | - | KPI, timezone e configurazione azienda |
| 3.8 | PENDING | - | - | Resi multi-riga e feedback operativo |
| 3.9 | PENDING | - | - | Accessibilita e preservazione bozze |
| 3.10 | PENDING | - | - | Gate E2E MVP |
| 4.1 | PENDING | - | - | Ordini fornitore |
| 4.2 | PENDING | - | - | Ricezione merce e costo |
| 4.3 | PENDING | - | - | Inventario fisico e rettifiche approvate |
| 4.4 | PENDING | - | - | Scorta minima e proposte riordino |
| 4.5 | PENDING | - | - | Importazione con staging |
| 4.6 | PENDING | - | - | Performance su volumi target |
| 4.7 | PENDING | - | - | Backup off-site e restore parallelo |
| 4.8 | PENDING | - | - | Restart, alert delivery e log centralizzati |
| 5.1 | PENDING | - | - | Supply-chain security |
| 5.2 | PENDING | - | - | Artifact immutabile, promozione e rollback |
| 5.3 | PENDING | - | - | Security hardening e pentest |
| 5.4 | PENDING | - | - | Load, concorrenza, failure e DR |
| 5.5 | PENDING | - | - | Privacy e retention |
| 5.6 | PENDING | - | - | Decisione perimetro pagamenti |
| 6.1 | PENDING | - | - | Packaging, onboarding e configurazione guidata |
| 6.2 | PENDING | - | - | Supportabilita e policy upgrade |
| 6.3 | PENDING | - | - | Permessi granulari e approvazioni |
| 6.4 | PENDING | - | - | Documenti fiscali tramite provider e consulenza |
| 6.5 | PENDING | - | - | Integrazioni e funzioni vendibili |

## Decisioni approvate

- `MASTER_SOURCE_AGENTE_AI.md` e la fonte operativa vincolante.
- Target produttivo: monolite modulare Spring Boot + React con PostgreSQL.
- La cartella `src/` e legacy e non riceve nuove funzioni salvo step esplicito.
- Un solo step viene eseguito per turno; avanzamento automatico disabilitato.
- Nessun deploy, commit, push, PR o operazione distruttiva senza autorizzazione esplicita.
- Nessuna nuova funzione commerciale prima della chiusura dei P0 e dei gate dipendenti.
- Il 2026-07-27 e stato autorizzato il commit della baseline e la configurazione della branch protection su `main`.
- Il 2026-07-27 e stato autorizzato il push della branch `miglioramenti-gestionale`.
- Il 2026-07-27 e stata autorizzata l'apertura della pull request e il completamento remoto dello Step 0.1.

## Blocker

- Nessun blocker aperto per lo Step 0.1.
- Lo Step 0.2 non e iniziato e richiede autorizzazione esplicita.

## Verifiche Step 0.1

- Branch locale verificata: `miglioramenti-gestionale`.
- Scansione Gitleaks cronologia: 3 commit, nessuna rilevazione.
- Scansione Gitleaks baseline candidata: circa 1,69 MB, nessuna rilevazione.
- File sensibili o artefatti locali candidati: nessuno; sono presenti soltanto template `.env.example` e migrazioni Flyway intenzionali.
- Baseline pubblicata su `origin/miglioramenti-gestionale`: commit `9f9e23a`.
- Workflow CI pubblicato tramite sessione GitHub autenticata: commit `945fcd0`.
- Workflow security pubblicato tramite sessione GitHub autenticata: commit `62a8d14`.
- Workflow CI corretto per separare il gate migrazioni e sospendere l'esecuzione prod-like fino allo Step 1.5: commit `fa52524`.
- I commit locali originali sono preservati nelle branch `baseline-pre-publication-20260727` e `miglioramenti-gestionale-pre-publication`.
- Clone pulito creato dal repository reale e verificato senza file esterni non documentati.
- Backend nel clone pulito: `mvn -B verify`, 154 test passati, JAR generato.
- Frontend nel clone pulito: `npm ci`; 32 test passati; typecheck applicativo ed E2E passati; build Vite passata.
- Legacy nel clone pulito: `mvn -B -f pom-legacy.xml test`, 41 test passati.
- Flyway: 18 migrazioni validate e applicate su schema H2 pulito durante i test.
- Compose locale e prod-like nel clone pulito: configurazioni valide con placeholder non sensibili.
- Stack prod-like ed E2E non avviati per rispettare la stop condition precedente allo Step 1.5.
- Runtime Maven verificato: Java 23.0.1 con compilazione `release 17`; il test della baseline passa anche su questo runtime.
- Artifact CI backend, frontend e diagnostica prod-like includono `github.sha`.
- Branch protection remota attiva su `main`: pull request obbligatoria, una approvazione, dismiss stale approvals, approvazione dell'ultimo push, conversazioni risolte, nessun bypass amministratore, force push e cancellazione disabilitati.
- Pull request draft aperta: `#2`, branch `miglioramenti-gestionale` verso `main`.
- Branch protection verificata con branch aggiornata obbligatoria e check richiesti: `Backend Spring Boot`, `Frontend React`, `Database migrations`, `Quality gate`.
- Run CI remoto `30287620556`: repository hygiene, backend, frontend, migrazioni e quality gate completati con successo; prod-like correttamente saltato.
- Run security remoto `30287620519`: Gitleaks, dependency review, npm audit, inventario dipendenze backend e CodeQL Java/TypeScript completati con successo.
- Dependency Graph del repository abilitato per rendere operativo il dependency review.

## Rischi residui

- F-20 e chiuso: baseline reale, clone pulito, CI remota e protezione branch sono verificati.
- Il gate migrazioni dello Step 0.1 usa H2 in modalita PostgreSQL; la suite PostgreSQL autorevole resta pianificata nello Step 2.9.
- I controlli security sono verdi ma non sono inclusi tra i quattro status check obbligatori richiesti dallo Step 0.1.
- Non eseguire i runner prod-like/E2E prima del completamento dello Step 1.5.
