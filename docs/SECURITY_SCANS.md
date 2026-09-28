# Security Scans

## Scopo

Questo documento descrive i controlli automatici introdotti per ridurre il rischio di vulnerabilita note, dipendenze pericolose e regressioni di sicurezza evidenti.

Le scansioni non sostituiscono una revisione di sicurezza completa, un penetration test o una valutazione GDPR.

## Workflow

La workflow principale si trova in `.github/workflows/security.yml`.

Viene eseguita su:

- push verso `main`;
- pull request verso `main`;
- esecuzione manuale;
- controllo programmato ogni lunedi mattina.

## Controlli presenti

### Gitleaks

Gitleaks analizza la storia Git completa e il contenuto del checkout tramite `gitleaks/gitleaks-action@v2`. Il checkout usa `fetch-depth: 0` per non limitare il controllo all'ultimo commit.

Ogni rilevazione deve essere verificata. Un valore reale committato va revocato anche se il commit viene successivamente corretto. Le eccezioni possono essere aggiunte solo per falsi positivi dimostrati, con regole strette e revisionate.

Controllo locale equivalente:

```bash
docker run --rm -v "$PWD:/repo" ghcr.io/gitleaks/gitleaks:v8.30.1 git /repo --redact --no-banner
```

### Dependency review

Sulle pull request, GitHub confronta le dipendenze modificate e blocca la PR se viene introdotta una vulnerabilita con severita alta o critica.

Questo controllo richiede il supporto GitHub Dependency Graph attivo sul repository.

### Frontend npm audit

Nel frontend viene eseguito:

```bash
npm run audit
```

Il controllo fallisce su vulnerabilita almeno `high` rilevate da npm.

### Backend dependency inventory e SCA

Nel backend viene eseguito:

```bash
mvn -B -DskipTests dependency:tree
mvn -B dependency-check:check
```

Il primo comando rende leggibile il grafo Maven. OWASP Dependency-Check analizza poi le dipendenze contro i feed CVE, fallisce in caso di errore dello scanner o vulnerabilita con CVSS almeno 7 e pubblica i report `target/dependency-check-report.html` e `target/dependency-check-report.json` per 14 giorni.

Baseline locale verificata il 2026-08-04: 66 dipendenze analizzate, 0 dipendenze vulnerabili e 0 vulnerabilita nel report finale. Il primo passaggio ha bloccato versioni vulnerabili di Tomcat e pgJDBC; l'aggiornamento e stato seguito dalla suite completa e da una nuova scansione pulita.

La CI genera inoltre il JAR e verifica che l'avvio senza configurazione e quello `prod` privo di segreti terminino fail-closed tramite `scripts/security/verify-backend-fail-closed.sh`.

### CodeQL

CodeQL analizza:

- Java/Spring Boot;
- JavaScript/TypeScript React.

Il backend viene compilato con:

```bash
mvn -B -DskipTests package
```

Il frontend viene compilato con:

```bash
npm ci
npm run build
```

## Dependabot

Il file `.github/dependabot.yml` abilita aggiornamenti settimanali per:

- dipendenze Maven;
- dipendenze npm;
- GitHub Actions.

Gli aggiornamenti devono essere trattati come pull request normali: CI, security workflow e review prima del merge.

## Soglie attuali

- Vulnerabilita frontend: blocco da severita `high`.
- Dependency review: blocco da severita `high`.
- Dipendenze backend: blocco da CVSS `7.0` e scanner fail-closed.
- CodeQL: risultati pubblicati nella sezione Security di GitHub.
- Gitleaks: workflow fallita in presenza di segreti rilevati nella storia o nel checkout.

## Limiti attuali

Restano fuori da questo step:

- SCA professionale con policy licenze avanzate;
- DAST contro ambiente avviato;
- container image scanning;
- test di sicurezza end-to-end.

Questi punti sono candidati per gli step successivi.
