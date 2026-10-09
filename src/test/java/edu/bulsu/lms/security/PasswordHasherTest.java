package edu.bulsu.lms.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.bulsu.lms.domain.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher(2_000); // cheap, tests only

    @Test
    void hashVerifiesOnlyTheRightPassword() {
        String stored = hasher.hash("Correct-Horse-9".toCharArray());
        assertTrue(hasher.verify("Correct-Horse-9".toCharArray(), stored));
        assertFalse(hasher.verify("correct-horse-9".toCharArray(), stored));
        assertFalse(hasher.verify("".toCharArray(), stored));
    }

    @Test
    void everyHashUsesAFreshSalt() {
        char[] password = "Same-Password-1".toCharArray();
        assertNotEquals(hasher.hash(password), hasher.hash(password));
    }

    @Test
    void storedFormatNamesTheAlgorithmAndCost() {
        String[] parts = hasher.hash("Abcdef12".toCharArray()).split("\\$");
        assertEquals(4, parts.length);
        assertEquals("pbkdf2-sha256", parts[0]);
        assertEquals("2000", parts[1]);
    }

    @Test
    void verifyUsesTheCostStoredInTheHash() {
        String stored = new PasswordHasher(1_500).hash("Abcdef12".toCharArray());
        assertTrue(hasher.verify("Abcdef12".toCharArray(), stored), "a hasher with a different default still verifies old hashes");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "plaintext", "pbkdf2-sha256$x$y$z", "pbkdf2-sha256$2000$!!$!!", "md5$1$a$b", "pbkdf2-sha256$99999999$AA==$AA=="})
    void malformedStoredValuesNeverVerify(String stored) {
        assertFalse(hasher.verify("Abcdef12".toCharArray(), stored));
    }

    @Test
    void nullStoredValueNeverVerifies() {
        assertFalse(hasher.verify("Abcdef12".toCharArray(), null));
    }

    @Test
    void theDefaultCostIsStrongEnough() {
        assertTrue(PasswordHasher.DEFAULT_ITERATIONS >= 600_000, "OWASP recommends at least 600,000 for PBKDF2-HMAC-SHA256");
    }

    @Test
    void policyAcceptsGoodPasswords() {
        PasswordPolicy.require("Library@2026".toCharArray());
        PasswordPolicy.require("abcdefg1".toCharArray());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "short1", "alllettersnodigits", "12345678"})
    void policyRejectsWeakPasswords(String weak) {
        assertThrows(ValidationException.class, () -> PasswordPolicy.require(weak.toCharArray()));
    }

    @Test
    void policyRejectsAbsurdlyLongPasswords() {
        assertThrows(ValidationException.class, () -> PasswordPolicy.require(("a1".repeat(100)).toCharArray()));
    }

    @Test
    void policyRejectsNull() {
        assertThrows(ValidationException.class, () -> PasswordPolicy.require(null));
    }
}
