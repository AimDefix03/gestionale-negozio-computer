package it.giovannidefilippo.gestionale.document;

import it.giovannidefilippo.gestionale.common.PageResponse;
import it.giovannidefilippo.gestionale.idempotency.IdempotencyService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
class FiscalDocumentController {
    private final FiscalDocumentService service;
    private final AuthSessionService authSessionService;
    private final IdempotencyService idempotencyService;

    FiscalDocumentController(FiscalDocumentService service, AuthSessionService authSessionService, IdempotencyService idempotencyService) {
        this.service = service;
        this.authSessionService = authSessionService;
        this.idempotencyService = idempotencyService;
    }

    @GetMapping
    PageResponse<FiscalDocumentResponse> findAll(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) FiscalDocumentType type
    ) {
        authSessionService.requirePermission(token, UserPermission.MANAGE_DOCUMENTS);
        return service.search(q, type, page, size);
    }

    @PostMapping("/invoice")
    @ResponseStatus(HttpStatus.CREATED)
    FiscalDocumentResponse createInvoice(
            @Valid @RequestBody FiscalDocumentRequests.CreateInvoiceRequest request,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_DOCUMENTS);
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/documents/invoice", request, FiscalDocumentResponse.class, HttpStatus.CREATED, () -> service.createInvoice(request, actor));
    }

    @PostMapping("/credit-note")
    @ResponseStatus(HttpStatus.CREATED)
    FiscalDocumentResponse createCreditNote(
            @Valid @RequestBody FiscalDocumentRequests.CreateCreditNoteRequest request,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = IdempotencyService.HEADER_NAME, required = false) String idempotencyKey
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_DOCUMENTS);
        return idempotencyService.execute(idempotencyKey, actor, "POST /api/documents/credit-note", request, FiscalDocumentResponse.class, HttpStatus.CREATED, () -> service.createCreditNote(request, actor));
    }
}
