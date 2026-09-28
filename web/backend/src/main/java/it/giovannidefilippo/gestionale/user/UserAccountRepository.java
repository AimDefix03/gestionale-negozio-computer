package it.giovannidefilippo.gestionale.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface UserAccountRepository extends JpaRepository<UserAccount, Long>, JpaSpecificationExecutor<UserAccount> {
    @Query("select account from UserAccount account where account.usernameCanonical = lower(trim(:username))")
    Optional<UserAccount> findByUsernameIgnoreCase(@Param("username") String username);
}
