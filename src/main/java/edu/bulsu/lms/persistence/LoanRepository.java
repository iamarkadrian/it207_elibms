package edu.bulsu.lms.persistence;

import edu.bulsu.lms.domain.Loan;
import edu.bulsu.lms.domain.Person;
import edu.bulsu.lms.domain.UserFacingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD for the {@code loan} table. One row covers a loan from issue to return:
 * {@code returned_at} is {@code NULL} while the item is still out.
 *
 * <p>Dates are passed from Java instead of calling {@code NOW()} / {@code GETDATE()}, which keeps
 * the SQL identical on MySQL and SQL Server and lets tests control "now".
 */
public final class LoanRepository {

    private static final String SELECT = """
            SELECT l.id, l.item_code, i.title AS item_title,
                   l.student_number, s.first_name AS s_first, s.middle_initial AS s_initial,
                   s.last_name AS s_last, s.course_code,
                   l.issued_by, t.first_name AS t_first, t.middle_initial AS t_initial, t.last_name AS t_last,
                   l.issued_at, l.due_at, l.returned_at
            FROM loan l
            JOIN item i ON i.code = l.item_code
            JOIN student s ON s.student_number = l.student_number
            JOIN staff t ON t.id = l.issued_by
            """;

    public List<Loan> findOpen(Connection connection) throws SQLException {
        return list(connection, "WHERE l.returned_at IS NULL ORDER BY l.issued_at DESC, l.id DESC", null);
    }

    public List<Loan> findReturned(Connection connection) throws SQLException {
        return list(connection, "WHERE l.returned_at IS NOT NULL ORDER BY l.returned_at DESC, l.id DESC", null);
    }

    /** Still out and past the due date at {@code now}. */
    public List<Loan> findOverdue(Connection connection, LocalDateTime now) throws SQLException {
        return list(connection, "WHERE l.returned_at IS NULL AND l.due_at < ? ORDER BY l.due_at, l.id", now);
    }

    public Optional<Loan> findById(Connection connection, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT + "WHERE l.id = ?")) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /** @return the id the database generated for the new loan */
    public int insert(Connection connection, String itemCode, String studentNumber, int issuedBy,
                      LocalDateTime issuedAt, LocalDateTime dueAt) throws SQLException {
        String sql = "INSERT INTO loan (item_code, student_number, issued_by, issued_at, due_at) VALUES (?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, itemCode);
            statement.setString(2, studentNumber);
            statement.setInt(3, issuedBy);
            statement.setObject(4, issuedAt);
            statement.setObject(5, dueAt);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("The database did not return the new loan id.");
                }
                return keys.getInt(1);
            }
        }
    }

    /** @return 1 if the loan was open and is now returned, 0 if it was already returned or does not exist */
    public int markReturned(Connection connection, int id, LocalDateTime returnedAt) throws SQLException {
        String sql = "UPDATE loan SET returned_at = ? WHERE id = ? AND returned_at IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, returnedAt);
            statement.setInt(2, id);
            return statement.executeUpdate();
        }
    }

    public int countOpen(Connection connection) throws SQLException {
        return Counts.single(connection, "SELECT COUNT(*) FROM loan WHERE returned_at IS NULL");
    }

    public int countReturned(Connection connection) throws SQLException {
        return Counts.single(connection, "SELECT COUNT(*) FROM loan WHERE returned_at IS NOT NULL");
    }

    public int countOverdue(Connection connection, LocalDateTime now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM loan WHERE returned_at IS NULL AND due_at < ?")) {
            statement.setObject(1, now);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** All loans (open or returned) for one member; used to refuse deleting a member with history. */
    public int countForStudent(Connection connection, String studentNumber) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM loan WHERE student_number = ?")) {
            statement.setString(1, studentNumber);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** All loans issued by one staff member; used to refuse deleting a librarian with history. */
    public int countIssuedBy(Connection connection, int staffId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM loan WHERE issued_by = ?")) {
            statement.setInt(1, staffId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private List<Loan> list(Connection connection, String clause, LocalDateTime parameter) throws SQLException {
        List<Loan> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(SELECT + clause)) {
            if (parameter != null) {
                statement.setObject(1, parameter);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    private static Loan map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        try {
            return new Loan(
                    id,
                    rs.getString("item_code"),
                    rs.getString("item_title"),
                    rs.getString("student_number"),
                    Person.formatName(rs.getString("s_first"), rs.getString("s_initial"), rs.getString("s_last")),
                    rs.getString("course_code"),
                    rs.getInt("issued_by"),
                    Person.formatName(rs.getString("t_first"), rs.getString("t_initial"), rs.getString("t_last")),
                    rs.getObject("issued_at", LocalDateTime.class),
                    rs.getObject("due_at", LocalDateTime.class),
                    rs.getObject("returned_at", LocalDateTime.class));
        } catch (UserFacingException e) {
            throw DataAccessException.corruptRow("loan", String.valueOf(id), e);
        }
    }
}
