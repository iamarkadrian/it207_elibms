package edu.bulsu.lms.domain;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A staff account: a librarian or the administrator. The two roles share this class; what differs
 * (for example whether the account can be locked) is decided by {@link Role}.
 *
 * <p>The password is never stored here, only its salted hash.
 */
public final class Staff extends Person {

    public static final int MAX_FAILED_ATTEMPTS = 3;

    public static final List<String> POSITIONS = List.of(
            "Librarian", "Assistant Librarian", "Reference Librarian",
            "Technical Librarian", "Unit Head Librarian", "University Librarian");

    private static final Pattern USERNAME = Pattern.compile("[a-z0-9._-]{4,20}");

    private final int id;
    private final Role role;
    private String position;
    private String departmentCode;
    private String username;
    private String passwordHash;
    private boolean locked;
    private int failedAttempts;

    /** Full constructor, used when loading an existing account from the database. */
    public Staff(int id, String firstName, String middleInitial, String lastName, String position,
                 String departmentCode, String username, String passwordHash, Role role,
                 boolean locked, int failedAttempts) {
        super(firstName, middleInitial, lastName);
        if (role == null) {
            throw new ValidationException("Role is required.");
        }
        this.id = id;
        this.role = role;
        setPosition(position);
        setDepartmentCode(departmentCode);
        setUsername(username);
        setPasswordHash(passwordHash);
        this.locked = locked;
        this.failedAttempts = Check.range(failedAttempts, "Failed attempts", 0, 1000);
    }

    /** Overloaded constructor for a new, not yet saved, unlocked account. */
    public Staff(String firstName, String middleInitial, String lastName, String position,
                 String departmentCode, String username, String passwordHash, Role role) {
        this(0, firstName, middleInitial, lastName, position, departmentCode, username, passwordHash, role, false, 0);
    }

    /** Usernames are case-insensitive: 4-20 characters of letters, digits, dot, underscore or hyphen. */
    public static String requireUsername(String raw) {
        String lowered = raw == null ? null : raw.strip().toLowerCase(Locale.ROOT);
        return Check.matching(lowered, "Username", 20, USERNAME,
                "must be 4-20 characters: letters, digits, '.', '_' or '-'.");
    }

    public int getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = Check.text(position, "Position", 60);
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = Check.text(departmentCode, "Designation", 8);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = requireUsername(username);
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = Check.text(passwordHash, "Password hash", 255);
    }

    public boolean isLocked() {
        return locked;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    /**
     * Registers one wrong password. Lockable roles are locked once {@link #MAX_FAILED_ATTEMPTS}
     * is reached.
     *
     * @return {@code true} if this attempt is the one that locked the account
     */
    public boolean recordFailedLogin() {
        if (failedAttempts < MAX_FAILED_ATTEMPTS) {
            failedAttempts++; // capped: the counter only needs to reach the lock threshold
        }
        if (role.canBeLocked() && failedAttempts >= MAX_FAILED_ATTEMPTS && !locked) {
            locked = true;
            return true;
        }
        return false;
    }

    public void recordSuccessfulLogin() {
        failedAttempts = 0;
    }

    /** Administrator action: reopens a locked account and forgives past failures. */
    public void unlock() {
        locked = false;
        failedAttempts = 0;
    }

    @Override
    public String roleLabel() {
        return role.label();
    }

    @Override
    public String identifier() {
        return String.valueOf(id);
    }

    @Override
    public String subtitle() {
        return position;
    }

    @Override
    public List<String> lines() {
        return List.of(
                roleLabel(),
                "Designation: " + departmentCode,
                "Username: " + username,
                "Account: " + (locked ? "Locked" : "Unlocked"));
    }
}
