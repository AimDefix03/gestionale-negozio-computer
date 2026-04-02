package main;

import model.AdminActions;
import model.CustomerActions;
import service.AuthService;

import javax.swing.*;
import java.awt.*;

/**
 * Classe principale per la gestione dell'interfaccia grafica del sistema di login e registrazione.
 * La logica di autenticazione è delegata alla classe AuthService.
 */
public class LoginSystem {
    private static AuthService authService = new AuthService(); // Servizio per la gestione dell'autenticazione

    /**
     * Metodo principale che avvia l'interfaccia grafica.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginSystem::createGUI);
    }

    /**
     * Crea l'interfaccia grafica principale con le opzioni per il login e la registrazione.
     */
    private static void createGUI() {
        JFrame frame = new JFrame("Login e Registrazione");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new GridLayout(3, 1));

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Registrazione");

        loginButton.addActionListener(e -> showLoginFrame());
        registerButton.addActionListener(e -> showRegisterFrame());

        frame.add(new JLabel("Benvenuto! Scegli un'opzione:", SwingConstants.CENTER));
        frame.add(loginButton);
        frame.add(registerButton);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Mostra la finestra per l'accesso degli utenti.
     * Verifica le credenziali e apre la dashboard corrispondente al ruolo dell'utente.
     */
    private static void showLoginFrame() {
        JFrame loginFrame = new JFrame("Login");
        loginFrame.setSize(400, 300);
        loginFrame.setLayout(new GridLayout(6, 1));

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        String[] rolesArray = {"Admin", "Cliente"};
        JComboBox<String> roleBox = new JComboBox<>(rolesArray);
        JButton loginButton = new JButton("Login");

        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String selectedRole = (String) roleBox.getSelectedItem();

            if (authService.login(username, password, selectedRole)) {
                JOptionPane.showMessageDialog(loginFrame, "Login effettuato con successo!");
                loginFrame.dispose();
                openDashboard(selectedRole);
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Credenziali errate o ruolo non corretto.", "Errore", JOptionPane.ERROR_MESSAGE);
            }
        });

        loginFrame.add(new JLabel("Username:"));
        loginFrame.add(usernameField);
        loginFrame.add(new JLabel("Password:"));
        loginFrame.add(passwordField);
        loginFrame.add(new JLabel("Ruolo:"));
        loginFrame.add(roleBox);
        loginFrame.add(loginButton);

        loginFrame.setLocationRelativeTo(null);
        loginFrame.setVisible(true);
    }

    /**
     * Mostra la finestra per la registrazione di un nuovo utente.
     * Consente di scegliere username, password e ruolo (Admin o Cliente).
     */
    private static void showRegisterFrame() {
        JFrame registerFrame = new JFrame("Registrazione");
        registerFrame.setSize(400, 300);
        registerFrame.setLayout(new GridLayout(6, 1));

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        String[] rolesArray = {"Admin", "Cliente"};
        JComboBox<String> roleBox = new JComboBox<>(rolesArray);
        JButton registerButton = new JButton("Registrati");

        registerButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String selectedRole = (String) roleBox.getSelectedItem();

            if (authService.register(username, password, selectedRole)) {
                JOptionPane.showMessageDialog(registerFrame, "Registrazione completata!");
                registerFrame.dispose();
            } else {
                JOptionPane.showMessageDialog(registerFrame, "Username già in uso.", "Errore", JOptionPane.ERROR_MESSAGE);
            }
        });

        registerFrame.add(new JLabel("Username:"));
        registerFrame.add(usernameField);
        registerFrame.add(new JLabel("Password:"));
        registerFrame.add(passwordField);
        registerFrame.add(new JLabel("Ruolo:"));
        registerFrame.add(roleBox);
        registerFrame.add(registerButton);

        registerFrame.setLocationRelativeTo(null);
        registerFrame.setVisible(true);
    }

    /**
     * Apre la dashboard corrispondente al ruolo dell'utente (Admin o Cliente).
     * Fornisce funzionalità diverse in base al ruolo selezionato.
     *
     * @param role Il ruolo dell'utente (Admin o Cliente)
     */
    private static void openDashboard(String role) {
        JFrame dashboard = new JFrame("Dashboard " + role);
        dashboard.setSize(400, 300);
        dashboard.setLayout(new GridLayout(3, 1));

        JLabel welcomeLabel = new JLabel("Benvenuto nella dashboard " + role, SwingConstants.CENTER);
        dashboard.add(welcomeLabel);

        if (role.equals("Admin")) {
            JButton addProductButton = new JButton("Aggiungi Prodotto");
            JButton viewProductsButton = new JButton("Visualizza Prodotti");
            addProductButton.addActionListener(e -> AdminActions.addProduct());
            viewProductsButton.addActionListener(e -> AdminActions.viewProducts());
            dashboard.add(addProductButton);
            dashboard.add(viewProductsButton);
        } else if (role.equals("Cliente")) {
            JButton viewProductsButton = new JButton("Visualizza Prodotti");
            JButton cartButton = new JButton("Carrello");
            JButton purchaseButton = new JButton("Acquista");

            viewProductsButton.addActionListener(e -> CustomerActions.viewProducts(AdminActions.getProductList()));
            cartButton.addActionListener(e -> CustomerActions.viewCart());
            purchaseButton.addActionListener(e -> CustomerActions.purchase());

            dashboard.add(viewProductsButton);
            dashboard.add(cartButton);
            dashboard.add(purchaseButton);
        }

        dashboard.setLocationRelativeTo(null);
        dashboard.setVisible(true);
    }
}
