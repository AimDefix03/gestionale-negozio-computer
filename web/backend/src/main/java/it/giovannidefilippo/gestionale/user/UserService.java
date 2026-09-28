package it.giovannidefilippo.gestionale.user;

import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditService;
import it.giovannidefilippo.gestionale.audit.AuditSeverity;
import it.giovannidefilippo.gestionale.common.PageRequests;
import it.giovannidefilippo.gestionale.common.PageResponse;
import it.giovannidefilippo.gestionale.common.DatabaseConstraintViolations;
import it.giovannidefilippo.gestionale.common.ResourceConflictException;
import it.giovannidefilippo.gestionale.common.TimeProvider;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UserService {
    private static final String SYSTEM_ACTOR = "Sistema";
    private static final String SELF_SERVICE_ACTOR = "SELF_SERVICE";
    private static final String SELF_SERVICE_ROLE = "Pubblico";

    private final UserAccountRepository repository;
    private final PasswordHasher passwordHasher;
    private final AuditService auditService;
    private final PasswordStrengthService passwordStrengthService;
    private final TimeProvider timeProvider;
    private final AuthSessionService authSessionService;

    UserService(
            UserAccountRepository repository,
            PasswordHasher passwordHasher,
            AuditService auditService,
            PasswordStrengthService passwordStrengthService,
            TimeProvider timeProvider,
            AuthSessionService authSessionService
    ) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.auditService = auditService;
        this.passwordStrengthService = passwordStrengthService;
        this.timeProvider = timeProvider;
        this.authSessionService = authSessionService;
    }

    public List<UserResponse> findAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(UserAccount::getUsername))
                .map(UserResponse::from)
                .toList();
    }

    public PageResponse<UserResponse> search(String q, UserRole role, Boolean enabled, int page, int size) {
        return PageResponse.from(repository.findAll(
                specification(q, role, enabled),
                PageRequests.of(page, size, Sort.by("username").ascending())
        ).map(UserResponse::from));
    }

    public boolean hasAccounts() {
        return repository.count() > 0;
    }

    public UserResponse requireCustomerAccount(long accountId) {
        UserAccount account = repository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account cliente non trovato."));
        if (account.getRole() != UserRole.CUSTOMER) {
            throw new IllegalArgumentException("Puoi collegare soltanto un account cliente.");
        }
        return UserResponse.from(account);
    }

    @Transactional
    public UserResponse login(String username, String password) {
        UserAccount account = repository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("Credenziali non valide."));
        boolean passwordMatches = passwordHasher.matches(
                password,
                account.getPasswordSalt(),
                account.getPasswordHash()
        );
        if (!passwordMatches
                || !account.isEnabled()
                || account.requiresOperationalReview()) {
            throw new IllegalArgumentException("Credenziali non valide.");
        }
        auditService.record(account.getUsername(), account.getRole().getLabel(), "LOGIN", account.getUsername(), "Accesso effettuato", AuditCategory.SECURITY, AuditSeverity.INFO, "SESSION");
        return UserResponse.from(account);
    }

    public void verifyPassword(String username, String password) {
        UserAccount account = repository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("Account non trovato."));
        if (!passwordHasher.matches(password, account.getPasswordSalt(), account.getPasswordHash())) {
            throw new IllegalArgumentException("Password di conferma non valida.");
        }
    }

    @Transactional
    public UserResponse registerPublic(String username, String password) {
        return persistAccount(
                username,
                password,
                UserRole.CUSTOMER,
                AccountProvisioningSource.SELF_SERVICE,
                SELF_SERVICE_ACTOR,
                SELF_SERVICE_ROLE,
                "Registrazione pubblica self-service"
        );
    }

    @Transactional
    public UserResponse createAccount(String username, String password, UserRole role, String actor, String details) {
        UserRole actorRole = resolveActorRole(actor);
        if (!isSystemActor(actor)) {
            validateAccountCreation(role, actorRole);
        }

        return persistAccount(
                username,
                password,
                role,
                provisioningSource(actor, details),
                actor,
                actorRole.getLabel(),
                details
        );
    }

    private UserResponse persistAccount(
            String username,
            String password,
            UserRole role,
            AccountProvisioningSource provisioningSource,
            String actor,
            String actorRole,
            String details
    ) {
        passwordStrengthService.validateStrongPassword(username, password);
        PasswordHasher.PasswordHash passwordHash = passwordHasher.hash(password);
        UserAccount account = saveAndFlush(new UserAccount(
                username.trim(),
                passwordHash.salt(),
                passwordHash.hash(),
                role,
                provisioningSource,
                true,
                timeProvider.instant(),
                actor
        ));
        auditService.record(actor, actorRole, "CREATE_ACCOUNT", account.getUsername(), details + " - ruolo " + role.getLabel(), AuditCategory.ACCOUNT, role == UserRole.ADMIN ? AuditSeverity.CRITICAL : AuditSeverity.WARNING, "USER_ACCOUNT");
        return UserResponse.from(account);
    }

    private UserAccount saveAndFlush(UserAccount account) {
        try {
            return repository.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            if (DatabaseConstraintViolations.matches(exception, "uk_user_accounts_username", "uk_user_accounts_username_canonical")) {
                throw new ResourceConflictException("Esiste gia un account con questo username.");
            }
            throw exception;
        }
    }

    @Transactional
    public UserResponse disable(String username, String reason, String actor) {
        UserAccount account = requireAccount(username);
        UserAccount operator = requireOperator(actor);
        validateAdministrativeTarget(account, operator, "disabilitare");
        if (!account.isEnabled()) {
            return UserResponse.from(account);
        }
        String normalizedReason = requireReason(reason);
        account.disable(timeProvider.instant(), operator.getUsername(), normalizedReason);
        authSessionService.revokeAllForAccount(account.getId());
        auditService.record(operator.getUsername(), operator.getRole().getLabel(), "DISABLE_ACCOUNT", account.getUsername(), normalizedReason + " - ruolo " + account.getRole().getLabel() + "; sessioni revocate", AuditCategory.ACCOUNT, AuditSeverity.CRITICAL, "USER_ACCOUNT");
        return UserResponse.from(account);
    }

    @Transactional
    public UserResponse enable(String username, String reason, String actor) {
        UserAccount account = requireAccount(username);
        UserAccount operator = requireOperator(actor);
        validateAdministrativeTarget(account, operator, "riabilitare");
        if (account.isEnabled()) {
            return UserResponse.from(account);
        }
        String normalizedReason = requireReason(reason);
        account.enable();
        auditService.record(operator.getUsername(), operator.getRole().getLabel(), "ENABLE_ACCOUNT", account.getUsername(), normalizedReason + " - le sessioni precedenti restano revocate", AuditCategory.ACCOUNT, AuditSeverity.CRITICAL, "USER_ACCOUNT");
        return UserResponse.from(account);
    }

    @Transactional
    public void delete(String username, String actor) {
        disable(username, "Disattivazione tramite endpoint DELETE compatibile", actor);
    }

    @Transactional
    public void deleteMany(List<String> usernames, String actor) {
        if (usernames == null || usernames.isEmpty()) {
            throw new IllegalArgumentException("Seleziona almeno un account.");
        }
        usernames.forEach(username -> delete(username, actor));
    }

    @Transactional
    public void changeOwnPassword(String username, String currentPassword, String newPassword) {
        UserAccount account = requireAccount(username);
        verifyPassword(account.getUsername(), currentPassword);
        passwordStrengthService.validateStrongPassword(account.getUsername(), newPassword);
        if (passwordHasher.matches(newPassword, account.getPasswordSalt(), account.getPasswordHash())) {
            throw new IllegalArgumentException("La nuova password deve essere diversa da quella corrente.");
        }
        PasswordHasher.PasswordHash passwordHash = passwordHasher.hash(newPassword);
        account.changePassword(passwordHash.salt(), passwordHash.hash());
        authSessionService.revokeAllForAccount(account.getId());
        auditService.record(
                account.getUsername(),
                account.getRole().getLabel(),
                "CHANGE_OWN_PASSWORD",
                account.getUsername(),
                "Password personale aggiornata e tutte le sessioni revocate",
                AuditCategory.SECURITY,
                AuditSeverity.CRITICAL,
                "USER_ACCOUNT"
        );
    }

    @Transactional
    public void resetPassword(String username, String newPassword, String reason, String actor) {
        UserAccount account = requireAccount(username);
        UserAccount operator = requireOperator(actor);
        validateAdministrativeTarget(account, operator, "reimpostare la password di");
        passwordStrengthService.validateStrongPassword(account.getUsername(), newPassword);
        PasswordHasher.PasswordHash passwordHash = passwordHasher.hash(newPassword);
        account.changePassword(passwordHash.salt(), passwordHash.hash());
        authSessionService.revokeAllForAccount(account.getId());
        auditService.record(operator.getUsername(), operator.getRole().getLabel(), "ADMIN_RESET_PASSWORD", account.getUsername(), requireReason(reason) + "; tutte le sessioni revocate", AuditCategory.SECURITY, AuditSeverity.CRITICAL, "USER_ACCOUNT");
    }

    @Transactional
    public int revokeSessions(String username, String reason, String actor) {
        UserAccount account = requireAccount(username);
        UserAccount operator = requireOperator(actor);
        validateAdministrativeTarget(account, operator, "revocare le sessioni di");
        int revoked = authSessionService.revokeAllForAccount(account.getId());
        auditService.record(operator.getUsername(), operator.getRole().getLabel(), "REVOKE_ACCOUNT_SESSIONS", account.getUsername(), requireReason(reason) + "; sessioni revocate: " + revoked, AuditCategory.SECURITY, AuditSeverity.CRITICAL, "SESSION");
        return revoked;
    }

    @Transactional
    public UserResponse changeRole(String username, UserRole newRole, String actor) {
        return changeRole(username, newRole, "Modifica ruolo amministrativa", actor);
    }

    @Transactional
    public UserResponse changeRole(String username, UserRole newRole, String reason, String actor) {
        UserAccount account = requireAccount(username);
        UserAccount operator = requireOperator(actor);
        if (operator.getRole() != UserRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Solo il super admin può modificare il ruolo di un account.");
        }
        if (account.getId().equals(operator.getId())) {
            throw new IllegalArgumentException("Non puoi modificare il ruolo della sessione corrente.");
        }
        if (account.getRole() == UserRole.SUPER_ADMIN || newRole == UserRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Il ruolo super admin non è modificabile dal pannello operativo.");
        }
        account.changeRole(newRole);
        authSessionService.revokeAllForAccount(account.getId());
        auditService.record(
                operator.getUsername(),
                operator.getRole().getLabel(),
                "CHANGE_ACCOUNT_ROLE",
                account.getUsername(),
                requireReason(reason) + "; ruolo aggiornato a " + newRole.getLabel() + " e sessioni revocate",
                AuditCategory.ACCOUNT,
                AuditSeverity.CRITICAL,
                "USER_ACCOUNT"
        );
        return UserResponse.from(account);
    }

    private UserRole resolveActorRole(String actor) {
        if (isSystemActor(actor)) {
            return UserRole.SUPER_ADMIN;
        }
        return repository.findByUsernameIgnoreCase(actor.trim())
                .map(UserAccount::getRole)
                .orElseThrow(() -> new IllegalArgumentException("Operatore non riconosciuto."));
    }

    private void validateAccountCreation(UserRole role, UserRole actorRole) {
        if (!actorRole.canManageAccounts()) {
            throw new IllegalArgumentException("Non hai i permessi per creare account.");
        }
        if (role == UserRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("La creazione di super admin non è disponibile dal pannello operativo.");
        }
        if (role == UserRole.ADMIN && !actorRole.canCreateAdmin()) {
            throw new IllegalArgumentException("Solo il super admin può creare altri account admin.");
        }
    }

    private void validateAdministrativeTarget(UserAccount account, UserAccount operator, String action) {
        if (!operator.getRole().canManageAccounts()) {
            throw new IllegalArgumentException("Non hai i permessi per gestire account.");
        }
        if (account.getId().equals(operator.getId())) {
            throw new IllegalArgumentException("Non puoi " + action + " il tuo account mentre sei autenticato.");
        }
        if (account.getRole() == UserRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Il super admin non può essere gestito dal pannello operativo.");
        }
        if (account.getRole() == UserRole.ADMIN && operator.getRole() != UserRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Solo il super admin può gestire account admin.");
        }
    }

    private UserAccount requireAccount(String username) {
        return repository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("Account non trovato."));
    }

    private UserAccount requireOperator(String actor) {
        return repository.findByUsernameIgnoreCase(actor.trim())
                .orElseThrow(() -> new IllegalArgumentException("Operatore non riconosciuto."));
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Inserisci una motivazione per continuare.");
        }
        return reason.trim();
    }

    private boolean isSystemActor(String actor) {
        return actor != null && actor.equalsIgnoreCase(SYSTEM_ACTOR);
    }

    private AccountProvisioningSource provisioningSource(String actor, String details) {
        if (isSystemActor(actor) && details != null && details.toLowerCase(Locale.ROOT).contains("bootstrap")) {
            return AccountProvisioningSource.BOOTSTRAP;
        }
        return AccountProvisioningSource.ADMIN_PROVISIONED;
    }

    private Specification<UserAccount> specification(String q, UserRole role, Boolean enabled) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(q)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), contains(q)));
            }
            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
            }
            if (enabled != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String contains(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
