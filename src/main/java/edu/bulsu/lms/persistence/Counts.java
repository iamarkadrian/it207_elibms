package edu.bulsu.lms.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Runs a constant {@code SELECT COUNT(*)} statement and returns the number. */
final class Counts {

    private Counts() {
        // static helper only
    }

    /** {@code sql} must be a fixed string with no parameters; never build it from user input. */
    static int single(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
