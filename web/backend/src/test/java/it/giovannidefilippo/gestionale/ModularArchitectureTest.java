package it.giovannidefilippo.gestionale;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static org.assertj.core.api.Assertions.assertThat;

class ModularArchitectureTest {
    private static final DescribedPredicate<JavaClass> TECHNICAL_PACKAGES = resideInAnyPackage(
            "it.giovannidefilippo.gestionale.audit..",
            "it.giovannidefilippo.gestionale.common..",
            "it.giovannidefilippo.gestionale.dashboard..",
            "it.giovannidefilippo.gestionale.idempotency..",
            "it.giovannidefilippo.gestionale.security..",
            "it.giovannidefilippo.gestionale.system.."
    );

    @Test
    void businessModulesHaveValidDependencies() {
        ApplicationModules modules = ApplicationModules.of(GestionaleApiApplication.class, TECHNICAL_PACKAGES);

        assertThat(modules.stream().map(module -> module.getName()).toList())
                .containsExactlyInAnyOrder("company", "document", "inventory", "order", "partner", "product", "reporting", "user");
        modules.verify();
    }
}
