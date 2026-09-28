package it.giovannidefilippo.gestionale.order;

import it.giovannidefilippo.gestionale.user.AuthSessionService;
import it.giovannidefilippo.gestionale.user.UserPermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financial-reconciliation")
class FinancialReconciliationController {
    private final FinancialReconciliationService service;
    private final AuthSessionService authSessionService;

    FinancialReconciliationController(FinancialReconciliationService service, AuthSessionService authSessionService) {
        this.service = service;
        this.authSessionService = authSessionService;
    }

    @GetMapping
    FinancialReconciliationResponse reconcile(
            @RequestHeader(value = "X-Session-Token", required = false) String token
    ) {
        authSessionService.requirePermission(token, UserPermission.VIEW_REPORTS);
        return service.reconcile();
    }
}
