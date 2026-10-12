package it.giovannidefilippo.gestionale.audit;

import it.giovannidefilippo.gestionale.common.PageResponse;
import it.giovannidefilippo.gestionale.user.AuthSessionService;
import it.giovannidefilippo.gestionale.user.UserPermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
class AuditController {
    private final AuditService auditService;
    private final AuthSessionService authSessionService;

    AuditController(AuditService auditService, AuthSessionService authSessionService) {
        this.auditService = auditService;
        this.authSessionService = authSessionService;
    }

    @GetMapping
    PageResponse<AuditEventResponse> findAll(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) AuditCategory category,
            @RequestParam(required = false) AuditSeverity severity
    ) {
        authSessionService.requirePermission(token, UserPermission.VIEW_AUDIT);
        return auditService.search(q, category, severity, page, size);
    }
}
