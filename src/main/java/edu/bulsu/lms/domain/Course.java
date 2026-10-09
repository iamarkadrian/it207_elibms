package edu.bulsu.lms.domain;

import java.util.regex.Pattern;

/** A degree program offered by a {@link Department}, e.g. {@code BSIT}. */
public record Course(String code, String name, String departmentCode) {

    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9-]+");

    public Course {
        code = Check.matching(code, "Course code", 16, CODE, "may only contain letters, digits and hyphens.");
        name = Check.text(name, "Course name", 120);
        departmentCode = Check.text(departmentCode, "Department code", 8);
    }

    /** Combo boxes display {@code toString()}. */
    @Override
    public String toString() {
        return name;
    }
}
