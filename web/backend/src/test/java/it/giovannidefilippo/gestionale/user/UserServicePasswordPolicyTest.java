package it.giovannidefilippo.gestionale.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserServicePasswordPolicyTest {
    @Autowired
    private UserService userService;

    @Test
    void publicRegistrationRejectsEmptyPassword() {
        assertThatThrownBy(() -> userService.registerPublic(uniqueUsername("empty"), "", UserRole.CUSTOMER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password non rispetta la policy")
                .hasMessageContaining("almeno 8 caratteri");
    }

    @Test
    void publicRegistrationRejectsShortPassword() {
        assertThatThrownBy(() -> userService.registerPublic(uniqueUsername("short"), "A1!a", UserRole.CUSTOMER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("almeno 8 caratteri");
    }

    @Test
    void publicRegistrationRejectsWeakPassword() {
        assertThatThrownBy(() -> userService.registerPublic(uniqueUsername("weak"), "password", UserRole.CUSTOMER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lettera maiuscola")
                .hasMessageContaining("numero")
                .hasMessageContaining("simbolo");
    }

    @Test
    void publicRegistrationRejectsPasswordSimilarToUsername() {
        String username = uniqueUsername("giovanni");

        assertThatThrownBy(() -> userService.registerPublic(username, username + "A1!", UserRole.CUSTOMER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("troppo simile allo username");
    }

    @Test
    void publicRegistrationAcceptsValidPassword() {
        UserResponse response = userService.registerPublic(uniqueUsername("valid"), "Secure123!", UserRole.CUSTOMER);

        assertThat(response.role()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    void administrativeAccountCreationRejectsWeakPassword() {
        String superAdmin = createSuperAdmin();

        assertThatThrownBy(() -> userService.createAccount(uniqueUsername("adminweak"), "admin", UserRole.ADMIN, superAdmin, "Test creazione admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password non rispetta la policy");
    }

    @Test
    void administrativeAccountCreationAcceptsValidPassword() {
        String superAdmin = createSuperAdmin();

        UserResponse response = userService.createAccount(uniqueUsername("adminok"), "AdminStrong123!", UserRole.ADMIN, superAdmin, "Test creazione admin");

        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
    }

    private String createSuperAdmin() {
        String username = uniqueUsername("super");
        userService.createAccount(username, "SuperStrong123!", UserRole.SUPER_ADMIN, "Sistema", "Test bootstrap super admin");
        return username;
    }

    private static String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
