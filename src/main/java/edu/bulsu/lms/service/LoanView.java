package edu.bulsu.lms.service;

/** The three loan lists a librarian can open. */
public enum LoanView {
    /** Not returned yet (includes overdue ones). */
    ISSUED,
    /** Already returned. */
    RETURNED,
    /** Not returned and past the due date. */
    MISSING
}
