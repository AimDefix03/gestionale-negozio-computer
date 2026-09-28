package it.giovannidefilippo.gestionale.partner;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface BusinessPartnerRepository extends JpaRepository<BusinessPartner, Long>, JpaSpecificationExecutor<BusinessPartner> {
    @Query("select partner from BusinessPartner partner where partner.codeCanonical = lower(trim(:code))")
    Optional<BusinessPartner> findByCodeIgnoreCase(@Param("code") String code);

    Optional<BusinessPartner> findByLinkedAccountId(Long accountId);
}
