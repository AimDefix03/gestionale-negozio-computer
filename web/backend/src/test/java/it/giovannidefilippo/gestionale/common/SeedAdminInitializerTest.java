package it.giovannidefilippo.gestionale.common;

import it.giovannidefilippo.gestionale.user.UserRole;
import it.giovannidefilippo.gestionale.user.UserService;
import org.junit.jupiter.api.Test;

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
        SeedAdminInitializer initializer = new SeedAdminInitializer(userService, false, "", "");

        assertThatThrownBy(initializer::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nessun account presente");

        verify(userService, never()).createAccount("", "", UserRole.SUPER_ADMIN, "Sistema", "Bootstrap super admin iniziale da configurazione ambiente");
    }

    @Test
    void bootstrapCreatesSuperAdminWhenDatabaseIsEmpty() {
        UserService userService = mock(UserService.class);
        when(userService.hasAccounts()).thenReturn(false);
        SeedAdminInitializer initializer = new SeedAdminInitializer(userService, true, "initial_super_admin", "Initial-Atlas-9842!");

        initializer.run();

        verify(userService).createAccount("initial_super_admin", "Initial-Atlas-9842!", UserRole.SUPER_ADMIN, "Sistema", "Bootstrap super admin iniziale da configurazione ambiente");
    }

    @Test
    void bootstrapIsIgnoredWhenAccountsAlreadyExist() {
        UserService userService = mock(UserService.class);
        when(userService.hasAccounts()).thenReturn(true);
        SeedAdminInitializer initializer = new SeedAdminInitializer(userService, true, "admin", "RootSecure123!");

        initializer.run();

        verify(userService, never()).createAccount("admin", "RootSecure123!", UserRole.SUPER_ADMIN, "Sistema", "Bootstrap super admin iniziale da configurazione ambiente");
    }

    @Test
    void bootstrapRejectsKnownLocalCredentialsWithoutDependingOnProfile() {
        UserService userService = mock(UserService.class);
        when(userService.hasAccounts()).thenReturn(false);
        SeedAdminInitializer initializer = new SeedAdminInitializer(userService, true, "root_admin", "RootSecure123!");

        assertThatThrownBy(initializer::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("credenziali locali di default");
    }

    @Test
    void bootstrapRequiresStrongPasswordWithoutDependingOnProfile() {
        UserService userService = mock(UserService.class);
        when(userService.hasAccounts()).thenReturn(false);
        SeedAdminInitializer initializer = new SeedAdminInitializer(userService, true, "root_admin", "Password1!");

        assertThatThrownBy(initializer::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("almeno 12 caratteri");
    }
}
