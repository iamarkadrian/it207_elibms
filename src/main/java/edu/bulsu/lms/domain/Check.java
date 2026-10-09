package edu.bulsu.lms.domain;

import java.time.Year;
import java.util.regex.Pattern;

/**
 * Small, reusable input validators.
 *
 * <p>Every setter and constructor in the domain model funnels through these methods, and the UI
 * wizards reuse them to validate each step. That keeps one source of truth for each rule.
 */
public final class Check {

    private Check() {
        // static helpers only
    }

    /** Required text: trimmed, not blank, and at most {@code max} characters. */
    public static String text(String value, String label, int max) {
        String trimmed = value == null ? "" : value.strip();
        if (trimmed.isEmpty()) {
            throw new ValidationException(label + " cannot be blank.");
        }
        if (trimmed.length() > max) {
            throw new ValidationException(label + " cannot be longer than " + max + " characters.");
        }
        if (trimmed.chars().anyMatch(Check::isUnsafeCharacter)) {
            throw new ValidationException(label + " contains characters that are not allowed.");
        }
        return trimmed;
    }

    /** Control characters and the bidirectional overrides that can make text lie about how it reads. */
    private static boolean isUnsafeCharacter(int c) {
        return Character.isISOControl(c) || (c >= 0x202A && c <= 0x202E) || (c >= 0x2066 && c <= 0x2069);
    }

    /** Optional text: a blank value becomes {@code null}; anything else follows {@link #text}. */
    public static String optional(String value, String label, int max) {
        return value == null || value.isBlank() ? null : text(value, label, max);
    }

    /** Required text that must also match {@code pattern}; {@code rule} completes the error message. */
    public static String matching(String value, String label, int max, Pattern pattern, String rule) {
        String trimmed = text(value, label, max);
        if (!pattern.matcher(trimmed).matches()) {
            throw new ValidationException(label + " " + rule);
        }
        return trimmed;
    }

    /** Whole number between {@code min} and {@code max} (inclusive). */
    public static int range(int value, String label, int min, int max) {
        if (value < min || value > max) {
            throw new ValidationException(label + " must be between " + min + " and " + max + ".");
        }
        return value;
    }

    /** Parses text typed into a form field as a whole number. */
    public static int integer(String raw, String label) {
        String trimmed = text(raw, label, 12);
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            throw new ValidationException(label + " must be a whole number.");
        }
    }

    /** A plausible publication year: 1000 up to next year (pre-orders). */
    public static int publicationYear(int year) {
        return range(year, "Publication year", 1000, Year.now().getValue() + 1);
    }
}
