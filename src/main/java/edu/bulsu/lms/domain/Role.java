package edu.bulsu.lms.domain;

/**
 * What a staff account is allowed to do. One login screen serves both roles; the role decides
 * which dashboard opens after sign-in.
 */
public enum Role {
    ADMIN("Administrator", false),
    LIBRARIAN("Librarian", true);

    private final String label;
    private final boolean lockable;

    Role(String label, boolean lockable) {
        this.label = label;
        this.lockable = lockable;
    }

    public String label() {
        return label;
    }

    /**
     * Librarian accounts are locked after repeated wrong passwords and must be unlocked by an
     * administrator. The administrator account is only throttled, otherwise nobody could unlock it.
     */
    public boolean canBeLocked() {
        return lockable;
    }
}
