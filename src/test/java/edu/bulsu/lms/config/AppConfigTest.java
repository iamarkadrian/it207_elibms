package edu.bulsu.lms.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class AppConfigTest {

    @Test
    void usesMySqlOnLocalhostByDefault() {
        AppConfig config = AppConfig.from(Map.of(), new Properties());
        assertEquals(AppConfig.DEFAULT_URL, config.dbUrl());
        assertEquals(AppConfig.DEFAULT_USER, config.dbUser());
        assertEquals("", config.dbPassword());
        assertEquals("MySQL", config.vendor());
    }

    @Test
    void fileOverridesDefaultsAndEnvironmentOverridesFile() {
        Properties file = new Properties();
        file.setProperty("lms.db.url", "jdbc:sqlserver://db:1433;databaseName=bulsu_lms");
        file.setProperty("lms.db.user", "file-user");
        file.setProperty("lms.db.password", "file-secret");

        AppConfig fromFile = AppConfig.from(Map.of(), file);
        assertEquals("file-user", fromFile.dbUser());
        assertEquals("SQL Server", fromFile.vendor());

        AppConfig fromEnv = AppConfig.from(Map.of("LMS_DB_USER", "env-user", "LMS_DB_PASSWORD", "env-secret"), file);
        assertEquals("env-user", fromEnv.dbUser());
        assertEquals("env-secret", fromEnv.dbPassword());
        assertEquals("jdbc:sqlserver://db:1433;databaseName=bulsu_lms", fromEnv.dbUrl(), "values not in the env still come from the file");
    }

    @Test
    void blankEnvironmentValuesAreIgnored() {
        AppConfig config = AppConfig.from(Map.of("LMS_DB_USER", "  "), new Properties());
        assertEquals(AppConfig.DEFAULT_USER, config.dbUser());
    }

    @Test
    void theToStringNeverShowsThePassword() {
        AppConfig config = new AppConfig("jdbc:mysql://h/db", "u", "super-secret-value");
        assertFalse(config.toString().contains("super-secret-value"));
    }

    @Test
    void thePlaceholderPasswordFromTheExampleFileIsRejected() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> new AppConfig("jdbc:mysql://h/db", "u", "CHANGE_ME"));
        assertTrue(refused.getMessage().contains("placeholder"));
        assertThrows(IllegalArgumentException.class, () -> new AppConfig("jdbc:mysql://h/db", "u", "YOUR_STRONG_PASSWORD"));
    }

    @Test
    void aBlankUrlIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AppConfig(" ", "u", "p"));
    }
}
