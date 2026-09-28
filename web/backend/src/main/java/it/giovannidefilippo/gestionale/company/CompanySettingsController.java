package it.giovannidefilippo.gestionale.company;

import it.giovannidefilippo.gestionale.user.AuthSessionService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserPermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company-settings")
class CompanySettingsController {
    private final CompanySettingsService service;
    private final AuthSessionService authSessionService;

    CompanySettingsController(CompanySettingsService service, AuthSessionService authSessionService) {
        this.service = service;
        this.authSessionService = authSessionService;
    }

    @GetMapping
    CompanySettingsResponse current(@RequestHeader(value = "X-Session-Token", required = false) String token) {
        authSessionService.requirePermission(token, UserPermission.MANAGE_COMPANY_SETTINGS);
        return service.current();
    }

    @PutMapping
    CompanySettingsResponse update(
            @Valid @RequestBody CompanySettingsRequests.UpdateRequest request,
            @RequestHeader(value = "X-Session-Token", required = false) String token
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_COMPANY_SETTINGS);
        return service.update(request, actor);
    }
}
