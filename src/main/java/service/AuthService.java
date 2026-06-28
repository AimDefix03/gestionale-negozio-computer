package service;

import utils.FileManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AuthService {
    private static final String FILE_UTENTI = "utenti.dat";
    private static final String FILE_RUOLI = "ruoli.dat";
    private Map<String, String> users;
    private Map<String, String> roles;

    public AuthService() {
        loadUsers();
    }

    public boolean login(String username, String password, String role) {
        return users.containsKey(username)
                && Objects.equals(users.get(username), password)
                && Objects.equals(roles.get(username), role);
    }

    public boolean register(String username, String password, String role) {
        if (!users.containsKey(username)) {
            users.put(username, password);
            roles.put(username, role);
            saveUsers();
            return true;
        }
        return false;
    }

    private void saveUsers() {
        FileManager.salvaSuFile(FILE_UTENTI, users);
        FileManager.salvaSuFile(FILE_RUOLI, roles);
    }

    private void loadUsers() {
        users = FileManager.caricaDaFile(FILE_UTENTI);
        roles = FileManager.caricaDaFile(FILE_RUOLI);

        if (users == null) {
            users = new HashMap<>();
        }
        if (roles == null) {
            roles = new HashMap<>();
        }
    }
}
