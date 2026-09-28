package it.giovannidefilippo.gestionale.user;

import it.giovannidefilippo.gestionale.common.BusinessIdentifierCanonicalizer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_accounts")
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String usernameCanonical;

    @Column(nullable = false)
    private String passwordSalt;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private long credentialVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AccountProvisioningSource provisioningSource;

    @Column(nullable = false)
    private boolean operationalAccessVerified;

    private Instant operationalVerifiedAt;

    private String operationalVerifiedBy;

    @Column(nullable = false)
    private boolean enabled;

    private Instant disabledAt;

    private String disabledBy;

    @Column(length = 900)
    private String disabledReason;

    protected UserAccount() {
    }

    UserAccount(
            String username,
            String passwordSalt,
            String passwordHash,
            UserRole role,
            AccountProvisioningSource provisioningSource,
            boolean operationalAccessVerified,
            Instant operationalVerifiedAt,
            String operationalVerifiedBy
    ) {
        this.username = BusinessIdentifierCanonicalizer.display(username);
        this.usernameCanonical = BusinessIdentifierCanonicalizer.canonical(username);
        this.passwordSalt = passwordSalt;
        this.passwordHash = passwordHash;
        this.credentialVersion = 1;
        this.role = role;
        this.provisioningSource = provisioningSource;
        this.operationalAccessVerified = operationalAccessVerified;
        this.operationalVerifiedAt = operationalVerifiedAt;
        this.operationalVerifiedBy = operationalVerifiedBy;
        this.enabled = true;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public long getCredentialVersion() {
        return credentialVersion;
    }

    public UserRole getRole() {
        return role;
    }

    public AccountProvisioningSource getProvisioningSource() {
        return provisioningSource;
    }

    public boolean isOperationalAccessVerified() {
        return operationalAccessVerified;
    }

    public Instant getOperationalVerifiedAt() {
        return operationalVerifiedAt;
    }

    public String getOperationalVerifiedBy() {
        return operationalVerifiedBy;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getDisabledAt() {
        return disabledAt;
    }

    public String getDisabledBy() {
        return disabledBy;
    }

    public String getDisabledReason() {
        return disabledReason;
    }

    public boolean requiresOperationalReview() {
        return role != UserRole.CUSTOMER && !operationalAccessVerified;
    }

    public void verifyOperationalAccess(Instant verifiedAt, String verifiedBy) {
        this.operationalAccessVerified = true;
        this.operationalVerifiedAt = verifiedAt;
        this.operationalVerifiedBy = verifiedBy;
    }

    public void changePassword(String passwordSalt, String passwordHash) {
        this.passwordSalt = passwordSalt;
        this.passwordHash = passwordHash;
        this.credentialVersion++;
    }

    public void changeRole(UserRole role) {
        if (this.role != role) {
            this.role = role;
            this.credentialVersion++;
        }
    }

    public void disable(Instant disabledAt, String disabledBy, String disabledReason) {
        if (enabled) {
            enabled = false;
            this.disabledAt = disabledAt;
            this.disabledBy = disabledBy;
            this.disabledReason = disabledReason;
            credentialVersion++;
        }
    }

    public void enable() {
        if (!enabled) {
            enabled = true;
            disabledAt = null;
            disabledBy = null;
            disabledReason = null;
            credentialVersion++;
        }
    }
}
