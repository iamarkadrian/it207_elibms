package edu.bulsu.lms.persistence;

import java.sql.SQLException;

/**
 * A technical database failure. Its message is deliberately generic: the details are logged, not
 * shown, so SQL text and server names never reach the screen.
 */
public final class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    /** A stored row could not be turned back into a valid domain object. */
    static DataAccessException corruptRow(String table, String key, Exception cause) {
        return new DataAccessException("Row '" + key + "' in " + table + " violates the domain rules: " + cause.getMessage(), cause);
    }

    /** Duplicate-key error numbers: MySQL 1062; SQL Server 2601 (unique index) and 2627 (unique/primary key constraint). */
    private static final int MYSQL_DUPLICATE_ENTRY = 1062;
    private static final int MSSQL_DUPLICATE_INDEX = 2601;
    private static final int MSSQL_DUPLICATE_KEY = 2627;

    /**
     * True only for "this value already exists", not for any other constraint. Use it to turn a race
     * between two users into a friendly "already taken" message; other constraint failures are bugs.
     */
    public boolean isDuplicateKey() {
        for (Throwable t = getCause(); t != null; t = t.getCause()) {
            if (t instanceof SQLException sql) {
                int code = sql.getErrorCode();
                if (code == MYSQL_DUPLICATE_ENTRY || code == MSSQL_DUPLICATE_INDEX || code == MSSQL_DUPLICATE_KEY) {
                    return true;
                }
            }
        }
        return false;
    }

    /** MySQL reports a violated CHECK constraint as error 3819 with the generic SQLSTATE HY000. */
    private static final int MYSQL_CHECK_VIOLATION = 3819;

    /** True for unique-key, foreign-key and check-constraint violations (SQLSTATE class 23, or MySQL 3819). */
    public boolean isConstraintViolation() {
        for (Throwable t = getCause(); t != null; t = t.getCause()) {
            if (t instanceof SQLException sql
                    && ((sql.getSQLState() != null && sql.getSQLState().startsWith("23"))
                    || sql.getErrorCode() == MYSQL_CHECK_VIOLATION)) {
                return true;
            }
        }
        return false;
    }
}
