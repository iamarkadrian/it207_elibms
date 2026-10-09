package edu.bulsu.lms.testsupport;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import edu.bulsu.lms.config.AppConfig;
import edu.bulsu.lms.persistence.Database;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A real database for the integration (*IT) tests.
 *
 * <p>Point it at an empty database with environment variables:
 * <pre>
 *   LMS_TEST_DB_URL        jdbc:mysql://127.0.0.1:3306/bulsu_lms_test   or   jdbc:sqlserver://localhost:1433;databaseName=bulsu_lms_test;encrypt=true;trustServerCertificate=true
 *                          (the database name MUST end in _test: the tests drop every table in it)
 *   LMS_TEST_DB_USER / LMS_TEST_DB_PASSWORD         the account the application services use
 *   LMS_TEST_ADMIN_USER / LMS_TEST_ADMIN_PASSWORD   optional: an account allowed to create tables (defaults to the above)
 *   LMS_TEST_ADMIN_URL     optional, SQL Server only: a URL for 'master', used to create the database if it is missing
 *   LMS_REQUIRE_DB=true    fail instead of skipping when no database is configured (set in CI)
 * </pre>
 * The first test that asks for the database drops all tables, then runs the project's own
 * {@code db/<vendor>/01_schema.sql} and {@code 02_seed.sql}, so the scripts are tested too.
 */
public final class TestDatabase {

    private static Database shared;

    private TestDatabase() {
    }

    public static boolean configured() {
        String url = System.getenv("LMS_TEST_DB_URL");
        return url != null && !url.isBlank();
    }

    /** Returns the shared, freshly seeded database, or skips the calling test class when none is configured. */
    public static Database require() {
        if (!configured()) {
            if ("true".equalsIgnoreCase(System.getenv("LMS_REQUIRE_DB"))) {
                fail("LMS_REQUIRE_DB=true but LMS_TEST_DB_URL is not set");
            }
            assumeTrue(false, "No test database: set LMS_TEST_DB_URL (see TestDatabase javadoc)");
        }
        return shared();
    }

    public static boolean isSqlServer() {
        return env("LMS_TEST_DB_URL", "").startsWith("jdbc:sqlserver:");
    }

    /** The database name inside a JDBC URL ("bulsu_lms_test"), or "" if it cannot be found. */
    static String databaseName(String url) {
        Matcher sqlServer = Pattern.compile("(?i)databaseName=([^;]+)").matcher(url);
        if (sqlServer.find()) {
            return sqlServer.group(1);
        }
        Matcher mysql = Pattern.compile("^jdbc:mysql://[^/]+/([^?;]+)").matcher(url);
        return mysql.find() ? mysql.group(1) : "";
    }

    /**
     * The first test DROPS EVERY TABLE, so refuse to run against anything that is not obviously a test
     * database. Set LMS_TEST_ALLOW_ANY_DB=true only if you are certain.
     */
    private static void refuseRealDatabase(String url) {
        String name = databaseName(url);
        boolean allowed = name.endsWith("_test") || "true".equalsIgnoreCase(System.getenv("LMS_TEST_ALLOW_ANY_DB"));
        if (!allowed || !name.matches("[A-Za-z0-9_]+")) {
            throw new IllegalStateException("Refusing to wipe database '" + name + "': the integration tests drop all "
                    + "tables, so the database name must end in _test (for example bulsu_lms_test).");
        }
    }

    private static synchronized Database shared() {
        if (shared == null) {
            String url = env("LMS_TEST_DB_URL", "");
            refuseRealDatabase(url);
            String user = env("LMS_TEST_DB_USER", "");
            String password = env("LMS_TEST_DB_PASSWORD", "");
            try {
                ensureSqlServerDatabase();
                String folder = isSqlServer() ? "mssql" : "mysql";
                try (Connection admin = DriverManager.getConnection(url,
                        env("LMS_TEST_ADMIN_USER", user), env("LMS_TEST_ADMIN_PASSWORD", password))) {
                    for (String script : List.of("99_reset.sql", "01_schema.sql", "02_seed.sql")) {
                        run(admin, Path.of("db", folder, script));
                    }
                }
            } catch (SQLException | IOException e) {
                throw new IllegalStateException("Could not prepare the test database: " + e.getMessage(), e);
            }
            shared = new Database(new AppConfig(url, user, password));
        }
        return shared;
    }

    private static void ensureSqlServerDatabase() throws SQLException {
        String adminUrl = System.getenv("LMS_TEST_ADMIN_URL");
        if (!isSqlServer() || adminUrl == null || adminUrl.isBlank()) {
            return;
        }
        try (Connection master = DriverManager.getConnection(adminUrl, env("LMS_TEST_ADMIN_USER", ""),
                env("LMS_TEST_ADMIN_PASSWORD", ""));
             Statement statement = master.createStatement()) {
            String name = databaseName(env("LMS_TEST_DB_URL", "")); // validated by refuseRealDatabase
            statement.execute("IF DB_ID(N'" + name + "') IS NULL CREATE DATABASE [" + name + "]");
        }
    }

    private static void run(Connection connection, Path script) throws IOException, SQLException {
        String text = Files.readString(script, StandardCharsets.UTF_8);
        try (Statement statement = connection.createStatement()) {
            for (String sql : split(text)) {
                statement.execute(sql);
            }
        }
    }

    /**
     * Splits a script into statements at semicolons, ignoring semicolons inside quoted text and
     * dropping {@code --} comments. Enough for this project's scripts (no procedures, no GO).
     */
    static List<String> split(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            if (quoted) {
                current.append(c);
                if (c == '\'') {
                    quoted = false; // a doubled '' re-opens on the next character
                }
            } else if (c == '\'') {
                quoted = true;
                current.append(c);
            } else if (c == '-' && i + 1 < script.length() && script.charAt(i + 1) == '-') {
                while (i < script.length() && script.charAt(i) != '\n') {
                    i++;
                }
                current.append('\n');
            } else if (c == ';') {
                String sql = current.toString().strip();
                if (!sql.isEmpty()) {
                    statements.add(sql);
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        String tail = current.toString().strip();
        if (!tail.isEmpty()) {
            statements.add(tail);
        }
        return statements;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }
}
