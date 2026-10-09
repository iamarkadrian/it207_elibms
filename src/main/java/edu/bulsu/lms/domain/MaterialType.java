package edu.bulsu.lms.domain;

/** Sub-kinds of {@link OtherMaterial}: things the library keeps but does not lend. */
public enum MaterialType {
    JOURNAL("Journals"),
    MAGAZINE("Magazine"),
    THESIS("Thesis");

    private final String label;

    MaterialType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Combo boxes display {@code toString()}. */
    @Override
    public String toString() {
        return label;
    }
}
