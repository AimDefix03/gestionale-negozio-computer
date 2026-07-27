package it.giovannidefilippo.gestionale.user;

import java.util.EnumSet;
import java.util.Set;

public enum UserRole {
    SUPER_ADMIN("Super admin", EnumSet.allOf(UserPermission.class)),
    ADMIN("Admin", EnumSet.of(
            UserPermission.VIEW_CATALOG,
            UserPermission.VIEW_PARTNERS,
            UserPermission.MANAGE_PARTNERS,
            UserPermission.MANAGE_PRODUCTS,
            UserPermission.MANAGE_INVENTORY,
            UserPermission.VIEW_ORDERS,
            UserPermission.CREATE_ORDERS,
            UserPermission.CONFIRM_ORDERS,
            UserPermission.FULFILL_ORDERS,
            UserPermission.CANCEL_ORDERS,
            UserPermission.RECORD_PAYMENTS,
            UserPermission.REFUND_PAYMENTS,
            UserPermission.REQUEST_RETURNS,
            UserPermission.MANAGE_RETURNS,
            UserPermission.MANAGE_DOCUMENTS,
            UserPermission.VIEW_REPORTS,
            UserPermission.MANAGE_ACCOUNTS,
            UserPermission.VIEW_AUDIT
    )),
    EMPLOYEE("Dipendente", EnumSet.of(
            UserPermission.VIEW_CATALOG,
            UserPermission.VIEW_PARTNERS,
            UserPermission.MANAGE_PARTNERS,
            UserPermission.MANAGE_PRODUCTS,
            UserPermission.MANAGE_INVENTORY,
            UserPermission.VIEW_ORDERS,
            UserPermission.CREATE_ORDERS,
            UserPermission.CONFIRM_ORDERS,
            UserPermission.FULFILL_ORDERS,
            UserPermission.CANCEL_ORDERS,
            UserPermission.RECORD_PAYMENTS,
            UserPermission.REFUND_PAYMENTS,
            UserPermission.REQUEST_RETURNS,
            UserPermission.MANAGE_RETURNS,
            UserPermission.MANAGE_DOCUMENTS,
            UserPermission.VIEW_REPORTS
    )),
    CUSTOMER("Cliente", EnumSet.of(
            UserPermission.VIEW_CATALOG,
            UserPermission.VIEW_ORDERS,
            UserPermission.CREATE_ORDERS,
            UserPermission.CONFIRM_ORDERS,
            UserPermission.CANCEL_ORDERS,
            UserPermission.REQUEST_RETURNS
    ));

    private final String label;
    private final Set<UserPermission> permissions;

    UserRole(String label, Set<UserPermission> permissions) {
        this.label = label;
        this.permissions = Set.copyOf(permissions);
    }

    public String getLabel() {
        return label;
    }

    public Set<UserPermission> getPermissions() {
        return permissions;
    }

    public boolean hasPermission(UserPermission permission) {
        return permissions.contains(permission);
    }

    public boolean canManageOperations() {
        return hasPermission(UserPermission.MANAGE_PRODUCTS)
                || hasPermission(UserPermission.MANAGE_INVENTORY)
                || hasPermission(UserPermission.MANAGE_DOCUMENTS);
    }

    public boolean canManageAccounts() {
        return hasPermission(UserPermission.MANAGE_ACCOUNTS);
    }

    public boolean canCreateAdmin() {
        return this == SUPER_ADMIN;
    }
}
