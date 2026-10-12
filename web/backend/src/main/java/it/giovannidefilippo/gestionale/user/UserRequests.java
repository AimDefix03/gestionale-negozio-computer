package it.giovannidefilippo.gestionale.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

public final class UserRequests {
    private UserRequests() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RegisterRequest(
            @NotBlank String username,
            @NotBlank String password,
            @Null(message = "Il ruolo non può essere scelto nella registrazione pubblica.") UserRole role
    ) {
    }

    public record CreateAccountRequest(@NotBlank String username, @NotBlank String password, @NotNull UserRole role) {
    }

    public record DeleteAccountsRequest(java.util.List<String> usernames) {
    }

    public record PasswordRequest(@NotBlank String password) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {
    }

    public record ResetPasswordRequest(@NotBlank String newPassword, @NotBlank @Size(max = 900) String reason) {
    }

    public record AccountStateRequest(@NotBlank @Size(max = 900) String reason) {
    }

    public record RevokeSessionsRequest(@NotBlank @Size(max = 900) String reason) {
    }

    public record ChangeRoleRequest(@NotNull UserRole role, @NotBlank @Size(max = 900) String reason) {
    }
}
