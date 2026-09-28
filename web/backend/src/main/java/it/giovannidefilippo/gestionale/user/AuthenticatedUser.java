package it.giovannidefilippo.gestionale.user;

public record AuthenticatedUser(Long accountId, String username, UserRole role) {
    public AuthenticatedUser(String username, UserRole role) {
        this(null, username, role);
    }

    public String roleLabel() {
        return role.getLabel();
    }

    public boolean hasPermission(UserPermission permission) {
        return role.hasPermission(permission);
    }
}
