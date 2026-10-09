package edu.bulsu.lms.testsupport;

import edu.bulsu.lms.domain.Address;
import edu.bulsu.lms.domain.DigitalBook;
import edu.bulsu.lms.domain.Gender;
import edu.bulsu.lms.domain.MaterialType;
import edu.bulsu.lms.domain.OtherMaterial;
import edu.bulsu.lms.domain.PrintedBook;
import edu.bulsu.lms.domain.Role;
import edu.bulsu.lms.domain.Staff;
import edu.bulsu.lms.domain.Student;
import java.util.concurrent.ThreadLocalRandom;

/** Builders for valid, unique, clearly fake test records. */
public final class TestData {

    private TestData() {
    }

    /** A random 10-digit student number that starts with 98 so it cannot clash with the seed data. */
    public static String studentNumber() {
        return "98" + digits(8);
    }

    /** A random 12-digit catalog code that starts with 98. */
    public static String itemCode() {
        return "98" + digits(10);
    }

    /** The seeded administrator (id 2100101) as the "who is asking" argument of administrator-only services. */
    public static Staff admin() {
        return new Staff(2100101, "System", null, "Administrator", "Library Administrator", "UWIDE", "admin", "unused-hash",
                Role.ADMIN, false, 0);
    }

    public static String username() {
        return "t" + digits(8);
    }

    public static Student student() {
        return student(studentNumber());
    }

    public static Student student(String number) {
        return new Student(number, "Test", "Q.", "Student", "CICT", "BSIT", "3A", Gender.FEMALE, "09" + digits(9),
                new Address("12", "Sample St.", "Poblacion", "Malolos", "Bulacan"));
    }

    public static PrintedBook book() {
        return new PrintedBook(itemCode(), "Test-Driven Learning " + digits(4), "Ada Tester", "Sample Press", 2020,
                "Computer Science", "Class Q – Science", 300, 30);
    }

    public static DigitalBook digitalBook() {
        String code = itemCode();
        return new DigitalBook(code, "Test E-Book " + digits(4), "Ada Tester", 2021, "Science", 150, 15,
                "https://library.example.edu/ebooks/" + code + ".epub");
    }

    public static OtherMaterial thesis() {
        return new OtherMaterial(itemCode(), "Test Thesis " + digits(4), null, null, 2019, 120, MaterialType.THESIS);
    }

    private static String digits(int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(ThreadLocalRandom.current().nextInt(10));
        }
        return builder.toString();
    }
}
