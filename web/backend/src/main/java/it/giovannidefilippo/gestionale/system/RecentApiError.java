package it.giovannidefilippo.gestionale.system;

import java.time.Instant;

public record RecentApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        String requestId
) {
}
