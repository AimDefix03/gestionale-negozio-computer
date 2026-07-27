# ADR 0007 - Lifecycle cliente e gestione della flotta di release

## Stato

Accettata come architettura target. L'implementazione non e ancora iniziata e il prodotto non dispone ancora di onboarding SaaS, white-label, provisioning automatico o aggiornamento di una flotta clienti.

## Contesto

L'installazione corrente viene preparata manualmente:

- esiste una sola configurazione aziendale;
- il bootstrap del super admin avviene tramite secret;
- il deployment prod-like usa un solo stack Docker Compose;
- Flyway aggiorna un solo database all'avvio;
- branding e feature commerciali non sono modellati;
- non esiste un inventario autorevole delle installazioni;
- non esiste un processo di sospensione o offboarding cliente;
- non esistono canali di release, coorti, firme o attestazioni degli artefatti.

ADR 0005 definisce il futuro isolamento multi-tenant pooled e dedicato. ADR 0006 definisce i confini privacy, retention e conservazione. Senza un lifecycle cliente verificabile, il provisioning rischierebbe di creare risorse parziali, tenant orfani, segreti non governati o installazioni con versioni e schemi divergenti.

## Decisione

Il prodotto adottera un control plane commerciale separato logicamente dal data plane gestionale.

Il control plane:

- governa clienti, tenant, installazioni, branding, entitlement e release;
- usa autorizzazioni amministrative separate da quelle del gestionale;
- non espone credenziali o dati operativi dei tenant agli operatori commerciali;
- orchestra provider e infrastruttura tramite adapter idempotenti;
- conserva stato, tentativi, approvazioni ed evidenze di ogni operazione;
- non viene introdotto come microservizio finche il carico e i confini operativi non lo richiedono.

### Lifecycle cliente

L'onboarding e una saga persistita e riprendibile:

`REQUESTED -> VERIFIED -> APPROVED -> PROVISIONING -> VALIDATING -> ACTIVE`

Sono inoltre previsti:

- `FAILED`, con errore redatto e step ripetibile;
- `SUSPENDED`, senza cancellazione automatica dei dati;
- `OFFBOARDING`, con export, retention e legal hold verificati;
- `CLOSED`, solo dopo approvazione e prova di deprovisioning.

Nessun cliente diventa `ACTIVE` per il solo fatto che una risorsa e stata creata. L'attivazione richiede controlli tecnici, sicurezza, bootstrap amministrativo e accettazione esplicita.

### Provisioning

Sono supportati due target:

- tenant pooled sullo stack condiviso definito da ADR 0005;
- deployment e database dedicati per requisiti superiori.

Entrambi usano:

- lo stesso codice applicativo;
- lo stesso modello logico;
- gli stessi artefatti immutabili identificati da digest;
- configurazione esterna e secret reference;
- migrazioni Flyway versionate;
- checklist di validazione e rollback.

Non sono ammessi fork del codice per cliente.

### Branding

Il branding e configurazione dati versionata, non codice eseguibile.

Sono ammessi esclusivamente:

- nome visualizzato;
- logo e favicon validati;
- palette tramite design token consentiti;
- recapiti e riferimenti approvati;
- template testuali con variabili in allowlist.

Non sono ammessi CSS, JavaScript o HTML arbitrari caricati dal cliente. Sicurezza, accessibilita, disclaimer, informazioni legali e identita del prodotto non possono essere rimossi da un tema.

### Entitlement

Le funzionalita commerciali sono governate da entitlement server-side separati da ruoli e permessi:

- un ruolo stabilisce cosa puo fare un utente nel tenant;
- un entitlement stabilisce quali moduli sono disponibili al tenant;
- una feature flag governa rollout tecnico e sperimentazione;
- nessuno dei tre concetti sostituisce gli altri.

Billing, pagamenti dell'abbonamento e fatturazione commerciale del servizio restano fuori da questa decisione.

### Release e aggiornamenti

Ogni release e immutabile e identificata da:

- versione SemVer;
- commit sorgente;
- digest degli artefatti;
- versione minima e massima dello schema compatibile;
- changelog e note operative;
- SBOM machine-readable;
- attestazione di provenienza della build;
- risultati dei quality gate;
- migrazioni incluse e strategia di rollback.

Le immagini non vengono ricostruite per cliente. Lo stesso digest promosso attraversa i canali:

`INTERNAL -> CANARY -> PILOT -> STABLE`

La promozione avviene per coorti, con pausa automatica su errori, osservazione e approvazione esplicita. Una release ritirata non viene sovrascritta: viene marcata `REVOKED`.

### Migrazioni database

Le migrazioni di produzione sono forward-only e seguono expand/contract:

1. aggiungere strutture compatibili;
2. distribuire codice capace di leggere vecchio e nuovo formato;
3. eseguire backfill riprendibili e misurabili;
4. spostare letture e scritture;
5. rimuovere il vecchio formato solo dopo l'adozione della flotta.

Il rollback applicativo e consentito solo se la versione precedente e compatibile con lo schema corrente. Il rollback del database usa normalmente fix-forward. Restore e point-in-time recovery sono procedure di incidente, non un normale pulsante di rollback.

### Offboarding

La chiusura di un cliente richiede:

- revoca accessi e sessioni;
- blocco di nuove operazioni;
- export concordato e verificato;
- applicazione di retention e legal hold;
- revoca o rotazione dei secret;
- deprovisioning idempotente delle risorse;
- evidenza finale priva di dati personali non necessari.

La sospensione commerciale non equivale a cancellazione.

## Alternative valutate

### Script manuali per ogni cliente

Pro:

- costo iniziale ridotto.

Contro:

- operazioni non ripetibili;
- segreti e configurazioni divergenti;
- nessun inventario o audit affidabile;
- rischio elevato durante aggiornamenti e offboarding.

Decisione: ammessi solo come strumenti di bootstrap controllati durante la fase pilota, non come modello operativo.

### Fork applicativo per cliente

Pro:

- personalizzazione senza vincoli.

Contro:

- patch di sicurezza e migrazioni divergenti;
- test e supporto moltiplicati;
- impossibilita di promuovere un artefatto unico;
- costo operativo crescente.

Decisione: non adottato.

### White-label con codice arbitrario

Pro:

- liberta grafica massima.

Contro:

- rischio XSS e supply chain;
- accessibilita e supporto non governabili;
- regressioni non riproducibili.

Decisione: non adottato.

### Control plane separato immediatamente come microservizio

Pro:

- isolamento di deploy e scalabilita indipendente.

Contro:

- autenticazione machine-to-machine, consistenza distribuita e osservabilita aggiuntive;
- complessita non giustificata nella fase iniziale.

Decisione: modulo logicamente separato nel monolite o strumento amministrativo dedicato; estrazione valutata solo con evidenze operative.

## Conseguenze

- il provisioning non sara eseguito dentro una richiesta HTTP lunga;
- ogni step avra idempotency key, stato, owner, retry policy e timeout;
- il control plane non interroghera direttamente le tabelle business dei tenant;
- branding e entitlement saranno versionati e auditable;
- le release saranno promosse per digest, mai ricostruite tra ambienti;
- la flotta avra un inventario di versione applicativa, schema e stato;
- le migrazioni distruttive richiederanno almeno due release compatibili;
- sospensione, offboarding e cancellazione resteranno processi distinti;
- installazioni dedicate non potranno restare indefinitamente su versioni vulnerabili;
- il gestionale corrente resta single-installation finche implementazione e gate non saranno completati.

## Condizioni prima dell'implementazione

- approvare modello commerciale, livelli di servizio e responsabilita di supporto;
- approvare ADR 0005 e 0006 come prerequisiti;
- scegliere orchestratore, registry, secret manager, DNS e certificati;
- definire API e trust boundary del control plane;
- dichiarare la public API e la policy SemVer;
- introdurre artefatti immutabili, SBOM e provenance verificabile;
- definire support window, canali, coorti e policy per aggiornamenti urgenti;
- provare expand/contract, backup, restore e rollback su copie realistiche;
- completare threat model di onboarding, provisioning e supply chain;
- eseguire un pilot con almeno un tenant pooled e uno dedicato prima del go-live commerciale.
