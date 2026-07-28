package it.giovannidefilippo.gestionale.partner;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class BusinessPartnerServiceTest {
    @Autowired
    private BusinessPartnerService service;

    @Test
    void createUpdateAndDeactivatePartner() {
        String code = uniqueCode("CLI");
        BusinessPartnerResponse created = service.create(request(code, BusinessPartnerType.CUSTOMER, "Cliente Test"), "admin", "Admin");

        assertThat(created.active()).isTrue();
        assertThat(created.typeLabel()).isEqualTo("Cliente");

        BusinessPartnerResponse updated = service.update(code, request(code, BusinessPartnerType.CUSTOMER, "Cliente Aggiornato"), "admin", "Admin");
        assertThat(updated.displayName()).isEqualTo("Cliente Aggiornato");

        service.deactivate(code, "admin", "Admin");

        assertThat(service.findByCode(code).active()).isFalse();
    }

    @Test
    void duplicatePartnerCodeIsRejected() {
        String code = uniqueCode("FOR");
        service.create(request(code, BusinessPartnerType.SUPPLIER, "Fornitore Test"), "admin", "Admin");

        assertThatThrownBy(() -> service.create(request(code, BusinessPartnerType.SUPPLIER, "Fornitore Duplicato"), "admin", "Admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("codice");
    }

    @Test
    void requireActiveCustomerRejectsSupplierAndInactiveCustomer() {
        String supplierCode = uniqueCode("FOR");
        String customerCode = uniqueCode("CLI");
        service.create(request(supplierCode, BusinessPartnerType.SUPPLIER, "Fornitore"), "admin", "Admin");
        service.create(request(customerCode, BusinessPartnerType.CUSTOMER, "Cliente"), "admin", "Admin");
        service.deactivate(customerCode, "admin", "Admin");

        assertThatThrownBy(() -> service.requireActiveCustomer(supplierCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non e un cliente");

        assertThatThrownBy(() -> service.requireActiveCustomer(customerCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non e attivo");
    }

    private BusinessPartnerRequest request(String code, BusinessPartnerType type, String displayName) {
        return new BusinessPartnerRequest(
                code,
                type,
                displayName,
                "RSSMRA80A01F839X",
                type == BusinessPartnerType.SUPPLIER ? "IT12345678901" : "",
                "test@example.com",
                "0810000000",
                "Via Test 1",
                "Napoli",
                "Anagrafica di test"
        );
    }

    private String uniqueCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
