package it.giovannidefilippo.gestionale.partner;

import jakarta.validation.constraints.NotNull;

public record BusinessPartnerAccountLinkRequest(@NotNull Long accountId) {
}
