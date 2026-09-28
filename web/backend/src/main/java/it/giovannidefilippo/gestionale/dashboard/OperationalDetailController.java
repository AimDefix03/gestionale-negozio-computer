package it.giovannidefilippo.gestionale.dashboard;

import it.giovannidefilippo.gestionale.user.AuthSessionService;
import it.giovannidefilippo.gestionale.user.AuthenticatedUser;
import it.giovannidefilippo.gestionale.user.UserPermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class OperationalDetailController {
    private final OperationalDetailService service;
    private final AuthSessionService authSessionService;

    OperationalDetailController(OperationalDetailService service, AuthSessionService authSessionService) {
        this.service = service;
        this.authSessionService = authSessionService;
    }

    @GetMapping("/products/{code}/detail")
    ProductOperationalDetailResponse product(
            @PathVariable String code,
            @RequestHeader(value = "X-Session-Token", required = false) String token
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.MANAGE_INVENTORY);
        return service.product(code, actor);
    }

    @GetMapping("/orders/{code}/detail")
    OrderOperationalDetailResponse order(
            @PathVariable String code,
            @RequestHeader(value = "X-Session-Token", required = false) String token
    ) {
        AuthenticatedUser actor = authSessionService.requirePermission(token, UserPermission.VIEW_ORDERS);
        return service.order(code, actor);
    }
}
