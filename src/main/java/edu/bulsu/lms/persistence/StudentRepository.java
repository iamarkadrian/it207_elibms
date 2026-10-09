package edu.bulsu.lms.persistence;

import edu.bulsu.lms.domain.Address;
import edu.bulsu.lms.domain.Gender;
import edu.bulsu.lms.domain.Student;
import edu.bulsu.lms.domain.UserFacingException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CRUD for the {@code student} table. Every statement is parameterised. */
public final class StudentRepository {

    private static final String COLUMNS = "student_number, first_name, middle_initial, last_name, department_code, "
            + "course_code, section, membership_date, gender, contact_number, house_no, street, barangay, "
            + "municipality, province";

    public List<Student> findAll(Connection connection) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM student ORDER BY last_name, first_name, student_number";
        List<Student> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
        }
        return result;
    }

    public Optional<Student> find(Connection connection, String studentNumber) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM student WHERE student_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, studentNumber);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public boolean exists(Connection connection, String studentNumber) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM student WHERE student_number = ?")) {
            statement.setString(1, studentNumber);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insert(Connection connection, Student student) throws SQLException {
        String sql = "INSERT INTO student (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int i = 1;
            statement.setString(i++, student.getStudentNumber());
            statement.setString(i++, student.getFirstName());
            statement.setString(i++, student.getMiddleInitial());
            statement.setString(i++, student.getLastName());
            statement.setString(i++, student.getDepartmentCode());
            statement.setString(i++, student.getCourseCode());
            statement.setString(i++, student.getSection());
            statement.setDate(i++, Date.valueOf(student.getMembershipDate()));
            statement.setString(i++, student.getGender().code());
            statement.setString(i++, student.getContactNumber());
            statement.setString(i++, student.getAddress().houseNo());
            statement.setString(i++, student.getAddress().street());
            statement.setString(i++, student.getAddress().barangay());
            statement.setString(i++, student.getAddress().municipality());
            statement.setString(i, student.getAddress().province());
            statement.executeUpdate();
        }
    }

    /** @return the number of rows removed (0 if the member did not exist) */
    public int delete(Connection connection, String studentNumber) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM student WHERE student_number = ?")) {
            statement.setString(1, studentNumber);
            return statement.executeUpdate();
        }
    }

    public int count(Connection connection) throws SQLException {
        return Counts.single(connection, "SELECT COUNT(*) FROM student");
    }

    private static Student map(ResultSet rs) throws SQLException {
        String number = rs.getString("student_number");
        try {
            return new Student(
                    number,
                    rs.getString("first_name"),
                    rs.getString("middle_initial"),
                    rs.getString("last_name"),
                    rs.getString("department_code"),
                    rs.getString("course_code"),
                    rs.getString("section"),
                    Gender.fromCode(rs.getString("gender")),
                    rs.getString("contact_number"),
                    new Address(rs.getString("house_no"), rs.getString("street"), rs.getString("barangay"),
                            rs.getString("municipality"), rs.getString("province")),
                    rs.getDate("membership_date").toLocalDate());
        } catch (UserFacingException | IllegalArgumentException e) {
            throw DataAccessException.corruptRow("student", number, e);
        }
    }
}
