package it.giovannidefilippo.gestionale.user;

public record AuthenticatedUser(String username, UserRole role) {
    public String roleLabel() {
        return role.getLabel();
    }

    public boolean hasPermission(UserPermission permission) {
        return role.hasPermission(permission);
    }
}
