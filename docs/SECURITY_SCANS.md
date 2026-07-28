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

### Backend dependency inventory

Nel backend viene eseguito:

```bash
mvn -B -DskipTests dependency:tree
```

Questo non e un vulnerability scanner completo, ma garantisce che il grafo dipendenze Maven sia risolvibile in CI e aiuta a leggere rapidamente la superficie del backend.

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
- CodeQL: risultati pubblicati nella sezione Security di GitHub.
- Gitleaks: workflow fallita in presenza di segreti rilevati nella storia o nel checkout.

## Limiti attuali

Restano fuori da questo step:

- OWASP Dependency-Check backend con database CVE locale o API NVD;
- SCA professionale con policy licenze avanzate;
- DAST contro ambiente avviato;
- container image scanning;
- test di sicurezza end-to-end.

Questi punti sono candidati per gli step successivi.
