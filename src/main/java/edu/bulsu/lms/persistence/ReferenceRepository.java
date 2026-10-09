package edu.bulsu.lms.persistence;

import edu.bulsu.lms.domain.Course;
import edu.bulsu.lms.domain.Department;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Read-only lookup tables: colleges/campuses and the courses they offer. */
public final class ReferenceRepository {

    public List<Department> departments(Connection connection) throws SQLException {
        String sql = "SELECT code, name FROM department ORDER BY name";
        List<Department> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(new Department(rs.getString("code"), rs.getString("name")));
            }
        }
        return result;
    }

    public List<Course> courses(Connection connection) throws SQLException {
        String sql = "SELECT code, name, department_code FROM course ORDER BY name";
        List<Course> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(new Course(rs.getString("code"), rs.getString("name"), rs.getString("department_code")));
            }
        }
        return result;
    }
}
