package edu.bulsu.lms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.bulsu.lms.domain.Role;
import edu.bulsu.lms.domain.Staff;
import edu.bulsu.lms.domain.ValidationException;
import edu.bulsu.lms.persistence.Database;
import edu.bulsu.lms.persistence.StaffRepository;
import edu.bulsu.lms.security.PasswordHasher;
import edu.bulsu.lms.testsupport.TestClock;
import edu.bulsu.lms.testsupport.TestData;
import edu.bulsu.lms.testsupport.TestDatabase;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** One sign-in for both roles, with lock-out and throttling, against the real seeded database. */
class AuthServiceIT {

    private static final PasswordHasher FAST = new PasswordHasher(2_000);

    private static Database db;

    @BeforeAll
    static void connect() {
        db = TestDatabase.require();
    }

    private static Services services(TestClock clock) {
        return Services.create(db, clock, FAST);
    }

    private static Staff storedState(String username) {
        return db.query(c -> new StaffRepository().findByUsername(c, username)).orElseThrow();
    }

    @Test
    @DisplayName("the administrator and a librarian use the very same sign-in")
    void bothRolesSignInTheSameWay() {
        AuthService auth = services(new TestClock()).auth();

        LoginResult admin = auth.login("admin", "Admin@2026".toCharArray());
        assertEquals(Role.ADMIN, assertInstanceOf(LoginResult.Success.class, admin).staff().getRole());

        LoginResult librarian = auth.login("librarian", "Library@2026".toCharArray());
        assertEquals(Role.LIBRARIAN, assertInstanceOf(LoginResult.Success.class, librarian).staff().getRole());
    }

    @Test
    void usernameIsCaseInsensitive() {
        LoginResult result = services(new TestClock()).auth().login("  LIBRARIAN ", "Library@2026".toCharArray());
        assertInstanceOf(LoginResult.Success.class, result);
    }

    @Test
    void wrongPasswordAndUnknownUserLookTheSame() {
        AuthService auth = services(new TestClock()).auth();
        assertInstanceOf(LoginResult.InvalidCredentials.class, auth.login("librarian", "wrong-Password1".toCharArray()));
        assertInstanceOf(LoginResult.InvalidCredentials.class, auth.login("nobody-here", "whatever-Pass1".toCharArray()));
    }

    @Test
    void blankFieldsAreRejectedWithTheOriginalMessages() {
        AuthService auth = services(new TestClock()).auth();
        assertTrue(assertThrows(ValidationException.class, () -> auth.login("", new char[0])).getMessage()
                .startsWith("The username and password field cannot be blank."));
        assertTrue(assertThrows(ValidationException.class, () -> auth.login("", "x".toCharArray())).getMessage()
                .startsWith("The username field cannot be blank."));
        assertTrue(assertThrows(ValidationException.class, () -> auth.login("librarian", new char[0])).getMessage()
                .startsWith("The password field cannot be blank."));
    }

    @Test
    @DisplayName("three wrong passwords lock a librarian until an administrator unlocks the account")
    void librarianIsLockedThenUnlocked() {
        Services s = services(new TestClock());
        String username = TestData.username();
        int id = s.staff().addLibrarian(TestData.admin(), "Lock", null, "Test", "Librarian", "CICT", username, "Passw0rd-ok".toCharArray());

        assertInstanceOf(LoginResult.InvalidCredentials.class, s.auth().login(username, "bad-one-1".toCharArray()));
        assertInstanceOf(LoginResult.InvalidCredentials.class, s.auth().login(username, "bad-two-2".toCharArray()));
        LoginResult third = s.auth().login(username, "bad-three-3".toCharArray());
        assertTrue(assertInstanceOf(LoginResult.Locked.class, third).justNow());
        assertTrue(storedState(username).isLocked(), "the lock is persisted, so it survives a restart");

        // Even the right password is refused while locked.
        LoginResult correct = s.auth().login(username, "Passw0rd-ok".toCharArray());
        assertFalse(assertInstanceOf(LoginResult.Locked.class, correct).justNow());

        s.staff().unlock(TestData.admin(), id);
        assertInstanceOf(LoginResult.Success.class, s.auth().login(username, "Passw0rd-ok".toCharArray()));
        assertEquals(0, storedState(username).getFailedAttempts());
    }

    @Test
    void aSuccessfulSignInForgivesEarlierMistakes() {
        Services s = services(new TestClock());
        String username = TestData.username();
        s.staff().addLibrarian(TestData.admin(), "Forgive", null, "Test", "Librarian", "CICT", username, "Passw0rd-ok".toCharArray());

        s.auth().login(username, "bad-one-1".toCharArray());
        s.auth().login(username, "bad-two-2".toCharArray());
        assertEquals(2, storedState(username).getFailedAttempts());

        assertInstanceOf(LoginResult.Success.class, s.auth().login(username, "Passw0rd-ok".toCharArray()));
        assertEquals(0, storedState(username).getFailedAttempts());
        assertFalse(storedState(username).isLocked());
    }

    @Test
    @DisplayName("three failures in a row pause sign-in for 15 seconds, then it works again")
    void throttleAfterThreeFailures() {
        TestClock clock = new TestClock();
        AuthService auth = services(clock).auth();

        assertInstanceOf(LoginResult.InvalidCredentials.class, auth.login("ghost-user", "bad-one-1".toCharArray()));
        assertInstanceOf(LoginResult.InvalidCredentials.class, auth.login("ghost-user", "bad-two-2".toCharArray()));
        LoginResult third = auth.login("ghost-user", "bad-three-3".toCharArray());
        assertEquals(15, assertInstanceOf(LoginResult.Throttled.class, third).secondsLeft());

        // Even correct credentials wait out the pause.
        LoginResult during = auth.login("librarian", "Library@2026".toCharArray());
        int left = assertInstanceOf(LoginResult.Throttled.class, during).secondsLeft();
        assertTrue(left >= 1 && left <= 15);

        clock.advance(Duration.ofSeconds(16));
        assertInstanceOf(LoginResult.Success.class, auth.login("librarian", "Library@2026".toCharArray()));
    }

    @Test
    @DisplayName("signing in as yourself does not reset the throttle on guesses against someone else")
    void aSuccessfulSignInDoesNotWipeTheThrottle() {
        AuthService auth = services(new TestClock()).auth();
        assertInstanceOf(LoginResult.InvalidCredentials.class, auth.login("admin", "guess-one-1".toCharArray()));
        assertInstanceOf(LoginResult.InvalidCredentials.class, auth.login("admin", "guess-two-2".toCharArray()));
        assertInstanceOf(LoginResult.Success.class, auth.login("librarian", "Library@2026".toCharArray()));
        // The attacker's trick (two guesses, a good login, two more guesses...) must not buy unlimited tries.
        assertInstanceOf(LoginResult.Throttled.class, auth.login("admin", "guess-three-3".toCharArray()));
    }

    @Test
    void oldMistakesAreForgottenAfterTheWindow() {
        TestClock clock = new TestClock();
        AuthService auth = services(clock).auth();
        assertInstanceOf(LoginResult.InvalidCredentials.class, auth.login("admin", "guess-one-1".toCharArray()));
        assertInstanceOf(LoginResult.InvalidCredentials.class, auth.login("admin", "guess-two-2".toCharArray()));
        clock.advance(Duration.ofSeconds(120));
        assertInstanceOf(LoginResult.InvalidCredentials.class, auth.login("admin", "guess-three-3".toCharArray()),
                "two typos a couple of minutes ago do not count toward a pause");
    }

    @Test
    void absurdlyLongPasswordsAreRefusedWithoutHashing() {
        AuthService auth = services(new TestClock()).auth();
        assertInstanceOf(LoginResult.InvalidCredentials.class, auth.login("librarian", "a1".repeat(5_000).toCharArray()));
    }

    @Test
    @DisplayName("the administrator is throttled but never locked out")
    void administratorIsNeverLocked() {
        TestClock clock = new TestClock();
        AuthService auth = services(clock).auth();

        for (int attempt = 1; attempt <= 7; attempt++) {
            LoginResult result = auth.login("admin", ("not-the-password-" + attempt).toCharArray());
            assertFalse(result instanceof LoginResult.Locked, "attempt " + attempt);
            clock.advance(Duration.ofSeconds(16));
        }
        assertFalse(storedState("admin").isLocked());
        assertInstanceOf(LoginResult.Success.class, auth.login("admin", "Admin@2026".toCharArray()));
        assertEquals(0, storedState("admin").getFailedAttempts());
    }
}
