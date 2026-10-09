package edu.bulsu.lms.domain;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Something the library catalogs. The concrete kinds ({@link PrintedBook}, {@link DigitalBook},
 * {@link OtherMaterial}) differ in how they are lent, displayed and described, which the abstract
 * methods below capture. Code that holds a {@code List<LibraryItem>} never needs {@code instanceof}.
 */
public abstract sealed class LibraryItem implements Displayable
        permits PrintedBook, DigitalBook, OtherMaterial {

    public static final int MAX_BORROW_DAYS = 365;

    public static final int CODE_MAX = 20;

    private static final Pattern CODE = Pattern.compile("\\d{6,20}");

    private final String codeLabel;
    private final boolean authorRequired;
    private final int minBorrowDays;

    private String code;
    private String title;
    private String author;
    private int publicationYear;
    private int pages;
    private int borrowDays;
    private boolean available;

    /**
     * @param codeLabel      what the identifier is called to users ("ISBN", "EPUB ID", "Barcode")
     * @param authorRequired whether a blank author is rejected
     * @param minBorrowDays  smallest allowed loan period (0 for items that are never lent)
     */
    protected LibraryItem(String codeLabel, String code, String title, String author, boolean authorRequired,
                          int publicationYear, int pages, int minBorrowDays, int borrowDays, boolean available) {
        this.codeLabel = codeLabel;
        this.authorRequired = authorRequired;
        this.minBorrowDays = minBorrowDays;
        setCode(code);
        setTitle(title);
        setAuthor(author);
        setPublicationYear(publicationYear);
        setPages(pages);
        setBorrowDays(borrowDays);
        this.available = available;
    }

    // ---- static validators, shared with the UI wizards ----

    /** 6-20 digits (ISBN-13, EPUB ids and magazine barcodes with add-ons); {@code label} names it in errors. */
    public static String requireCode(String raw, String label) {
        return Check.matching(raw, label, CODE_MAX, CODE, "must be 6 to 20 digits.");
    }

    public static String requireTitle(String raw) {
        return Check.text(raw, "Title", 255);
    }

    public static String requireAuthor(String raw, boolean required) {
        return required ? Check.text(raw, "Author", 120) : Check.optional(raw, "Author", 120);
    }

    public static int requirePages(int pages) {
        return Check.range(pages, "Pages", 1, 20000);
    }

    public static int requireBorrowDays(int days, int min) {
        return Check.range(days, "Borrow duration (days)", min, MAX_BORROW_DAYS);
    }

    // ---- behavior that differs per kind ----

    public abstract ItemType type();

    /** Can this kind of item ever be lent out? */
    public abstract boolean isBorrowable();

    /** Does lending remove the only copy from the shelf? Printed books do; digital copies do not. */
    public abstract boolean consumesCopyOnLoan();

    /** Cells for the catalog table; lines up with {@link ItemType#columns()}. */
    public abstract List<String> tableRow();

    /** Short loan status such as "30 days - Available". */
    protected abstract String borrowText();

    // ---- behavior shared by all kinds ----

    public final boolean canBeBorrowedNow() {
        return isBorrowable() && (!consumesCopyOnLoan() || available);
    }

    /**
     * A loan is due at the <em>end</em> of the last day of the loan period. The screens show only that
     * date, so "overdue" must not start at the hour of day the book was issued.
     */
    public final LocalDateTime dueDateFrom(LocalDateTime issuedAt) {
        return issuedAt.toLocalDate().plusDays(borrowDays).atTime(LocalTime.of(23, 59, 59));
    }

    /** Marks one copy as lent out. */
    public final void markLent() {
        if (!isBorrowable()) {
            throw new BusinessRuleException("\"" + title + "\" is for reference only and cannot be borrowed.");
        }
        if (!canBeBorrowedNow()) {
            throw new BusinessRuleException("\"" + title + "\" is currently borrowed.");
        }
        if (consumesCopyOnLoan()) {
            available = false;
        }
    }

    /** Puts the copy back on the shelf. */
    public final void markReturned() {
        if (consumesCopyOnLoan()) {
            available = true;
        }
    }

    protected final String authorOrNotAvailable() {
        return author == null ? "N/A" : author;
    }

    // ---- accessors ----

    public final String getCodeLabel() {
        return codeLabel;
    }

    public final String getCode() {
        return code;
    }

    public final void setCode(String code) {
        this.code = requireCode(code, codeLabel);
    }

    public final String getTitle() {
        return title;
    }

    public final void setTitle(String title) {
        this.title = requireTitle(title);
    }

    /** The author, or {@code null} for items without one (some magazines and theses). */
    public final String getAuthor() {
        return author;
    }

    public final void setAuthor(String author) {
        this.author = requireAuthor(author, authorRequired);
    }

    public final int getPublicationYear() {
        return publicationYear;
    }

    public final void setPublicationYear(int publicationYear) {
        this.publicationYear = Check.publicationYear(publicationYear);
    }

    public final int getPages() {
        return pages;
    }

    public final void setPages(int pages) {
        this.pages = requirePages(pages);
    }

    public final int getBorrowDays() {
        return borrowDays;
    }

    public final void setBorrowDays(int borrowDays) {
        this.borrowDays = requireBorrowDays(borrowDays, minBorrowDays);
    }

    public final boolean isAvailable() {
        return available;
    }

    // ---- Displayable ----

    @Override
    public String title() {
        return title;
    }

    @Override
    public String subtitle() {
        return authorOrNotAvailable();
    }
}
