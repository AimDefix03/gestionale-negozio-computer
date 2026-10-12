package it.giovannidefilippo.gestionale.purchase;

import it.giovannidefilippo.gestionale.common.PageResponse;
import it.giovannidefilippo.gestionale.idempotency.IdempotencyService;
import it.giovannidefilippo.gestionale.user.AuthSessionService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserPermission;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchase-orders")
class SupplierOrderController {
    private final SupplierOrderService service;
    private final AuthSessionService authSessionService;
    private final IdempotencyService idempotencyService;

    SupplierOrderController(SupplierOrderService service, AuthSessionService authSessionService, IdempotencyService idempotencyService) {
        this.service = service;
        this.authSessionService = authSessionService;
        this.idempotencyService = idempotencyService;
    }

    @GetMapping
    PageResponse<SupplierOrderSummaryResponse> search(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) SupplierOrderStatus status,
            @RequestParam(required = false) Long supplierId
    ) {
        authSessionService.requirePermission(token, UserPermission.VIEW_PURCHASE_ORDERS);
        return service.search(q, status, supplierId, page, size);
    }

    @GetMapping("/{code}")
    SupplierOrderResponse findByCode(@PathVariable String code, @RequestHeader(value = "X-Session-Token", required = false) String token) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.VIEW_PURCHASE_ORDERS);
        return service.findByCode(code, actor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SupplierOrderResponse create(
            @Valid @RequestBody SupplierOrderRequests.CreateRequest request,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_PURCHASE_ORDERS);
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/purchase-orders", request, SupplierOrderResponse.class, HttpStatus.CREATED, () -> service.create(request, actor));
    }

    @PostMapping("/{code}/send")
    SupplierOrderResponse send(
            @PathVariable String code,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_PURCHASE_ORDERS);
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/purchase-orders/{code}/send", code, SupplierOrderResponse.class, HttpStatus.OK, () -> service.send(code, actor));
    }

    @PostMapping("/{code}/receipts")
    SupplierOrderResponse receive(
            @PathVariable String code,
            @Valid @RequestBody SupplierOrderRequests.ReceiveRequest request,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_PURCHASE_ORDERS);
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/purchase-orders/{code}/receipts", new OperationPayload(code, request), SupplierOrderResponse.class, HttpStatus.OK, () -> service.receive(code, request, actor));
    }

    @PostMapping("/{code}/cancel")
    SupplierOrderResponse cancel(
            @PathVariable String code,
            @Valid @RequestBody SupplierOrderRequests.CancelRequest request,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_PURCHASE_ORDERS);
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/purchase-orders/{code}/cancel", new OperationPayload(code, request), SupplierOrderResponse.class, HttpStatus.OK, () -> service.cancel(code, request, actor));
    }

    private record OperationPayload(String orderCode, Object request) {
    }
}
