# Gestionale Negozio Computer (Java Swing)

Applicazione desktop sviluppata in Java Swing per simulare la gestione di un negozio di computer.

Il progetto nasce in ambito universitario e implementa un gestionale con utenti, ruoli, catalogo prodotti, carrello, acquisto simulato, servizi extra sui prodotti e persistenza locale tramite serializzazione su file.

## Funzionalità principali

- Sistema di login e registrazione utenti
- Gestione ruoli Admin e Cliente
- Inserimento e visualizzazione prodotti
- Gestione carrello
- Simulazione acquisto
- Servizi extra applicabili ai prodotti
- Interfaccia grafica desktop con Java Swing

## Obiettivi tecnici

- Separare interfaccia Swing e logica applicativa dove possibile.
- Applicare pattern progettuali in modo riconoscibile e coerente.
- Gestire prodotti hardware/software tramite Factory.
- Gestire metodi di pagamento intercambiabili tramite Strategy.
- Incapsulare l'aggiunta al carrello tramite Command.
- Estendere i prodotti con servizi extra tramite Decorator.

## Tecnologie utilizzate

- Java
- Java Swing
- Maven
- JUnit 5
- Programmazione orientata agli oggetti

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
- Maven 3.8+

## Compilazione e test

Compilazione:

```bash
mvn compile
```

Test automatici:

```bash
mvn test
```

Generazione del jar:

```bash
mvn package
```

## Avvio

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

## Autore

Giovanni De Filippo
