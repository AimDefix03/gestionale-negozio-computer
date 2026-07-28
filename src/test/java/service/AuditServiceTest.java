package service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void registraEventoEPersisteSuFile() {
        String storageFile = tempDir.resolve("audit.dat").toString();
        AuditService service = new AuditService(storageFile);

        service.record("admin", "Admin", "Creazione account", "Account mario", "Ruolo assegnato: Dipendente");

        AuditService reloadedService = new AuditService(storageFile);

        assertEquals(1, reloadedService.getEvents().size());
        assertEquals("admin", reloadedService.getEvents().get(0).actor());
        assertEquals("Creazione account", reloadedService.getEvents().get(0).action());
    }

    @Test
    void restituisceEventiDalPiuRecente() {
        AuditService service = new AuditService(tempDir.resolve("audit.dat").toString());

        service.record("admin", "Admin", "Prima azione", "Target A", "Dettaglio A");
        service.record("admin", "Admin", "Seconda azione", "Target B", "Dettaglio B");

        assertEquals("Seconda azione", service.getEvents().get(0).action());
        assertEquals("Prima azione", service.getEvents().get(1).action());
    }
}
