package it.giovannidefilippo.gestionale.dashboard;

import it.giovannidefilippo.gestionale.inventory.InventoryService;
import it.giovannidefilippo.gestionale.product.ProductDashboardSummary;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.order.OrderService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserPermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private static final int LOW_STOCK_THRESHOLD = 3;
    private static final int RECENT_ITEMS_LIMIT = 4;

    private final ProductService productService;
    private final OrderService orderService;
    private final InventoryService inventoryService;

    DashboardService(ProductService productService, OrderService orderService, InventoryService inventoryService) {
        this.productService = productService;
        this.orderService = orderService;
        this.inventoryService = inventoryService;
    }

    public DashboardResponse summary(AuthenticatedUser actor) {
        ProductDashboardSummary products = productService.dashboardSummary(LOW_STOCK_THRESHOLD);
        return new DashboardResponse(
                products.products(),
                products.inventoryValue(),
                products.lowStock(),
                products.outOfStock(),
                orderService.countForDashboard(actor),
                orderService.revenueForDashboard(actor),
                orderService.recentForDashboard(actor, RECENT_ITEMS_LIMIT),
                actor.hasPermission(UserPermission.MANAGE_INVENTORY) ? inventoryService.recentMovements(RECENT_ITEMS_LIMIT) : List.of()
        );
    }
}
