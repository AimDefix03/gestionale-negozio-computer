package it.giovannidefilippo.gestionale.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerLifecycleAndReleaseArchitectureProposalTest {
    @Test
    void decisionSeparatesControlPlaneDataPlaneBrandingAndEntitlements() throws Exception {
        String decision = Files.readString(Path.of("../../docs/DECISIONS/0007-customer-lifecycle-and-release-fleet.md"));

        assertThat(decision)
                .contains("L'implementazione non e ancora iniziata")
                .contains("control plane commerciale separato logicamente dal data plane")
                .contains("Non sono ammessi fork del codice per cliente")
                .contains("Il branding e configurazione dati versionata, non codice eseguibile")
                .contains("un ruolo stabilisce cosa puo fare un utente")
                .contains("un entitlement stabilisce quali moduli sono disponibili")
                .contains("una feature flag governa rollout tecnico");
    }

    @Test
    void proposalDefinesResumableApprovedAndSafeCustomerLifecycle() throws Exception {
        String proposal = Files.readString(Path.of("../../docs/CUSTOMER_LIFECYCLE_AND_RELEASE_ARCHITECTURE.md"));

        assertThat(proposal)
                .contains("Il provisioning e asincrono e usa step persistiti")
                .contains("idempotency_key")
                .contains("Il retry riparte dal primo step non completato")
                .contains("Nessun cliente diventa `ACTIVE`")
                .contains("bootstrap monouso")
                .contains("La sospensione commerciale non equivale a cancellazione")
                .contains("applicare retention e legal hold di ADR 0006")
                .contains("chiudere solo con doppia approvazione");
    }

    @Test
    void proposalRequiresImmutableVerifiableAndProgressiveReleases() throws Exception {
        String proposal = Files.readString(Path.of("../../docs/CUSTOMER_LIFECYCLE_AND_RELEASE_ARCHITECTURE.md"));

        assertThat(proposal)
                .contains("version: 1.4.0")
                .contains("digest: \"sha256:<digest>\"")
                .contains("SBOM CycloneDX")
                .contains("provenance SLSA verificabile")
                .contains("`INTERNAL`: ambienti tecnici non cliente")
                .contains("`CANARY`: installazioni controllate")
                .contains("Auto-pause")
                .contains("Una release non modifica mai artefatti gia pubblicati");
    }

    @Test
    void proposalRequiresFleetGatedMigrationsRollbackAndPilot() throws Exception {
        String proposal = Files.readString(Path.of("../../docs/CUSTOMER_LIFECYCLE_AND_RELEASE_ARCHITECTURE.md"));

        assertThat(proposal)
                .contains("Release N - Expand")
                .contains("Release N+1 - Transition")
                .contains("Release N+2 - Contract")
                .contains("nessun target supportato usa il vecchio formato")
                .contains("Il rollback applicativo e consentito solo")
                .contains("fix-forward")
                .contains("Restore o point-in-time recovery sono ammessi solo come procedura di incidente")
                .contains("pilot con almeno un tenant pooled e uno dedicato");
    }
}
