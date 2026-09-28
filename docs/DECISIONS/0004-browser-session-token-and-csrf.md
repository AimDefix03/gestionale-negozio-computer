# ADR 0004 - Token di sessione browser e protezione CSRF

## Stato

Accettata per il deployment browser-first corrente.

## Contesto

Il frontend React e il backend Spring Boot sono pubblicati dallo stesso reverse proxy. Il login restituisce un token casuale conservato solo nella memoria JavaScript del processo pagina e inviato tramite l'header custom `X-Session-Token`. Il database conserva esclusivamente l'hash SHA-256 del token.

Un cookie HttpOnly ridurrebbe l'esposizione del token in caso di XSS, ma verrebbe allegato automaticamente dal browser e renderebbe necessaria una protezione CSRF completa. La migrazione richiederebbe inoltre HTTPS reale, attributi `Secure`, `HttpOnly` e `SameSite` definiti per il deployment e una strategia coerente per frontend e API eventualmente pubblicati su origini diverse.

## Decisione

Il deployment corrente mantiene il token nell'header custom e non usa cookie di autenticazione ambientali.

- il token resta esclusivamente in memoria e non viene scritto in `localStorage` o `sessionStorage`;
- ogni rinnovo ruota il token in modo atomico e revoca immediatamente quello precedente;
- la sessione ha scadenza assoluta e timeout di inattivita configurabili;
- login e rinnovo restituiscono `Cache-Control: no-store` e `Pragma: no-cache`;
- il token e limitato in lunghezza, non compare nei log e viene salvato sul database solo come hash;
- CSRF resta disabilitato intenzionalmente finche l'autenticazione usa esclusivamente un header custom valorizzato dal client;
- il traffico di produzione deve essere protetto da TLS.

## Alternative valutate

### Cookie HttpOnly immediato

Pro: il codice JavaScript non puo leggere direttamente il token.

Contro: introduce autenticazione ambientale e quindi una superficie CSRF; una migrazione parziale senza TLS, cookie sicuri e token CSRF sarebbe meno sicura della configurazione corrente.

### Token in localStorage

Pro: persiste tra ricaricamenti e riavvii del browser.

Contro: amplia la finestra di esposizione in caso di XSS ed e quindi escluso.

## Conseguenze

- un refresh della pagina richiede un nuovo accesso;
- un XSS eseguito nella pagina puo ancora leggere il token in memoria o inviare richieste autenticate;
- Content Security Policy, sanitizzazione degli input e controllo delle dipendenze frontend sono il prossimo livello di difesa obbligatorio;
- un futuro passaggio a cookie HttpOnly deve essere un cambiamento atomico con protezione CSRF, TLS, test browser e piano di rollback.

## Condizioni per rivalutare la decisione

- autenticazione condivisa tra piu applicazioni browser;
- frontend e API pubblicati su origini differenti;
- introduzione di un identity provider o flusso OAuth/OIDC;
- disponibilita di TLS e gestione cookie uniformi in tutti gli ambienti;
- requisiti di conformita che impongono sessioni browser non accessibili a JavaScript.
