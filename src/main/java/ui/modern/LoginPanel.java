package ui.modern;

import service.AuthService;
import service.AuditService;
import service.PasswordStrength;
import service.PasswordStrengthEvaluator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

class LoginPanel extends JPanel {
    private static final String START_PAGE = "start";
    private static final String FORM_PAGE = "form";

    private final AppFrame appFrame;
    private final AuthService authService;
    private final AuditService auditService;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel pages = Ui.panel(cardLayout);
    private final JTextField usernameField = Ui.textField();
    private final JPasswordField passwordField = Ui.passwordField();
    private final JComboBox<String> roleBox = new JComboBox<>();
    private final JLabel formBadge = Ui.text("");
    private final JLabel formTitle = Ui.title("", 34);
    private final JLabel formSubtitle = Ui.text("");
    private final JLabel statusLabel = Ui.text("");
    private final JLabel strengthLabel = Ui.text("");
    private final JProgressBar strengthBar = new JProgressBar(0, 100);
    private final JPanel strengthPanel = Ui.panel(new BorderLayout(0, 8));
    private final JCheckBox showPasswordCheck = new JCheckBox("Mostra password");
    private final JButton submitButton = Ui.primaryButton("");
    private final char defaultEchoChar;
    private AuthMode currentMode = AuthMode.LOGIN;

    LoginPanel(AppFrame appFrame, AuthService authService, AuditService auditService) {
        this.appFrame = appFrame;
        this.authService = authService;
        this.auditService = auditService;
        defaultEchoChar = passwordField.getEchoChar();
        setLayout(new GridBagLayout());
        setBackground(Ui.BACKGROUND);
        setBorder(new EmptyBorder(28, 28, 28, 28));
        Ui.styleComboBox(roleBox);
        pages.setOpaque(false);
        pages.add(buildStartCard(), START_PAGE);
        pages.add(buildFormCard(), FORM_PAGE);
        add(pages, centered());
    }

    private JPanel buildStartCard() {
        JPanel card = Ui.card(new GridBagLayout());
        card.setPreferredSize(new Dimension(520, 292));

        JPanel stack = Ui.panel(new FlowLayout());
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);

        stack.add(centeredLabel(Ui.eyebrow("JAVA SWING DESKTOP APP")));
        stack.add(Box.createVerticalStrut(12));
        stack.add(centeredLabel(Ui.title("Gestionale Computer", 30)));
        stack.add(Box.createVerticalStrut(10));
        stack.add(centeredLabel(Ui.text("Accedi al workspace oppure crea un nuovo profilo.")));
        stack.add(Box.createVerticalStrut(18));
        stack.add(centeredLabel(Ui.text("Il ruolo verra selezionato nella schermata successiva.")));
        stack.add(Box.createVerticalStrut(16));
        stack.add(buildStartActions());
        stack.add(Box.createVerticalStrut(18));
        stack.add(buildRoleStrip());

        card.add(stack, centered());
        return card;
    }

    private JPanel buildStartActions() {
        JPanel actions = Ui.panel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.CENTER_ALIGNMENT);
        actions.setMaximumSize(new Dimension(320, 40));

        JButton loginButton = Ui.primaryButton("Accedi");
        JButton registerButton = Ui.secondaryButton("Registrati");
        loginButton.setPreferredSize(new Dimension(136, 38));
        registerButton.setPreferredSize(new Dimension(136, 38));
        loginButton.addActionListener(e -> showForm(AuthMode.LOGIN));
        registerButton.addActionListener(e -> showForm(AuthMode.REGISTER));

        actions.add(loginButton);
        actions.add(registerButton);
        return actions;
    }

    private JPanel buildRoleStrip() {
        JPanel strip = Ui.compactCard(new FlowLayout(FlowLayout.CENTER, 8, 0));
        strip.setBackground(Ui.SURFACE_SOFT);
        strip.setAlignmentX(Component.CENTER_ALIGNMENT);
        strip.setMaximumSize(new Dimension(360, 42));
        strip.add(Ui.eyebrow("REGISTRAZIONE"));
        strip.add(Ui.text("Dipendente  |  Cliente"));
        return strip;
    }

    private JLabel centeredLabel(JLabel label) {
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private JPanel buildFormCard() {
        JPanel card = Ui.card(new BorderLayout(0, 20));
        card.setPreferredSize(new Dimension(520, 570));

        JPanel header = Ui.panel(new GridLayout(0, 1, 0, 7));
        header.setOpaque(false);
        formBadge.setForeground(Ui.ACCENT);
        header.add(formBadge);
        header.add(formTitle);
        header.add(formSubtitle);

        JPanel form = Ui.panel(new GridBagLayout());
        form.setOpaque(false);
        addField(form, "Username", usernameField, 0);
        addPasswordField(form, 1);
        addField(form, "Ruolo", roleBox, 3);

        JPanel actions = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton backButton = Ui.secondaryButton("Indietro");
        backButton.addActionListener(e -> showStart());
        submitButton.addActionListener(e -> submit());
        actions.add(backButton);
        actions.add(submitButton);

        JPanel footer = Ui.panel(new BorderLayout(0, 14));
        footer.setOpaque(false);
        footer.add(statusLabel, BorderLayout.CENTER);
        footer.add(actions, BorderLayout.SOUTH);

        card.add(header, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        installPasswordInteractions();
        return card;
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
        fieldConstraints.insets = new Insets(0, 0, 16, 0);
        form.add(field, fieldConstraints);
    }

    private void addPasswordField(JPanel form, int row) {
        addField(form, "Password", passwordField, row);

        showPasswordCheck.setOpaque(false);
        showPasswordCheck.setForeground(Ui.MUTED);
        showPasswordCheck.setFocusPainted(false);

        JPanel passwordOptions = Ui.panel(new BorderLayout(0, 8));
        passwordOptions.setOpaque(false);
        passwordOptions.add(showPasswordCheck, BorderLayout.NORTH);

        strengthPanel.setOpaque(false);
        strengthBar.setStringPainted(false);
        strengthBar.setBorderPainted(false);
        strengthPanel.add(strengthBar, BorderLayout.NORTH);
        strengthPanel.add(strengthLabel, BorderLayout.CENTER);
        passwordOptions.add(strengthPanel, BorderLayout.CENTER);

        GridBagConstraints optionsConstraints = new GridBagConstraints();
        optionsConstraints.gridx = 0;
        optionsConstraints.gridy = row * 2 + 2;
        optionsConstraints.weightx = 1;
        optionsConstraints.fill = GridBagConstraints.HORIZONTAL;
        optionsConstraints.insets = new Insets(-8, 0, 16, 0);
        form.add(passwordOptions, optionsConstraints);
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

    private void showForm(AuthMode mode) {
        currentMode = mode;
        usernameField.setText("");
        passwordField.setText("");
        showPasswordCheck.setSelected(false);
        passwordField.setEchoChar(defaultEchoChar);
        updateRoleOptions(mode);
        statusLabel.setText(mode == AuthMode.LOGIN ? "Inserisci le credenziali per accedere." : "Crea il profilo e controlla la robustezza della password.");
        formBadge.setText(mode == AuthMode.LOGIN ? "Accesso account" : "Nuova registrazione");
        formTitle.setText(mode == AuthMode.LOGIN ? "Accedi" : "Crea account");
        formSubtitle.setText(mode == AuthMode.LOGIN ? "Usa username, password e ruolo associato." : "La registrazione pubblica non consente profili Admin.");
        submitButton.setText(mode == AuthMode.LOGIN ? "Prosegui" : "Crea account");
        submitButton.setPreferredSize(new Dimension(mode == AuthMode.LOGIN ? 128 : 150, 38));
        submitButton.setMinimumSize(submitButton.getPreferredSize());
        strengthPanel.setVisible(mode == AuthMode.REGISTER);
        updatePasswordStrength();
        revalidate();
        repaint();
        cardLayout.show(pages, FORM_PAGE);
    }

    private void showStart() {
        statusLabel.setText("");
        cardLayout.show(pages, START_PAGE);
    }

    private void submit() {
        if (currentMode == AuthMode.LOGIN) {
            login();
        } else {
            register();
        }
    }

    private void login() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String role = (String) roleBox.getSelectedItem();

        if (authService.login(username, password, role)) {
            auditService.record(username, role, "Login", "Sessione", "Accesso completato");
            statusLabel.setText("Accesso completato.");
            appFrame.showDashboard(role, username.trim());
            return;
        }

        statusLabel.setText("Credenziali errate oppure ruolo non corretto.");
    }

    private void register() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String role = (String) roleBox.getSelectedItem();

        if (username.isBlank() || password.isBlank()) {
            statusLabel.setText("Inserisci username e password prima di registrarti.");
            return;
        }

        if (authService.registerPublic(username, password, role)) {
            auditService.record(username, role, "Registrazione pubblica", "Account " + username.trim(), "Profilo creato da registrazione pubblica");
            statusLabel.setText("Registrazione completata. Torna all'accesso per entrare.");
        } else {
            statusLabel.setText("Username gia in uso, dati non validi o ruolo non consentito.");
        }
    }

    private void updateRoleOptions(AuthMode mode) {
        roleBox.removeAllItems();
        java.util.List<String> roles = mode == AuthMode.LOGIN ? AuthService.AVAILABLE_ROLES : AuthService.PUBLIC_REGISTRATION_ROLES;
        for (String role : roles) {
            roleBox.addItem(role);
        }
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

    private GridBagConstraints centered() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        return constraints;
    }

    private enum AuthMode {
        LOGIN,
        REGISTER
    }
}
