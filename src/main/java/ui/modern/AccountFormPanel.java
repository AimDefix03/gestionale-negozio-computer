package ui.modern;

import service.AuthService;
import service.AuditService;
import service.PasswordStrength;
import service.PasswordStrengthEvaluator;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

class AccountFormPanel extends JPanel {
    private final AuthService authService;
    private final AuditService auditService;
    private final Runnable onAccountCreated;
    private final String actorUsername;
    private final String actorRole;
    private final JTextField usernameField = Ui.textField();
    private final JPasswordField passwordField = Ui.passwordField();
    private final JComboBox<String> roleBox = new JComboBox<>(AuthService.AVAILABLE_ROLES.toArray(new String[0]));
    private final JCheckBox showPasswordCheck = new JCheckBox("Mostra password");
    private final JLabel statusLabel = Ui.text("Crea un profilo e assegna il ruolo corretto.");
    private final JLabel strengthLabel = Ui.text("");
    private final JProgressBar strengthBar = new JProgressBar(0, 100);
    private final char defaultEchoChar;

    AccountFormPanel(AuthService authService, Runnable onAccountCreated) {
        this(authService, onAccountCreated, null, null, null);
    }

    AccountFormPanel(AuthService authService, Runnable onAccountCreated, AuditService auditService, String actorUsername, String actorRole) {
        this.authService = authService;
        this.onAccountCreated = onAccountCreated;
        this.auditService = auditService;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.defaultEchoChar = passwordField.getEchoChar();
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);
        Ui.styleComboBox(roleBox);
        add(buildFields(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        installPasswordInteractions();
        updatePasswordStrength();
    }

    private JPanel buildFields() {
        JPanel panel = Ui.panel(new GridBagLayout());
        panel.setOpaque(false);
        addField(panel, "Username", usernameField, 0);
        addField(panel, "Password", passwordField, 1);
        addPasswordTools(panel, 2);
        addField(panel, "Ruolo", roleBox, 3);
        return panel;
    }

    private void addField(JPanel form, String labelText, JComponent field, int row) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row * 2;
        labelConstraints.weightx = 1;
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.insets = new Insets(0, 0, 6, 0);

        JLabel label = Ui.text(labelText);
        label.setForeground(Ui.TEXT);
        form.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 0;
        fieldConstraints.gridy = row * 2 + 1;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(0, 0, 14, 0);
        form.add(field, fieldConstraints);
    }

    private void addPasswordTools(JPanel form, int row) {
        showPasswordCheck.setOpaque(false);
        showPasswordCheck.setForeground(Ui.MUTED);
        showPasswordCheck.setFocusPainted(false);
        strengthBar.setStringPainted(false);
        strengthBar.setBorderPainted(false);

        JPanel tools = Ui.panel(new BorderLayout(0, 8));
        tools.setOpaque(false);
        tools.add(showPasswordCheck, BorderLayout.NORTH);
        tools.add(strengthBar, BorderLayout.CENTER);
        tools.add(strengthLabel, BorderLayout.SOUTH);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row * 2;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(-8, 0, 14, 0);
        form.add(tools, constraints);
    }

    private JPanel buildFooter() {
        JPanel footer = Ui.panel(new BorderLayout(0, 10));
        footer.setOpaque(false);
        JPanel actions = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);

        JButton saveButton = Ui.primaryButton("Crea account");
        saveButton.addActionListener(e -> createAccount());
        actions.add(saveButton);

        footer.add(statusLabel, BorderLayout.CENTER);
        footer.add(actions, BorderLayout.SOUTH);
        return footer;
    }

    private void createAccount() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String role = (String) roleBox.getSelectedItem();

        if (username.isBlank() || password.isBlank()) {
            statusLabel.setText("Inserisci username e password.");
            return;
        }

        if (AuthService.ROLE_ADMIN.equals(role) && !confirmAdminCreation(username)) {
            statusLabel.setText("Creazione admin annullata.");
            return;
        }

        if (authService.createAccount(username, password, role)) {
            recordAudit(username, role);
            statusLabel.setText("Account creato correttamente.");
            clearFields();
            onAccountCreated.run();
        } else {
            statusLabel.setText("Username gia in uso oppure dati non validi.");
        }
    }

    private boolean confirmAdminCreation(String username) {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Creare un nuovo account Admin per \"" + username.trim() + "\"?\nQuesta operazione assegna privilegi completi.",
                "Conferma creazione admin",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return choice == JOptionPane.YES_OPTION;
    }

    private void installPasswordInteractions() {
        showPasswordCheck.addActionListener(e -> passwordField.setEchoChar(showPasswordCheck.isSelected() ? (char) 0 : defaultEchoChar));
        passwordField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePasswordStrength();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePasswordStrength();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updatePasswordStrength();
            }
        });
    }

    private void updatePasswordStrength() {
        PasswordStrength strength = PasswordStrengthEvaluator.evaluate(new String(passwordField.getPassword()));
        strengthLabel.setText("Password: " + strength.getLabel());
        strengthBar.setValue(strength.getScore());
        Color color = switch (strength) {
            case EMPTY -> Ui.BORDER;
            case WEAK -> Ui.DANGER;
            case MEDIUM -> Ui.WARNING;
            case STRONG -> Ui.ACCENT;
            case EXCELLENT -> Ui.SUCCESS;
        };
        strengthLabel.setForeground(color);
        strengthBar.setForeground(color);
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        roleBox.setSelectedItem(AuthService.ROLE_EMPLOYEE);
    }

    private void recordAudit(String targetUsername, String targetRole) {
        if (auditService != null) {
            String details = "Ruolo assegnato: " + targetRole;
            if (AuthService.ROLE_ADMIN.equals(targetRole)) {
                details += " (operazione delicata)";
            }
            auditService.record(actorUsername, actorRole, "Creazione account", "Account " + targetUsername.trim(), details);
        }
    }
}
