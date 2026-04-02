# Gestionale Negozio Computer (Java Swing)

Applicazione desktop sviluppata in Java per la gestione di un negozio di computer, realizzata come progetto universitario.

## Descrizione
Il progetto implementa un sistema gestionale con interfaccia grafica sviluppata tramite Java Swing.  
L’applicazione consente la gestione degli utenti, dei prodotti e delle operazioni di acquisto, simulando un contesto reale di negozio.

La struttura del codice è organizzata secondo i principi della programmazione orientata agli oggetti e utilizza diversi design pattern per garantire modularità e scalabilità.

## Funzionalità principali
- Sistema di login e registrazione utenti
- Gestione ruoli (Admin / Cliente)
- Visualizzazione prodotti
- Gestione carrello
- Simulazione acquisto
- Interfaccia grafica desktop (Swing)

## Tecnologie utilizzate
- Java
- Java Swing (GUI desktop)
- OOP (Object-Oriented Programming)

## Architettura del progetto
Il progetto è organizzato in package:

- `main` → avvio dell’applicazione e gestione GUI principale
- `model` → rappresentazione dei dati
- `service` → logica applicativa
- `factory` → creazione oggetti (Factory Pattern)
- `strategy` → gestione comportamenti dinamici (Strategy Pattern)
- `command` → gestione delle azioni (Command Pattern)
- `decorator` → estensione funzionalità (Decorator Pattern)
- `utils` → metodi di supporto

## Struttura dell’avvio
L’applicazione viene avviata tramite:

- `Main.java` → entry point del programma
- `LoginSystem.java` → gestione interfaccia grafica di login e registrazione

## Design Pattern utilizzati
- Factory
- Strategy
- Command
- Decorator

## Competenze sviluppate
- Sviluppo di interfacce grafiche con Java Swing
- Gestione eventi (ActionListener, gestione input utente)
- Progettazione software modulare
- Applicazione dei design pattern
- Separazione tra logica e presentazione (MVC semplificato)

## Come eseguire il progetto
1. Clonare il repository
2. Aprire il progetto con IntelliJ IDEA
3. Eseguire la classe `Main.java`

## Note
I file `.dat` vengono utilizzati per la simulazione della persistenza dei dati.

## Autore
Giovanni De Filippo
