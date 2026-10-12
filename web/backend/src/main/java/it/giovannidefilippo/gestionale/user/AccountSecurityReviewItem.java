package it.giovannidefilippo.gestionale.user;

import java.time.Instant;

public record AccountSecurityReviewItem(
        String username,
        UserRole role,
        AccountProvisioningSource provisioningSource,
        AccountSecurityClassification classification,
        boolean operationalAccessVerified,
        Instant operationalVerifiedAt,
        String operationalVerifiedBy,
        long totalSessions,
        long potentiallyActiveSessions,
        long inventoryActions,
        long orderActions,
        long paymentActions,
        long returnActions,
        long documentActions
) {
}
