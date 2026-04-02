package service;

import utils.FileManager;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe di servizio che gestisce la logica di autenticazione degli utenti.
 * Si occupa di login, registrazione e persistenza delle credenziali su file.
 */
public class AuthService {
    private static final String FILE_UTENTI = "utenti.dat"; // File per la memorizzazione delle credenziali degli utenti
    private static final String FILE_RUOLI = "ruoli.dat";   // File per la memorizzazione dei ruoli degli utenti
    private Map<String, String> users;                      // Mappa per gestire username e password
    private Map<String, String> roles;                      // Mappa per gestire i ruoli degli utenti

    /**
     * Costruttore della classe AuthService.
     * All'avvio del servizio, carica i dati utente dai file salvati in precedenza.
     */
    public AuthService() {
        loadUsers(); // Carica gli utenti e i ruoli dai file
    }

    /**
     * Effettua il login verificando che username, password e ruolo siano corretti.
     *
     * @param username Il nome utente inserito dall'utente
     * @param password La password inserita dall'utente
     * @param role     Il ruolo selezionato (Admin o Cliente)
     * @return true se le credenziali sono corrette e il ruolo corrisponde, false altrimenti
     */
    public boolean login(String username, String password, String role) {
        return users.containsKey(username) &&           // Verifica se l'username esiste
                users.get(username).equals(password) &&  // Verifica la corrispondenza della password
                roles.get(username).equals(role);        // Verifica il ruolo associato all'utente
    }

    /**
     * Registra un nuovo utente nel sistema se l'username non è già presente.
     * Salva automaticamente i dati su file dopo una registrazione riuscita.
     *
     * @param username Il nome utente da registrare
     * @param password La password associata all'utente
     * @param role     Il ruolo dell'utente (Admin o Cliente)
     * @return true se la registrazione è andata a buon fine, false se l'utente esiste già
     */
    public boolean register(String username, String password, String role) {
        if (!users.containsKey(username)) {              // Controlla se l'username non è già registrato
            users.put(username, password);               // Aggiunge username e password
            roles.put(username, role);                   // Assegna il ruolo
            saveUsers();                                 // Salva i dati aggiornati su file
            return true;
        }
        return false;                                    // L'username è già in uso
    }

    /**
     * Salva le credenziali degli utenti e i ruoli associati su file.
     * Questo metodo viene richiamato dopo ogni registrazione per garantire la persistenza dei dati.
     */
    private void saveUsers() {
        FileManager.salvaSuFile(FILE_UTENTI, users);    // Salva la mappa degli utenti
        FileManager.salvaSuFile(FILE_RUOLI, roles);     // Salva la mappa dei ruoli
    }

    /**
     * Carica le credenziali e i ruoli degli utenti dai file di persistenza.
     * Se i file non esistono o sono vuoti, inizializza le mappe come vuote per evitare errori di null pointer.
     */
    private void loadUsers() {
        users = FileManager.caricaDaFile(FILE_UTENTI);  // Carica la mappa degli utenti dal file
        roles = FileManager.caricaDaFile(FILE_RUOLI);   // Carica la mappa dei ruoli dal file

        // Se i file sono vuoti o non esistono, inizializza le mappe per evitare errori
        if (users == null) users = new HashMap<>();
        if (roles == null) roles = new HashMap<>();
    }
}

