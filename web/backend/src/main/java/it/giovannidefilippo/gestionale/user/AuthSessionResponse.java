package it.giovannidefilippo.gestionale.user;

import java.time.Instant;

public record AuthSessionResponse(UserResponse user, String token, Instant expiresAt) {
}
