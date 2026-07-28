package it.giovannidefilippo.gestionale.user;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceTransactionTest {

    @Test
    void loginUsesWritableTransactionBecauseItRecordsSecurityEvents() throws Exception {
        Transactional classTransaction = UserService.class.getAnnotation(Transactional.class);
        Method login = UserService.class.getDeclaredMethod("login", String.class, String.class, UserRole.class);
        Transactional loginTransaction = login.getAnnotation(Transactional.class);

        assertThat(classTransaction).isNotNull();
        assertThat(classTransaction.readOnly()).isTrue();
        assertThat(loginTransaction).isNotNull();
        assertThat(loginTransaction.readOnly()).isFalse();
    }
}
