package it.giovannidefilippo.gestionale.inventory;

import it.giovannidefilippo.gestionale.common.PageResponse;
import it.giovannidefilippo.gestionale.idempotency.IdempotencyService;
import it.giovannidefilippo.gestionale.product.ProductResponse;
import it.giovannidefilippo.gestionale.user.AuthSessionService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserPermission;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
class InventoryController {
    private final InventoryService service;
    private final AuthSessionService authSessionService;
    private final IdempotencyService idempotencyService;

    InventoryController(InventoryService service, AuthSessionService authSessionService, IdempotencyService idempotencyService) {
        this.service = service;
        this.authSessionService = authSessionService;
        this.idempotencyService = idempotencyService;
    }

    @GetMapping("/movements")
    PageResponse<StockMovementResponse> movements(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) StockMovementType type,
            @RequestParam(required = false) String productCode
    ) {
        authSessionService.requirePermission(token, UserPermission.MANAGE_INVENTORY);
        return service.search(q, type, productCode, page, size);
    }

    @PostMapping("/movements")
    @ResponseStatus(HttpStatus.CREATED)
    StockMovementResponse register(
            @Valid @RequestBody StockMovementRequest request,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_INVENTORY);
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/inventory/movements", request, StockMovementResponse.class, HttpStatus.CREATED, () -> service.register(request, actor));
    }

    @GetMapping("/low-stock")
    List<ProductResponse> lowStock(@RequestHeader(value = "X-Session-Token", required = false) String token) {
        authSessionService.requirePermission(token, UserPermission.MANAGE_INVENTORY);
        return service.lowStockProducts();
    }
}
