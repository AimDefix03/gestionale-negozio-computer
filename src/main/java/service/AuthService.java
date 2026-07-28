package service;

import model.UserAccount;
import repository.DataRepository;
import repository.FileDataRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AuthService {
    private static final String FILE_UTENTI = "utenti.dat";
    private static final String FILE_RUOLI = "ruoli.dat";
    public static final String ROLE_ADMIN = "Admin";
    public static final String ROLE_EMPLOYEE = "Dipendente";
    public static final String ROLE_CUSTOMER = "Cliente";
    public static final List<String> AVAILABLE_ROLES = List.of(ROLE_ADMIN, ROLE_EMPLOYEE, ROLE_CUSTOMER);
    public static final List<String> PUBLIC_REGISTRATION_ROLES = List.of(ROLE_EMPLOYEE, ROLE_CUSTOMER);
    private final DataRepository<Map<String, String>> usersRepository;
    private final DataRepository<Map<String, String>> rolesRepository;
    private Map<String, String> users;
    private Map<String, String> roles;

    public AuthService() {
        this(new FileDataRepository<>(FILE_UTENTI), new FileDataRepository<>(FILE_RUOLI));
    }

    public AuthService(String usersFile, String rolesFile) {
        this(new FileDataRepository<>(usersFile), new FileDataRepository<>(rolesFile));
    }

    public AuthService(DataRepository<Map<String, String>> usersRepository, DataRepository<Map<String, String>> rolesRepository) {
        if (usersRepository == null || rolesRepository == null) {
            throw new IllegalArgumentException("I repository utenti e ruoli sono obbligatori.");
        }
        this.usersRepository = usersRepository;
        this.rolesRepository = rolesRepository;
        loadUsers();
    }

    public boolean login(String username, String password, String role) {
        if (username == null || password == null || role == null) {
            return false;
        }

        String normalizedUsername = username.trim();
        return users.containsKey(normalizedUsername)
                && Objects.equals(users.get(normalizedUsername), password)
                && Objects.equals(roles.get(normalizedUsername), role);
    }

    public boolean register(String username, String password, String role) {
        return createAccount(username, password, role);
    }

    public boolean registerPublic(String username, String password, String role) {
        if (ROLE_ADMIN.equals(role) || !PUBLIC_REGISTRATION_ROLES.contains(role)) {
            return false;
        }
        return createAccount(username, password, role);
    }

    public boolean createAccount(String username, String password, String role) {
        if (username == null || username.isBlank() || password == null || password.isBlank() || !AVAILABLE_ROLES.contains(role)) {
            return false;
        }

        String normalizedUsername = username.trim();
        if (users.containsKey(normalizedUsername)) {
            return false;
        }

        users.put(normalizedUsername, password);
        roles.put(normalizedUsername, role);
        saveUsers();
        return true;
    }

    public List<UserAccount> getAccounts() {
        return users.keySet().stream()
                .sorted(Comparator.naturalOrder())
                .map(username -> new UserAccount(username, roles.getOrDefault(username, "Ruolo non assegnato")))
                .toList();
    }

    public boolean deleteAccount(String username) {
        if (username == null || !users.containsKey(username)) {
            return false;
        }

        users.remove(username);
        roles.remove(username);
        saveUsers();
        return true;
    }

    public int deleteAccounts(List<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return 0;
        }

        int removed = 0;
        for (String username : usernames) {
            if (username != null && users.containsKey(username)) {
                users.remove(username);
                roles.remove(username);
                removed++;
            }
        }
        if (removed > 0) {
            saveUsers();
        }
        return removed;
    }

    private void saveUsers() {
        usersRepository.save(users);
        rolesRepository.save(roles);
    }

    private void loadUsers() {
        users = usersRepository.load();
        roles = rolesRepository.load();

        if (users == null) {
            users = new HashMap<>();
        }
        if (roles == null) {
            roles = new HashMap<>();
        }
    }
}
