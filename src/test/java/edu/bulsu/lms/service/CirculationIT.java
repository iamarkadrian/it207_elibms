package edu.bulsu.lms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.bulsu.lms.domain.BusinessRuleException;
import edu.bulsu.lms.domain.DigitalBook;
import edu.bulsu.lms.domain.ItemType;
import edu.bulsu.lms.domain.LibraryItem;
import edu.bulsu.lms.domain.Loan;
import edu.bulsu.lms.domain.OtherMaterial;
import edu.bulsu.lms.domain.PrintedBook;
import edu.bulsu.lms.domain.Staff;
import edu.bulsu.lms.domain.Student;
import edu.bulsu.lms.persistence.Database;
import edu.bulsu.lms.security.PasswordHasher;
import edu.bulsu.lms.testsupport.TestClock;
import edu.bulsu.lms.testsupport.TestData;
import edu.bulsu.lms.testsupport.TestDatabase;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Lending and returning against the real database, including a genuine race between borrowers. */
class CirculationIT {

    private static final PasswordHasher FAST = new PasswordHasher(2_000);

    private static Database db;
    private static Services services;
    private static Staff librarian;

    @BeforeAll
    static void connect() {
        db = TestDatabase.require();
        services = Services.create(db, Clock.systemDefaultZone(), FAST);
        librarian = ((LoginResult.Success) services.auth().login("librarian", "Library@2026".toCharArray())).staff();
    }

    private static PrintedBook newBook() {
        PrintedBook book = TestData.book();
        services.catalog().add(book);
        return book;
    }

    private static Student newMember() {
        Student student = TestData.student();
        services.members().register(student);
        return student;
    }

    private static boolean isAvailable(String code, ItemType type) {
        return services.catalog().items(type).stream().filter(i -> i.getCode().equals(code)).findFirst().orElseThrow().isAvailable();
    }

    @Test
    @DisplayName("issue then return: the loan and the book's availability move together")
    void issueAndReturn() {
        PrintedBook book = newBook();
        Student student = newMember();

        Loan loan = services.circulation().issue(book.getCode(), student.getStudentNumber(), librarian);
        assertNull(loan.returnedAt());
        assertEquals(librarian.getId(), loan.issuedById());
        assertEquals(student.fullName(), loan.studentName());
        assertEquals(book.getTitle(), loan.itemTitle());
        assertEquals(loan.issuedAt().toLocalDate().plusDays(30).atTime(23, 59, 59), loan.dueAt(),
                "due at the end of (issue date + the book's borrow days)");
        assertFalse(isAvailable(book.getCode(), ItemType.BOOK));
        assertTrue(services.circulation().loans(LoanView.ISSUED).stream().anyMatch(l -> l.id() == loan.id()));
        assertTrue(services.circulation().loans(LoanView.RETURNED).stream().noneMatch(l -> l.id() == loan.id()));

        Loan returned = services.circulation().giveBack(loan.id());
        assertNotNull(returned.returnedAt());
        assertTrue(isAvailable(book.getCode(), ItemType.BOOK));
        assertTrue(services.circulation().loans(LoanView.ISSUED).stream().noneMatch(l -> l.id() == loan.id()));
        assertTrue(services.circulation().loans(LoanView.RETURNED).stream().anyMatch(l -> l.id() == loan.id()));
    }

    @Test
    void aBookThatIsOutCannotBeIssuedAgain() {
        PrintedBook book = newBook();
        Student first = newMember();
        Student second = newMember();
        services.circulation().issue(book.getCode(), first.getStudentNumber(), librarian);

        BusinessRuleException refused = assertThrows(BusinessRuleException.class,
                () -> services.circulation().issue(book.getCode(), second.getStudentNumber(), librarian));
        assertTrue(refused.getMessage().contains("currently borrowed"));
    }

    @Test
    void returningTwiceIsRefused() {
        Loan loan = services.circulation().issue(newBook().getCode(), newMember().getStudentNumber(), librarian);
        services.circulation().giveBack(loan.id());
        assertThrows(BusinessRuleException.class, () -> services.circulation().giveBack(loan.id()));
        assertThrows(BusinessRuleException.class, () -> services.circulation().giveBack(999_999_999));
    }

    @Test
    void aBookCanBeBorrowedAgainAfterItIsReturned() {
        PrintedBook book = newBook();
        Student student = newMember();
        Loan first = services.circulation().issue(book.getCode(), student.getStudentNumber(), librarian);
        services.circulation().giveBack(first.id());
        Loan second = services.circulation().issue(book.getCode(), student.getStudentNumber(), librarian);
        assertTrue(second.id() > first.id());
    }

    @Test
    @DisplayName("digital books never run out: many members can borrow the same one")
    void digitalBooksAreAlwaysAvailable() {
        DigitalBook ebook = TestData.digitalBook();
        services.catalog().add(ebook);
        Loan one = services.circulation().issue(ebook.getCode(), newMember().getStudentNumber(), librarian);
        Loan two = services.circulation().issue(ebook.getCode(), newMember().getStudentNumber(), librarian);
        assertTrue(isAvailable(ebook.getCode(), ItemType.DIGITAL));
        assertEquals(ebook.getBorrowDays(), Duration.between(one.issuedAt(), one.dueAt()).toDays());

        services.circulation().giveBack(one.id());
        services.circulation().giveBack(two.id());
        assertTrue(isAvailable(ebook.getCode(), ItemType.DIGITAL));
    }

    @Test
    void referenceMaterialCannotBeBorrowed() {
        OtherMaterial thesis = TestData.thesis();
        services.catalog().add(thesis);
        BusinessRuleException refused = assertThrows(BusinessRuleException.class,
                () -> services.circulation().issue(thesis.getCode(), newMember().getStudentNumber(), librarian));
        assertTrue(refused.getMessage().contains("reference only"));
    }

    @Test
    @DisplayName("a librarian who was locked after signing in can no longer lend")
    void issuerIsCheckedAgainstTheDatabase() {
        String username = TestData.username();
        int id = services.staff().addLibrarian(TestData.admin(), "Late", null, "Lock", "Librarian", "CICT", username,
                "Passw0rd-ok".toCharArray());
        Staff signedIn = ((LoginResult.Success) services.auth().login(username, "Passw0rd-ok".toCharArray())).staff();
        PrintedBook book = newBook();
        Student member = newMember();

        // Meanwhile their account gets locked (three wrong passwords elsewhere).
        for (int i = 0; i < 3; i++) {
            services.auth().login(username, ("nope-" + i + "-x").toCharArray());
        }
        BusinessRuleException refused = assertThrows(BusinessRuleException.class,
                () -> services.circulation().issue(book.getCode(), member.getStudentNumber(), signedIn));
        assertTrue(refused.getMessage().contains("can no longer lend"));
        assertTrue(isAvailable(book.getCode(), ItemType.BOOK), "nothing was lent");

        services.staff().unlock(TestData.admin(), id);
        services.circulation().issue(book.getCode(), member.getStudentNumber(), signedIn); // fine again
    }

    @Test
    void unknownItemOrMemberIsRefused() {
        PrintedBook book = newBook();
        assertThrows(BusinessRuleException.class,
                () -> services.circulation().issue(book.getCode(), TestData.studentNumber(), librarian));
        assertThrows(BusinessRuleException.class,
                () -> services.circulation().issue(TestData.itemCode(), newMember().getStudentNumber(), librarian));
        assertTrue(isAvailable(book.getCode(), ItemType.BOOK), "a refused loan leaves the book untouched");
    }

    @Test
    @DisplayName("a loan past its due date shows up as Missing, and only then")
    void overdueLoansAreMissing() {
        Loan loan = services.circulation().issue(newBook().getCode(), newMember().getStudentNumber(), librarian);
        assertTrue(services.circulation().loans(LoanView.MISSING).stream().noneMatch(l -> l.id() == loan.id()));

        TestClock later = new TestClock();
        later.advance(Duration.ofDays(31));
        Services inTheFuture = Services.create(db, later, FAST);
        assertTrue(inTheFuture.circulation().loans(LoanView.MISSING).stream().anyMatch(l -> l.id() == loan.id()));
        assertTrue(inTheFuture.circulation().loans(LoanView.MISSING).stream().allMatch(l -> l.isOverdue(LocalDateTime.now().plusDays(31))));

        services.circulation().giveBack(loan.id());
        assertTrue(inTheFuture.circulation().loans(LoanView.MISSING).stream().noneMatch(l -> l.id() == loan.id()),
                "returning the book clears it from Missing");
    }

    @Test
    @DisplayName("eight librarians race for the last copy: exactly one wins")
    void concurrentBorrowersCannotDoubleBookACopy() throws Exception {
        PrintedBook book = newBook();
        List<Student> members = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            members.add(newMember());
        }

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger won = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();
        List<Throwable> unexpected = java.util.Collections.synchronizedList(new ArrayList<>());
        for (Student member : members) {
            pool.submit(() -> {
                try {
                    start.await();
                    services.circulation().issue(book.getCode(), member.getStudentNumber(), librarian);
                    won.incrementAndGet();
                } catch (BusinessRuleException e) {
                    refused.incrementAndGet();
                } catch (Throwable t) {
                    unexpected.add(t);
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS));

        assertTrue(unexpected.isEmpty(), "unexpected failures: " + unexpected);
        assertEquals(1, won.get(), "only one borrower may get the single copy");
        assertEquals(7, refused.get());
        long openLoansForBook = services.circulation().loans(LoanView.ISSUED).stream().filter(l -> l.itemCode().equals(book.getCode())).count();
        assertEquals(1, openLoansForBook);
        assertFalse(isAvailable(book.getCode(), ItemType.BOOK));
    }

    @Test
    void dashboardNumbersMatchTheLists() {
        newMember();
        services.circulation().issue(newBook().getCode(), newMember().getStudentNumber(), librarian);

        LibrarySummary summary = services.dashboard().summary();
        assertEquals(services.members().count(), summary.members());
        assertEquals(services.catalog().count(ItemType.BOOK), summary.books());
        assertEquals(services.catalog().count(ItemType.DIGITAL), summary.digitalBooks());
        assertEquals(services.catalog().count(ItemType.OTHER), summary.otherDetails());
        assertEquals(services.circulation().loans(LoanView.ISSUED).size(), summary.issued());
        assertEquals(services.circulation().loans(LoanView.RETURNED).size(), summary.returned());
        assertEquals(services.circulation().loans(LoanView.MISSING).size(), summary.missing());
        assertTrue(summary.missing() <= summary.issued());
    }

    @Test
    void itemsComeBackPolymorphically() {
        List<LibraryItem> mixed = new ArrayList<>();
        for (ItemType type : ItemType.values()) {
            mixed.addAll(services.catalog().items(type));
        }
        assertTrue(mixed.stream().anyMatch(i -> i instanceof PrintedBook));
        assertTrue(mixed.stream().anyMatch(i -> i instanceof DigitalBook));
        assertTrue(mixed.stream().anyMatch(i -> i instanceof OtherMaterial));
        assertTrue(mixed.stream().noneMatch(i -> i.tableRow().size() != i.type().columns().size()));
    }
}
