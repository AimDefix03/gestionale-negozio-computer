package it.giovannidefilippo.gestionale.order;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentMethodTest {
    @Test
    void acceptsCanonicalCodesAndLegacyLabels() {
        assertThat(PaymentMethod.fromValue("CARD")).isEqualTo(PaymentMethod.CARD);
        assertThat(PaymentMethod.fromValue("Carta")).isEqualTo(PaymentMethod.CARD);
        assertThat(PaymentMethod.fromValue("BANK_TRANSFER")).isEqualTo(PaymentMethod.BANK_TRANSFER);
        assertThat(PaymentMethod.fromValue("Bonifico bancario")).isEqualTo(PaymentMethod.BANK_TRANSFER);
        assertThat(PaymentMethod.fromValue("Contanti")).isEqualTo(PaymentMethod.CASH);
    }

    @Test
    void rejectsUnsupportedMethod() {
        assertThatThrownBy(() -> PaymentMethod.fromValue("Criptovaluta"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Metodo di pagamento non supportato.");
    }
}
