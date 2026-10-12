package it.giovannidefilippo.gestionale.inventory;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory/counts")
class PhysicalInventoryController {
    private final PhysicalInventoryService service;
    private final AuthSessionService authSessionService;
    private final IdempotencyService idempotencyService;

    PhysicalInventoryController(PhysicalInventoryService service, AuthSessionService authSessionService, IdempotencyService idempotencyService) {
        this.service = service;
        this.authSessionService = authSessionService;
        this.idempotencyService = idempotencyService;
    }

    @GetMapping
    PageResponse<PhysicalInventorySessionResponse> search(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(required = false) PhysicalInventoryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_INVENTORY);
        return service.search(status, page, size, actor);
    }

    @GetMapping("/{id}")
    PhysicalInventorySessionResponse find(
            @PathVariable Long id,
            @RequestHeader(value = "X-Session-Token", required = false) String token
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_INVENTORY);
        return service.find(id, actor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PhysicalInventorySessionResponse create(
            @Valid @RequestBody PhysicalInventoryRequests.Create request,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_INVENTORY);
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/inventory/counts", request,
                PhysicalInventorySessionResponse.class, HttpStatus.CREATED, () -> service.create(request, actor));
    }

    @PutMapping("/{sessionId}/items/{itemId}")
    PhysicalInventorySessionResponse count(
            @PathVariable Long sessionId,
            @PathVariable Long itemId,
            @Valid @RequestBody PhysicalInventoryRequests.Count request,
            @RequestHeader(value = "X-Session-Token", required = false) String token
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_INVENTORY);
        return service.recordCount(sessionId, itemId, request, actor);
    }

    @PostMapping("/{id}/submit")
    PhysicalInventorySessionResponse submit(
            @PathVariable Long id,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_INVENTORY);
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/inventory/counts/" + id + "/submit", id,
                PhysicalInventorySessionResponse.class, HttpStatus.OK, () -> service.submit(id, actor));
    }

    @PostMapping("/{id}/approve")
    PhysicalInventorySessionResponse approve(
            @PathVariable Long id,
            @Valid @RequestBody PhysicalInventoryRequests.Decision request,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.APPROVE_INVENTORY_COUNTS);
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/inventory/counts/" + id + "/approve", request,
                PhysicalInventorySessionResponse.class, HttpStatus.OK, () -> service.approve(id, request, actor));
    }

    @PostMapping("/{id}/cancel")
    PhysicalInventorySessionResponse cancel(
            @PathVariable Long id,
            @Valid @RequestBody PhysicalInventoryRequests.Decision request,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_INVENTORY);
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/inventory/counts/" + id + "/cancel", request,
                PhysicalInventorySessionResponse.class, HttpStatus.OK, () -> service.cancel(id, request, actor));
    }
}
