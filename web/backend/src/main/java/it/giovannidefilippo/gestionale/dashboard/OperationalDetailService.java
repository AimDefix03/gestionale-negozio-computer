package it.giovannidefilippo.gestionale.dashboard;

import it.giovannidefilippo.gestionale.document.FiscalDocumentService;
import it.giovannidefilippo.gestionale.inventory.InventoryService;
import it.giovannidefilippo.gestionale.order.OrderService;
import it.giovannidefilippo.gestionale.product.ProductService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OperationalDetailService {
    private static final int HISTORY_LIMIT = 4;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final FiscalDocumentService documentService;

    OperationalDetailService(ProductService productService, InventoryService inventoryService, OrderService orderService, FiscalDocumentService documentService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.orderService = orderService;
        this.documentService = documentService;
    }

    public ProductOperationalDetailResponse product(String code, AuthenticatedUser actor) {
        return new ProductOperationalDetailResponse(
                productService.findByCode(code, actor),
                inventoryService.recentForProduct(code, HISTORY_LIMIT),
                orderService.recentByProduct(code, HISTORY_LIMIT)
        );
    }

    public OrderOperationalDetailResponse order(String code, AuthenticatedUser actor) {
        return new OrderOperationalDetailResponse(
                orderService.findByCode(code, actor),
                documentService.capabilitiesForOrder(code, actor)
        );
    }
}
