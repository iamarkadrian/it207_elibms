package edu.bulsu.lms.domain;

/** The input is well-formed but a library rule forbids the action (duplicate ISBN, book already lent, ...). */
public final class BusinessRuleException extends UserFacingException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
