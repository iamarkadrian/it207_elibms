package edu.bulsu.lms.domain;

/** The input itself is wrong (blank, too long, wrong format). */
public final class ValidationException extends UserFacingException {

    public ValidationException(String message) {
        super(message);
    }
}
