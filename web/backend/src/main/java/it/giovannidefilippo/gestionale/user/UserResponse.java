package it.giovannidefilippo.gestionale.user;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Set;
import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        UserRole role,
        String roleLabel,
        Set<UserPermission> permissions,
        boolean enabled,
        Instant disabledAt,
        String disabledBy,
        String disabledReason,
        @JsonIgnore long credentialVersion
) {
    static UserResponse from(UserAccount account) {
        return new UserResponse(
                account.getId(),
                account.getUsername(),
                account.getRole(),
                account.getRole().getLabel(),
                account.getRole().getPermissions(),
                account.isEnabled(),
                account.getDisabledAt(),
                account.getDisabledBy(),
                account.getDisabledReason(),
                account.getCredentialVersion()
        );
    }
}
