package edu.bulsu.lms.service;

import edu.bulsu.lms.domain.Staff;
import edu.bulsu.lms.domain.ValidationException;
import edu.bulsu.lms.persistence.Database;
import edu.bulsu.lms.persistence.StaffRepository;
import edu.bulsu.lms.security.PasswordHasher;
import edu.bulsu.lms.security.PasswordPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Signs staff in. One service handles librarians and the administrator; the {@code Role} on the
 * returned account decides what happens next.
 *
 * <p>Protections: passwords are verified against salted PBKDF2 hashes; a librarian account is
 * locked after three wrong passwords (an administrator unlocks it); and three failures within a
 * minute, for any username, pause sign-in for 15 seconds.
 *
 * <p>Trade-offs worth knowing (see SECURITY.md): the login screen shows the <em>same</em> message for
 * "wrong password", "unknown user" and "locked", and unknown users and locked accounts still cost one
 * hash check, so the screen and the timing do not reveal which usernames exist. Counting failures per
 * account (the lock) means anyone who knows a username can lock it; that is the price of a lock-out.
 */
public final class AuthService {

    public static final int THROTTLE_AFTER_FAILURES = 3;
    public static final Duration THROTTLE_DURATION = Duration.ofSeconds(15);
    /** Failures further apart than this are not counted together. */
    public static final Duration FAILURE_WINDOW = Duration.ofSeconds(60);

    private final Database database;
    private final StaffRepository staffRepository;
    private final PasswordHasher hasher;
    private final Clock clock;
    /** A hash of a random password, verified against for unknown users so they cost the same time as real ones. */
    private final String decoyHash;

    // ponytail: throttle state is per running app, not per user or machine; fine for a desktop app.
    // A restart resets it. Persist it in the database if an attacker could restart the program.
    private int consecutiveFailures;
    private Instant lastFailure = Instant.MIN;
    private Instant blockedUntil = Instant.MIN;

    public AuthService(Database database, StaffRepository staffRepository, PasswordHasher hasher, Clock clock) {
        this.database = database;
        this.staffRepository = staffRepository;
        this.hasher = hasher;
        this.clock = clock;
        this.decoyHash = hasher.hash(UUID.randomUUID().toString().toCharArray());
    }

    /**
     * @throws ValidationException if the username or password is blank
     */
    public synchronized LoginResult login(String rawUsername, char[] password) {
        String username = rawUsername == null ? "" : rawUsername.strip().toLowerCase(Locale.ROOT);
        requireCredentials(username, password);

        Instant now = clock.instant();
        if (now.isBefore(blockedUntil)) {
            return new LoginResult.Throttled(secondsUntil(blockedUntil, now));
        }
        if (password.length > PasswordPolicy.MAX_LENGTH) {
            // No real password is this long; do not spend time hashing a megabyte.
            return registerFailure(now);
        }

        Optional<Staff> found = database.query(connection -> staffRepository.findByUsername(connection, username));
        if (found.isEmpty()) {
            hasher.verify(password, decoyHash);
            return registerFailure(now);
        }

        Staff staff = found.get();
        if (staff.isLocked()) {
            hasher.verify(password, decoyHash); // same cost as a real attempt
            return new LoginResult.Locked(false);
        }
        if (!hasher.verify(password, staff.getPasswordHash())) {
            boolean lockedNow = staff.recordFailedLogin();
            database.query(connection -> staffRepository.updateLoginState(connection, staff));
            return lockedNow ? new LoginResult.Locked(true) : registerFailure(now);
        }

        if (staff.getFailedAttempts() > 0) {
            staff.recordSuccessfulLogin();
            database.query(connection -> staffRepository.updateLoginState(connection, staff));
        }
        // Deliberately NOT resetting the throttle here: otherwise an attacker with one valid account could
        // alternate "two guesses at admin" with "sign in as myself" and never be slowed down.
        return new LoginResult.Success(staff);
    }

    private static void requireCredentials(String username, char[] password) {
        boolean noUser = username.isEmpty();
        boolean noPassword = password == null || password.length == 0;
        String tail = " Please enter complete login credentials and try to sign in again.";
        if (noUser && noPassword) {
            throw new ValidationException("The username and password field cannot be blank." + tail);
        }
        if (noUser) {
            throw new ValidationException("The username field cannot be blank." + tail);
        }
        if (noPassword) {
            throw new ValidationException("The password field cannot be blank." + tail);
        }
    }

    private LoginResult registerFailure(Instant now) {
        if (now.isAfter(lastFailure.plus(FAILURE_WINDOW))) {
            consecutiveFailures = 0; // old, isolated typos do not add up
        }
        lastFailure = now;
        consecutiveFailures++;
        if (consecutiveFailures >= THROTTLE_AFTER_FAILURES) {
            consecutiveFailures = 0;
            blockedUntil = now.plus(THROTTLE_DURATION);
            return new LoginResult.Throttled((int) THROTTLE_DURATION.toSeconds());
        }
        return new LoginResult.InvalidCredentials();
    }

    private static int secondsUntil(Instant until, Instant now) {
        return (int) Math.max(1, Math.ceil(Duration.between(now, until).toMillis() / 1000.0));
    }
}
