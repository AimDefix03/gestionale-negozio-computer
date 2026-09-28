package model;

import java.io.Serializable;
import java.time.LocalDateTime;

public record AuditEvent(
        LocalDateTime timestamp,
        String actor,
        String role,
        String action,
        String target,
        String details
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
