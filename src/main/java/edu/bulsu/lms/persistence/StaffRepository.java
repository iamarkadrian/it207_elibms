package edu.bulsu.lms.persistence;

import edu.bulsu.lms.domain.Role;
import edu.bulsu.lms.domain.Staff;
import edu.bulsu.lms.domain.UserFacingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CRUD for the {@code staff} table (librarians and the administrator). */
public final class StaffRepository {

    private static final String COLUMNS = "id, first_name, middle_initial, last_name, position, department_code, "
            + "username, password_hash, role, locked, failed_attempts";

    public Optional<Staff> findByUsername(Connection connection, String username) throws SQLException {
        return findOne(connection, "username = ?", username);
    }

    public Optional<Staff> findById(Connection connection, int id) throws SQLException {
        return findOne(connection, "id = ?", id);
    }

    public List<Staff> findByRole(Connection connection, Role role) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM staff WHERE role = ? ORDER BY id";
        List<Staff> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, role.name());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    /** @return the id the database generated for the new account */
    public int insert(Connection connection, Staff staff) throws SQLException {
        String sql = "INSERT INTO staff (first_name, middle_initial, last_name, position, department_code, "
                + "username, password_hash, role, locked, failed_attempts) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, staff.getFirstName());
            statement.setString(2, staff.getMiddleInitial());
            statement.setString(3, staff.getLastName());
            statement.setString(4, staff.getPosition());
            statement.setString(5, staff.getDepartmentCode());
            statement.setString(6, staff.getUsername());
            statement.setString(7, staff.getPasswordHash());
            statement.setString(8, staff.getRole().name());
            statement.setBoolean(9, staff.isLocked());
            statement.setInt(10, staff.getFailedAttempts());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("The database did not return the new staff id.");
                }
                return keys.getInt(1);
            }
        }
    }

    /** Saves what an administrator can edit: username, password hash, designation and position. */
    public int updateAccount(Connection connection, Staff staff) throws SQLException {
        String sql = "UPDATE staff SET username = ?, password_hash = ?, department_code = ?, position = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, staff.getUsername());
            statement.setString(2, staff.getPasswordHash());
            statement.setString(3, staff.getDepartmentCode());
            statement.setString(4, staff.getPosition());
            statement.setInt(5, staff.getId());
            return statement.executeUpdate();
        }
    }

    /** Saves the lock state and the failed-attempt counter after a sign-in attempt or an unlock. */
    public int updateLoginState(Connection connection, Staff staff) throws SQLException {
        String sql = "UPDATE staff SET locked = ?, failed_attempts = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, staff.isLocked());
            statement.setInt(2, staff.getFailedAttempts());
            statement.setInt(3, staff.getId());
            return statement.executeUpdate();
        }
    }

    public int delete(Connection connection, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM staff WHERE id = ?")) {
            statement.setInt(1, id);
            return statement.executeUpdate();
        }
    }

    public boolean usernameTaken(Connection connection, String username, int exceptId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM staff WHERE username = ? AND id <> ?")) {
            statement.setString(1, username);
            statement.setInt(2, exceptId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Optional<Staff> findOne(Connection connection, String where, Object parameter) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM staff WHERE " + where;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, parameter);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private static Staff map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        try {
            return new Staff(
                    id,
                    rs.getString("first_name"),
                    rs.getString("middle_initial"),
                    rs.getString("last_name"),
                    rs.getString("position"),
                    rs.getString("department_code"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    Role.valueOf(rs.getString("role")),
                    rs.getBoolean("locked"),
                    rs.getInt("failed_attempts"));
        } catch (UserFacingException | IllegalArgumentException e) {
            throw DataAccessException.corruptRow("staff", String.valueOf(id), e);
        }
    }
}
