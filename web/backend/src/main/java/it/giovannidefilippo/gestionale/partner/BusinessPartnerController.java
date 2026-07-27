package it.giovannidefilippo.gestionale.partner;

import it.giovannidefilippo.gestionale.common.PageResponse;
import it.giovannidefilippo.gestionale.user.AuthSessionService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserPermission;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/partners")
class BusinessPartnerController {
    private final BusinessPartnerService service;
    private final AuthSessionService authSessionService;

    BusinessPartnerController(BusinessPartnerService service, AuthSessionService authSessionService) {
        this.service = service;
        this.authSessionService = authSessionService;
    }

    @GetMapping
    PageResponse<BusinessPartnerResponse> findAll(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BusinessPartnerType type,
            @RequestParam(required = false) Boolean active
    ) {
        authSessionService.requirePermission(token, UserPermission.VIEW_PARTNERS);
        return service.search(q, type, active, page, size);
    }

    @GetMapping("/{code}")
    BusinessPartnerResponse findByCode(@PathVariable String code, @RequestHeader(value = "X-Session-Token", required = false) String token) {
        authSessionService.requirePermission(token, UserPermission.VIEW_PARTNERS);
        return service.findByCode(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BusinessPartnerResponse create(@Valid @RequestBody BusinessPartnerRequest request, @RequestHeader(value = "X-Session-Token", required = false) String token) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_PARTNERS);
        return service.create(request, actor.username(), actor.roleLabel());
    }

    @PutMapping("/{code}")
    BusinessPartnerResponse update(@PathVariable String code, @Valid @RequestBody BusinessPartnerRequest request, @RequestHeader(value = "X-Session-Token", required = false) String token) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_PARTNERS);
        return service.update(code, request, actor.username(), actor.roleLabel());
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@PathVariable String code, @RequestHeader(value = "X-Session-Token", required = false) String token) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_PARTNERS);
        service.deactivate(code, actor.username(), actor.roleLabel());
    }
}
