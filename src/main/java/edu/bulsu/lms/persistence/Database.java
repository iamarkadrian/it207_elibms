package edu.bulsu.lms.persistence;

import edu.bulsu.lms.config.AppConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The only place that opens and closes JDBC connections.
 *
 * <p>Every operation borrows one connection inside try-with-resources, so a connection is always
 * closed, even when the work throws. Repositories never open connections themselves.
 */
public final class Database {

    private static final System.Logger LOG = System.getLogger(Database.class.getName());
    private static final int LOGIN_TIMEOUT_SECONDS = 10;

    private final AppConfig config;

    public Database(AppConfig config) {
        this.config = config;
        DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
    }

    public AppConfig config() {
        return config;
    }

    /** Runs read-only (or single-statement) work on a fresh connection. */
    public <T> T query(SqlFunction<T> work) {
        try (Connection connection = open()) {
            return work.apply(connection);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    /** Runs several statements as one unit: all of them commit, or none do. */
    public <T> T transaction(SqlFunction<T> work) {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                T result = work.apply(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    e.addSuppressed(rollbackFailure);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    /** Fails fast at start-up with a clear message if the database cannot be reached. */
    public void verifyConnection() {
        query(connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT 1")) {
                return rs.next();
            }
        });
    }

    private Connection open() throws SQLException {
        if (config.dbUser().isBlank()) {
            return DriverManager.getConnection(config.dbUrl());
        }
        return DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPassword());
    }

    private static DataAccessException translate(SQLException e) {
        LOG.log(System.Logger.Level.ERROR,
                "SQL failure (SQLState=" + e.getSQLState() + ", code=" + e.getErrorCode() + "): " + e.getMessage(), e);
        return new DataAccessException("The database could not complete the request.", e);
    }
}
