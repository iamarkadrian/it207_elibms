package edu.bulsu.lms.security;

import edu.bulsu.lms.domain.ValidationException;

/** Rules a new password must satisfy before it is hashed and stored. */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;

    private PasswordPolicy() {
        // static helpers only
    }

    /** @throws ValidationException with a message that tells the user what to fix */
    public static void require(char[] password) {
        if (password == null || password.length < MIN_LENGTH) {
            throw new ValidationException("Password must be at least " + MIN_LENGTH + " characters.");
        }
        if (password.length > MAX_LENGTH) {
            throw new ValidationException("Password cannot be longer than " + MAX_LENGTH + " characters.");
        }
        boolean letter = false;
        boolean digit = false;
        for (char c : password) {
            letter |= Character.isLetter(c);
            digit |= Character.isDigit(c);
        }
        if (!letter || !digit) {
            throw new ValidationException("Password must contain at least one letter and one digit.");
        }
    }
}
