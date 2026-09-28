package it.giovannidefilippo.gestionale.user;

import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditService;
import it.giovannidefilippo.gestionale.audit.AuditSeverity;
import it.giovannidefilippo.gestionale.common.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AccountSecurityReviewServiceTest {
    @Autowired
    private UserAccountRepository accountRepository;

    @Autowired
    private AuthSessionRepository sessionRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private AccountSecurityReviewService securityReviewService;

    @Autowired
    private AuditService auditService;

    @Test
    void quarantinesSuspiciousSelfServiceEmployeeAndPermanentlyRejectsIssuedToken() {
        String username = unique("self_employee");
        UserAccount account = saveHistoricalAccount(username, UserRole.EMPLOYEE, AccountProvisioningSource.SELF_SERVICE, false);
        AuthSessionResponse issuedSession = saveHistoricalSession(account);

        assertThatThrownBy(() -> userService.login(username, "Employee123!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credenziali non valide");
        assertThatThrownBy(() -> authSessionService.require(issuedSession.token()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Sessione non valida");

        AuthSession storedSession = sessionRepository.findByAccountIdOrderByCreatedAtDesc(account.getId())
                .get(0);
        assertThat(storedSession.getRevokedAt()).isNotNull();
        assertThatThrownBy(() -> authSessionService.require(issuedSession.token()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void reportsSelfServiceCustomersSeparatelyFromSuspiciousEmployeesAndPreservesAuditEvidence() {
        String customer = unique("self_customer");
        String employee = unique("self_employee_report");
        saveHistoricalAccount(customer, UserRole.CUSTOMER, AccountProvisioningSource.SELF_SERVICE, true);
        saveHistoricalAccount(employee, UserRole.EMPLOYEE, AccountProvisioningSource.SELF_SERVICE, false);
        auditService.record(employee, UserRole.EMPLOYEE.getLabel(), "STOCK_UNLOAD", "SKU-1", "Evidenza stock", AuditCategory.INVENTORY, AuditSeverity.WARNING, "STOCK_MOVEMENT");
        auditService.record(employee, UserRole.EMPLOYEE.getLabel(), "RECORD_PAYMENT", "ORD-1", "Evidenza pagamento", AuditCategory.ORDER, AuditSeverity.WARNING, "PAYMENT_TRANSACTION");
        auditService.record(employee, UserRole.EMPLOYEE.getLabel(), "REQUEST_RETURN", "RET-1", "Evidenza reso", AuditCategory.ORDER, AuditSeverity.WARNING, "ORDER_RETURN");
        auditService.record(employee, UserRole.EMPLOYEE.getLabel(), "CREATE_DOCUMENT", "FS-1", "Evidenza documento", AuditCategory.DOCUMENT, AuditSeverity.WARNING, "FISCAL_DOCUMENT");

        AccountSecurityReviewReport report = securityReviewService.report();
        AccountSecurityReviewItem customerItem = item(report, customer);
        AccountSecurityReviewItem employeeItem = item(report, employee);

        assertThat(customerItem.classification()).isEqualTo(AccountSecurityClassification.SELF_SERVICE_CUSTOMER);
        assertThat(employeeItem.classification()).isEqualTo(AccountSecurityClassification.SUSPICIOUS_OPERATIONAL);
        assertThat(employeeItem.inventoryActions()).isEqualTo(1);
        assertThat(employeeItem.orderActions()).isEqualTo(2);
        assertThat(employeeItem.paymentActions()).isEqualTo(1);
        assertThat(employeeItem.returnActions()).isEqualTo(1);
        assertThat(employeeItem.documentActions()).isEqualTo(1);
        assertThat(report.suspiciousOperationalAccounts()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void manualSuperAdminReviewRestoresFutureLoginButDoesNotReactivateOldTokens() {
        String superAdmin = unique("reviewer");
        String employee = unique("pending_employee");
        userService.createAccount(superAdmin, "SuperStrong123!", UserRole.SUPER_ADMIN, "Sistema", "Bootstrap test");
        UserAccount pending = saveHistoricalAccount(employee, UserRole.EMPLOYEE, AccountProvisioningSource.UNKNOWN, false);
        AuthSessionResponse oldSession = saveHistoricalSession(pending);

        assertThatThrownBy(() -> authSessionService.require(oldSession.token()))
                .isInstanceOf(UnauthorizedException.class);

        securityReviewService.verifyOperationalAccount(employee, superAdmin);

        UserResponse authenticated = userService.login(employee, "Employee123!");
        assertThat(authenticated.role()).isEqualTo(UserRole.EMPLOYEE);
        assertThatThrownBy(() -> authSessionService.require(oldSession.token()))
                .isInstanceOf(UnauthorizedException.class);
        assertThat(accountRepository.findByUsernameIgnoreCase(employee).orElseThrow().isOperationalAccessVerified()).isTrue();
    }

    private UserAccount saveHistoricalAccount(
            String username,
            UserRole role,
            AccountProvisioningSource source,
            boolean verified
    ) {
        PasswordHasher.PasswordHash password = passwordHasher.hash("Employee123!");
        return accountRepository.save(new UserAccount(
                username,
                password.salt(),
                password.hash(),
                role,
                source,
                verified,
                null,
                null
        ));
    }

    private AuthSessionResponse saveHistoricalSession(UserAccount account) {
        String token = "historical-" + UUID.randomUUID();
        Instant createdAt = Instant.now();
        AuthSession session = sessionRepository.save(new AuthSession(
                sha256(token),
                account,
                createdAt,
                createdAt.plusSeconds(3600)
        ));
        return new AuthSessionResponse(UserResponse.from(account), token, session.getExpiresAt());
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 non disponibile.", exception);
        }
    }

    private AccountSecurityReviewItem item(AccountSecurityReviewReport report, String username) {
        return report.accounts().stream()
                .filter(item -> item.username().equals(username))
                .findFirst()
                .orElseThrow();
    }

    private String unique(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
