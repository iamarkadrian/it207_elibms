package edu.bulsu.lms.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Database connection settings. Nothing sensitive is hard-coded in the source.
 *
 * <p>Each value is looked up in this order: environment variable, then the first
 * {@code lms.properties} file found, then the built-in default (MySQL on localhost).
 *
 * <pre>
 *   LMS_DB_URL       / lms.db.url       e.g. jdbc:mysql://localhost:3306/bulsu_lms
 *   LMS_DB_USER      / lms.db.user
 *   LMS_DB_PASSWORD  / lms.db.password
 * </pre>
 *
 * Point the URL at a {@code jdbc:sqlserver:} address to use Microsoft SQL Server instead.
 */
public record AppConfig(String dbUrl, String dbUser, String dbPassword) {

    public static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/bulsu_lms";
    public static final String DEFAULT_USER = "bulsu_lms";

    /** Values copied from the example and the setup scripts; running with one of these is always a mistake. */
    private static final Set<String> PLACEHOLDERS = Set.of("CHANGE_ME", "YOUR_STRONG_PASSWORD", "YOUR_Strong_Password_1!");

    public AppConfig {
        if (dbUrl == null || dbUrl.isBlank()) {
            throw new IllegalArgumentException("The database URL is required (LMS_DB_URL or lms.db.url).");
        }
        dbUser = dbUser == null ? "" : dbUser;
        dbPassword = dbPassword == null ? "" : dbPassword;
        if (PLACEHOLDERS.contains(dbPassword)) {
            throw new IllegalArgumentException("The database password is still the placeholder from the example file. "
                    + "Set lms.db.password (or LMS_DB_PASSWORD) to the password you chose in the database setup script.");
        }
    }

    /** Loads settings from the real environment and the first config file that exists. */
    public static AppConfig load() {
        return from(System.getenv(), readConfigFile());
    }

    /** Pure function of its inputs, so it can be unit tested without touching the machine. */
    public static AppConfig from(Map<String, String> env, Properties file) {
        return new AppConfig(
                pick(env.get("LMS_DB_URL"), file.getProperty("lms.db.url"), DEFAULT_URL),
                pick(env.get("LMS_DB_USER"), file.getProperty("lms.db.user"), DEFAULT_USER),
                pick(env.get("LMS_DB_PASSWORD"), file.getProperty("lms.db.password"), ""));
    }

    /** "MySQL", "SQL Server" or the raw JDBC subprotocol; shown in the log and the README. */
    public String vendor() {
        if (dbUrl.startsWith("jdbc:mysql:")) {
            return "MySQL";
        }
        if (dbUrl.startsWith("jdbc:sqlserver:")) {
            return "SQL Server";
        }
        return dbUrl.split(":", 3).length > 1 ? dbUrl.split(":", 3)[1] : dbUrl;
    }

    /** Never print the password, even by accident in a log line. */
    @Override
    public String toString() {
        return "AppConfig[dbUrl=" + dbUrl + ", dbUser=" + dbUser + ", dbPassword=****]";
    }

    private static String pick(String fromEnv, String fromFile, String fallback) {
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.strip();
        }
        return fallback;
    }

    private static Properties readConfigFile() {
        List<Path> candidates = List.of(
                Path.of(System.getProperty("lms.config", "lms.properties")),
                Path.of(System.getProperty("user.home"), ".bulsu-lms", "lms.properties"));
        for (Path path : candidates) {
            if (Files.isRegularFile(path)) {
                Properties properties = new Properties();
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                    return properties;
                } catch (IOException e) {
                    throw new IllegalStateException("Cannot read the config file " + path + ": " + e.getMessage(), e);
                }
            }
        }
        return new Properties();
    }
}
