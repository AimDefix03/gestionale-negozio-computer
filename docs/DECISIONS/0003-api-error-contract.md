# 0003 - Contratto errori API stabile

Data: 2026-07-05

## Stato

Accettata

## Contesto

Il frontend deve distinguere sessione scaduta, permessi insufficienti, errori di validazione e problemi applicativi senza affidarsi a messaggi testuali fragili.

## Decisione

La web API espone un formato errore unico con:

- timestamp;
- status HTTP;
- codice applicativo stabile;
- messaggio leggibile;
- dettagli;
- path;
- requestId.

Gli errori di Spring Security e quelli dei controller usano lo stesso contratto.

## Conseguenze

- Il frontend puo gestire gli errori in modo piu prevedibile.
- I test possono verificare codici stabili.
- Le prossime API devono rispettare lo stesso formato.
- Log richiesta e audit operativo possono essere collegati allo stesso `requestId`.
