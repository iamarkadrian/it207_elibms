package edu.bulsu.lms.service;

import edu.bulsu.lms.domain.Staff;

/**
 * Every way a sign-in attempt can end. Because the interface is sealed, a {@code switch} over it
 * must handle all four cases, so the UI cannot forget one.
 */
public sealed interface LoginResult {

    /** Credentials matched; {@code staff.getRole()} decides which dashboard opens. */
    record Success(Staff staff) implements LoginResult {
    }

    /** Wrong username or password. Deliberately does not say which, to avoid leaking valid usernames. */
    record InvalidCredentials() implements LoginResult {
    }

    /** The account is locked; {@code justNow} is true when this very attempt caused the lock. */
    record Locked(boolean justNow) implements LoginResult {
    }

    /** Too many failures in a row: sign-in is paused for a short while. */
    record Throttled(int secondsLeft) implements LoginResult {
    }
}
