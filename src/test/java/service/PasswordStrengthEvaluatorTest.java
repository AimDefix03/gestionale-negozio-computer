package service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PasswordStrengthEvaluatorTest {
    @Test
    void riconoscePasswordVuota() {
        assertEquals(PasswordStrength.EMPTY, PasswordStrengthEvaluator.evaluate(""));
    }

    @Test
    void riconoscePasswordDebole() {
        assertEquals(PasswordStrength.WEAK, PasswordStrengthEvaluator.evaluate("abc"));
    }

    @Test
    void riconoscePasswordMedia() {
        assertEquals(PasswordStrength.MEDIUM, PasswordStrengthEvaluator.evaluate("password12"));
    }

    @Test
    void riconoscePasswordMoltoForte() {
        assertEquals(PasswordStrength.EXCELLENT, PasswordStrengthEvaluator.evaluate("Password-2026!"));
    }
}
