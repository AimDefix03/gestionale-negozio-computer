package it.giovannidefilippo.gestionale.user;

import java.util.Set;

public record UserResponse(Long id, String username, UserRole role, String roleLabel, Set<UserPermission> permissions) {
    static UserResponse from(UserAccount account) {
        return new UserResponse(account.getId(), account.getUsername(), account.getRole(), account.getRole().getLabel(), account.getRole().getPermissions());
    }
}
