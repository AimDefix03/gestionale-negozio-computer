package it.giovannidefilippo.gestionale.common;

import it.giovannidefilippo.gestionale.user.UserRole;
import it.giovannidefilippo.gestionale.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeedAdminInitializerTest {
    @Test
    void emptyDatabaseRequiresExplicitBootstrap() {
        UserService userService = mock(UserService.class);
        when(userService.hasAccounts()).thenReturn(false);
        SeedAdminInitializer initializer = new SeedAdminInitializer(userService, new MockEnvironment(), false, "", "");

        assertThatThrownBy(initializer::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nessun account presente");

        verify(userService, never()).createAccount("", "", UserRole.SUPER_ADMIN, "Sistema", "Bootstrap super admin iniziale da configurazione ambiente");
    }

    @Test
    void bootstrapCreatesSuperAdminWhenDatabaseIsEmpty() {
        UserService userService = mock(UserService.class);
        when(userService.hasAccounts()).thenReturn(false);
        SeedAdminInitializer initializer = new SeedAdminInitializer(userService, new MockEnvironment(), true, "admin", "RootSecure123!");

        initializer.run();

        verify(userService).createAccount("admin", "RootSecure123!", UserRole.SUPER_ADMIN, "Sistema", "Bootstrap super admin iniziale da configurazione ambiente");
    }

    @Test
    void bootstrapIsIgnoredWhenAccountsAlreadyExist() {
        UserService userService = mock(UserService.class);
        when(userService.hasAccounts()).thenReturn(true);
        SeedAdminInitializer initializer = new SeedAdminInitializer(userService, new MockEnvironment(), true, "admin", "RootSecure123!");

        initializer.run();

        verify(userService, never()).createAccount("admin", "RootSecure123!", UserRole.SUPER_ADMIN, "Sistema", "Bootstrap super admin iniziale da configurazione ambiente");
    }

    @Test
    void productionBootstrapRejectsLocalDefaultCredentials() {
        UserService userService = mock(UserService.class);
        when(userService.hasAccounts()).thenReturn(false);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        SeedAdminInitializer initializer = new SeedAdminInitializer(userService, environment, true, "root_admin", "RootSecure123!");

        assertThatThrownBy(initializer::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("credenziali locali di default");
    }

    @Test
    void productionBootstrapRequiresStrongPassword() {
        UserService userService = mock(UserService.class);
        when(userService.hasAccounts()).thenReturn(false);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        SeedAdminInitializer initializer = new SeedAdminInitializer(userService, environment, true, "root_admin", "Password1!");

        assertThatThrownBy(initializer::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("almeno 12 caratteri");
    }
}
