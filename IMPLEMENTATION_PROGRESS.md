# Implementation progress

## Stato generale

- Current step: 0.1
- Last completed step: none
- Last update: 2026-07-27
- Open P0: F-01, F-02, F-03
- Open P1: F-04, F-05, F-06, F-07, F-08, F-09, F-10, F-11, F-12, F-13, F-14, F-15, F-16, F-17, F-18, F-19, F-20, F-20A
- Open P2: F-21, F-22, F-23, F-24, F-25, F-26, F-27, F-28, F-29, F-30, F-31, F-31A, F-31B
- Open P3: F-32, F-33, F-34

## Step

| Step | Stato | Commit/patch | Test | Note |
|---|---|---|---|---|
| 0.1 | PARTIAL | `bd06256` baseline; `7eb650f` allineamento test CI | clone pulito: backend 154/154; frontend 32/32; legacy 41/41; build e config Compose OK | Baseline locale e branch protection applicate; pubblicazione remota e status check obbligatori ancora da completare |
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

## Blocker

- I commit locali non sono stati pubblicati: il push non era compreso nell'autorizzazione e resta vietato senza consenso esplicito.
- La branch protection remota e attiva su `main`, ma i required status check non sono selezionabili finche il workflow non viene pubblicato e non produce i relativi contesti almeno una volta.

## Verifiche Step 0.1

- Branch locale verificata: `miglioramenti-gestionale`.
- Scansione Gitleaks cronologia: 3 commit, nessuna rilevazione.
- Scansione Gitleaks baseline candidata: circa 1,69 MB, nessuna rilevazione.
- File sensibili o artefatti locali candidati: nessuno; sono presenti soltanto template `.env.example` e migrazioni Flyway intenzionali.
- Commit baseline creato nel repository reale: `bd06256cf57dd5b97de75caeff749892f55eb8b7`.
- Contratto del test CI allineato al nome artifact con SHA: commit `7eb650f`.
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

## Rischi residui

- F-20 e corretto nella baseline locale, ma resta formalmente aperto finche i commit non vengono pubblicati sul remoto e i check CI non diventano obbligatori.
- I risultati del clone pulito non sostituiscono una CI eseguita sul repository remoto.
- La branch protection non impone ancora i contesti `Backend Spring Boot`, `Frontend React`, `Prod-like Docker stack` e `Quality gate`.
- Non eseguire i runner prod-like/E2E prima del completamento dello Step 1.5.
