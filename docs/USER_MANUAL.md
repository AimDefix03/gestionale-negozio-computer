# Manuale utente operativo

## Scopo

Questo manuale descrive l'uso della web app Gestionale Negozio Computer per super admin, admin, dipendenti e clienti.

Le procedure fanno riferimento alle funzionalita effettivamente disponibili nella versione corrente. La precedente applicazione Java Swing non fa parte del flusso operativo descritto.

Il gestionale produce documenti simulati per finalita operative e dimostrative. Non invia fatture elettroniche, non comunica con il Sistema di Interscambio e non certifica adempimenti fiscali, contabili, legali o GDPR.

## Accesso

### Login

1. Aprire l'indirizzo comunicato dall'amministratore del sistema.
2. Selezionare `Login`.
3. Inserire username e password.
4. Selezionare il ruolo associato all'account.
5. Premere `Accedi`.

Il ruolo selezionato deve coincidere con quello registrato per l'account. In caso contrario l'accesso viene rifiutato.

Tentativi di accesso errati ripetuti possono produrre un blocco temporaneo. Non continuare a provare password casuali: attendere il termine indicato dal messaggio oppure contattare un amministratore.

### Registrazione pubblica

La registrazione pubblica permette di creare esclusivamente:

- un account `Dipendente`;
- un account `Cliente`.

Non e possibile registrarsi pubblicamente come admin o super admin. Gli account admin vengono creati esclusivamente da un super admin autenticato.

La password deve:

- avere almeno 8 caratteri;
- contenere una lettera maiuscola;
- contenere una lettera minuscola;
- contenere un numero;
- contenere un carattere speciale;
- non essere troppo simile allo username.

L'indicatore di robustezza compare durante registrazione e creazione account, non durante il login.

### Sessione e rinnovo

La sessione scade comunque dopo 45 minuti e viene chiusa anche dopo 30 minuti di inattivita. L'orario di scadenza assoluta e visibile nell'area sessione del workspace.

Quando la sessione non e piu valida:

1. il gestionale mantiene aperta la schermata corrente;
2. compare la finestra `Riconferma accesso`;
3. inserire nuovamente la password;
4. premere `Rinnova sessione`;
5. verificare l'esito dell'operazione prima di ripeterla.

Il rinnovo sostituisce il token della sessione: quello precedente non e piu utilizzabile. Se la sessione e gia scaduta, il gestionale verifica nuovamente le credenziali e apre una nuova sessione mantenendo la schermata corrente.

Se era in corso un'operazione sensibile, il backend non la completa con una sessione scaduta. Dopo il rinnovo controllare lo stato dei dati e inviare nuovamente l'operazione solo se non risulta gia registrata.

Usare `Logout` al termine del lavoro, soprattutto su dispositivi condivisi.

## Navigazione del workspace

La barra superiore e organizzata in menu orizzontali:

- `Workspace`: Dashboard, Catalogo prodotti e Anagrafiche quando autorizzate;
- `Operazioni`: Magazzino, Ordini, Documenti e Report secondo i permessi;
- `Amministrazione`: Account, Configurazione azienda, Monitoraggio e Audit log;
- `Sessione`: aggiornamento dati e logout.

Ogni voce aperta crea una scheda nel workspace. E possibile passare da una scheda all'altra o chiuderla senza uscire dalla sessione. Il comando `Aggiorna dati` ricarica le informazioni operative dal server.

## Ruoli e responsabilita

| Funzione | Super admin | Admin | Dipendente | Cliente |
| --- | --- | --- | --- | --- |
| Consultare catalogo e dashboard | Si | Si | Si | Si |
| Gestire prodotti | Si | Si | Si | No |
| Gestire magazzino | Si | Si | Si | No |
| Consultare anagrafiche | Si | Si | Si | No |
| Gestire anagrafiche | Si | Si | Si | No |
| Consultare ordini | Tutti | Tutti | Tutti | Solo propri |
| Confermare ordini | Si | Si | Si | Solo propri |
| Evadere ordini | Si | Si | Si | No |
| Annullare ordini | Si | Si | Si | Solo proprie bozze |
| Registrare incassi | Si | Si | Si | No |
| Richiedere resi | Si | Si | Si | Solo propri ordini |
| Gestire e rimborsare resi | Si | Si | Si | No |
| Gestire documenti simulati | Si | Si | Si | No |
| Consultare ed esportare report | Si | Si | Si | No |
| Gestire account dipendente/cliente | Si | Si | No | No |
| Creare o eliminare admin | Si | No | No | No |
| Configurare azienda, IVA e numerazioni | Si | No | No | No |
| Consultare monitoraggio e audit | Si | Si | No | No |

Il frontend nasconde le funzioni non autorizzate. Il backend verifica comunque ogni permesso e rifiuta richieste non consentite.

## Dashboard

La Dashboard offre una sintesi di:

- prodotti presenti;
- valore inventario;
- scorte basse ed esaurite;
- ordini e valore complessivo;
- ordini recenti;
- movimenti di magazzino recenti.

I dati mostrati dipendono dal ruolo. Per aggiornare la sintesi usare il comando di aggiornamento nella barra del workspace.

## Catalogo prodotti

### Ricerca e consultazione

Usare i filtri per cercare per codice, nome, brand, tipo, categoria e stato stock. La scheda di dettaglio mostra dati del prodotto, movimenti recenti e ordini collegati quando consentito.

### Creazione prodotto

1. Aprire `Workspace` e `Catalogo prodotti`.
2. Compilare codice, nome, categoria, brand, tipo prodotto, quantita, prezzo, sconto e descrizione.
3. Compilare `Utilizzo opzionale` solo quando utile.
4. Premere `Crea prodotto`.

Il codice identifica stabilmente il prodotto. Scegliere un codice coerente prima di utilizzare l'articolo negli ordini.

### Modifica, disattivazione ed eliminazione

- `Modifica` aggiorna l'anagrafica del prodotto.
- Il codice non puo essere cambiato dopo che il prodotto e stato usato in un ordine.
- `Disattiva` mantiene il prodotto nello storico ma impedisce nuovi acquisti.
- L'eliminazione e bloccata se il prodotto e collegato a ordini o ha quantita riservata.
- Per prodotti storici usare la disattivazione invece dell'eliminazione.

Per rettifiche operative della giacenza usare preferibilmente il modulo Magazzino, in modo da conservare causale e tracciabilita.

## Magazzino

### Registrare un movimento

1. Aprire `Operazioni` e `Magazzino`.
2. Selezionare il prodotto.
3. Scegliere `Carico` o `Scarico`.
4. Inserire una quantita positiva.
5. Scrivere una causale verificabile.
6. Premere `Registra movimento`.

Lo scarico non puo rendere negativa la disponibilita. La tabella movimenti puo essere filtrata per testo, tipo e prodotto.

Il movimento `Reso cliente` viene generato dal workflow di ricezione reso. Non deve essere sostituito con un carico manuale, altrimenti si perde il collegamento operativo con il reso.

## Anagrafiche

Il modulo contiene clienti e fornitori strutturati.

### Creazione o modifica

1. Aprire `Workspace` e `Anagrafiche`.
2. Scegliere il tipo `Cliente` o `Fornitore`.
3. Inserire codice e nome o ragione sociale.
4. Compilare soltanto dati fiscali e di contatto verificati.
5. Premere `Crea anagrafica` oppure `Salva modifiche`.

La disattivazione conserva lo storico e impedisce l'uso operativo futuro. Evitare dati fiscali inventati: i documenti simulati copiano una fotografia dell'anagrafica disponibile al momento della generazione.

## Ordini

### Ciclo ordine

Il ciclo standard e:

```text
Bozza -> Confermato -> Evaso
  |          |
  +----------+-> Annullato
```

- `Bozza`: ordine creato, stock non ancora impegnato.
- `Confermato`: il sistema riserva lo stock necessario.
- `Evaso`: la merce viene scaricata fisicamente dalla giacenza.
- `Annullato`: l'ordine non prosegue; le prenotazioni ancora presenti vengono liberate.

### Acquisto cliente

1. Aprire il Catalogo.
2. Aggiungere al carrello prodotti attivi e disponibili.
3. Selezionare il metodo di pagamento: carta, bonifico bancario o contanti.
4. Completare il checkout per creare l'ordine in bozza.
5. Aprire `Operazioni` e `Ordini`.
6. Confermare il proprio ordine per riservare lo stock.

Il cliente vede esclusivamente i propri ordini. Puo annullare una propria bozza, ma non puo evadere ordini, registrare incassi o gestire rimborsi.

### Gestione ordine da parte dello staff

1. Cercare l'ordine per codice, cliente o metodo di pagamento.
2. Usare `Conferma` per riservare lo stock di una bozza.
3. Usare `Evadi` solo dopo avere verificato la disponibilita e la consegna operativa.
4. Usare `Annulla` solo per ordini in bozza o confermati.
5. Aprire `Operazioni` per consultare pagamenti e resi.

Prima di ripetere un comando dopo un errore o una sessione scaduta, aggiornare l'ordine e verificarne lo stato.

## Pagamenti

Il gestionale registra movimenti finanziari interni; non esegue addebiti bancari o transazioni con provider di pagamento.

### Registrare un incasso

1. Aprire le operazioni di un ordine confermato o evaso.
2. Verificare totale, incassato, rimborsato e residuo.
3. Inserire un importo non superiore al residuo.
4. Aggiungere riferimento e causale.
5. Premere `Registra incasso`.

Sono supportati incassi parziali. Ogni incasso produce un movimento immutabile nel ledger dell'ordine.

## Resi e rimborsi

Il ciclo reso e:

```text
Richiesto -> Approvato -> Ricevuto -> Parzialmente rimborsato -> Rimborsato
     |
     +-> Rifiutato
```

### Richiedere un reso

1. Aprire le operazioni di un ordine evaso.
2. Selezionare prodotto e quantita.
3. Inserire una motivazione verificabile.
4. Premere `Richiedi reso`.

Il cliente puo richiedere resi solo sui propri ordini.

### Gestire un reso

1. Verificare ordine, articoli e motivazione.
2. Approvare oppure rifiutare aggiungendo una nota di revisione.
3. Dopo l'effettivo rientro della merce, usare `Registra ricezione`.
4. Verificare il movimento automatico di reintegro magazzino.
5. Preparare e registrare il rimborso, anche parziale, senza superare il valore rimborsabile.

Non registrare la ricezione prima del rientro fisico della merce. Il rimborso e consentito soltanto dopo la ricezione e non puo superare gli importi effettivamente incassati e rimborsabili.

## Documenti simulati

### Fattura simulata

1. Verificare che l'ordine sia evaso.
2. Dalla tabella Ordini premere `Fattura`.
3. Controllare il documento nella sezione `Documenti`.

Per ogni ordine puo esistere una sola fattura simulata. Il documento conserva snapshot di azienda, cliente, aliquota IVA e righe ordine.

### Nota credito simulata

1. Aprire `Operazioni` e `Documenti`.
2. Inserire un motivo preciso nel campo dedicato.
3. Individuare la fattura simulata.
4. Premere `Nota credito`.

Per lo stesso ordine puo essere creata una sola nota credito simulata. La funzione non sostituisce una procedura fiscale reale.

## Report ed esportazioni

### Report vendite

1. Aprire `Operazioni` e `Report`.
2. Selezionare `Vendite`.
3. Impostare intervallo date e stato ordine.
4. Verificare valore ordini, incassato, rimborsato, netto e residuo.
5. Consultare dettaglio ordini e prodotti principali.

### Report magazzino

1. Selezionare `Magazzino` nella pagina Report.
2. Filtrare per testo, categoria, stock e stato prodotto.
3. Verificare giacenza fisica, riservata, disponibile e valore stock.

I report possono essere esportati in `CSV`, `Excel` e `PDF`. Il periodo vendite non puo superare cinque anni e ogni esportazione e limitata a 10.000 righe. Le esportazioni sono registrate nell'audit log.

I file contengono dati aziendali: conservarli e condividerli secondo le policy dell'organizzazione.

## Gestione account

La sezione Account e disponibile a super admin e admin.

### Creazione account

1. Inserire username e password forte.
2. Selezionare il ruolo consentito.
3. Inserire la password dell'account attualmente autenticato in `Password sessione`.
4. Premere `Crea account`.

Un admin puo creare dipendenti e clienti. Solo il super admin puo creare un altro admin. Il super admin iniziale viene creato tramite configurazione protetta dell'ambiente, non tramite registrazione pubblica.

### Eliminazione account

- Nessun utente puo eliminare il proprio account durante la sessione attiva.
- Un super admin e protetto dall'eliminazione tramite interfaccia.
- Un admin non puo eliminare un altro admin.
- Solo il super admin puo eliminare un account admin.
- La password della sessione e richiesta anche per l'eliminazione.

Verificare sempre identita e ruolo prima di confermare l'operazione.

## Configurazione azienda

Questa sezione e riservata al super admin.

1. Inserire esclusivamente dati reali dell'organizzazione.
2. Configurare aliquota IVA predefinita.
3. Configurare prefissi fattura e nota credito.
4. Scegliere il numero di cifre del progressivo.
5. Controllare l'anteprima.
6. Premere `Salva configurazione`.

I nuovi documenti copiano questi dati come snapshot. I documenti gia creati non vengono riscritti. Prefissi e lunghezza della numerazione non possono essere modificati dopo il primo documento del relativo esercizio.

## Monitoraggio e audit

Super admin e admin possono consultare:

- stato applicazione e database;
- latenza database;
- sessioni attive e blocchi login;
- utilizzo memoria e runtime;
- errori API recenti con codice richiesta;
- eventi sensibili warning e critical;
- audit log filtrabile per testo, categoria e severita.

Quando un utente segnala un errore, annotare il `Codice richiesta` mostrato dal frontend e cercarlo nel Monitoraggio o nell'Audit log. Non chiedere all'utente di comunicare password o token.

## Procedure consigliate per ruolo

### Super admin

1. Completare e verificare la configurazione aziendale.
2. Creare gli admin strettamente necessari.
3. Verificare ruoli, account e separazione delle responsabilita.
4. Controllare periodicamente monitoraggio e audit.
5. Verificare backup e restore con l'amministratore tecnico.

### Admin

1. Controllare dashboard e anomalie.
2. Gestire account dipendente e cliente.
3. Verificare catalogo, ordini, incassi, resi e documenti.
4. Consultare report e audit per riconciliare le operazioni.
5. Segnalare al super admin modifiche di configurazione aziendale necessarie.

### Dipendente

1. Controllare scorte e ordini aperti.
2. Registrare movimenti con causali verificabili.
3. Confermare ed evadere ordini seguendo il flusso reale.
4. Registrare incassi, ricezioni reso e rimborsi soltanto su evidenze operative.
5. Usare i report per i controlli quotidiani.

### Cliente

1. Consultare il catalogo.
2. Creare e confermare il proprio ordine.
3. Consultare stato e pagamento del proprio ordine.
4. Annullare soltanto bozze non piu necessarie.
5. Richiedere un reso motivato dopo l'evasione.

## Errori e assistenza

| Situazione | Significato | Azione consigliata |
| --- | --- | --- |
| Sessione scaduta | Il token non e piu valido | Rinnovare la sessione e verificare lo stato dell'operazione |
| Troppi tentativi di accesso | Il reverse proxy ha temporaneamente limitato le richieste provenienti dallo stesso indirizzo | Attendere il tempo indicato dal sistema senza ripetere il login; se il problema persiste, contattare l'amministratore |
| Accesso negato | Il ruolo non possiede il permesso | Non tentare percorsi alternativi; contattare un amministratore |
| Conflitto risorsa | Il dato e cambiato o l'operazione esiste gia | Aggiornare la schermata e verificare la versione corrente |
| Prodotto non eliminabile | Esistono ordini o stock riservato | Disattivare il prodotto oppure chiudere correttamente i flussi collegati |
| Stock insufficiente | La disponibilita vendibile non copre la richiesta | Verificare riservato e fisico, poi registrare un carico reale se giustificato |
| Documento duplicato | Esiste gia il documento previsto per l'ordine | Consultare la sezione Documenti, senza ripetere la richiesta |
| Errore interno con codice richiesta | Il server ha registrato un'anomalia | Annotare il codice e cercarlo nel Monitoraggio |

## Limiti della versione corrente

- Nessuna integrazione con banche o provider di pagamento.
- Nessuna fatturazione elettronica o trasmissione fiscale.
- Una sola configurazione aziendale.
- Una sola aliquota IVA predefinita per documento.
- Nessuna gestione multi-sede o multi-magazzino.
- Nessuna garanzia di conformita legale o GDPR senza valutazione professionale dedicata.

Le procedure tecniche di configurazione, deployment e backup sono descritte rispettivamente in `docs/CONFIGURATION.md`, `docs/DEPLOYMENT.md` e `docs/BACKUP_RESTORE.md`.
