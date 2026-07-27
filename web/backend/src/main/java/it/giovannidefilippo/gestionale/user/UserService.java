package it.giovannidefilippo.gestionale.user;

import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditService;
import it.giovannidefilippo.gestionale.audit.AuditSeverity;
import it.giovannidefilippo.gestionale.common.PageRequests;
import it.giovannidefilippo.gestionale.common.PageResponse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserAccountRepository repository;
    private final PasswordHasher passwordHasher;
    private final AuditService auditService;
    private final PasswordStrengthService passwordStrengthService;

    UserService(UserAccountRepository repository, PasswordHasher passwordHasher, AuditService auditService, PasswordStrengthService passwordStrengthService) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.auditService = auditService;
        this.passwordStrengthService = passwordStrengthService;
    }

    public List<UserResponse> findAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(UserAccount::getUsername))
                .map(UserResponse::from)
                .toList();
    }

    public PageResponse<UserResponse> search(String q, UserRole role, int page, int size) {
        return PageResponse.from(repository.findAll(
                specification(q, role),
                PageRequests.of(page, size, Sort.by("username").ascending())
        ).map(UserResponse::from));
    }

    public boolean hasAccounts() {
        return repository.count() > 0;
    }

    @Transactional
    public UserResponse login(String username, String password, UserRole role) {
        UserAccount account = repository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("Credenziali non valide."));
        if (account.getRole() != role || !passwordHasher.matches(password, account.getPasswordSalt(), account.getPasswordHash())) {
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
    public UserResponse registerPublic(String username, String password, UserRole role) {
        if (role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("La registrazione di ruoli amministrativi non è consentita dalla schermata pubblica.");
        }
        return createAccount(username, password, role, "Sistema", "Registrazione pubblica");
    }

    @Transactional
    public UserResponse createAccount(String username, String password, UserRole role, String actor, String details) {
        UserRole actorRole = resolveActorRole(actor);
        if (!isSystemActor(actor)) {
            validateAccountCreation(role, actorRole);
        }

        if (repository.existsByUsernameIgnoreCase(username.trim())) {
            throw new IllegalArgumentException("Esiste già un account con questo username.");
        }
        passwordStrengthService.validateStrongPassword(username, password);
        PasswordHasher.PasswordHash passwordHash = passwordHasher.hash(password);
        UserAccount account = repository.save(new UserAccount(username.trim(), passwordHash.salt(), passwordHash.hash(), role));
        auditService.record(actor, actorRole.getLabel(), "CREATE_ACCOUNT", account.getUsername(), details + " - ruolo " + role.getLabel(), AuditCategory.ACCOUNT, role == UserRole.ADMIN ? AuditSeverity.CRITICAL : AuditSeverity.WARNING, "USER_ACCOUNT");
        return UserResponse.from(account);
    }

    @Transactional
    public void delete(String username, String actor) {
        UserAccount account = repository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("Account non trovato."));
        UserRole actorRole = resolveActorRole(actor);
        validateAccountDeletion(account, actor, actorRole);
        repository.delete(account);
        auditService.record(actor, actorRole.getLabel(), "DELETE_ACCOUNT", account.getUsername(), "Account eliminato - ruolo " + account.getRole().getLabel(), AuditCategory.ACCOUNT, AuditSeverity.CRITICAL, "USER_ACCOUNT");
    }

    @Transactional
    public void deleteMany(List<String> usernames, String actor) {
        if (usernames == null || usernames.isEmpty()) {
            throw new IllegalArgumentException("Seleziona almeno un account.");
        }
        usernames.forEach(username -> delete(username, actor));
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

    private void validateAccountDeletion(UserAccount account, String actor, UserRole actorRole) {
        if (!actorRole.canManageAccounts()) {
            throw new IllegalArgumentException("Non hai i permessi per eliminare account.");
        }
        if (account.getUsername().equalsIgnoreCase(actor.trim())) {
            throw new IllegalArgumentException("Non puoi eliminare il tuo account mentre sei autenticato.");
        }
        if (account.getRole() == UserRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Il super admin non può essere eliminato dal pannello operativo.");
        }
        if (account.getRole() == UserRole.ADMIN && !actorRole.canCreateAdmin()) {
            throw new IllegalArgumentException("Solo il super admin può eliminare account admin.");
        }
    }

    private boolean isSystemActor(String actor) {
        return actor != null && actor.equalsIgnoreCase("Sistema");
    }

    private Specification<UserAccount> specification(String q, UserRole role) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(q)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), contains(q)));
            }
            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
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
