package it.giovannidefilippo.gestionale.user;

import java.time.Instant;
import java.util.List;

public record AccountSecurityReviewReport(
        Instant generatedAt,
        long selfServiceCustomers,
        long suspiciousOperationalAccounts,
        long verifiedOperationalAccounts,
        List<AccountSecurityReviewItem> accounts
) {
}
