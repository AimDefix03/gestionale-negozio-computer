# Roadmap

Stato verificato il 2026-07-16.

## Fase 0 - Baseline

- Audit repository: completato.
- Test root Swing: 41 test passati.
- Test backend web: 54 test passati.
- Build frontend: completata.
- Documentazione governance: completata nello Step 1.

## Fase 1 - Stabilizzazione del prodotto

1. Governance documentale e decisioni architetturali.
2. PostgreSQL locale con Docker Compose.
3. Flyway per migrazioni versionate.
4. Separazione profili `dev` e `test`.
5. Rimozione di `ddl-auto: update` dalla base web.
6. Consolidamento Git: rendere tracciata la parte `web/` e chiarire rapporto legacy/web.

## Fase 2 - Fondamenta applicative

7. Formato errori API stabile con codice errore applicativo: completato.
8. Paginazione e filtri server-side per prodotti, ordini, audit e movimenti: completato lato API e collegato ai controlli frontend principali.
9. Correlation ID e logging piu strutturato: completato per header, errori API, frontend e log richieste.
10. Revisione DTO per rimuovere dati derivabili dal client, come actor e role nelle richieste operative: completato per magazzino, ordini e documenti.
11. Test di autorizzazione per ogni modulo: completato per prodotti, magazzino, ordini, documenti, account e audit.
12. Permessi granulari sulle transizioni ordine: completato con `CONFIRM_ORDERS`, `FULFILL_ORDERS` e `CANCEL_ORDERS`.

## Fase 3 - Sicurezza professionale

13. Sessioni persistenti con token hashati e revoca logout: completato con migrazione `auth_sessions`.
14. Account demo separati per profilo `dev`: completato con bootstrap super admin configurabile, disattivato di default in `prod` e bloccato se usa credenziali locali.
15. Disabilitazione H2 console fuori da `dev`: completato nelle configurazioni web correnti.
16. Audit piu dettagliato su operazioni critiche: completato con `requestId`, origine richiesta, tipo entita e riepiloghi di modifica prodotto.
17. Valutazione CSRF/cookie o header token in base al deployment reale: completata con ADR 0004, token in memoria/header custom, rotazione al rinnovo, timeout assoluto e inattivita; cookie HttpOnly rinviato a una migrazione atomica con CSRF e TLS.
18. Protezione brute force applicativa e infrastrutturale: completata con `login_attempts`, cleanup programmato, rate limiting Nginx per IP, backend prod-like su loopback e test automatico del contratto `429`.
19. Idempotenza sulle operazioni critiche: completato per ordini, documenti simulati e movimenti magazzino tramite `Idempotency-Key`.
20. Osservabilita applicativa di base: completata con endpoint protetto `/api/system/status`, metriche sessione/login, stato database, runtime, audit warning/critical ed errori API recenti.

## Fase 4 - Dominio operativo

19. Catalogo con regole di eliminazione piu sicure.
20. Magazzino con concorrenza, optimistic locking, stock riservato e movimenti obbligatori: completato per aggiornamenti stock, scarichi concorrenti, storico movimenti e disponibilita vendibile.
21. Ordini con stati e transizioni controllate: completato con bozza, conferma, evasione, annullamento, audit, permessi separati, prenotazione stock su conferma e scarico fisico su evasione.
22. Documenti simulati con workflow esplicito, disclaimer invariabile e snapshot anagrafica cliente: completato.
23. Anagrafiche clienti e fornitori: completato con modello dati, API protette, permessi dedicati, pagina frontend e collegamento opzionale agli ordini.

## Fase 5 - Frontend gestionale

24. Scomposizione `App.tsx` in pagine, layout e componenti: completata per viste, shell workspace, navigazione, componenti condivisi e hook di interazione.
25. Separazione client API per dominio con trasporto condiviso: completata per catalogo, anagrafiche, account, magazzino, ordini, documenti, audit, dashboard e monitoraggio.
26. Navigazione stile workspace/tab piu coerente.
27. Tabelle professionali con stati, filtri, paginazione, vuoto ed errori: completato per catalogo prodotti, ordini, movimenti e audit.
28. Accessibilita base e focus management.
29. Design system interno con componenti riutilizzabili.
30. Layout fluido per notebook e tablet: completato rimuovendo la larghezza minima globale e introducendo breakpoint progressivi.

Test frontend automatici: completati con 32 casi Vitest/React Testing Library su componenti, trasporto HTTP e flussi API critici, oltre a tre smoke test Playwright sullo stack reale.

## Fase 6 - Preparazione al rilascio

31. Docker Compose completo.
32. README operativo aggiornato.
33. Checklist release e configurazione ambiente: completato con `docs/CONFIGURATION.md`, `.env.example`, `docs/RELEASE_CHECKLIST.md` e `docs/CHANGELOG.md`.
34. CI con test backend/frontend: completato con GitHub Actions, hygiene repository, build backend e build frontend.
35. Dati seed controllati.
36. Ambiente demo protetto.

## Prossimo step tecnico raccomandato

Step completato: CI con GitHub Actions. Sono stati aggiunti controlli automatici per hygiene repository, backend Spring Boot, frontend React e quality gate.

Step completato: scansioni di sicurezza automatiche. Sono stati aggiunti security workflow, Dependabot, audit npm, dependency review, inventory Maven e CodeQL.

Step completato: Docker prod-like. Sono stati aggiunti Dockerfile backend/frontend, Nginx proxy, compose completo con PostgreSQL reale e controllo CI dello stack.

Step completato: backup/restore PostgreSQL. Sono stati aggiunti backup atomici, checksum obbligatori, lock, retention, controllo freschezza, verifica automatica su database temporaneo e restore drill dell'ultimo backup reale.

Step completato: osservabilita amministrativa. Sono stati aggiunti endpoint e schermata Monitoraggio per stato sistema, database, sessioni, audit sensibile ed errori API recenti.

Step completato: test frontend automatici. La suite copre 32 casi su autenticazione, registrazione, sessione, catalogo, ordini, pagamenti, resi, configurazione aziendale, report, dashboard ed errori API ed e eseguita dalla CI.

Step completato: smoke test browser end-to-end. Playwright verifica login negativo, autenticazione super admin, navigazione, creazione prodotto, registrazione cliente, carrello e conferma ordine contro PostgreSQL, Spring Boot e Nginx reali.

Step completato: modello pagamenti strutturato. Ogni ordine persiste metodo controllato, stato, importi, valuta e timestamp in `order_payments`; il checkout permette di scegliere il metodo e la vista ordini distingue stato ordine e stato pagamento.

Step completato: hardening architetturale. La build root e ora web-first, Swing usa un POM legacy dedicato, Spring Modulith verifica gli otto moduli business e la precedente dipendenza ciclica tra catalogo e ordini e stata rimossa tramite una porta applicativa.

Step completato: pagamenti e resi operativi. Incassi e rimborsi producono un ledger immutabile, i saldi supportano pagamenti parziali e il reso segue un workflow autorizzato con reintegro di magazzino, audit e idempotenza.

Step completato: configurazione aziendale e numerazioni documentali. Il super admin gestisce identita, contatti, aliquota IVA predefinita e formato dei progressivi; i documenti usano contatori atomici per tipo/esercizio e conservano snapshot storici dell'emittente e dell'aliquota.

Step completato: report vendite e magazzino. I dataset sono filtrati lato server, protetti da permesso e limite righe, consultabili nel frontend ed esportabili in CSV, Excel e PDF con audit dedicato.

Step completato: manuale utente operativo. Sono documentati accesso, navigazione, permessi, catalogo, magazzino, anagrafiche, ordini, pagamenti, resi, documenti simulati, report, amministrazione, monitoraggio, gestione errori e procedure distinte per ruolo.

Step completato: rate limiting e protezione login infrastrutturale. Nginx limita il login per IP, restituisce un errore `429` coerente e tracciabile, registra i rifiuti e viene verificato nello smoke test prod-like; il lockout persistente backend resta il secondo livello di difesa.

Step completato: hardening token e sessioni. Il rinnovo ruota atomicamente il token e revoca quello precedente; sessioni assolute e inattive scadono in modo configurabile, login/rinnovo non sono memorizzabili in cache e l'ADR 0004 documenta la scelta header token/CSRF.

Step completato: Content Security Policy e hardening frontend. Nginx applica una policy same-origin senza `unsafe-inline` o `unsafe-eval`, blocca framing e plugin, uniforma gli header browser e differenzia la cache; verifiche HTTP e Chromium sono integrate nella CI.

Step completato: Actuator limitato in produzione. Il management plane usa una porta interna separata, espone soltanto health senza dettagli, distingue liveness e readiness e viene verificato contro esposizioni accidentali sulla porta API.

Step completato: gestione dei segreti. Il runtime accetta valori protetti o file secret in modo fail-closed, lo stack E2E usa mount read-only, la CI verifica l'assenza di valori nei container e Gitleaks controlla la storia Git; rotazione e rollback sono documentati.

Step completato: hardening container non-root. Le immagini usano utenti espliciti, Nginx ascolta su porta non privilegiata e Compose impone root filesystem read-only, `no-new-privileges`, capability eliminate e percorsi scrivibili confinati. La verifica runtime prod-like e integrata nello smoke E2E e nella CI.

Step completato: backup PostgreSQL schedulati e restore testato. Timer systemd persistenti eseguono il backup giornaliero e il drill settimanale isolato; CI e script locali verificano lifecycle, scheduling e restore sintetico. Replica off-site, cifratura e immutabilita restano responsabilita dell'infrastruttura reale.

Step completato: osservabilita prod-like. Il backend produce log JSON correlati, espone metriche Prometheus sul management plane interno e registra contatori a cardinalita limitata per autenticazione, sessioni ed errori API. Prometheus usa retention configurabile, cinque regole di alert e un container non-root con filesystem read-only. Verifiche statiche e runtime sono integrate nella CI.

Step completato: verifica prod-like ricorrente. Un runner unico costruisce le immagini con aggiornamento delle basi, verifica Flyway su PostgreSQL reale, sicurezza, osservabilita, smoke test browser e recovery, acquisisce sempre la diagnostica e certifica il cleanup Docker. La CI lo esegue su push, pull request, manualmente e ogni settimana.

Step completato: proposta architetturale multi-azienda e multi-tenant. ADR 0005 definisce schema condiviso con isolamento multilivello e opzione database dedicato; il documento operativo separa tenant, azienda legale, sede e magazzino e specifica migrazione, test e rollback. Nessuna funzionalita multi-tenant e ancora dichiarata attiva.

Step completato: proposta separata per GDPR, conservazione documentale e fatturazione elettronica. ADR 0006 mantiene distinti privacy, retention, backup, conservazione e trasmissione SdI; il documento operativo definisce ruoli, dati, workflow, provider, migrazione e gate senza dichiarare conformita.

Step completato: proposta per onboarding clienti, branding, provisioning, aggiornamenti e migrazioni. ADR 0007 definisce lifecycle cliente, control plane, branding sicuro, installazioni pooled/dedicate e flotta di release; il documento operativo specifica workflow, sicurezza, rollout, rollback e gate senza dichiarare funzionalita gia disponibili.

La roadmap architetturale richiesta e completa: 35/35. Questo non equivale a dichiarare il prodotto SaaS, conforme o pronto alla vendita. L'eventuale programma successivo non e un nuovo requisito documentale: parte dalla Fase A di `CUSTOMER_LIFECYCLE_AND_RELEASE_ARCHITECTURE.md`, dopo approvazione del modello commerciale e della piattaforma operativa.
