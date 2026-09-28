package it.giovannidefilippo.gestionale.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowBenchmarkContractTest {
    private static final Path BENCHMARK = Path.of("../..", "docs", "WORKFLOW_BENCHMARKS.md");

    @Test
    void everyWorkflowUsesTheMandatoryDecisionTemplate() throws Exception {
        String document = Files.readString(BENCHMARK);

        assertThat(document)
                .contains("### 1. Problema operativo")
                .contains("### 2. Comportamento osservato in due o piu gestionali maturi")
                .contains("### 3. Regola di dominio da preservare")
                .contains("### 4. Adattamento al monolite attuale")
                .contains("### 5. Cosa non implementare")
                .contains("### 6. Test di accettazione con un operatore reale")
                .contains("`ADOPT`")
                .contains("`ADAPT`")
                .contains("`REJECT`")
                .contains("`DEFER`");
    }

    @Test
    void assistedSalesBenchmarkUsesIndependentOfficialSourcesAndProtectsCurrentInvariants() throws Exception {
        String document = Files.readString(BENCHMARK);

        assertThat(document)
                .contains("## Scheda WB-001 - Vendita assistita da personale")
                .contains("www.odoo.com/documentation")
                .contains("docs.frappe.io/erpnext")
                .contains("learn.microsoft.com/en-us/dynamics365/business-central")
                .contains("ID stabile del `BusinessPartner`")
                .contains("La creazione produce una bozza e non riserva o scarica stock")
                .contains("La conferma riserva stock")
                .contains("Stato ordine, stato pagamento ed evasione restano indipendenti")
                .contains("Nessuna conferma, evasione o incasso impliciti")
                .contains("Stato: `IMPLEMENTED`")
                .contains("Stato: `PENDING_OPERATOR_EXECUTION`")
                .contains("non dichiara una validazione umana non avvenuta");
    }

    @Test
    void benchmarkExplicitlyRejectsProductCopyingAndTracksFutureCards() throws Exception {
        String document = Files.readString(BENCHMARK);

        assertThat(document)
                .contains("non autorizza a copiarne codice, testi, asset, grafica o identita visiva")
                .contains("## Registro schede future")
                .contains("| Dashboard cliente | 3.2 | WB-002 | IMPLEMENTED |")
                .contains("| Ordini fornitore | 4.1 | WB-005 | IMPLEMENTED |");
    }

    @Test
    void customerProjectionBenchmarkUsesOfficialSourcesAndDefinesADataMinimizationPolicy() throws Exception {
        String document = Files.readString(BENCHMARK);

        assertThat(document)
                .contains("## Scheda WB-002 - Catalogo e dashboard cliente")
                .contains("www.odoo.com/documentation")
                .contains("docs.frappe.io")
                .contains("learn.microsoft.com/en-us/dynamics365")
                .contains("Il catalogo cliente non contiene ID interni, giacenza fisica, quantita riservata, disponibilita numerica, valore potenziale di vendita")
                .contains("`AVAILABLE` oltre tre unita vendibili, `LIMITED` da una a tre, `UNAVAILABLE` a zero")
                .contains("KPI globali, ricavi aziendali, movimenti di magazzino e dati di altri clienti non attraversano la rete verso `CUSTOMER`")
                .contains("Nessun calcolo di disponibilita autorevole nel browser")
                .contains("Stato: `IMPLEMENTED`")
                .contains("Stato: `PENDING_OPERATOR_EXECUTION`");
    }

    @Test
    void multiLineReturnBenchmarkProtectsQuantityIdentityAndOperatorValidation() throws Exception {
        String document = Files.readString(BENCHMARK);

        assertThat(document)
                .contains("## Scheda WB-004 - Resi multi-riga e rimborso parziale")
                .contains("www.odoo.com/documentation")
                .contains("docs.frappe.io/erpnext")
                .contains("La somma delle quantita gia restituite o ancora impegnate")
                .contains("Il backend ricalcola il residuo nella transazione")
                .contains("all'ID stabile del reso")
                .contains("Nessun reintegro stock alla semplice richiesta o approvazione")
                .contains("Stato: `IMPLEMENTED` in staging")
                .contains("Stato: `PENDING_OPERATOR_EXECUTION`")
                .contains("| Resi multi-riga | 3.8 | WB-004 | IMPLEMENTED |");
    }

    @Test
    void supplierOrderBenchmarkProtectsResidualQuantitiesAndDefersPhysicalStock() throws Exception {
        String document = Files.readString(BENCHMARK);

        assertThat(document)
                .contains("## Scheda WB-005 - Ordini fornitore")
                .contains("www.odoo.com/documentation")
                .contains("docs.frappe.io/erpnext")
                .contains("learn.microsoft.com/en-us/dynamics365/business-central")
                .contains("Il fornitore deve essere un `BusinessPartner` di tipo `SUPPLIER` selezionato tramite ID stabile")
                .contains("La quantita ricevuta e cumulativa, non negativa e mai superiore alla quantita ordinata")
                .contains("non modifica ancora il ledger fisico")
                .contains("Stato: `IMPLEMENTED` in staging")
                .contains("Stato: `PENDING_OPERATOR_EXECUTION`")
                .contains("| Ordini fornitore | 4.1 | WB-005 | IMPLEMENTED |");
    }
}
