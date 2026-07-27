package it.giovannidefilippo.gestionale.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class UserRequests {
    private UserRequests() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password, @NotNull UserRole role) {
    }

    public record RegisterRequest(@NotBlank String username, @NotBlank String password, @NotNull UserRole role) {
    }

    public record CreateAccountRequest(@NotBlank String username, @NotBlank String password, @NotNull UserRole role) {
    }

    public record DeleteAccountsRequest(java.util.List<String> usernames) {
    }

    public record PasswordRequest(@NotBlank String password) {
    }
}
