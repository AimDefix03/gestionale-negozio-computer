# Gestionale Negozio Computer

Applicazione desktop Java Swing per simulare la gestione di un negozio di computer.

Il progetto include gestione utenti con ruoli, catalogo prodotti, carrello, acquisto simulato, servizi extra sui prodotti e persistenza locale tramite serializzazione su file.

## Obiettivi tecnici

- Separare interfaccia Swing e logica applicativa dove possibile.
- Applicare pattern progettuali in modo riconoscibile e coerente.
- Gestire prodotti hardware/software tramite Factory.
- Gestire metodi di pagamento intercambiabili tramite Strategy.
- Incapsulare l'aggiunta al carrello tramite Command.
- Estendere i prodotti con servizi extra tramite Decorator.

## Pattern utilizzati

| Pattern | Package | Responsabilità |
| --- | --- | --- |
| Factory | `factory` | Creazione di prodotti hardware/software e selezione della factory corretta. |
| Strategy | `strategy` | Selezione del metodo di pagamento senza accoppiare il carrello alle classi concrete. |
| Command | `command` | Incapsulamento dell'azione di aggiunta al carrello. |
| Decorator | `decorator` | Aggiunta di servizi extra ai prodotti senza modificare la classe base. |

## Struttura principale

```text
src/
├── main/java
│   ├── command      # comandi applicativi
│   ├── decorator    # servizi extra applicati ai prodotti
│   ├── factory      # creazione dei prodotti
│   ├── main         # avvio applicazione e GUI login
│   ├── model        # entità di dominio
│   ├── service      # logica applicativa e persistenza
│   ├── strategy     # strategie di pagamento
│   ├── ui           # azioni e schermate Swing
│   └── utils        # utility di serializzazione
└── test/java        # test automatici JUnit
```

## Requisiti

- Java 17 o superiore
- Maven 3.8+ opzionale, consigliato per compilazione standard

## Compilazione

Con Maven:

```bash
mvn compile
```

Per eseguire anche i test:

```bash
mvn test
```

Per generare il jar:

```bash
mvn package
```

Senza Maven:

```bash
javac -encoding UTF-8 -d out $(find src/main/java -name "*.java")
```

## Avvio

Con classi compilate manualmente:

```bash
java -cp out main.Main
```

Con jar generato da Maven:

```bash
java -jar target/gestionale-negozio-computer-1.0.0.jar
```

Da IntelliJ IDEA è possibile avviare direttamente la classe `main.Main`.

## Note

I file `.dat` sono usati per simulare la persistenza locale di utenti, ruoli e prodotti. Non sono pensati per rappresentare una soluzione di sicurezza reale o un database di produzione.

## Possibili evoluzioni

- Estendere i test automatici ai servizi applicativi e ai flussi Swing principali.
- Separare ulteriormente UI Swing e logica applicativa.
- Sostituire la persistenza su file con database o repository dedicato.
- Migliorare la gestione delle credenziali utente.
