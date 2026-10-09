package edu.bulsu.lms.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.bulsu.lms.testsupport.TestData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Pure unit tests of the object model: validation, inheritance and polymorphism. No database needed. */
class DomainModelTest {

    // ---------- Person / Student: encapsulation and validation ----------

    @Test
    @DisplayName("names are trimmed and the middle initial is optional")
    void namesAreTrimmed() {
        Student s = new Student("2019125410", "  Mark ", "", " Santos", "CICT", "BSIT", "3A", Gender.MALE, null,
                new Address("221", "Paraluman St.", "San Mateo", "Baliwag", "Bulacan"));
        assertEquals("Mark", s.getFirstName());
        assertNull(s.getMiddleInitial());
        assertEquals("Mark Santos", s.fullName());
        assertEquals("Santos, Mark", s.listName());
        s.setMiddleInitial("E.");
        assertEquals("Mark E. Santos", s.fullName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "R2D2", "Mark<script>", "1234"})
    void rejectsBadNames(String bad) {
        Student s = TestData.student();
        assertThrows(ValidationException.class, () -> s.setFirstName(bad));
        assertThrows(ValidationException.class, () -> s.setLastName(bad));
    }

    @Test
    void acceptsRealWorldNames() {
        Student s = TestData.student();
        s.setLastName("Dela Cruz");
        s.setFirstName("Mary-Ann");
        s.setLastName("O'Brien");
        s.setFirstName("Mañgalile"); // letters outside ASCII
        assertEquals("Mañgalile", s.getFirstName());
    }

    @Test
    void rejectsOverlongName() {
        Student s = TestData.student();
        assertThrows(ValidationException.class, () -> s.setFirstName("A".repeat(31)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456789", "20191254100", "20191254AB", "", "2019-12541"})
    void studentNumberMustBeTenDigits(String bad) {
        assertThrows(ValidationException.class, () -> Student.requireStudentNumber(bad));
        assertThrows(ValidationException.class, () -> TestData.student().setStudentNumber(bad));
    }

    @Test
    void contactNumberRules() {
        Student s = TestData.student();
        s.setContactNumber("09551233606");
        assertEquals("09551233606", s.getContactNumber());
        s.setContactNumber("  ");
        assertNull(s.getContactNumber(), "blank clears the optional contact number");
        assertThrows(ValidationException.class, () -> s.setContactNumber("0855123360"));
        assertThrows(ValidationException.class, () -> s.setContactNumber("08551233606"));
        assertThrows(ValidationException.class, () -> Student.requireContactNumber(""));
    }

    @Test
    void membershipCannotStartInTheFuture() {
        Student s = TestData.student();
        assertThrows(ValidationException.class, () -> s.setMembershipDate(LocalDate.now().plusDays(1)));
        assertThrows(ValidationException.class, () -> s.setMembershipDate(null));
    }

    @Test
    @DisplayName("the overloaded constructor starts the membership today")
    void overloadedConstructorUsesToday() {
        assertEquals(LocalDate.now(), TestData.student().getMembershipDate());
    }

    @Test
    void addressIsValidatedPart_by_part() {
        assertThrows(ValidationException.class, () -> new Address("", "St", "B", "M", "P"));
        assertThrows(ValidationException.class, () -> new Address("1", "", "B", "M", "P"));
        assertThrows(ValidationException.class, () -> new Address("1", "St", "B", "M", "P".repeat(41)));
        Address a = new Address("221", "Paraluman St.", "San Mateo", "Baliwag", "Bulacan");
        assertEquals("221 Paraluman St.", a.streetLine());
        assertEquals("San Mateo, Baliwag, Bulacan", a.areaLine());
    }

    // ---------- Person subclasses behave differently ----------

    @Test
    void personSubclassesOverrideRoleAndIdentifier() {
        Student student = TestData.student("2019125410");
        Staff librarian = staff(Role.LIBRARIAN);
        List<Person> people = List.of(student, librarian);
        assertEquals(List.of("Student", "Librarian"), people.stream().map(Person::roleLabel).toList());
        assertEquals("2019125410", student.identifier());
        assertEquals("2100150", librarian.identifier());
    }

    // ---------- Staff: lock-out policy ----------

    private static Staff staff(Role role) {
        return new Staff(2100150, "Juan", "D.", "Dela Cruz", "Librarian", "CICT", "jdelacruz", "hash", role, false, 0);
    }

    @Test
    void librarianIsLockedOnTheThirdFailure() {
        Staff s = staff(Role.LIBRARIAN);
        assertFalse(s.recordFailedLogin());
        assertFalse(s.recordFailedLogin());
        assertTrue(s.recordFailedLogin(), "the third wrong password locks the account");
        assertTrue(s.isLocked());
        assertFalse(s.recordFailedLogin(), "already locked: no second 'just locked' signal");
        assertEquals(Staff.MAX_FAILED_ATTEMPTS, s.getFailedAttempts(), "the counter is capped");
    }

    @Test
    void administratorIsNeverLocked() {
        Staff admin = staff(Role.ADMIN);
        for (int i = 0; i < 10; i++) {
            assertFalse(admin.recordFailedLogin());
        }
        assertFalse(admin.isLocked());
    }

    @Test
    void unlockForgivesFailures() {
        Staff s = staff(Role.LIBRARIAN);
        for (int i = 0; i < 3; i++) {
            s.recordFailedLogin();
        }
        s.unlock();
        assertFalse(s.isLocked());
        assertEquals(0, s.getFailedAttempts());
    }

    @Test
    void usernamesAreCaseInsensitiveAndRestricted() {
        assertEquals("juan.d", Staff.requireUsername("  Juan.D "));
        assertThrows(ValidationException.class, () -> Staff.requireUsername("abc"));
        assertThrows(ValidationException.class, () -> Staff.requireUsername("has space"));
        assertThrows(ValidationException.class, () -> Staff.requireUsername("a".repeat(21)));
    }

    // ---------- LibraryItem: inheritance, abstraction, polymorphism ----------

    @Test
    @DisplayName("a List<LibraryItem> is processed without instanceof, and each kind answers differently")
    void polymorphicProcessing() {
        List<LibraryItem> catalog = List.of(TestData.book(), TestData.digitalBook(), TestData.thesis());

        for (LibraryItem item : catalog) {
            assertEquals(item.type().columns().size(), item.tableRow().size(),
                    item.type() + " row must line up with its column headers");
            assertFalse(item.lines().isEmpty());
            assertFalse(item.title().isBlank());
        }
        assertEquals(List.of("ISBN", "EPUB ID", "Barcode"), catalog.stream().map(LibraryItem::getCodeLabel).toList());
        assertEquals(List.of(true, true, false), catalog.stream().map(LibraryItem::isBorrowable).toList());
        assertEquals(List.of(true, false, false), catalog.stream().map(LibraryItem::consumesCopyOnLoan).toList());
        assertEquals(List.of(ItemType.BOOK, ItemType.DIGITAL, ItemType.OTHER), catalog.stream().map(LibraryItem::type).toList());
    }

    @Test
    void printedBookIsLentOnceUntilReturned() {
        PrintedBook book = TestData.book();
        assertTrue(book.canBeBorrowedNow());
        book.markLent();
        assertFalse(book.isAvailable());
        BusinessRuleException refused = assertThrows(BusinessRuleException.class, book::markLent);
        assertTrue(refused.getMessage().contains("currently borrowed"));
        book.markReturned();
        assertTrue(book.isAvailable());
        assertTrue(book.canBeBorrowedNow());
    }

    @Test
    void digitalBookCanBeLentRepeatedly() {
        DigitalBook ebook = TestData.digitalBook();
        ebook.markLent();
        ebook.markLent();
        assertTrue(ebook.isAvailable(), "lending a digital copy never removes it");
        assertTrue(ebook.canBeBorrowedNow());
    }

    @Test
    void otherMaterialIsReferenceOnly() {
        OtherMaterial thesis = TestData.thesis();
        assertFalse(thesis.canBeBorrowedNow());
        BusinessRuleException refused = assertThrows(BusinessRuleException.class, thesis::markLent);
        assertTrue(refused.getMessage().contains("reference only"));
    }

    @Test
    void dueDateIsTheEndOfTheLastDayOfTheLoan() {
        PrintedBook book = TestData.book(); // 30 days
        LocalDateTime issued = LocalDateTime.of(2026, 1, 1, 9, 0);
        assertEquals(LocalDateTime.of(2026, 1, 31, 23, 59, 59), book.dueDateFrom(issued),
                "the screens show only the date, so overdue must not begin at the hour the book was issued");
        assertEquals(book.dueDateFrom(issued), book.dueDateFrom(issued.withHour(23)), "same day, same deadline");
    }

    @Test
    void textWithControlOrDirectionOverrideCharactersIsRejected() {
        assertThrows(ValidationException.class, () -> Check.text("Hello\u0007World", "Title", 50));
        assertThrows(ValidationException.class, () -> Check.text("evil‮txt.exe", "Title", 50));
        assertEquals("Mañgalile", Check.text("Mañgalile", "Title", 50));
    }

    @Test
    void digitalBookUrlCannotCarryCredentials() {
        assertThrows(ValidationException.class, () -> DigitalBook.requireUrl("https://user:secret@library.example.edu/a.epub"));
        assertEquals("https://library.example.edu/a.epub", DigitalBook.requireUrl("https://library.example.edu/a.epub"));
    }

    @Test
    void authorIsRequiredForBooksButNotForOtherMaterial() {
        assertThrows(ValidationException.class, () -> new PrintedBook(TestData.itemCode(), "T", null, "P", 2020, "G", "L", 10, 30));
        assertThrows(ValidationException.class, () -> new DigitalBook(TestData.itemCode(), "T", " ", 2020, "G", 10, 30, "https://x.example/a"));
        assertNull(TestData.thesis().getAuthor());
        assertEquals("N/A", TestData.thesis().subtitle());
    }

    @Test
    void itemFieldRules() {
        PrintedBook book = TestData.book();
        assertThrows(ValidationException.class, () -> book.setCode("12345"));          // too short
        assertThrows(ValidationException.class, () -> book.setCode("97812ABC4567"));   // not digits
        assertThrows(ValidationException.class, () -> book.setPages(0));
        assertThrows(ValidationException.class, () -> book.setBorrowDays(0));          // books must be lendable
        assertThrows(ValidationException.class, () -> book.setBorrowDays(366));
        assertThrows(ValidationException.class, () -> book.setPublicationYear(999));
        assertThrows(ValidationException.class, () -> book.setPublicationYear(LocalDate.now().getYear() + 2));
        assertThrows(ValidationException.class, () -> book.setTitle(" "));
        assertThrows(ValidationException.class, () -> book.setGenre(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://x.example/a.epub", "not a url", "javascript:alert(1)", "https://", ""})
    void digitalBookUrlMustBeAWebAddress(String bad) {
        assertThrows(ValidationException.class, () -> DigitalBook.requireUrl(bad));
    }

    @Test
    void itemKindsShowDifferentDetails() {
        assertTrue(TestData.book().lines().stream().anyMatch(l -> l.contains("days - Available")));
        assertTrue(TestData.digitalBook().lines().stream().anyMatch(l -> l.startsWith("https://")));
        assertTrue(TestData.thesis().lines().contains("Reference only"));
        assertInstanceOf(PrintedBook.class, TestData.book());
    }

    // ---------- Loan ----------

    @Test
    void loanKnowsWhenItIsOverdue() {
        LocalDateTime issued = LocalDateTime.of(2026, 1, 1, 9, 0);
        Loan open = loan(issued, issued.plusDays(30), null);
        assertFalse(open.isOverdue(issued.plusDays(30)));
        assertTrue(open.isOverdue(issued.plusDays(31)));
        Loan returned = loan(issued, issued.plusDays(30), issued.plusDays(40));
        assertFalse(returned.isOverdue(issued.plusDays(99)), "a returned loan is never overdue");
        assertTrue(returned.isReturned());
        assertTrue(open.lines().contains("Not Returned Yet"));
    }

    @Test
    void loanRejectsImpossibleDates() {
        LocalDateTime issued = LocalDateTime.of(2026, 1, 10, 9, 0);
        assertThrows(ValidationException.class, () -> loan(issued, issued.minusDays(1), null));
        assertThrows(ValidationException.class, () -> loan(issued, issued.plusDays(5), issued.minusDays(1)));
    }

    private static Loan loan(LocalDateTime issued, LocalDateTime due, LocalDateTime returned) {
        return new Loan(1000, "9781118977811", "A Practical Introduction", "2019125410", "Mark E. Santos", "BSIT",
                2100102, "Juan D. Dela Cruz", issued, due, returned);
    }

    // ---------- Validation helpers ----------

    @Test
    void checkHelpers() {
        assertEquals("abc", Check.text("  abc ", "Field", 5));
        assertThrows(ValidationException.class, () -> Check.text("abcdef", "Field", 5));
        assertNull(Check.optional("   ", "Field", 5));
        assertEquals(42, Check.integer(" 42 ", "Pages"));
        assertThrows(ValidationException.class, () -> Check.integer("4x2", "Pages"));
        assertThrows(ValidationException.class, () -> Check.range(11, "Pages", 1, 10));
    }
}
