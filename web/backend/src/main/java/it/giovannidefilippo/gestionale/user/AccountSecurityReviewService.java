package it.giovannidefilippo.gestionale.user;

import it.giovannidefilippo.gestionale.audit.AccountAuditSummary;
import it.giovannidefilippo.gestionale.audit.AuditCategory;
import it.giovannidefilippo.gestionale.audit.AuditService;
import it.giovannidefilippo.gestionale.audit.AuditSeverity;
import it.giovannidefilippo.gestionale.common.TimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
class AccountSecurityReviewService {
    private final UserAccountRepository accountRepository;
    private final AuthSessionRepository sessionRepository;
    private final AuditService auditService;
    private final TimeProvider timeProvider;

    AccountSecurityReviewService(
            UserAccountRepository accountRepository,
            AuthSessionRepository sessionRepository,
            AuditService auditService,
            TimeProvider timeProvider
    ) {
        this.accountRepository = accountRepository;
        this.sessionRepository = sessionRepository;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
    }

    AccountSecurityReviewReport report() {
        Instant now = timeProvider.instant();
        List<AccountSecurityReviewItem> accounts = accountRepository.findAll().stream()
                .filter(this::belongsToReview)
                .sorted(Comparator.comparing(UserAccount::getUsername, String.CASE_INSENSITIVE_ORDER))
                .map(account -> item(account, now))
                .toList();
        return new AccountSecurityReviewReport(
                now,
                count(accounts, AccountSecurityClassification.SELF_SERVICE_CUSTOMER),
                count(accounts, AccountSecurityClassification.SUSPICIOUS_OPERATIONAL),
                count(accounts, AccountSecurityClassification.VERIFIED_OPERATIONAL),
                accounts
        );
    }

    @Transactional
    void verifyOperationalAccount(String username, String reviewerUsername) {
        UserAccount reviewer = accountRepository.findByUsernameIgnoreCase(reviewerUsername.trim())
                .orElseThrow(() -> new IllegalArgumentException("Revisore non riconosciuto."));
        if (reviewer.getRole() != UserRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Solo il super admin può verificare un account operativo.");
        }
        UserAccount account = accountRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("Account non trovato."));
        if (account.getRole() == UserRole.CUSTOMER) {
            throw new IllegalArgumentException("Un account cliente non richiede verifica operativa.");
        }
        if (account.isOperationalAccessVerified()) {
            return;
        }
        account.verifyOperationalAccess(timeProvider.instant(), reviewer.getUsername());
        auditService.record(
                reviewer.getUsername(),
                reviewer.getRole().getLabel(),
                "VERIFY_OPERATIONAL_ACCOUNT",
                account.getUsername(),
                "Provenienza " + account.getProvisioningSource() + " verificata manualmente; le sessioni precedenti restano revocate",
                AuditCategory.SECURITY,
                AuditSeverity.CRITICAL,
                "USER_ACCOUNT"
        );
    }

    private boolean belongsToReview(UserAccount account) {
        return account.getProvisioningSource() == AccountProvisioningSource.SELF_SERVICE
                || account.getRole() != UserRole.CUSTOMER;
    }

    private AccountSecurityReviewItem item(UserAccount account, Instant now) {
        AccountAuditSummary audit = auditService.summarizeAccountActivity(account.getUsername());
        return new AccountSecurityReviewItem(
                account.getUsername(),
                account.getRole(),
                account.getProvisioningSource(),
                classification(account),
                account.isOperationalAccessVerified(),
                account.getOperationalVerifiedAt(),
                account.getOperationalVerifiedBy(),
                sessionRepository.countByAccountId(account.getId()),
                sessionRepository.countByAccountIdAndRevokedAtIsNullAndExpiresAtAfter(account.getId(), now),
                audit.inventoryActions(),
                audit.orderActions(),
                audit.paymentActions(),
                audit.returnActions(),
                audit.documentActions()
        );
    }

    private AccountSecurityClassification classification(UserAccount account) {
        if (account.getRole() == UserRole.CUSTOMER) {
            return AccountSecurityClassification.SELF_SERVICE_CUSTOMER;
        }
        return account.isOperationalAccessVerified()
                ? AccountSecurityClassification.VERIFIED_OPERATIONAL
                : AccountSecurityClassification.SUSPICIOUS_OPERATIONAL;
    }

    private long count(List<AccountSecurityReviewItem> accounts, AccountSecurityClassification classification) {
        return accounts.stream().filter(account -> account.classification() == classification).count();
    }
}
