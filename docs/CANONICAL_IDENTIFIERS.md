# Identificatori canonici

## Contratto

Username, codice prodotto e codice anagrafica hanno due rappresentazioni:

- valore visuale: il testo inserito dall'operatore, ripulito dagli spazi esterni per le nuove scritture;
- valore canonico: `lower(trim(valore))`, usato per ricerca e unicita.

La migrazione `V23__canonical_business_identifiers.sql` aggiunge le colonne canoniche, le valorizza per i record storici e applica vincoli `NOT NULL`, `CHECK` e `UNIQUE`. Il database e quindi l'autorita finale anche quando due richieste arrivano nello stesso momento. Le collisioni vengono restituite dalle API come `409 RESOURCE_CONFLICT`.

## Preflight obbligatorio

Prima di applicare V23 a un database esistente:

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 \
  -f scripts/db/preflight-canonical-identifiers.sql
```

Il risultato atteso e zero righe per tutte e tre le query. Eseguire prima un backup verificato e provare l'upgrade su una copia ripristinata.

## Collisioni storiche

Se il preflight restituisce righe, V23 si arresta sul vincolo univoco. Non eliminare, unire o rinominare automaticamente i record:

1. identificare i record coinvolti interrogando il valore `lower(trim(...))` segnalato;
2. decidere con il responsabile del dato quale identificatore assegnare a ciascun record;
3. per gli account, conservare gli ID stabili e rinominare soltanto il valore visuale scelto;
4. per prodotti e anagrafiche, verificare ordini, giacenze e collegamenti prima della rinomina;
5. conservare gli snapshot storici di ordini e documenti, senza riscriverli come se il nuovo codice fosse sempre esistito;
6. rieseguire il preflight e applicare V23 soltanto quando tutte le query restituiscono zero righe.

La migrazione preserva i valori visuali storici; la normalizzazione serve esclusivamente come identita tecnica coerente.
