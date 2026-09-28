package service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void registraEAutenticaDipendente() {
        AuthService service = authService();

        assertTrue(service.registerPublic("mario", "password", AuthService.ROLE_EMPLOYEE));
        assertTrue(service.login("mario", "password", AuthService.ROLE_EMPLOYEE));
        assertFalse(service.login("mario", "password", AuthService.ROLE_ADMIN));
    }

    @Test
    void registrazionePubblicaRifiutaAdmin() {
        AuthService service = authService();

        assertFalse(service.registerPublic("admin", "password", AuthService.ROLE_ADMIN));
        assertFalse(service.login("admin", "password", AuthService.ROLE_ADMIN));
    }

    @Test
    void creazioneInternaPermetteAdmin() {
        AuthService service = authService();

        assertTrue(service.createAccount("admin", "password", AuthService.ROLE_ADMIN));
        assertTrue(service.login("admin", "password", AuthService.ROLE_ADMIN));
    }

    @Test
    void restituisceAccountRegistratiOrdinatiPerUsername() {
        AuthService service = authService();
        service.register("zeta", "password", AuthService.ROLE_CUSTOMER);
        service.register("admin", "password", AuthService.ROLE_ADMIN);

        assertEquals("admin", service.getAccounts().get(0).username());
        assertEquals(AuthService.ROLE_ADMIN, service.getAccounts().get(0).role());
        assertEquals("zeta", service.getAccounts().get(1).username());
    }

    @Test
    void eliminaPiuAccountRegistrati() {
        AuthService service = authService();
        service.register("admin", "password", AuthService.ROLE_ADMIN);
        service.register("dipendente", "password", AuthService.ROLE_EMPLOYEE);
        service.register("cliente", "password", AuthService.ROLE_CUSTOMER);

        int removed = service.deleteAccounts(List.of("dipendente", "cliente"));

        assertEquals(2, removed);
        assertEquals(1, service.getAccounts().size());
        assertTrue(service.login("admin", "password", AuthService.ROLE_ADMIN));
        assertFalse(service.login("dipendente", "password", AuthService.ROLE_EMPLOYEE));
    }

    private AuthService authService() {
        return new AuthService(
                tempDir.resolve("utenti.dat").toString(),
                tempDir.resolve("ruoli.dat").toString()
        );
    }
}
