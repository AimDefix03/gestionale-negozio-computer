package it.giovannidefilippo.gestionale.dashboard;

import it.giovannidefilippo.gestionale.user.AuthSessionService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
class DashboardController {
    private final DashboardService dashboardService;
    private final AuthSessionService authSessionService;

    DashboardController(DashboardService dashboardService, AuthSessionService authSessionService) {
        this.dashboardService = dashboardService;
        this.authSessionService = authSessionService;
    }

    @GetMapping
    DashboardResponse summary(@RequestHeader(value = "X-Session-Token", required = false) String token) {
        AuthenticatedUser actor = authSessionService.require(token);
        return dashboardService.summary(actor);
    }
}
