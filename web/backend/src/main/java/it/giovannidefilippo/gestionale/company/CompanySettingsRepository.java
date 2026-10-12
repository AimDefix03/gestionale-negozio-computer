package it.giovannidefilippo.gestionale.company;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface CompanySettingsRepository extends JpaRepository<CompanySettings, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select settings from CompanySettings settings where settings.id = :id")
    Optional<CompanySettings> findByIdForUpdate(@Param("id") Integer id);
}
