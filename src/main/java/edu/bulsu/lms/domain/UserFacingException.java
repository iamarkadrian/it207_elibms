package edu.bulsu.lms.domain;

/**
 * Base type for problems whose message is safe and useful to show to the person using the app.
 *
 * <p>Technical failures (SQL errors, I/O) are <em>not</em> user-facing; they use
 * {@code DataAccessException} and are logged instead of displayed.
 */
public abstract sealed class UserFacingException extends RuntimeException
        permits ValidationException, BusinessRuleException {

    protected UserFacingException(String message) {
        super(message);
    }
}
