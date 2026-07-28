package it.giovannidefilippo.gestionale.user;

import it.giovannidefilippo.gestionale.common.PageResponse;
import it.giovannidefilippo.gestionale.common.OperationalMetrics;
import it.giovannidefilippo.gestionale.common.UnauthorizedException;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
class UserController {
    private final UserService userService;
    private final AuthSessionService authSessionService;
    private final PasswordStrengthService passwordStrengthService;
    private final OperationalMetrics operationalMetrics;

    UserController(UserService userService, AuthSessionService authSessionService, PasswordStrengthService passwordStrengthService, OperationalMetrics operationalMetrics) {
        this.userService = userService;
        this.authSessionService = authSessionService;
        this.passwordStrengthService = passwordStrengthService;
        this.operationalMetrics = operationalMetrics;
    }

    @PostMapping("/login")
    AuthSessionResponse login(@Valid @RequestBody UserRequests.LoginRequest request, HttpServletResponse response) {
        try {
            authSessionService.assertLoginAllowed(request.username());
            UserResponse user = userService.login(request.username(), request.password(), request.role());
            authSessionService.clearFailedLogin(request.username());
            preventCaching(response);
            AuthSessionResponse session = authSessionService.create(user);
            operationalMetrics.recordAuthentication(OperationalMetrics.AuthenticationOutcome.SUCCESS);
            return session;
        } catch (IllegalArgumentException exception) {
            operationalMetrics.recordAuthentication(OperationalMetrics.AuthenticationOutcome.INVALID_CREDENTIALS);
            authSessionService.registerFailedLogin(request.username());
            throw exception;
        } catch (UnauthorizedException exception) {
            operationalMetrics.recordAuthentication(OperationalMetrics.AuthenticationOutcome.LOCKED);
            throw exception;
        }
    }

    @PostMapping("/session/renew")
    AuthSessionResponse renewSession(
            @Valid @RequestBody UserRequests.PasswordRequest request,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            Authentication authentication,
            HttpServletResponse response
    ) {
        try {
            userService.verifyPassword(authentication.getName(), request.password());
            preventCaching(response);
            AuthSessionResponse session = authSessionService.rotate(token);
            operationalMetrics.recordSessionEvent(OperationalMetrics.SessionEvent.RENEWED);
            return session;
        } catch (RuntimeException exception) {
            operationalMetrics.recordSessionEvent(OperationalMetrics.SessionEvent.RENEWAL_FAILED);
            throw exception;
        }
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@RequestHeader(value = "X-Session-Token", required = false) String token) {
        authSessionService.logout(token);
        operationalMetrics.recordSessionEvent(OperationalMetrics.SessionEvent.LOGOUT);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@Valid @RequestBody UserRequests.RegisterRequest request) {
        return userService.registerPublic(request.username(), request.password(), request.role());
    }

    @GetMapping
    PageResponse<UserResponse> findAll(
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UserRole role
    ) {
        authSessionService.requireManageAccounts(token);
        return userService.search(q, role, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse create(
            @Valid @RequestBody UserRequests.CreateAccountRequest request,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = "X-Reauth-Password", required = false) String reauthPassword
    ) {
        AuthenticatedUser actor = authSessionService.requireManageAccounts(token);
        requirePasswordConfirmation(actor, reauthPassword);
        return userService.createAccount(request.username(), request.password(), request.role(), actor.username(), "Creazione da pannello admin");
    }

    @DeleteMapping("/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @PathVariable String username,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = "X-Reauth-Password", required = false) String reauthPassword
    ) {
        AuthenticatedUser actor = authSessionService.requireManageAccounts(token);
        requirePasswordConfirmation(actor, reauthPassword);
        userService.delete(username, actor.username());
    }

    @PostMapping("/bulk-delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteMany(
            @RequestBody UserRequests.DeleteAccountsRequest request,
            @RequestHeader(value = "X-Session-Token", required = false) String token,
            @RequestHeader(value = "X-Reauth-Password", required = false) String reauthPassword
    ) {
        AuthenticatedUser actor = authSessionService.requireManageAccounts(token);
        requirePasswordConfirmation(actor, reauthPassword);
        userService.deleteMany(request.usernames(), actor.username());
    }

    @PostMapping("/password-strength")
    PasswordStrengthResponse passwordStrength(@Valid @RequestBody UserRequests.PasswordRequest request) {
        return passwordStrengthService.evaluate(request.password());
    }

    private void requirePasswordConfirmation(AuthenticatedUser actor, String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Conferma la password della sessione per continuare.");
        }
        userService.verifyPassword(actor.username(), password);
    }

    private void preventCaching(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
    }
}
