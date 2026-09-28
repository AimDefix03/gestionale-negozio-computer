package it.giovannidefilippo.gestionale.system;

import it.giovannidefilippo.gestionale.user.AuthSessionService;
import it.giovannidefilippo.gestionale.user.UserPermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemStatusController {
    private final AuthSessionService authSessionService;
    private final SystemStatusService systemStatusService;

    SystemStatusController(AuthSessionService authSessionService, SystemStatusService systemStatusService) {
        this.authSessionService = authSessionService;
        this.systemStatusService = systemStatusService;
    }

    @GetMapping("/status")
    public SystemStatusResponse status(@RequestHeader(value = "X-Session-Token", required = false) String token) {
        authSessionService.requirePermission(token, UserPermission.VIEW_AUDIT);
        return systemStatusService.currentStatus();
    }
}
