# Legacy Swing

## Stato

La precedente applicazione desktop Java Swing e congelata come riferimento funzionale e dimostrazione didattica. Non riceve nuove funzionalita commerciali e non fa parte della build produttiva predefinita.

Il codice rimane in `src/` per consentire confronti durante la verifica di parita della web app.

## Build dedicata

```bash
mvn -f pom-legacy.xml test
mvn -f pom-legacy.xml package
```

Il JAR generato e `target/gestionale-negozio-computer-legacy-1.0.0.jar`.

## Pattern legacy

- Factory per distinguere prodotti hardware e software.
- Strategy dimostrativa per i metodi di pagamento.
- Command per l'aggiunta al carrello.
- Decorator per servizi aggiuntivi.

Questi pattern non vengono trasferiti automaticamente nel backend web. Verranno sostituiti da modelli di dominio o adapter soltanto quando esiste una necessita reale.

## Condizioni per l'archiviazione

- Tutti i workflow rilevanti sono disponibili nella web app.
- I dati `.dat` necessari sono importabili e verificabili in PostgreSQL.
- La suite web copre i flussi sostitutivi.
- Non esistono procedure operative documentate che richiedono Swing.

Al termine della parita, `src/` e `pom-legacy.xml` potranno essere spostati in un archivio o mantenuti tramite tag Git senza essere distribuiti con il prodotto.
