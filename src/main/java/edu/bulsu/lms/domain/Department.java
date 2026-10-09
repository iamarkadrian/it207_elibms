package edu.bulsu.lms.domain;

import java.util.regex.Pattern;

/** A college, campus or unit of the university, e.g. {@code CICT}. */
public record Department(String code, String name) {

    private static final Pattern CODE = Pattern.compile("[A-Z]{2,8}");

    public Department {
        code = Check.matching(code, "Department code", 8, CODE, "must be 2-8 capital letters.");
        name = Check.text(name, "Department name", 100);
    }

    /** Combo boxes display {@code toString()}. */
    @Override
    public String toString() {
        return name;
    }
}
