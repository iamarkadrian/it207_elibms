package edu.bulsu.lms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.bulsu.lms.domain.BusinessRuleException;
import edu.bulsu.lms.domain.DigitalBook;
import edu.bulsu.lms.domain.ItemType;
import edu.bulsu.lms.domain.LibraryItem;
import edu.bulsu.lms.domain.Loan;
import edu.bulsu.lms.domain.MaterialType;
import edu.bulsu.lms.domain.OtherMaterial;
import edu.bulsu.lms.domain.PrintedBook;
import edu.bulsu.lms.domain.Student;
import edu.bulsu.lms.domain.ValidationException;
import edu.bulsu.lms.security.PasswordHasher;
import edu.bulsu.lms.testsupport.TestClock;
import edu.bulsu.lms.testsupport.TestData;
import edu.bulsu.lms.testsupport.TestDatabase;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Create / read / delete for members, and create / read for every kind of catalog entry. */
class MemberCatalogIT {

    private static Services services;

    @BeforeAll
    static void connect() {
        services = Services.create(TestDatabase.require(), new TestClock(), new PasswordHasher(2_000));
    }

    // ---------- members ----------

    @Test
    void registeredMemberPersistsWithEveryField() {
        int before = services.members().count();
        Student student = TestData.student();
        services.members().register(student);

        assertEquals(before + 1, services.members().count());
        Student loaded = services.members().members().stream()
                .filter(s -> s.getStudentNumber().equals(student.getStudentNumber())).findFirst().orElseThrow();
        assertEquals(student.fullName(), loaded.fullName());
        assertEquals(student.getContactNumber(), loaded.getContactNumber());
        assertEquals(student.getAddress(), loaded.getAddress());
        assertEquals(student.getGender(), loaded.getGender());
        assertEquals(student.getMembershipDate(), loaded.getMembershipDate());
        assertEquals("CICT", loaded.getDepartmentCode());
        assertEquals("BSIT", loaded.getCourseCode());
    }

    @Test
    void duplicateStudentNumberIsRefused() {
        Student student = TestData.student();
        services.members().register(student);
        assertThrows(BusinessRuleException.class, () -> services.members().register(TestData.student(student.getStudentNumber())));
        assertThrows(BusinessRuleException.class, () -> services.members().ensureNumberIsFree(student.getStudentNumber()));
        services.members().ensureNumberIsFree(TestData.studentNumber()); // a fresh number is fine
        assertThrows(ValidationException.class, () -> services.members().ensureNumberIsFree("12AB"));
    }

    @Test
    void memberWithoutHistoryCanBeRemoved() {
        Student student = TestData.student();
        services.members().register(student);
        services.members().remove(student.getStudentNumber());
        assertTrue(services.members().members().stream().noneMatch(s -> s.getStudentNumber().equals(student.getStudentNumber())));
        assertThrows(BusinessRuleException.class, () -> services.members().remove(student.getStudentNumber()));
    }

    @Test
    void memberWithBorrowingHistoryIsKept() {
        Loan someLoan = services.circulation().loans(LoanView.RETURNED).get(0);
        BusinessRuleException refused = assertThrows(BusinessRuleException.class,
                () -> services.members().remove(someLoan.studentNumber()));
        assertTrue(refused.getMessage().contains("borrowing records"));
    }

    // ---------- catalog ----------

    @Test
    @DisplayName("each kind round-trips through the database as the right subclass with every field intact")
    void everyKindRoundTrips() {
        PrintedBook book = TestData.book();
        DigitalBook ebook = TestData.digitalBook();
        OtherMaterial thesis = TestData.thesis();
        int books = services.catalog().count(ItemType.BOOK);
        int ebooks = services.catalog().count(ItemType.DIGITAL);
        int others = services.catalog().count(ItemType.OTHER);

        services.catalog().add(book);
        services.catalog().add(ebook);
        services.catalog().add(thesis);

        assertEquals(books + 1, services.catalog().count(ItemType.BOOK));
        assertEquals(ebooks + 1, services.catalog().count(ItemType.DIGITAL));
        assertEquals(others + 1, services.catalog().count(ItemType.OTHER));

        PrintedBook loadedBook = assertInstanceOf(PrintedBook.class, find(ItemType.BOOK, book.getCode()));
        assertEquals(book.getTitle(), loadedBook.getTitle());
        assertEquals(book.getPublisher(), loadedBook.getPublisher());
        assertEquals(book.getLcClass(), loadedBook.getLcClass());
        assertEquals(book.getBorrowDays(), loadedBook.getBorrowDays());
        assertTrue(loadedBook.isAvailable());

        DigitalBook loadedEbook = assertInstanceOf(DigitalBook.class, find(ItemType.DIGITAL, ebook.getCode()));
        assertEquals(ebook.getUrl(), loadedEbook.getUrl());
        assertTrue(loadedEbook.isAvailable());

        OtherMaterial loadedThesis = assertInstanceOf(OtherMaterial.class, find(ItemType.OTHER, thesis.getCode()));
        assertEquals(MaterialType.THESIS, loadedThesis.getMaterialType());
        assertEquals(null, loadedThesis.getAuthor());
        assertEquals(null, loadedThesis.getPublisher());
    }

    @Test
    void duplicateCodeIsRefusedAcrossKinds() {
        PrintedBook book = TestData.book();
        services.catalog().add(book);
        assertThrows(BusinessRuleException.class, () -> services.catalog().add(book));
        // The same number as a digital book is a clash too: all kinds share one code space.
        DigitalBook clash = new DigitalBook(book.getCode(), "Clash", "Someone", 2020, "Science", 10, 15, "https://library.example.edu/x");
        assertThrows(BusinessRuleException.class, () -> services.catalog().add(clash));
        assertThrows(BusinessRuleException.class, () -> services.catalog().ensureCodeIsFree(book.getCode(), "ISBN"));
        services.catalog().ensureCodeIsFree(TestData.itemCode(), "ISBN");
    }

    @Test
    void unicodeSurvivesTheRoundTrip() {
        PrintedBook book = new PrintedBook(TestData.itemCode(), "Mañgalile: Ang Aklat ng Pag-asa – Unang Bahagi", "José Rizal Ñ.",
                "Anvil Publishing", 2018, "Literature", "Class P – Language and Literature", 200, 30);
        services.catalog().add(book);
        LibraryItem loaded = find(ItemType.BOOK, book.getCode());
        assertEquals(book.getTitle(), loaded.getTitle());
        assertEquals(book.getAuthor(), loaded.getAuthor());
        assertEquals(book.getLcClass(), ((PrintedBook) loaded).getLcClass());
    }

    private static LibraryItem find(ItemType type, String code) {
        LibraryItem found = services.catalog().items(type).stream().filter(i -> i.getCode().equals(code)).findFirst().orElse(null);
        assertNotNull(found, code + " should be in the catalog");
        return found;
    }

    @Test
    void referenceDataIsAvailableForTheForms() {
        assertTrue(services.reference().departments().size() >= 23);
        List<String> cictCourses = services.reference().coursesOf("CICT").stream().map(c -> c.code()).toList();
        assertTrue(cictCourses.contains("BSIT"));
        assertTrue(services.reference().coursesOf("NOPE").isEmpty());
        assertTrue(services.reference().departments().stream().anyMatch(d -> d.code().equals("CAFA")
                && d.name().equals("College of Architecture and Fine Arts")),
                "CAFA is CAFA (the legacy code mapped it to CAL by mistake)");
    }
}
