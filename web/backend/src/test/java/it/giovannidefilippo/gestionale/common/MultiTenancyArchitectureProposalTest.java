package it.giovannidefilippo.gestionale.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MultiTenancyArchitectureProposalTest {
    @Test
    void decisionKeepsTenantCompanySiteAndWarehouseAsSeparateConcepts() throws Exception {
        String decision = Files.readString(Path.of("../../docs/DECISIONS/0005-multi-tenant-isolation-strategy.md"));

        assertThat(decision)
                .contains("schema PostgreSQL condiviso con `tenant_id`")
                .contains("database o deployment dedicato")
                .contains("nessuno schema PostgreSQL creato dinamicamente per tenant")
                .contains("`Tenant`, `LegalEntity`, `Site` e `Warehouse` sono concetti distinti")
                .contains("L'implementazione non e ancora iniziata");
    }

    @Test
    void proposalRequiresFailClosedIsolationAtApplicationAndDatabaseLevels() throws Exception {
        String proposal = Files.readString(Path.of("../../docs/MULTI_TENANCY_ARCHITECTURE.md"));

        assertThat(proposal)
                .contains("contesto tenant derivato dalla sessione")
                .contains("foreign key composte")
                .contains("force row level security")
                .contains("set_config('app.tenant_id', :tenantId, true)")
                .contains("Il runtime non usa `BYPASSRLS`")
                .contains("richiesta senza contesto fallisce chiusa")
                .contains("H2 non e sufficiente per certificare RLS");
    }

    @Test
    void proposalDefinesReversibleMigrationAndCrossTenantAcceptanceTests() throws Exception {
        String proposal = Files.readString(Path.of("../../docs/MULTI_TENANCY_ARCHITECTURE.md"));

        assertThat(proposal)
                .contains("La migrazione usa expand/contract")
                .contains("tenant predefinito")
                .contains("tenant A non elenca dati di B")
                .contains("connessione del pool riutilizzata non conserva il tenant precedente")
                .contains("idempotency key uguale in tenant diversi non collide")
                .contains("il tenant predefinito preserva l'installazione singola")
                .contains("Step 34 e 35");
    }
}
