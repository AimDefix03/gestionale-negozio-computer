package it.giovannidefilippo.gestionale.partner;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

interface BusinessPartnerRepository extends JpaRepository<BusinessPartner, Long>, JpaSpecificationExecutor<BusinessPartner> {
    Optional<BusinessPartner> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
