package edu.bulsu.lms.domain;

/** Gender as recorded on a student membership. The one-letter code is what the database stores. */
public enum Gender {
    MALE("M", "Male"),
    FEMALE("F", "Female");

    private final String code;
    private final String label;

    Gender(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static Gender fromCode(String code) {
        for (Gender gender : values()) {
            if (gender.code.equals(code)) {
                return gender;
            }
        }
        throw new IllegalArgumentException("Unknown gender code: " + code);
    }

    /** Combo boxes display {@code toString()}. */
    @Override
    public String toString() {
        return label;
    }
}
