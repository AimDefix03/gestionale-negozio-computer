package it.giovannidefilippo.gestionale.order;

import it.giovannidefilippo.gestionale.common.ForbiddenException;
import it.giovannidefilippo.gestionale.common.PageResponse;
import it.giovannidefilippo.gestionale.idempotency.IdempotencyService;
import it.giovannidefilippo.gestionale.user.AuthSessionService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserPermission;
import it.giovannidefilippo.gestionale.user.UserRole;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
class OrderController {
    private final OrderService service;
    private final AuthSessionService authSessionService;
    private final IdempotencyService idempotencyService;

    OrderController(OrderService service, AuthSessionService authSessionService, IdempotencyService idempotencyService) {
        this.service = service;
        this.authSessionService = authSessionService;
        this.idempotencyService = idempotencyService;
    }

    @GetMapping
    PageResponse<OrderResponse> findAll(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String customer
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.VIEW_ORDERS);
        if (actor.role() == UserRole.CUSTOMER) {
            return service.search(q, actor.username(), page, size);
        }
        return service.search(q, customer, page, size);
    }

    @GetMapping("/customer/{customer}")
    PageResponse<OrderResponse> findByCustomer(
            @PathVariable String customer,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String q
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.VIEW_ORDERS);
        if (actor.role() == UserRole.CUSTOMER && !actor.username().equalsIgnoreCase(customer)) {
            throw new ForbiddenException("Puoi visualizzare solo i tuoi ordini.");
        }
        return service.search(q, customer, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrderResponse create(
            @Valid @RequestBody OrderRequests.CreateOrderRequest request,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.CREATE_ORDERS);
        String customer = actor.role() == UserRole.CUSTOMER ? actor.username() : request.customer();
        CreateOrderIdempotencyPayload payload = new CreateOrderIdempotencyPayload(customer, request.customerCode(), request.paymentMethod(), request.items());
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/orders", payload, OrderResponse.class, HttpStatus.CREATED, () -> service.create(request, customer, actor));
    }

    @PostMapping("/{code}/confirm")
    OrderResponse confirm(
            @PathVariable String code,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.CONFIRM_ORDERS);
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/orders/{code}/confirm", code, OrderResponse.class, HttpStatus.OK, () -> service.confirm(code, actor));
    }

    @PostMapping("/{code}/fulfill")
    OrderResponse fulfill(
            @PathVariable String code,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.FULFILL_ORDERS);
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/orders/{code}/fulfill", code, OrderResponse.class, HttpStatus.OK, () -> service.fulfill(code, actor));
    }

    @PostMapping("/{code}/cancel")
    OrderResponse cancel(
            @PathVariable String code,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.CANCEL_ORDERS);
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/orders/{code}/cancel", code, OrderResponse.class, HttpStatus.OK, () -> service.cancel(code, actor));
    }

    @PostMapping("/{code}/payments/receipts")
    OrderResponse recordReceipt(
            @PathVariable String code,
            @Valid @RequestBody OrderOperationRequests.ReceiptRequest request,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.RECORD_PAYMENTS);
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/orders/{code}/payments/receipts", new OperationPayload(code, request), OrderResponse.class, HttpStatus.OK, () -> service.recordReceipt(code, request, actor));
    }

    @PostMapping("/{code}/returns")
    @ResponseStatus(HttpStatus.CREATED)
    OrderResponse requestReturn(
            @PathVariable String code,
            @Valid @RequestBody OrderOperationRequests.ReturnRequest request,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.REQUEST_RETURNS);
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/orders/{code}/returns", new OperationPayload(code, request), OrderResponse.class, HttpStatus.CREATED, () -> service.requestReturn(code, request, actor));
    }

    @PostMapping("/{code}/returns/{returnCode}/approve")
    OrderResponse approveReturn(
            @PathVariable String code,
            @PathVariable String returnCode,
            @Valid @RequestBody OrderOperationRequests.ReturnReviewRequest request,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_RETURNS);
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/orders/{code}/returns/{returnCode}/approve", new ReturnOperationPayload(code, returnCode, request), OrderResponse.class, HttpStatus.OK, () -> service.approveReturn(code, returnCode, request, actor));
    }

    @PostMapping("/{code}/returns/{returnCode}/reject")
    OrderResponse rejectReturn(
            @PathVariable String code,
            @PathVariable String returnCode,
            @Valid @RequestBody OrderOperationRequests.ReturnRejectionRequest request,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_RETURNS);
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/orders/{code}/returns/{returnCode}/reject", new ReturnOperationPayload(code, returnCode, request), OrderResponse.class, HttpStatus.OK, () -> service.rejectReturn(code, returnCode, request, actor));
    }

    @PostMapping("/{code}/returns/{returnCode}/receive")
    OrderResponse receiveReturn(
            @PathVariable String code,
            @PathVariable String returnCode,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_RETURNS);
        ReturnOperationPayload payload = new ReturnOperationPayload(code, returnCode, null);
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/orders/{code}/returns/{returnCode}/receive", payload, OrderResponse.class, HttpStatus.OK, () -> service.receiveReturn(code, returnCode, actor));
    }

    @PostMapping("/{code}/returns/{returnCode}/refund")
    OrderResponse refundReturn(
            @PathVariable String code,
            @PathVariable String returnCode,
            @Valid @RequestBody OrderOperationRequests.ReturnRefundRequest request,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.REFUND_PAYMENTS);
        if (!actor.hasPermission(UserPermission.MANAGE_RETURNS)) {
            throw new ForbiddenException("Servono i permessi di gestione resi per registrare il rimborso.");
        }
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/orders/{code}/returns/{returnCode}/refund", new ReturnOperationPayload(code, returnCode, request), OrderResponse.class, HttpStatus.OK, () -> service.refundReturn(code, returnCode, request, actor));
    }

    private record CreateOrderIdempotencyPayload(
            String customer,
            String customerCode,
            PaymentMethod paymentMethod,
            List<OrderRequests.CreateOrderItemRequest> items
    ) {
    }

    private record OperationPayload(String orderCode, Object request) {
    }

    private record ReturnOperationPayload(String orderCode, String returnCode, Object request) {
    }
}
