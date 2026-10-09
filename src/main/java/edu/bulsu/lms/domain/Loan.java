package edu.bulsu.lms.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * One lending transaction, as shown in the Issued, Returned and Missing screens.
 *
 * <p>It is a read model: it carries the names that were joined in for display, so the UI never
 * needs a second query. A {@code returnedAt} of {@code null} means the item is still out.
 */
public record Loan(int id, String itemCode, String itemTitle, String studentNumber, String studentName,
                   String courseCode, int issuedById, String issuedByName,
                   LocalDateTime issuedAt, LocalDateTime dueAt, LocalDateTime returnedAt) implements Displayable {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a");

    public Loan {
        if (itemCode == null || itemTitle == null || studentNumber == null || studentName == null
                || courseCode == null || issuedByName == null || issuedAt == null || dueAt == null) {
            throw new ValidationException("A loan needs its item, student, librarian and dates.");
        }
        if (dueAt.isBefore(issuedAt)) {
            throw new ValidationException("A loan cannot be due before it was issued.");
        }
        if (returnedAt != null && returnedAt.isBefore(issuedAt)) {
            throw new ValidationException("A loan cannot be returned before it was issued.");
        }
    }

    public boolean isReturned() {
        return returnedAt != null;
    }

    /** Still out and past its due date at {@code now}. These are the "Missing" loans. */
    public boolean isOverdue(LocalDateTime now) {
        return returnedAt == null && now.isAfter(dueAt);
    }

    public String issuedText() {
        return DATE.format(issuedAt);
    }

    public String dueText() {
        return DATE.format(dueAt);
    }

    /** The return date, or an empty string while the item is still out. */
    public String returnedText() {
        return returnedAt == null ? "" : DATE.format(returnedAt);
    }

    @Override
    public String title() {
        return itemTitle;
    }

    @Override
    public String subtitle() {
        return "Issue Number " + id;
    }

    @Override
    public List<String> lines() {
        return List.of(
                returnedAt == null ? "Not Returned Yet" : "Returned on " + DATE_TIME.format(returnedAt),
                "Issued on " + DATE_TIME.format(issuedAt),
                "Due on " + DATE.format(dueAt),
                itemCode,
                "",
                "Issued to:",
                studentName,
                studentNumber,
                "",
                "Issued by:",
                issuedByName,
                String.valueOf(issuedById));
    }
}
