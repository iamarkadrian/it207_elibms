package edu.bulsu.lms.domain;

import java.util.regex.Pattern;

/**
 * A named person known to the library. Concrete kinds are {@link Student} (a library member) and
 * {@link Staff} (a librarian or administrator).
 *
 * <p>The name rules live here once, so every subclass gets the same validation.
 */
public abstract sealed class Person implements Displayable permits Student, Staff {

    public static final int NAME_MAX = 30;
    public static final int INITIAL_MAX = 5;

    private static final Pattern NAME = Pattern.compile("\\p{L}[\\p{L} .'’-]*");
    private static final Pattern INITIAL = Pattern.compile("[\\p{L}.]+");

    private String firstName;
    private String middleInitial;
    private String lastName;

    protected Person(String firstName, String middleInitial, String lastName) {
        setFirstName(firstName);
        setMiddleInitial(middleInitial);
        setLastName(lastName);
    }

    /** Validates a first or last name typed by a user; usable before an object exists. */
    public static String requireName(String raw, String label) {
        return Check.matching(raw, label, NAME_MAX, NAME, "may only contain letters, spaces, dots, hyphens and apostrophes.");
    }

    /** Validates an optional middle initial; blank becomes {@code null}. */
    public static String optionalInitial(String raw) {
        String value = Check.optional(raw, "Middle initial", INITIAL_MAX);
        if (value != null && !INITIAL.matcher(value).matches()) {
            throw new ValidationException("Middle initial may only contain letters and dots.");
        }
        return value;
    }

    public final String getFirstName() {
        return firstName;
    }

    public final void setFirstName(String firstName) {
        this.firstName = requireName(firstName, "First name");
    }

    /** The middle initial, or {@code null} when the person has none. */
    public final String getMiddleInitial() {
        return middleInitial;
    }

    public final void setMiddleInitial(String middleInitial) {
        this.middleInitial = optionalInitial(middleInitial);
    }

    public final String getLastName() {
        return lastName;
    }

    public final void setLastName(String lastName) {
        this.lastName = requireName(lastName, "Last name");
    }

    /** "Mark E. Santos" - also usable for names that come straight from a database row. */
    public static String formatName(String first, String middleInitial, String last) {
        return middleInitial == null || middleInitial.isBlank() ? first + " " + last : first + " " + middleInitial + " " + last;
    }

    /** "Mark E. Santos" - the middle initial is skipped when absent. */
    public final String fullName() {
        return formatName(firstName, middleInitial, lastName);
    }

    /** "Santos, Mark E." - used where lists are read alphabetically. */
    public final String listName() {
        return middleInitial == null ? lastName + ", " + firstName : lastName + ", " + firstName + " " + middleInitial;
    }

    /** Human-readable role such as "Student" or "Librarian"; differs per subclass. */
    public abstract String roleLabel();

    /** The value that identifies this person to users (student number, staff id, ...). */
    public abstract String identifier();

    @Override
    public String title() {
        return firstName;
    }
}
