package it.giovannidefilippo.gestionale.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PrivacyRetentionEInvoicingArchitectureProposalTest {
    @Test
    void decisionSeparatesPrivacyConservationAndElectronicInvoicing() throws Exception {
        String decision = Files.readString(Path.of("../../docs/DECISIONS/0006-privacy-retention-einvoicing-boundaries.md"));

        assertThat(decision)
                .contains("non costituisce attestazione di conformita")
                .contains("Privacy, gestione documentale, conservazione e fatturazione elettronica resteranno contesti distinti")
                .contains("servizio esterno qualificato o validato")
                .contains("I documenti fiscali simulati esistenti resteranno separati")
                .contains("Un PDF resta una copia di cortesia");
    }

    @Test
    void proposalDefinesPrivacyLifecycleAndSafeRetention() throws Exception {
        String proposal = Files.readString(Path.of("../../docs/PRIVACY_RETENTION_EINVOICING_ARCHITECTURE.md"));

        assertThat(proposal)
                .contains("workflow completo per i diritti")
                .contains("dry-run con conteggi, motivazioni e impatti")
                .contains("approvazione a quattro occhi")
                .contains("Un legal hold sospende cancellazione e anonimizzazione")
                .contains("erasure ledger minimale")
                .contains("riapplica cancellazioni, anonimizzazioni e restrizioni")
                .contains("non decide automaticamente se notificare");
    }

    @Test
    void proposalRequiresImmutableAsynchronousAndReconcilableInvoiceFlow() throws Exception {
        String proposal = Files.readString(Path.of("../../docs/PRIVACY_RETENTION_EINVOICING_ARCHITECTURE.md"));

        assertThat(proposal)
                .contains("`FiscalDocument` resta un documento gestionale simulato")
                .contains("outbox transazionale")
                .contains("inbox autenticata")
                .contains("`ACCEPTED` e `DELIVERED` non sono sinonimi")
                .contains("Il PDF e una rappresentazione di cortesia")
                .contains("evidenza del conservatore obbligatoria")
                .contains("test sandbox provider");
    }

    @Test
    void proposalRequiresOfficialSourcesProfessionalApprovalAndStagedRollout() throws Exception {
        String proposal = Files.readString(Path.of("../../docs/PRIVACY_RETENTION_EINVOICING_ARCHITECTURE.md"));

        assertThat(proposal)
                .contains("https://eur-lex.europa.eu/legal-content/IT/TXT/?uri=CELEX:32016R0679")
                .contains("https://www.edpb.europa.eu/sme_en")
                .contains("https://www.agid.gov.it/it/ambiti-intervento/gestione-documentale")
                .contains("https://www1.agenziaentrate.gov.it/web_app_entrate/fatturazione_elettronica.html")
                .contains("La migrazione usa expand/contract e feature flag")
                .contains("sandbox e pilot superati")
                .contains("approvazione professionale documentata");
    }
}
