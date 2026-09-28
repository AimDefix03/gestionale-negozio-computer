package ui.modern;

import model.FiscalDocument;
import model.Order;
import model.OrderItem;
import model.Prodotto;
import model.StockMovement;
import model.UserAccount;
import service.AuthService;
import service.AuditService;
import service.FiscalDocumentService;
import service.InventoryService;
import service.OrderReceiptService;
import service.OrderService;
import service.ProductService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

class DashboardPanel extends JPanel {
    private static final DateTimeFormatter DASHBOARD_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final String SIDEBAR_EXPANDED = "expanded";
    private static final String SIDEBAR_COLLAPSED = "collapsed";
    private final AppFrame appFrame;
    private final ProductService productService;
    private final AuthService authService;
    private final AuditService auditService;
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final FiscalDocumentService fiscalDocumentService;
    private final OrderReceiptService orderReceiptService = new OrderReceiptService();
    private final String role;
    private final String username;
    private final ProductTablePanel productTablePanel = new ProductTablePanel();
    private final AccountTablePanel accountTablePanel = new AccountTablePanel();
    private final AuditTablePanel auditTablePanel = new AuditTablePanel();
    private final ProductTablePanel lowStockTablePanel = new ProductTablePanel();
    private final StockMovementTablePanel stockMovementTablePanel = new StockMovementTablePanel();
    private final OrderTablePanel orderTablePanel = new OrderTablePanel();
    private final FiscalDocumentTablePanel fiscalDocumentTablePanel = new FiscalDocumentTablePanel();
    private final ProductTablePanel customerProductTablePanel = new ProductTablePanel();
    private final ProductFilterPanel productFilterPanel = new ProductFilterPanel(this::applyProductFilters);
    private final OrderFilterPanel orderFilterPanel = new OrderFilterPanel(this::applyOrderFilters);
    private final JPanel content = Ui.panel(new BorderLayout(0, 18));
    private final JTabbedPane workspaceTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
    private final CardLayout sidebarLayout = new CardLayout();
    private final JPanel sidebarShell = Ui.panel(sidebarLayout);
    private final List<JButton> navigationButtons = new ArrayList<>();
    private final JLabel pageEyebrowLabel = Ui.eyebrow("WORKSPACE");
    private final JLabel pageTitleLabel = Ui.title("Dashboard", 28);
    private final JLabel pageSubtitleLabel = Ui.text("Panoramica operativa del gestionale.");
    private JButton overviewNavButton;
    private JButton productsNavButton;
    private JButton ordersNavButton;
    private JButton newProductNavButton;
    private JButton inventoryNavButton;
    private JButton documentsNavButton;
    private JButton accountsNavButton;
    private JButton auditNavButton;
    private JButton cartNavButton;
    private boolean sidebarExpanded = true;
    private CartPanel cartPanel;

    DashboardPanel(
            AppFrame appFrame,
            ProductService productService,
            AuthService authService,
            AuditService auditService,
            InventoryService inventoryService,
            OrderService orderService,
            FiscalDocumentService fiscalDocumentService,
            String role,
            String username
    ) {
        this.appFrame = appFrame;
        this.productService = productService;
        this.authService = authService;
        this.auditService = auditService;
        this.inventoryService = inventoryService;
        this.orderService = orderService;
        this.fiscalDocumentService = fiscalDocumentService;
        this.role = role;
        this.username = username;
        setLayout(new BorderLayout());
        setBackground(Ui.BACKGROUND);
        productTablePanel.setLowStockThreshold(InventoryService.LOW_STOCK_THRESHOLD);
        lowStockTablePanel.setLowStockThreshold(InventoryService.LOW_STOCK_THRESHOLD);
        customerProductTablePanel.setLowStockThreshold(InventoryService.LOW_STOCK_THRESHOLD);
        add(buildSidebarHost(), BorderLayout.WEST);
        add(buildContent(), BorderLayout.CENTER);
        showOverview();
    }

    private JPanel buildSidebarHost() {
        JPanel host = Ui.panel(new BorderLayout());
        host.setBorder(new EmptyBorder(24, 24, 24, 0));
        host.setOpaque(true);

        sidebarShell.setOpaque(false);
        sidebarShell.add(buildSidebar(), SIDEBAR_EXPANDED);
        sidebarShell.add(buildCollapsedSidebar(), SIDEBAR_COLLAPSED);
        host.add(sidebarShell, BorderLayout.CENTER);
        return host;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = Ui.panel(new BorderLayout(0, 14));
        sidebar.setPreferredSize(new Dimension(304, 0));
        sidebar.setOpaque(false);

        JPanel top = Ui.panel(new GridLayout(0, 1, 0, 12));
        top.setOpaque(false);
        top.add(buildAppIdentityBlock());
        top.add(buildSessionBlock());

        JPanel nav = buildNavigationBlock();

        sidebar.add(top, BorderLayout.NORTH);
        sidebar.add(nav, BorderLayout.CENTER);
        sidebar.add(buildSidebarActions(), BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel buildAppIdentityBlock() {
        JPanel block = Ui.compactCard(new BorderLayout(14, 0));

        JPanel markPanel = Ui.panel(new GridBagLayout());
        markPanel.setOpaque(false);
        markPanel.setPreferredSize(new Dimension(42, 52));
        JLabel mark = Ui.title(">_", 22);
        mark.setHorizontalAlignment(SwingConstants.CENTER);
        mark.setForeground(Ui.ACCENT);
        markPanel.add(mark);

        JPanel brandCopy = Ui.panel(new GridLayout(0, 1, 0, 5));
        brandCopy.setOpaque(false);
        brandCopy.add(Ui.eyebrow("APP DESKTOP"));
        brandCopy.add(Ui.title("Negozio Computer", 17));
        brandCopy.add(Ui.text("Gestionale operativo"));
        block.add(markPanel, BorderLayout.WEST);
        block.add(brandCopy, BorderLayout.CENTER);
        return block;
    }

    private JPanel buildSessionBlock() {
        JPanel block = Ui.compactCard(new BorderLayout(0, 10));
        JPanel content = Ui.panel(new GridLayout(0, 1, 0, 7));
        content.setOpaque(false);
        content.add(Ui.eyebrow("SESSIONE ATTIVA"));
        content.add(sidebarText(username, Font.BOLD, Ui.TEXT));
        content.add(sidebarText(role, Font.PLAIN, Ui.MUTED));
        block.add(content, BorderLayout.CENTER);
        return block;
    }

    private JPanel buildNavigationBlock() {
        JPanel nav = Ui.card(new BorderLayout(0, 12));
        nav.setOpaque(false);
        JPanel navContent = Ui.panel(new BorderLayout());
        navContent.setLayout(new BoxLayout(navContent, BoxLayout.Y_AXIS));
        navContent.setOpaque(false);

        overviewNavButton = navigationButton("Dashboard", this::showOverview);
        productsNavButton = navigationButton("Catalogo prodotti", this::showProducts);
        ordersNavButton = navigationButton("Ordini", this::showOrders);
        addSidebarSection(navContent, new SidebarSection("Workspace", true, overviewNavButton, productsNavButton, ordersNavButton));

        if (canManageProducts()) {
            newProductNavButton = navigationButton("Nuovo prodotto", this::showProductForm);
            inventoryNavButton = navigationButton("Magazzino", this::showInventory);
            documentsNavButton = navigationButton("Documenti", this::showFiscalDocuments);
            addSidebarSection(navContent, new SidebarSection("Operazioni", true, newProductNavButton, inventoryNavButton, documentsNavButton));
        }

        if (isAdmin()) {
            accountsNavButton = navigationButton("Account", this::showAccounts);
            auditNavButton = navigationButton("Audit log", this::showAuditLog);
            addSidebarSection(navContent, new SidebarSection("Amministrazione", true, accountsNavButton, auditNavButton));
        }

        if (!canManageProducts()) {
            cartNavButton = navigationButton("Carrello", this::showCustomerWorkspace);
            addSidebarSection(navContent, new SidebarSection("Acquisti", true, cartNavButton));
        }

        nav.add(Ui.eyebrow("MENU"), BorderLayout.NORTH);
        nav.add(navContent, BorderLayout.CENTER);
        return nav;
    }

    private JPanel buildSidebarActions() {
        JPanel actions = Ui.compactCard(new GridLayout(1, 2, 10, 0));
        JButton closeSidebarButton = Ui.secondaryButton("Nascondi");
        JButton logoutButton = Ui.ghostButton("Logout");
        closeSidebarButton.addActionListener(e -> toggleSidebar());
        logoutButton.addActionListener(e -> appFrame.showLogin());
        actions.add(closeSidebarButton);
        actions.add(logoutButton);
        return actions;
    }

    private JTextArea sidebarText(String text, int style, Color color) {
        JTextArea area = new JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setForeground(color);
        area.setFont(Ui.font(style, 13));
        area.setBorder(new EmptyBorder(0, 0, 0, 0));
        return area;
    }

    private JPanel buildCollapsedSidebar() {
        JPanel collapsed = Ui.card(new BorderLayout(0, 14));
        collapsed.setPreferredSize(new Dimension(112, 0));
        collapsed.setBackground(Ui.SIDEBAR);
        collapsed.setBorder(new EmptyBorder(16, 12, 16, 12));

        JLabel mark = Ui.title(">_", 18);
        mark.setHorizontalAlignment(SwingConstants.CENTER);
        mark.setForeground(Ui.ACCENT);

        JButton openButton = Ui.primaryButton("Apri");
        openButton.addActionListener(e -> toggleSidebar());

        JPanel top = Ui.panel(new GridLayout(0, 1, 0, 12));
        top.setOpaque(false);
        top.add(mark);
        top.add(openButton);

        JButton logoutButton = Ui.ghostButton("Esci");
        logoutButton.addActionListener(e -> appFrame.showLogin());

        collapsed.add(top, BorderLayout.NORTH);
        collapsed.add(logoutButton, BorderLayout.SOUTH);
        return collapsed;
    }

    private void toggleSidebar() {
        sidebarExpanded = !sidebarExpanded;
        sidebarLayout.show(sidebarShell, sidebarExpanded ? SIDEBAR_EXPANDED : SIDEBAR_COLLAPSED);
        revalidate();
        repaint();
    }

    private JButton navigationButton(String text, Runnable action) {
        JButton button = Ui.navButton(text);
        button.addActionListener(event -> action.run());
        navigationButtons.add(button);
        return button;
    }

    private void setActiveNavigation(JButton activeButton) {
        for (JButton button : navigationButtons) {
            Ui.setButtonActive(button, button == activeButton);
        }
    }

    private void addSidebarSection(JPanel navContent, SidebarSection section) {
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        navContent.add(section);
        navContent.add(Box.createVerticalStrut(10));
    }

    private JPanel buildContent() {
        JPanel wrapper = Ui.panel(new BorderLayout(0, 22));
        wrapper.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel header = Ui.card(new BorderLayout(18, 0));
        JPanel copy = Ui.panel(new GridLayout(0, 1, 0, 5));
        copy.setOpaque(false);
        copy.add(pageEyebrowLabel);
        copy.add(pageTitleLabel);
        copy.add(pageSubtitleLabel);

        JPanel session = Ui.panel(new GridLayout(0, 1, 0, 6));
        session.setOpaque(false);
        session.setPreferredSize(new Dimension(220, 0));
        session.add(Ui.eyebrow("SESSIONE"));
        session.add(sidebarText(username, Font.BOLD, Ui.TEXT));
        session.add(sidebarText(role, Font.PLAIN, Ui.MUTED));

        header.add(copy, BorderLayout.CENTER);
        header.add(session, BorderLayout.EAST);

        content.setOpaque(false);
        workspaceTabs.setOpaque(false);
        workspaceTabs.setBackground(Ui.BACKGROUND);
        workspaceTabs.setForeground(Ui.TEXT);
        workspaceTabs.setFont(Ui.font(Font.BOLD, 12));
        workspaceTabs.setBorder(new EmptyBorder(0, 0, 0, 0));
        workspaceTabs.setFocusable(false);
        workspaceTabs.putClientProperty("JTabbedPane.tabType", "card");
        workspaceTabs.putClientProperty("JTabbedPane.hasFullBorder", false);
        workspaceTabs.putClientProperty("JTabbedPane.showTabSeparators", false);
        workspaceTabs.putClientProperty("JTabbedPane.tabHeight", 38);
        workspaceTabs.addChangeListener(event -> syncSelectedWorkspaceTab());
        content.add(workspaceTabs, BorderLayout.CENTER);
        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    private void setPage(String eyebrow, String title, String subtitle) {
        pageEyebrowLabel.setText(eyebrow);
        pageTitleLabel.setText(title);
        pageSubtitleLabel.setText(subtitle);
    }

    private void openWorkspaceTab(
            String key,
            String tabTitle,
            String eyebrow,
            String pageTitle,
            String subtitle,
            JButton navigationButton,
            JComponent component,
            boolean closable
    ) {
        WorkspaceTabInfo info = new WorkspaceTabInfo(key, eyebrow, pageTitle, subtitle, navigationButton);
        component.putClientProperty("workspace.info", info);
        component.setBorder(new EmptyBorder(0, 0, 0, 0));

        int existingIndex = findWorkspaceTab(key);
        if (existingIndex >= 0) {
            workspaceTabs.setComponentAt(existingIndex, component);
            workspaceTabs.setTabComponentAt(existingIndex, buildWorkspaceTabHeader(component, tabTitle, closable));
            workspaceTabs.setSelectedIndex(existingIndex);
            syncSelectedWorkspaceTab();
            return;
        }

        workspaceTabs.addTab(tabTitle, component);
        int index = workspaceTabs.indexOfComponent(component);
        workspaceTabs.setTabComponentAt(index, buildWorkspaceTabHeader(component, tabTitle, closable));
        workspaceTabs.setSelectedIndex(index);
        syncSelectedWorkspaceTab();
    }

    private JPanel buildWorkspaceTabHeader(Component component, String title, boolean closable) {
        JPanel header = Ui.panel(new FlowLayout(FlowLayout.CENTER, 7, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(6, 10, 6, 10));

        JLabel label = Ui.text(title);
        label.setForeground(Ui.TEXT);
        label.setFont(Ui.font(Font.BOLD, 12));
        header.add(label);

        if (closable) {
            JButton closeButton = new JButton("x");
            closeButton.setFont(Ui.font(Font.BOLD, 11));
            closeButton.setForeground(Ui.MUTED);
            closeButton.setBorder(new EmptyBorder(0, 4, 0, 4));
            closeButton.setFocusPainted(false);
            closeButton.setContentAreaFilled(false);
            closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            closeButton.addActionListener(event -> closeWorkspaceTab(component));
            header.add(closeButton);
        }

        return header;
    }

    private void closeWorkspaceTab(Component component) {
        int index = workspaceTabs.indexOfComponent(component);
        if (index >= 0) {
            workspaceTabs.removeTabAt(index);
        }

        if (workspaceTabs.getTabCount() == 0) {
            showOverview();
            return;
        }

        syncSelectedWorkspaceTab();
    }

    private int findWorkspaceTab(String key) {
        for (int index = 0; index < workspaceTabs.getTabCount(); index++) {
            WorkspaceTabInfo info = tabInfo(workspaceTabs.getComponentAt(index));
            if (info != null && info.key().equals(key)) {
                return index;
            }
        }
        return -1;
    }

    private void syncSelectedWorkspaceTab() {
        Component selectedComponent = workspaceTabs.getSelectedComponent();
        WorkspaceTabInfo info = tabInfo(selectedComponent);
        if (info == null) {
            return;
        }
        setPage(info.eyebrow(), info.pageTitle(), info.subtitle());
        setActiveNavigation(info.navigationButton());
    }

    private WorkspaceTabInfo tabInfo(Component component) {
        if (component instanceof JComponent jComponent) {
            Object info = jComponent.getClientProperty("workspace.info");
            if (info instanceof WorkspaceTabInfo workspaceTabInfo) {
                return workspaceTabInfo;
            }
        }
        return null;
    }

    private void showOverview() {
        List<Prodotto> prodotti = productService.getProdotti();
        List<Order> orders = baseOrders();

        JPanel panel = Ui.panel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.add(buildDashboardMetrics(prodotti, orders), BorderLayout.NORTH);

        JPanel activity = Ui.panel(new GridLayout(1, 2, 12, 0));
        activity.setOpaque(false);
        activity.add(buildRecentOrdersCard(orders));
        if (canManageProducts()) {
            activity.add(buildRecentMovementsCard(inventoryService.getMovements()));
        } else {
            activity.add(buildQuickActionsCard());
        }
        panel.add(activity, BorderLayout.CENTER);

        if (canManageProducts()) {
            panel.add(buildQuickActionsCard(), BorderLayout.SOUTH);
        }
        openWorkspaceTab(
                "dashboard",
                "Dashboard",
                "Workspace",
                "Dashboard operativa",
                "Controlla catalogo, ordini, scorte e attivita recenti.",
                overviewNavButton,
                panel,
                false
        );
    }

    private JPanel buildDashboardMetrics(List<Prodotto> prodotti, List<Order> orders) {
        JPanel grid = Ui.panel(new GridLayout(2, 3, 12, 12));
        grid.setOpaque(false);

        long availableProducts = prodotti.stream().filter(product -> product.getQuantita() > 0).count();
        long lowStockProducts = prodotti.stream()
                .filter(product -> product.getQuantita() > 0 && product.getQuantita() <= InventoryService.LOW_STOCK_THRESHOLD)
                .count();
        long outOfStockProducts = prodotti.stream().filter(product -> product.getQuantita() == 0).count();
        double ordersTotal = orders.stream().mapToDouble(Order::total).sum();

        if (canManageProducts()) {
            grid.add(statCard("Prodotti", String.valueOf(prodotti.size()), "Elementi nel catalogo"));
            grid.add(statCard("Valore inventario", formatCurrency(catalogValue(prodotti)), "Prezzo scontato per quantità"));
            grid.add(statCard("Scorte basse", String.valueOf(lowStockProducts), "Da monitorare"));
            grid.add(statCard("Esauriti", String.valueOf(outOfStockProducts), "Quantità pari a zero"));
            grid.add(statCard("Ordini", String.valueOf(orders.size()), "Acquisti registrati"));
            grid.add(statCard("Fatturato simulato", formatCurrency(ordersTotal), "Totale ordini"));
        } else {
            grid.add(statCard("Catalogo", String.valueOf(prodotti.size()), "Prodotti consultabili"));
            grid.add(statCard("Disponibili", String.valueOf(availableProducts), "Quantità maggiore di zero"));
            grid.add(statCard("Non disponibili", String.valueOf(outOfStockProducts), "Prodotti esauriti"));
            grid.add(statCard("I miei ordini", String.valueOf(orders.size()), "Acquisti registrati"));
            grid.add(statCard("Spesa simulata", formatCurrency(ordersTotal), "Totale personale"));
            grid.add(statCard("Ruolo", role, "Sessione corrente"));
        }

        return grid;
    }

    private JPanel statCard(String label, String value, String caption) {
        JPanel card = Ui.compactCard(new BorderLayout(12, 0));
        JLabel marker = Ui.title(">", 24);
        marker.setForeground(Ui.ACCENT);
        marker.setHorizontalAlignment(SwingConstants.CENTER);
        marker.setVerticalAlignment(SwingConstants.CENTER);
        marker.setPreferredSize(new Dimension(24, 0));

        JPanel copy = Ui.panel(new GridLayout(0, 1, 0, 5));
        copy.setOpaque(false);
        JLabel labelView = Ui.eyebrow(label.toUpperCase());
        copy.add(labelView);
        copy.add(Ui.title(value, 24));
        copy.add(Ui.text(caption));

        card.add(marker, BorderLayout.WEST);
        card.add(copy, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildRecentOrdersCard(List<Order> orders) {
        JPanel card = Ui.card(new BorderLayout(0, 14));
        card.add(sectionHeader("Ultimi ordini", canManageProducts()
                ? "Acquisti registrati più recenti."
                : "I tuoi acquisti più recenti."), BorderLayout.NORTH);

        JPanel rows = summaryRowsPanel();
        List<Order> recentOrders = orders.stream().limit(2).toList();
        if (recentOrders.isEmpty()) {
            rows.add(emptyState("Nessun ordine registrato."));
        } else {
            recentOrders.forEach(order -> addSummaryRow(rows, summaryRow(
                    order.code() + " - " + order.customer(),
                    order.timestamp().format(DASHBOARD_DATE_FORMAT) + " · " + order.paymentMethod(),
                    formatCurrency(order.total())
            )));
        }
        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildRecentMovementsCard(List<StockMovement> movements) {
        JPanel card = Ui.card(new BorderLayout(0, 14));
        card.add(sectionHeader("Movimenti magazzino", "Ultime operazioni su carichi e scarichi."), BorderLayout.NORTH);

        JPanel rows = summaryRowsPanel();
        List<StockMovement> recentMovements = movements.stream().limit(2).toList();
        if (recentMovements.isEmpty()) {
            rows.add(emptyState("Nessun movimento registrato."));
        } else {
            recentMovements.forEach(movement -> addSummaryRow(rows, summaryRow(
                    movement.type().getLabel() + " · " + movement.productCode(),
                    movement.productName(),
                    movement.quantity() + " unità"
            )));
        }
        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildQuickActionsCard() {
        JPanel card = Ui.card(new BorderLayout(0, 14));
        card.add(sectionHeader("Azioni rapide", "Percorsi frequenti della sessione corrente."), BorderLayout.NORTH);

        JPanel actions = Ui.panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);

        JButton catalogButton = Ui.secondaryButton("Catalogo");
        JButton ordersButton = Ui.secondaryButton("Ordini");
        catalogButton.addActionListener(e -> showProducts());
        ordersButton.addActionListener(e -> showOrders());
        actions.add(catalogButton);
        actions.add(ordersButton);

        if (canManageProducts()) {
            JButton newProductButton = Ui.primaryButton("Nuovo prodotto");
            JButton inventoryButton = Ui.secondaryButton("Magazzino");
            JButton documentsButton = Ui.secondaryButton("Documenti");
            newProductButton.addActionListener(e -> showProductForm());
            inventoryButton.addActionListener(e -> showInventory());
            documentsButton.addActionListener(e -> showFiscalDocuments());
            actions.add(newProductButton);
            actions.add(inventoryButton);
            actions.add(documentsButton);
        } else {
            JButton cartButton = Ui.primaryButton("Carrello");
            cartButton.addActionListener(e -> showCustomerWorkspace());
            actions.add(cartButton);
        }

        if (isAdmin()) {
            JButton accountButton = Ui.secondaryButton("Account");
            accountButton.addActionListener(e -> showAccounts());
            actions.add(accountButton);
        }

        card.add(actions, BorderLayout.CENTER);
        return card;
    }

    private JPanel summaryRow(String title, String subtitle, String value) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(true);
        row.setBackground(Ui.SURFACE_SOFT);
        row.setBorder(new EmptyBorder(10, 14, 10, 14));
        row.setPreferredSize(new Dimension(0, 66));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));

        JPanel copy = new JPanel();
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.setOpaque(false);
        JLabel titleLabel = Ui.text(title);
        titleLabel.setForeground(Ui.TEXT);
        titleLabel.setFont(Ui.font(Font.BOLD, 13));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel subtitleLabel = Ui.text(subtitle);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(titleLabel);
        copy.add(Box.createVerticalStrut(6));
        copy.add(subtitleLabel);

        JLabel valueLabel = Ui.eyebrow(value);
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        valueLabel.setVerticalAlignment(SwingConstants.CENTER);
        valueLabel.setPreferredSize(new Dimension(118, 0));
        row.add(copy, BorderLayout.CENTER);
        row.add(valueLabel, BorderLayout.EAST);
        return row;
    }

    private JPanel summaryRowsPanel() {
        JPanel rows = Ui.panel(new BorderLayout());
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        return rows;
    }

    private void addSummaryRow(JPanel rows, JPanel row) {
        rows.add(row);
        rows.add(Box.createVerticalStrut(8));
    }

    private JLabel emptyState(String text) {
        JLabel label = Ui.text(text);
        label.setBorder(new EmptyBorder(10, 0, 10, 0));
        return label;
    }

    private void showProducts() {
        JPanel panel = Ui.card(new BorderLayout(0, 14));
        panel.add(sectionHeader("Catalogo prodotti", "Seleziona una riga per modificarla o piu righe per eliminarle."), BorderLayout.NORTH);

        JPanel catalog = Ui.panel(new BorderLayout(0, 12));
        catalog.setOpaque(false);
        catalog.add(productFilterPanel, BorderLayout.NORTH);
        catalog.add(productTablePanel, BorderLayout.CENTER);
        panel.add(catalog, BorderLayout.CENTER);

        if (canManageProducts()) {
            panel.add(buildAdminProductActions(), BorderLayout.SOUTH);
        } else {
            JButton addToCart = Ui.primaryButton("Aggiungi selezionato al carrello");
            addToCart.addActionListener(e -> addSelectedProductToCart());
            panel.add(singleAction(addToCart), BorderLayout.SOUTH);
        }

        productFilterPanel.updateFilterOptions(productService.getProdotti());
        applyProductFilters(productFilterPanel.currentFilters());
        openWorkspaceTab(
                "products",
                "Catalogo",
                "Catalogo",
                "Prodotti",
                "Ricerca, filtra e gestisci gli elementi disponibili.",
                productsNavButton,
                panel,
                true
        );
    }

    private void showInventory() {
        lowStockTablePanel.setProducts(inventoryService.getLowStockProducts());
        stockMovementTablePanel.setMovements(inventoryService.getMovements());

        JPanel panel = Ui.panel(new GridLayout(2, 1, 0, 14));
        panel.setOpaque(false);

        JPanel lowStockCard = Ui.card(new BorderLayout(0, 14));
        lowStockCard.add(sectionHeader("Scorte basse", "Prodotti con quantità minore o uguale a " + InventoryService.LOW_STOCK_THRESHOLD + "."), BorderLayout.NORTH);
        lowStockCard.add(lowStockTablePanel, BorderLayout.CENTER);

        JPanel movementsCard = Ui.card(new BorderLayout(0, 14));
        movementsCard.add(sectionHeader("Storico movimenti", "Carichi e scarichi registrati dagli utenti operativi."), BorderLayout.NORTH);
        movementsCard.add(stockMovementTablePanel, BorderLayout.CENTER);

        panel.add(lowStockCard);
        panel.add(movementsCard);
        openWorkspaceTab(
                "inventory",
                "Magazzino",
                "Magazzino",
                "Scorte e movimenti",
                "Monitora disponibilita, carichi, scarichi e causali operative.",
                inventoryNavButton,
                panel,
                true
        );
    }

    private void showOrders() {
        JPanel panel = Ui.card(new BorderLayout(0, 14));
        String subtitle = canManageProducts()
                ? "Consulta gli acquisti registrati e il relativo metodo di pagamento."
                : "Consulta lo storico degli acquisti effettuati con il tuo account.";

        JPanel header = Ui.panel(new BorderLayout(0, 12));
        header.setOpaque(false);
        header.add(sectionHeader("Ordini", subtitle), BorderLayout.NORTH);
        header.add(orderFilterPanel, BorderLayout.CENTER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(orderTablePanel, BorderLayout.CENTER);
        panel.add(buildOrderActions(), BorderLayout.SOUTH);
        applyOrderFilters(orderFilterPanel.currentFilters());
        openWorkspaceTab(
                "orders",
                "Ordini",
                "Ordini",
                "Storico ordini",
                "Consulta acquisti, dettagli, riepiloghi e metodi di pagamento.",
                ordersNavButton,
                panel,
                true
        );
    }

    private JPanel buildOrderActions() {
        JPanel actions = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton detailButton = Ui.secondaryButton("Dettaglio ordine");
        JButton receiptButton = Ui.secondaryButton("Esporta riepilogo");

        detailButton.addActionListener(e -> showSelectedOrderDetail());
        receiptButton.addActionListener(e -> exportSelectedOrderReceipt());

        actions.add(detailButton);
        actions.add(receiptButton);

        if (canManageProducts()) {
            JButton invoiceButton = Ui.primaryButton("Fattura simulata");
            JButton creditNoteButton = Ui.secondaryButton("Nota credito simulata");
            invoiceButton.addActionListener(e -> createSimulatedInvoiceForSelectedOrder());
            creditNoteButton.addActionListener(e -> createSimulatedCreditNoteForSelectedOrder());
            actions.add(invoiceButton);
            actions.add(creditNoteButton);
        }
        return actions;
    }

    private void showFiscalDocuments() {
        fiscalDocumentTablePanel.setDocuments(baseFiscalDocuments());

        JPanel panel = Ui.card(new BorderLayout(0, 14));
        panel.add(sectionHeader("Archivio documenti", "Fatture e note credito simulate con stato e ordine collegato."), BorderLayout.NORTH);
        panel.add(fiscalDocumentTablePanel, BorderLayout.CENTER);
        panel.add(buildFiscalDocumentActions(), BorderLayout.SOUTH);
        openWorkspaceTab(
                "documents",
                "Documenti",
                "Documenti",
                "Documenti simulati",
                "Consulta documenti non validi fiscalmente generati dagli ordini.",
                documentsNavButton,
                panel,
                true
        );
    }

    private JPanel buildFiscalDocumentActions() {
        JPanel actions = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton refreshButton = Ui.secondaryButton("Aggiorna");
        JButton exportButton = Ui.primaryButton("Esporta documento");
        refreshButton.addActionListener(e -> showFiscalDocuments());
        exportButton.addActionListener(e -> exportSelectedFiscalDocument());

        actions.add(refreshButton);
        actions.add(exportButton);
        return actions;
    }

    private void showSelectedOrderDetail() {
        Order order = selectedOrderOrWarn();
        if (order == null) {
            return;
        }

        openWorkspaceTab(
                "order-detail-" + order.code(),
                "Dettaglio " + order.code(),
                "Ordini",
                "Dettaglio ordine " + order.code(),
                "Righe, quantita, prezzi e totale dell'ordine selezionato.",
                ordersNavButton,
                new OrderDetailPanel(order),
                true
        );
    }

    private void exportSelectedOrderReceipt() {
        Order order = selectedOrderOrWarn();
        if (order == null) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Salva riepilogo ordine");
        chooser.setSelectedFile(new File("riepilogo-" + order.code().toLowerCase(Locale.ROOT) + ".txt"));

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path path = withTxtExtension(chooser.getSelectedFile()).toPath();
        try {
            Files.writeString(path, orderReceiptService.buildReceipt(order));
            auditService.record(username, role, "Riepilogo ordine", "Ordine " + order.code(), "File: " + path.getFileName());
            JOptionPane.showMessageDialog(this, "Riepilogo ordine salvato correttamente.", "Riepilogo ordine", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, "Impossibile salvare il riepilogo: " + exception.getMessage(), "Riepilogo ordine", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createSimulatedInvoiceForSelectedOrder() {
        Order order = selectedOrderOrWarn();
        if (order == null) {
            return;
        }

        try {
            FiscalDocument document = fiscalDocumentService.createInvoiceFromOrder(order, username, role);
            auditService.record(username, role, "Fattura simulata", "Ordine " + order.code(), "Documento " + document.code());
            JOptionPane.showMessageDialog(this, "Fattura simulata generata: " + document.code(), "Documenti simulati", JOptionPane.INFORMATION_MESSAGE);
            showFiscalDocuments();
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Documenti simulati", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void createSimulatedCreditNoteForSelectedOrder() {
        Order order = selectedOrderOrWarn();
        if (order == null) {
            return;
        }

        String reason = JOptionPane.showInputDialog(
                this,
                "Causale della nota credito simulata",
                "Nota credito simulata",
                JOptionPane.QUESTION_MESSAGE
        );
        if (reason == null) {
            return;
        }

        try {
            FiscalDocument document = fiscalDocumentService.createCreditNoteFromOrder(order, reason, username, role);
            auditService.record(username, role, "Nota credito simulata", "Ordine " + order.code(), "Documento " + document.code() + " - " + reason.trim());
            JOptionPane.showMessageDialog(this, "Nota credito simulata generata: " + document.code(), "Documenti simulati", JOptionPane.INFORMATION_MESSAGE);
            showFiscalDocuments();
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Documenti simulati", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void exportSelectedFiscalDocument() {
        FiscalDocument document = fiscalDocumentTablePanel.getSelectedDocument();
        if (document == null) {
            JOptionPane.showMessageDialog(this, "Seleziona un documento dalla tabella.", "Documenti simulati", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Salva documento simulato");
        chooser.setSelectedFile(new File("documento-" + document.code().toLowerCase(Locale.ROOT) + ".txt"));

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path path = withTxtExtension(chooser.getSelectedFile()).toPath();
        try {
            Files.writeString(path, fiscalDocumentService.buildDocumentText(document));
            auditService.record(username, role, "Export documento simulato", document.code(), "File: " + path.getFileName());
            JOptionPane.showMessageDialog(this, "Documento simulato salvato correttamente.", "Documenti simulati", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, "Impossibile salvare il documento: " + exception.getMessage(), "Documenti simulati", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Order selectedOrderOrWarn() {
        Order order = orderTablePanel.getSelectedOrder();
        if (order == null) {
            JOptionPane.showMessageDialog(this, "Seleziona un ordine dalla tabella.", "Ordini", JOptionPane.INFORMATION_MESSAGE);
        }
        return order;
    }

    private File withTxtExtension(File file) {
        if (file.getName().toLowerCase(Locale.ROOT).endsWith(".txt")) {
            return file;
        }
        return new File(file.getParentFile(), file.getName() + ".txt");
    }

    private void showProductForm() {
        ProductFormPanel formPanel = new ProductFormPanel(productService, this::showProducts, null, auditService, username, role);
        JPanel panel = Ui.card(new BorderLayout(0, 14));
        panel.add(sectionHeader("Nuovo prodotto", "Inserisci un elemento nel catalogo amministratore."), BorderLayout.NORTH);
        panel.add(formPanel, BorderLayout.CENTER);
        openWorkspaceTab(
                "new-product",
                "Nuovo prodotto",
                "Catalogo",
                "Nuovo prodotto",
                "Aggiungi un prodotto con classificazione, brand e dati commerciali.",
                newProductNavButton,
                panel,
                true
        );
    }

    private void showProductEditForm(Prodotto prodotto) {
        ProductFormPanel formPanel = new ProductFormPanel(productService, this::showProducts, prodotto, auditService, username, role);
        JPanel panel = Ui.card(new BorderLayout(0, 14));
        panel.add(sectionHeader("Modifica prodotto", "Aggiorna dati, categoria, prezzo o quantita."), BorderLayout.NORTH);
        panel.add(formPanel, BorderLayout.CENTER);
        openWorkspaceTab(
                "edit-product-" + prodotto.getCodice(),
                "Modifica " + prodotto.getCodice(),
                "Catalogo",
                "Modifica prodotto",
                "Aggiorna in modo controllato i dati del prodotto selezionato.",
                productsNavButton,
                panel,
                true
        );
    }

    private void showCustomerWorkspace() {
        cartPanel = cartPanel == null
                ? new CartPanel(orderService, inventoryService, auditService, username, role, this::refreshCatalog)
                : cartPanel;
        customerProductTablePanel.setProducts(productService.getProdotti());

        JPanel workspace = Ui.panel(new GridLayout(1, 2, 12, 0));
        workspace.setOpaque(false);

        JPanel catalog = Ui.card(new BorderLayout(0, 14));
        catalog.add(Ui.title("Catalogo", 22), BorderLayout.NORTH);
        catalog.add(customerProductTablePanel, BorderLayout.CENTER);
        JButton addToCart = Ui.primaryButton("Aggiungi al carrello");
        addToCart.addActionListener(e -> addSelectedProductToCart(customerProductTablePanel));
        catalog.add(singleAction(addToCart), BorderLayout.SOUTH);

        JPanel cart = Ui.card(new BorderLayout());
        cart.add(cartPanel, BorderLayout.CENTER);

        workspace.add(catalog);
        workspace.add(cart);
        openWorkspaceTab(
                "customer-cart",
                "Carrello",
                "Acquisti",
                "Carrello cliente",
                "Consulta il catalogo e prepara un acquisto simulato.",
                cartNavButton,
                workspace,
                true
        );
    }

    private void showAccounts() {
        accountTablePanel.setAccounts(authService.getAccounts());
        JPanel panel = Ui.card(new BorderLayout(0, 14));
        panel.add(sectionHeader("Account registrati", "Gestisci i profili creati nell'applicazione."), BorderLayout.NORTH);
        panel.add(accountTablePanel, BorderLayout.CENTER);
        panel.add(buildAccountActions(), BorderLayout.SOUTH);
        openWorkspaceTab(
                "accounts",
                "Account",
                "Amministrazione",
                "Account",
                "Visualizza, crea o rimuovi profili interni.",
                accountsNavButton,
                panel,
                true
        );
    }

    private void showAuditLog() {
        auditTablePanel.setEvents(auditService.getEvents());
        JPanel panel = Ui.card(new BorderLayout(0, 14));
        panel.add(sectionHeader("Audit log", "Consulta le operazioni rilevanti registrate dal sistema."), BorderLayout.NORTH);
        panel.add(auditTablePanel, BorderLayout.CENTER);
        openWorkspaceTab(
                "audit",
                "Audit log",
                "Amministrazione",
                "Audit log",
                "Controlla le operazioni rilevanti registrate dal sistema.",
                auditNavButton,
                panel,
                true
        );
    }

    private void addSelectedProductToCart() {
        addSelectedProductToCart(productTablePanel);
    }

    private void addSelectedProductToCart(ProductTablePanel tablePanel) {
        if (cartPanel == null) {
            cartPanel = new CartPanel(orderService, inventoryService, auditService, username, role, this::refreshCatalog);
        }

        Prodotto selected = tablePanel.getSelectedProduct();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Seleziona un prodotto dal catalogo.", "Catalogo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        cartPanel.addProduct(selected);
    }

    private void applyProductFilters(ProductCatalogFilters filters) {
        List<Prodotto> filteredProducts = productService.getProdotti().stream()
                .filter(product -> matchesQuery(product, filters.query()))
                .filter(product -> matchesCategory(product, filters.category()))
                .filter(product -> matchesBrand(product, filters.brand()))
                .filter(product -> matchesProductType(product, filters.productType()))
                .filter(product -> matchesStock(product, filters.stock()))
                .sorted(productComparator(filters.sort()))
                .toList();
        productTablePanel.setProducts(filteredProducts);
    }

    private void refreshCatalog() {
        applyProductFilters(productFilterPanel.currentFilters());
    }

    private void applyOrderFilters(OrderCatalogFilters filters) {
        List<Order> filteredOrders = baseOrders().stream()
                .filter(order -> matchesOrderQuery(order, filters.query()))
                .filter(order -> matchesPayment(order, filters.paymentMethod()))
                .sorted(orderComparator(filters.sort()))
                .toList();
        orderTablePanel.setOrders(filteredOrders);
    }

    private List<Order> baseOrders() {
        if (canManageProducts()) {
            return orderService.getOrders();
        }
        return orderService.getOrdersByCustomer(username);
    }

    private List<FiscalDocument> baseFiscalDocuments() {
        if (canManageProducts()) {
            return fiscalDocumentService.getDocuments();
        }
        return fiscalDocumentService.getDocumentsByCustomer(username);
    }

    private boolean matchesOrderQuery(Order order, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        return order.code().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || order.customer().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || order.paymentMethod().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || order.items().stream().anyMatch(item -> matchesOrderItem(item, normalizedQuery));
    }

    private boolean matchesOrderItem(OrderItem item, String query) {
        return item.productCode().toLowerCase(Locale.ROOT).contains(query)
                || item.productName().toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean matchesPayment(Order order, String paymentMethod) {
        return OrderFilterPanel.ALL_PAYMENTS.equals(paymentMethod)
                || order.paymentMethod().equals(paymentMethod);
    }

    private Comparator<Order> orderComparator(String sort) {
        if (OrderFilterPanel.SORT_OLDEST.equals(sort)) {
            return Comparator.comparing(Order::timestamp);
        }
        if (OrderFilterPanel.SORT_CUSTOMER.equals(sort)) {
            return Comparator.comparing(Order::customer, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Order::timestamp, Comparator.reverseOrder());
        }
        if (OrderFilterPanel.SORT_TOTAL_ASC.equals(sort)) {
            return Comparator.comparingDouble(Order::total);
        }
        if (OrderFilterPanel.SORT_TOTAL_DESC.equals(sort)) {
            return Comparator.comparingDouble(Order::total).reversed();
        }
        return Comparator.comparing(Order::timestamp).reversed();
    }

    private boolean matchesQuery(Prodotto product, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        return product.getCodice().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || product.getNome().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || product.getBrand().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || product.getTipoProdotto().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || product.getUtilizzo().toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private boolean matchesCategory(Prodotto product, String category) {
        return ProductFilterPanel.ALL_CATEGORIES.equals(category)
                || product.getCategoria().equals(category);
    }

    private boolean matchesBrand(Prodotto product, String brand) {
        return ProductFilterPanel.ALL_BRANDS.equals(brand)
                || product.getBrand().equals(brand);
    }

    private boolean matchesProductType(Prodotto product, String productType) {
        return ProductFilterPanel.ALL_TYPES.equals(productType)
                || product.getTipoProdotto().equals(productType);
    }

    private boolean matchesStock(Prodotto product, String stock) {
        if (ProductFilterPanel.LOW_STOCK.equals(stock)) {
            return product.getQuantita() > 0 && product.getQuantita() <= InventoryService.LOW_STOCK_THRESHOLD;
        }
        if (ProductFilterPanel.AVAILABLE.equals(stock)) {
            return product.getQuantita() > 0;
        }
        if (ProductFilterPanel.OUT_OF_STOCK.equals(stock)) {
            return product.getQuantita() == 0;
        }
        return true;
    }

    private Comparator<Prodotto> productComparator(String sort) {
        if (ProductFilterPanel.SORT_CODE.equals(sort)) {
            return Comparator.comparing(Prodotto::getCodice, String.CASE_INSENSITIVE_ORDER);
        }
        if (ProductFilterPanel.SORT_BRAND.equals(sort)) {
            return Comparator.comparing(Prodotto::getBrand, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Prodotto::getNome, String.CASE_INSENSITIVE_ORDER);
        }
        if (ProductFilterPanel.SORT_TYPE.equals(sort)) {
            return Comparator.comparing(Prodotto::getTipoProdotto, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Prodotto::getNome, String.CASE_INSENSITIVE_ORDER);
        }
        if (ProductFilterPanel.SORT_PRICE.equals(sort)) {
            return Comparator.comparingDouble(Prodotto::getCostoScontato);
        }
        if (ProductFilterPanel.SORT_QUANTITY.equals(sort)) {
            return Comparator.comparingInt(Prodotto::getQuantita);
        }
        return Comparator.comparing(Prodotto::getNome, String.CASE_INSENSITIVE_ORDER);
    }

    private JPanel buildAdminProductActions() {
        JPanel actions = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton newButton = Ui.secondaryButton("Nuovo");
        JButton stockButton = Ui.secondaryButton("Movimento stock");
        JButton editButton = Ui.primaryButton("Modifica selezionato");
        JButton deleteButton = Ui.dangerButton("Elimina selezionati");

        newButton.addActionListener(e -> showProductForm());
        stockButton.addActionListener(e -> showStockMovementForm());
        editButton.addActionListener(e -> editSelectedProduct());
        deleteButton.addActionListener(e -> deleteSelectedProducts());

        actions.add(newButton);
        actions.add(stockButton);
        actions.add(editButton);
        actions.add(deleteButton);
        return actions;
    }

    private JPanel buildAccountActions() {
        JPanel actions = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton refreshButton = Ui.secondaryButton("Aggiorna lista");
        JButton createButton = Ui.primaryButton("Nuovo account");
        JButton deleteButton = Ui.dangerButton("Elimina selezionati");

        refreshButton.addActionListener(e -> showAccounts());
        createButton.addActionListener(e -> showAccountForm());
        deleteButton.addActionListener(e -> deleteSelectedAccounts());

        actions.add(refreshButton);
        actions.add(createButton);
        actions.add(deleteButton);
        return actions;
    }

    private void showAccountForm() {
        AccountFormPanel formPanel = new AccountFormPanel(authService, this::showAccounts, auditService, username, role);
        JPanel panel = Ui.card(new BorderLayout(0, 14));
        panel.add(sectionHeader("Nuovo account", "Crea profili interni. Gli account Admin richiedono conferma."), BorderLayout.NORTH);
        panel.add(formPanel, BorderLayout.CENTER);
        openWorkspaceTab(
                "new-account",
                "Nuovo account",
                "Amministrazione",
                "Nuovo account",
                "Crea un profilo interno con controllo esplicito sui ruoli.",
                accountsNavButton,
                panel,
                true
        );
    }

    private JPanel singleAction(JButton button) {
        JPanel actions = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(button);
        return actions;
    }

    private void editSelectedProduct() {
        List<Prodotto> selectedProducts = productTablePanel.getSelectedProducts();
        if (selectedProducts.size() != 1) {
            JOptionPane.showMessageDialog(this, "Seleziona un solo prodotto da modificare.", "Modifica prodotto", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        showProductEditForm(selectedProducts.get(0));
    }

    private void showStockMovementForm() {
        List<Prodotto> selectedProducts = productTablePanel.getSelectedProducts();
        if (selectedProducts.size() != 1) {
            JOptionPane.showMessageDialog(this, "Seleziona un solo prodotto per registrare un movimento.", "Movimento stock", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Prodotto selectedProduct = selectedProducts.get(0);

        StockMovementFormPanel formPanel = new StockMovementFormPanel(
                inventoryService,
                auditService,
                selectedProduct,
                this::showProducts,
                username,
                role
        );
        JPanel panel = Ui.card(new BorderLayout(0, 14));
        panel.add(sectionHeader("Movimento stock", "Registra un carico o uno scarico con causale."), BorderLayout.NORTH);
        panel.add(formPanel, BorderLayout.CENTER);
        openWorkspaceTab(
                "stock-movement-" + selectedProduct.getCodice(),
                "Stock " + selectedProduct.getCodice(),
                "Magazzino",
                "Movimento stock",
                "Registra un carico o uno scarico con causale tracciabile.",
                inventoryNavButton,
                panel,
                true
        );
    }

    private void deleteSelectedProducts() {
        List<Prodotto> selectedProducts = productTablePanel.getSelectedProducts();
        if (selectedProducts.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleziona uno o piu prodotti da eliminare.", "Elimina prodotti", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Eliminare " + selectedProducts.size() + " prodotto/i selezionato/i?",
                "Conferma eliminazione",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            String target = selectedProducts.stream()
                    .map(Prodotto::getCodice)
                    .reduce((first, second) -> first + ", " + second)
                    .orElse("-");
            productService.eliminaProdotti(selectedProducts);
            auditService.record(username, role, "Eliminazione prodotti", "Prodotti " + target, "Elementi eliminati: " + selectedProducts.size());
            showProducts();
        }
    }

    private void deleteSelectedAccounts() {
        List<UserAccount> selectedAccounts = accountTablePanel.getSelectedAccounts();
        if (selectedAccounts.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleziona uno o piu account da eliminare.", "Account", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        boolean containsCurrentUser = selectedAccounts.stream()
                .anyMatch(account -> account.username().equals(username));
        if (containsCurrentUser) {
            JOptionPane.showMessageDialog(this, "Non puoi eliminare l'account attualmente in uso.", "Account protetto", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Eliminare " + selectedAccounts.size() + " account selezionato/i?",
                "Conferma eliminazione account",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            List<String> usernames = selectedAccounts.stream().map(UserAccount::username).toList();
            int removed = authService.deleteAccounts(usernames);
            if (removed > 0) {
                auditService.record(username, role, "Eliminazione account", "Account " + String.join(", ", usernames), "Account eliminati: " + removed);
            }
            showAccounts();
        }
    }

    private boolean canManageProducts() {
        return AuthService.ROLE_ADMIN.equals(role) || AuthService.ROLE_EMPLOYEE.equals(role);
    }

    private boolean isAdmin() {
        return AuthService.ROLE_ADMIN.equals(role);
    }

    private JPanel sectionHeader(String title, String subtitle) {
        JPanel header = Ui.panel(new BorderLayout(0, 5));
        header.setOpaque(false);
        header.add(Ui.title(title, 22), BorderLayout.NORTH);
        header.add(Ui.text(subtitle), BorderLayout.CENTER);
        return header;
    }

    private double catalogValue(List<Prodotto> prodotti) {
        return prodotti.stream()
                .mapToDouble(prodotto -> prodotto.getCostoScontato() * prodotto.getQuantita())
                .sum();
    }

    private String formatCurrency(double value) {
        return String.format(Locale.ITALY, "%.2f euro", value);
    }

    private record WorkspaceTabInfo(
            String key,
            String eyebrow,
            String pageTitle,
            String subtitle,
            JButton navigationButton
    ) {
    }
}
