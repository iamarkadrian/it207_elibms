package edu.bulsu.lms.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.bulsu.lms.domain.ItemType;
import edu.bulsu.lms.domain.LibraryItem;
import edu.bulsu.lms.domain.Loan;
import edu.bulsu.lms.domain.Role;
import edu.bulsu.lms.domain.Staff;
import edu.bulsu.lms.security.PasswordHasher;
import edu.bulsu.lms.testsupport.TestDatabase;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Checks the SQL scripts themselves: the demo data loads, every row is a valid domain object, the
 * data is internally consistent, and the database rejects bad rows on its own.
 */
class SchemaIT {

    private static Database db;

    @BeforeAll
    static void connect() {
        db = TestDatabase.require();
    }

    private static int count(String sql, Object... parameters) {
        return db.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < parameters.length; i++) {
                    statement.setObject(i + 1, parameters[i]);
                }
                try (var rs = statement.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        });
    }

    @Test
    @DisplayName("the seed script loads a realistic volume of data")
    void seedVolume() {
        assertTrue(count("SELECT COUNT(*) FROM department") >= 23);
        assertTrue(count("SELECT COUNT(*) FROM course") >= 100);
        assertTrue(count("SELECT COUNT(*) FROM staff") >= 30);
        assertTrue(count("SELECT COUNT(*) FROM student") >= 600);
        assertTrue(count("SELECT COUNT(*) FROM item WHERE item_type = 'BOOK'") >= 300);
        assertTrue(count("SELECT COUNT(*) FROM item WHERE item_type = 'DIGITAL'") >= 180);
        assertTrue(count("SELECT COUNT(*) FROM item WHERE item_type = 'OTHER'") >= 130);
        assertTrue(count("SELECT COUNT(*) FROM loan") >= 1000);
        assertTrue(count("SELECT COUNT(*) FROM loan WHERE returned_at IS NULL") >= 80, "some loans are still open");
    }

    @Test
    @DisplayName("every seeded row satisfies the domain rules (rows load into objects)")
    void everySeededRowLoadsIntoADomainObject() {
        StudentRepository students = new StudentRepository();
        ItemRepository items = new ItemRepository();
        LoanRepository loans = new LoanRepository();
        StaffRepository staff = new StaffRepository();

        assertEquals(count("SELECT COUNT(*) FROM student"), db.query(students::findAll).size());
        for (ItemType type : ItemType.values()) {
            List<LibraryItem> found = db.query(c -> items.findAll(c, type));
            assertEquals(count("SELECT COUNT(*) FROM item WHERE item_type = ?", type.name()), found.size());
            assertTrue(found.stream().allMatch(item -> item.type() == type), "the discriminator picks the right subclass");
        }
        assertEquals(count("SELECT COUNT(*) FROM loan WHERE returned_at IS NULL"), db.query(loans::findOpen).size());
        assertEquals(count("SELECT COUNT(*) FROM loan WHERE returned_at IS NOT NULL"), db.query(loans::findReturned).size());
        assertEquals(count("SELECT COUNT(*) FROM staff WHERE role = 'LIBRARIAN'"),
                db.query(c -> staff.findByRole(c, Role.LIBRARIAN)).size());
    }

    @Test
    @DisplayName("the demo accounts' password hashes verify with the app's own hasher")
    void seededAccountsCanSignIn() {
        StaffRepository staff = new StaffRepository();
        PasswordHasher hasher = new PasswordHasher();

        Staff admin = db.query(c -> staff.findByUsername(c, "admin")).orElseThrow();
        assertEquals(Role.ADMIN, admin.getRole());
        assertTrue(hasher.verify("Admin@2026".toCharArray(), admin.getPasswordHash()));
        assertFalse(hasher.verify("Library@2026".toCharArray(), admin.getPasswordHash()));

        Staff librarian = db.query(c -> staff.findByUsername(c, "librarian")).orElseThrow();
        assertEquals(Role.LIBRARIAN, librarian.getRole());
        assertTrue(hasher.verify("Library@2026".toCharArray(), librarian.getPasswordHash()));

        assertTrue(count("SELECT COUNT(*) FROM staff WHERE locked = ?", true) >= 2, "demo data includes locked accounts to unlock");
    }

    @Test
    void availabilityAgreesWithOpenLoans() {
        assertEquals(0, count("SELECT COUNT(*) FROM item i WHERE i.item_type = 'BOOK' AND i.available = ? "
                + "AND NOT EXISTS (SELECT 1 FROM loan l WHERE l.item_code = i.code AND l.returned_at IS NULL)", false),
                "a book marked unavailable must have an open loan");
        assertEquals(0, count("SELECT COUNT(*) FROM item i WHERE i.item_type = 'BOOK' AND i.available = ? "
                + "AND EXISTS (SELECT 1 FROM loan l WHERE l.item_code = i.code AND l.returned_at IS NULL)", true),
                "a book with an open loan must be marked unavailable");
        assertEquals(0, count("SELECT COUNT(*) FROM (SELECT item_code FROM loan l JOIN item i ON i.code = l.item_code "
                + "WHERE l.returned_at IS NULL AND i.item_type = 'BOOK' GROUP BY item_code HAVING COUNT(*) > 1) x"),
                "a printed book is never lent to two people at once");
    }

    @Test
    void overdueLoansAreWhatTheMissingScreenShows() {
        LoanRepository loans = new LoanRepository();
        LocalDateTime now = LocalDateTime.now();
        List<Loan> overdue = db.query(c -> loans.findOverdue(c, now));
        assertFalse(overdue.isEmpty(), "the demo data contains overdue loans");
        assertTrue(overdue.stream().allMatch(l -> l.isOverdue(now)));
        int counted = db.query(c -> loans.countOverdue(c, now));
        assertEquals(overdue.size(), counted);
    }

    // ---------- the database itself rejects bad rows ----------

    private static void assertRejected(SqlFunction<?> badStatement) {
        DataAccessException e = assertThrows(DataAccessException.class, () -> db.query(badStatement));
        assertTrue(e.isConstraintViolation(), "expected a constraint violation but got: " + e.getCause());
    }

    private static SqlFunction<Integer> insertStudent(String number, String gender, String contact, String course) {
        return c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO student (student_number, first_name, middle_initial, "
                    + "last_name, department_code, course_code, section, membership_date, gender, contact_number, house_no, "
                    + "street, barangay, municipality, province) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, number);
                ps.setString(2, "Test");
                ps.setString(3, null);
                ps.setString(4, "Row");
                ps.setString(5, "CICT");
                ps.setString(6, course);
                ps.setString(7, "1A");
                ps.setDate(8, java.sql.Date.valueOf("2024-01-01"));
                ps.setString(9, gender);
                ps.setString(10, contact);
                ps.setString(11, "1");
                ps.setString(12, "Street");
                ps.setString(13, "Brgy");
                ps.setString(14, "Town");
                ps.setString(15, "Province");
                return ps.executeUpdate();
            }
        };
    }

    @Test
    void rejectsBadStudentRows() {
        assertRejected(insertStudent("97000000AB", "M", null, "BSIT"));   // not ten digits
        assertRejected(insertStudent("9700000001", "X", null, "BSIT"));   // unknown gender
        assertRejected(insertStudent("9700000002", "M", "12345", "BSIT")); // malformed contact number
        assertRejected(insertStudent("9700000003", "M", null, "NOPE"));   // foreign key: no such course
        String existing = db.query(c -> {
            try (var ps = c.prepareStatement("SELECT MIN(student_number) FROM student"); var rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        });
        assertRejected(insertStudent(existing, "M", null, "BSIT"));       // primary key: duplicate
    }

    private static SqlFunction<Integer> insertItem(String code, String type, String publisher, boolean available, int borrowDays) {
        return c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO item (code, item_type, title, author, publisher, "
                    + "publication_year, genre, lc_class, pages, borrow_days, url, material_type, available) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, code);
                ps.setString(2, type);
                ps.setString(3, "Constraint Probe");
                ps.setString(4, "Nobody");
                ps.setString(5, publisher);
                ps.setInt(6, 2020);
                ps.setString(7, "Guide");
                ps.setString(8, "Class Z");
                ps.setInt(9, 10);
                ps.setInt(10, borrowDays);
                ps.setString(11, "DIGITAL".equals(type) ? "https://library.example.edu/x" : null);
                ps.setString(12, "OTHER".equals(type) ? "THESIS" : null);
                ps.setBoolean(13, available);
                return ps.executeUpdate();
            }
        };
    }

    @Test
    void rejectsItemsThatDoNotMatchTheirKind() {
        assertRejected(insertItem("970000000001", "BOOK", null, true, 30));      // a book needs a publisher
        assertRejected(insertItem("970000000002", "BOOK", "Press", true, 0));    // a book must be lendable
        assertRejected(insertItem("970000000003", "OTHER", null, true, 0));      // reference material is never available
        assertRejected(insertItem("970000000004", "COMIC", "Press", true, 30));  // unknown kind
        assertRejected(insertItem("970000000005", "BOOK", "Press", true, 400));  // loan period out of range
    }

    @Test
    void rejectsBadLoansAndStaff() {
        assertRejected(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO loan (item_code, student_number, issued_by, issued_at, due_at) "
                    + "SELECT (SELECT MIN(code) FROM item), (SELECT MIN(student_number) FROM student), "
                    + "(SELECT MIN(id) FROM staff), ?, ?")) {
                ps.setObject(1, LocalDateTime.of(2026, 5, 10, 9, 0));
                ps.setObject(2, LocalDateTime.of(2026, 5, 1, 9, 0)); // due before issued
                return ps.executeUpdate();
            }
        });
        assertRejected(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO loan (item_code, student_number, issued_by, issued_at, due_at) "
                    + "VALUES ('000000000000', '0000000000', 1, ?, ?)")) {
                ps.setObject(1, LocalDateTime.of(2026, 5, 1, 9, 0));
                ps.setObject(2, LocalDateTime.of(2026, 5, 31, 9, 0)); // foreign keys point nowhere
                return ps.executeUpdate();
            }
        });
        assertRejected(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO staff (first_name, last_name, position, department_code, "
                    + "username, password_hash, role) VALUES ('Dup', 'Licate', 'Librarian', 'CICT', 'admin', 'x', 'LIBRARIAN')")) {
                return ps.executeUpdate(); // username must be unique
            }
        });
        assertRejected(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO staff (first_name, last_name, position, department_code, "
                    + "username, password_hash, role) VALUES ('Bad', 'Role', 'Librarian', 'CICT', 'badrole1', 'x', 'JANITOR')")) {
                return ps.executeUpdate();
            }
        });
    }
}
