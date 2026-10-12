# ADR 0006 - Confini tra privacy, conservazione e fatturazione elettronica

## Stato

Accettata come architettura target. L'implementazione non e ancora iniziata e non costituisce attestazione di conformita legale, GDPR, fiscale o di conservazione.

## Contesto

Il gestionale conserva account, anagrafiche, ordini, pagamenti, resi, audit e documenti simulati. Questi dati hanno finalita, basi giuridiche e tempi di conservazione differenti.

Le funzionalita attuali non includono:

- registro dei trattamenti approvato dal titolare;
- workflow per i diritti degli interessati;
- policy di retention applicata e verificabile;
- legal hold;
- conservazione elettronica a norma;
- generazione e validazione del tracciato XML vigente;
- trasmissione al Sistema di Interscambio;
- gestione delle ricevute SdI;
- integrazione con un conservatore.

Backup, audit applicativo, PDF interno, file XML e conservazione a norma non sono equivalenti.

## Decisione

Privacy, gestione documentale, conservazione e fatturazione elettronica resteranno contesti distinti, integrati attraverso porte applicative esplicite.

### Privacy

Il prodotto adottera un ciclo di vita guidato da policy:

- inventario dei dati e finalita per dominio;
- ruoli titolare, responsabile e sub-responsabile definiti contrattualmente;
- minimizzazione e accesso per necessita;
- workflow verificabile per le richieste degli interessati;
- retention per classe dati, tenant, finalita e base giuridica;
- legal hold con motivazione, approvazione e scadenza;
- cancellazione o anonimizzazione a lotti, con anteprima e audit;
- registro delle cancellazioni privo del dato cancellato;
- gestione degli incidenti e delle decisioni di notifica.

La richiesta di cancellazione non produrra una cancellazione indiscriminata. I dati soggetti a obblighi di conservazione verranno limitati e segregati fino alla scadenza o alla rimozione del legal hold, previa decisione del titolare e dei professionisti incaricati.

### Conservazione

Il gestionale non implementera internamente un conservatore a norma. Integrera un servizio esterno qualificato o validato dall'organizzazione tramite una porta sostituibile.

Il sistema applicativo conservera:

- oggetto documentale originale;
- hash e metadati versionati;
- riferimenti ai pacchetti di versamento;
- esiti, ricevute ed evidenze restituite dal conservatore;
- stato del processo e audit tecnico.

La presenza di un backup o di un file su storage applicativo non verra descritta come conservazione a norma.

### Fatturazione elettronica

I documenti fiscali simulati esistenti resteranno separati dal futuro aggregato `ElectronicInvoice`.

Il nuovo contesto usera:

- formato XML e schemi versionati;
- validazione sintattica e regole business prima dell'invio;
- outbox transazionale per la trasmissione asincrona;
- adapter verso provider o canale SdI autorizzato;
- idempotenza e hash del payload;
- inbox verificata per ricevute, scarti e notifiche;
- stati distinti per preparazione, trasmissione, esito SdI e consegna;
- conservazione dell'XML trasmesso e delle ricevute senza riscritture distruttive.

Un PDF resta una copia di cortesia e non sostituisce il file XML rilevante per il processo.

## Alternative valutate

### Estendere direttamente i documenti simulati

Pro:

- meno entita e schermate iniziali.

Contro:

- confonde un documento interno con un documento trasmesso;
- rende ambigui stati, numerazione e immutabilita;
- aumenta il rischio di dichiarazioni fiscali scorrette.

Decisione: non adottata.

### Implementare internamente conservazione e collegamento SdI

Pro:

- controllo completo del codice.

Contro:

- responsabilita operativa e normativa molto elevata;
- protocolli, specifiche, certificati e regole soggetti a evoluzione;
- richiede competenze, procedure e verifiche non presenti nel progetto.

Decisione: non adottata come primo percorso commerciale.

### Adapter verso provider specializzati

Pro:

- confini di responsabilita espliciti;
- aggiornamenti di formato e canale isolati;
- sandbox e riconciliazione verificabili;
- provider sostituibile senza contaminare il dominio.

Contro:

- dipendenza contrattuale e operativa;
- costi, SLA e sub-responsabili da valutare;
- l'integrazione non trasferisce automaticamente tutte le responsabilita del titolare.

Decisione: modello predefinito.

## Conseguenze

- nessuna schermata potra chiamare "fattura elettronica" un documento simulato;
- i tempi di conservazione non saranno costanti hard-coded nel dominio;
- cancellazioni, anonimizzazioni e legal hold saranno operazioni autorizzate, idempotenti e auditate;
- restore da backup dovra riapplicare cancellazioni e restrizioni successive al backup;
- payload XML, ricevute ed evidenze saranno immutabili e separati dai read model;
- specifiche, schemi e adapter saranno versionati;
- credenziali del provider resteranno in un secret manager;
- ogni tenant avra policy e dati isolati secondo ADR 0005;
- il go-live richiedera approvazione legale, fiscale, privacy e del responsabile della conservazione, ove applicabile.

## Condizioni prima dell'implementazione

- completare la classificazione dei dati e il registro dei trattamenti reale;
- definire titolare, responsabili, sub-responsabili e accordi applicabili;
- approvare una matrice di retention e legal hold;
- validare informativa, basi giuridiche, diritti e procedura data breach;
- selezionare provider SdI e conservatore, con sandbox, SLA, export e strategia di uscita;
- approvare manuale e piano di conservazione quando richiesti;
- verificare specifiche XML e regole fiscali vigenti al momento del rilascio;
- superare test privacy, sicurezza, provider, restore e riconciliazione su ambienti reali.
