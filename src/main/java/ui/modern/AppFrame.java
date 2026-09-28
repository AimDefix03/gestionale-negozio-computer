package ui.modern;

import service.AuthService;
import service.AuditService;
import service.FiscalDocumentService;
import service.InventoryService;
import service.OrderService;
import service.ProductService;

import javax.swing.*;

public class AppFrame extends JFrame {
    private final AuthService authService = new AuthService();
    private final ProductService productService = new ProductService();
    private final InventoryService inventoryService = new InventoryService(productService);
    private final AuditService auditService = new AuditService();
    private final OrderService orderService = new OrderService();
    private final FiscalDocumentService fiscalDocumentService = new FiscalDocumentService();

    public AppFrame() {
        super("Gestionale Negozio Computer");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1240, 800);
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
        showLogin();
    }

    void showLogin() {
        setContentPane(new LoginPanel(this, authService, auditService));
        revalidate();
        repaint();
    }

    void showDashboard(String role, String username) {
        setContentPane(new DashboardPanel(this, productService, authService, auditService, inventoryService, orderService, fiscalDocumentService, role, username));
        revalidate();
        repaint();
    }
}
