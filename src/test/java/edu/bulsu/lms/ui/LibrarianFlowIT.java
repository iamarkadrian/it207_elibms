package edu.bulsu.lms.ui;

import static edu.bulsu.lms.testsupport.SwingTestKit.choose;
import static edu.bulsu.lms.testsupport.SwingTestKit.click;
import static edu.bulsu.lms.testsupport.SwingTestKit.exists;
import static edu.bulsu.lms.testsupport.SwingTestKit.find;
import static edu.bulsu.lms.testsupport.SwingTestKit.rowCount;
import static edu.bulsu.lms.testsupport.SwingTestKit.selectRow;
import static edu.bulsu.lms.testsupport.SwingTestKit.settle;
import static edu.bulsu.lms.testsupport.SwingTestKit.text;
import static edu.bulsu.lms.testsupport.SwingTestKit.type;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.bulsu.lms.domain.DigitalBook;
import edu.bulsu.lms.domain.ItemType;
import edu.bulsu.lms.domain.LibraryItem;
import edu.bulsu.lms.domain.Loan;
import edu.bulsu.lms.domain.PrintedBook;
import edu.bulsu.lms.domain.Student;
import edu.bulsu.lms.persistence.Database;
import edu.bulsu.lms.security.PasswordHasher;
import edu.bulsu.lms.service.LoanView;
import edu.bulsu.lms.service.Services;
import edu.bulsu.lms.testsupport.GuiHarness;
import edu.bulsu.lms.testsupport.TestClock;
import edu.bulsu.lms.testsupport.TestData;
import edu.bulsu.lms.testsupport.TestDatabase;
import edu.bulsu.lms.ui.kit.DetailCard;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import javax.swing.JButton;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The librarian workspace end to end: every wizard, every button, against the real database. */
class LibrarianFlowIT {

    private static Database db;
    private GuiHarness gui;

    @BeforeAll
    static void connect() {
        db = TestDatabase.require();
    }

    @BeforeEach
    void signInAsLibrarian() {
        gui = new GuiHarness(db);
        gui.signIn("librarian", "Library@2026");
    }

    private void open(String nav) {
        click(gui.shell(), "nav." + nav);
        settle();
    }

    private void next() {
        click(gui.shell(), "primary");
        settle();
    }

    private static String detailLines(GuiHarness gui) {
        return String.join("\n", find(gui.shell(), "detail", DetailCard.class).lineTexts());
    }

    private static String reviewLines(GuiHarness gui) {
        return String.join("\n", find(gui.shell(), "review", DetailCard.class).lineTexts());
    }

    // ---------- dashboard & navigation ----------

    @Test
    void dashboardShowsTheNumbersInTheDatabase() {
        var summary = gui.services().dashboard().summary();
        assertEquals(String.format("%,d", summary.books()), text(gui.shell(), "count.books"));
        assertEquals(String.format("%,d", summary.digitalBooks()), text(gui.shell(), "count.digital"));
        assertEquals(String.format("%,d", summary.otherDetails()), text(gui.shell(), "count.others"));
        assertEquals(String.format("%,d", summary.members()), text(gui.shell(), "count.members"));
        assertEquals(String.format("%,d", summary.issued()), text(gui.shell(), "count.issued"));
        assertEquals(String.format("%,d", summary.returned()), text(gui.shell(), "count.returned"));
        assertEquals(String.format("%,d", summary.missing()), text(gui.shell(), "count.missing"));
        assertTrue(text(gui.shell(), "greeting").contains("Juan"));
    }

    @Test
    @DisplayName("every table shows its column headings")
    void tablesHaveHeaders() {
        var expected = java.util.Map.of(
                "members", List.of("Student Number", "Name", "Course", "Section"),
                "books", ItemType.BOOK.columns(),
                "ebooks", ItemType.DIGITAL.columns(),
                "others", ItemType.OTHER.columns(),
                "issued", List.of("Issue No", "Item Code", "Title", "Student Name", "Course", "Issued", "Due"));
        expected.forEach((screen, headings) -> {
            open(screen);
            javax.swing.JTable table = find(gui.shell(), screen + ".table", javax.swing.JTable.class);
            assertTrue(table.getTableHeader().isVisible() && table.getParent().getParent() instanceof javax.swing.JScrollPane scroll
                    && scroll.getColumnHeader() != null && scroll.getColumnHeader().getView() == table.getTableHeader(),
                    screen + " has its header installed in the scroll pane");
            List<String> shown = new java.util.ArrayList<>();
            for (int i = 0; i < table.getColumnCount(); i++) {
                shown.add(table.getColumnName(i));
            }
            assertEquals(headings, shown, screen);
        });
    }

    @Test
    void everyNavigationItemOpensItsScreenWithRows() {
        for (String screen : List.of("members", "books", "ebooks", "others", "issued", "returned", "missing")) {
            open(screen);
            assertTrue(exists(gui.shell(), "screen." + screen), screen + " screen is shown");
            assertTrue(rowCount(gui.shell(), screen + ".table") > 0, screen + " has rows from the database");
        }
        open("dashboard");
        assertTrue(exists(gui.shell(), "screen.dashboard"));
        assertFalse(exists(gui.shell(), "screen.missing"), "only one screen at a time");
    }

    // ---------- members ----------

    private void fillMemberWizard(String number, String first, String last) {
        type(gui.shell(), "studentNumber", number);
        type(gui.shell(), "firstName", first);
        type(gui.shell(), "middleInitial", "Q.");
        type(gui.shell(), "lastName", last);
        next();
        choose(gui.shell(), "college", "Information and Communications");
        choose(gui.shell(), "course", "Information Technology");
        type(gui.shell(), "section", "3B");
        next();
        choose(gui.shell(), "gender", "Female");
        type(gui.shell(), "contact", "09171234567");
        next();
        type(gui.shell(), "houseNo", "12");
        type(gui.shell(), "street", "Rizal St.");
        type(gui.shell(), "barangay", "Poblacion");
        type(gui.shell(), "municipality", "Malolos");
        type(gui.shell(), "province", "Bulacan");
        next();
    }

    @Test
    @DisplayName("members: add through the five-step wizard, look it up, and remove it")
    void memberLifecycle() {
        open("members");
        int before = rowCount(gui.shell(), "members.table");
        String number = TestData.studentNumber();

        click(gui.shell(), "add");
        fillMemberWizard(number, "Wiz", "Ardtest");

        String review = reviewLines(gui);
        assertTrue(review.contains(number) && review.contains("BSIT 3B") && review.contains("CICT"), review);
        assertTrue(text(gui.shell(), "title").contains("Wiz"), "the review card's heading is the member's first name");

        click(gui.shell(), "primary"); // CONFIRM
        settle();
        assertEquals("Member added successfully", gui.messenger().lastInfo());
        assertEquals(before + 1, rowCount(gui.shell(), "members.table"));
        assertTrue(gui.services().members().members().stream().anyMatch(s -> s.getStudentNumber().equals(number)));

        selectRow(gui.shell(), "members.table", number);
        assertTrue(detailLines(gui).contains("Poblacion, Malolos, Bulacan"));

        gui.messenger().answerConfirm(true);
        click(gui.shell(), "primary"); // REMOVE
        settle();
        assertEquals("Member removed successfully", gui.messenger().lastInfo());
        assertEquals(before, rowCount(gui.shell(), "members.table"));
    }

    @Test
    void memberWizardCatchesMistakesOnTheStepWhereTheyHappen() {
        open("members");
        click(gui.shell(), "add");

        type(gui.shell(), "studentNumber", "12345");
        type(gui.shell(), "firstName", "Mia");
        type(gui.shell(), "lastName", "Tester");
        next();
        assertTrue(gui.messenger().lastWarning().contains("Student number must be exactly 10 digits"), gui.messenger().lastWarning());
        assertTrue(exists(gui.shell(), "studentNumber"), "still on step one");

        Student existing = gui.services().members().members().get(0);
        type(gui.shell(), "studentNumber", existing.getStudentNumber());
        next();
        assertEquals("The Student ID has already registered.", gui.messenger().lastWarning());

        type(gui.shell(), "studentNumber", TestData.studentNumber());
        type(gui.shell(), "firstName", "R2D2");
        next();
        assertTrue(gui.messenger().lastWarning().contains("First name may only contain letters"), gui.messenger().lastWarning());
    }

    @Test
    void memberWizardRejectsABadContactNumberAndKeepsEarlierAnswers() {
        open("members");
        click(gui.shell(), "add");
        type(gui.shell(), "studentNumber", TestData.studentNumber());
        type(gui.shell(), "firstName", "Cora");
        type(gui.shell(), "lastName", "Contact");
        next();
        type(gui.shell(), "section", "2A");
        next();
        type(gui.shell(), "contact", "0912");
        next();
        assertTrue(gui.messenger().lastWarning().contains("Contact number must be 11 digits"), gui.messenger().lastWarning());
        assertTrue(exists(gui.shell(), "contact"));
    }

    @Test
    void cancellingTheWizardAsksFirstWhenSomethingWasTyped() {
        open("members");
        click(gui.shell(), "add");
        click(gui.shell(), "secondary");
        assertTrue(gui.messenger().questions().isEmpty(), "an untouched wizard closes without asking");
        assertTrue(exists(gui.shell(), "add"));

        click(gui.shell(), "add");
        type(gui.shell(), "firstName", "Half");
        gui.messenger().answerConfirm(false);
        click(gui.shell(), "secondary");
        assertEquals(1, gui.messenger().questions().size());
        assertEquals("Half", text(gui.shell(), "firstName"), "No keeps the wizard open");

        gui.messenger().answerConfirm(true);
        click(gui.shell(), "secondary");
        assertTrue(exists(gui.shell(), "add"), "Yes closes it");
        click(gui.shell(), "add");
        assertEquals("", text(gui.shell(), "firstName"), "and the answers are gone");
    }

    @Test
    void editGoesBackToStepOneWithoutLosingAnswers() {
        open("members");
        click(gui.shell(), "add");
        String number = TestData.studentNumber();
        fillMemberWizard(number, "Back", "Again");
        click(gui.shell(), "secondary"); // EDIT
        assertEquals(number, text(gui.shell(), "studentNumber"));
        assertEquals("Back", text(gui.shell(), "firstName"));
    }

    @Test
    void aMemberWithBorrowingHistoryCannotBeRemoved() {
        Loan loan = gui.services().circulation().loans(LoanView.RETURNED).get(0);
        open("members");
        selectRow(gui.shell(), "members.table", loan.studentNumber());
        gui.messenger().answerConfirm(true);
        click(gui.shell(), "primary");
        settle();
        assertTrue(gui.messenger().lastWarning().contains("borrowing records"), gui.messenger().lastWarning());
    }

    // ---------- books: add, borrow, return ----------

    private void fillBookWizard(String isbn, String title) {
        type(gui.shell(), "isbn", isbn);
        type(gui.shell(), "title", title);
        type(gui.shell(), "author", "Flow Author");
        next();
        type(gui.shell(), "year", "2021");
        type(gui.shell(), "publisher", "Test Press");
        next();
        choose(gui.shell(), "genre", "Computer Science");
        choose(gui.shell(), "lcClass", "Class Q");
        next();
        type(gui.shell(), "pages", "321");
        type(gui.shell(), "days", "14");
        next();
    }

    @Test
    @DisplayName("books: add, borrow (it becomes unavailable), return from Issued, and it shows in Returned")
    void bookLifecycle() {
        String isbn = TestData.itemCode();
        String title = "Flow Book " + isbn.substring(8);
        Student member = TestData.student();
        gui.services().members().register(member);

        open("books");
        click(gui.shell(), "add");
        fillBookWizard(isbn, title);
        assertTrue(reviewLines(gui).contains("Test Press [2021]") && reviewLines(gui).contains("321 pages"), reviewLines(gui));
        click(gui.shell(), "primary"); // CONFIRM
        settle();
        assertEquals("Book added successfully", gui.messenger().lastInfo());

        // Borrow it.
        selectRow(gui.shell(), "books.table", title);
        assertTrue(detailLines(gui).contains("14 days - Available"));
        click(gui.shell(), "primary"); // BORROW
        settle();
        assertEquals(isbn, text(gui.shell(), "code"));
        assertEquals(title, text(gui.shell(), "title"));
        type(gui.shell(), "studentNumber", member.getStudentNumber());
        click(gui.shell(), "primary"); // ISSUE
        settle();
        assertEquals("Book issued successfully", gui.messenger().lastInfo());

        // It is now out: the table says so and a second borrow is refused.
        selectRow(gui.shell(), "books.table", title);
        assertTrue(detailLines(gui).contains("14 days - Unavailable"), detailLines(gui));
        click(gui.shell(), "primary");
        settle();
        assertTrue(gui.messenger().lastWarning().contains("currently borrowed"), gui.messenger().lastWarning());

        // Return it from the Issued screen.
        open("issued");
        selectRow(gui.shell(), "issued.table", title);
        assertTrue(detailLines(gui).contains("Not Returned Yet"));
        assertTrue(detailLines(gui).contains(member.fullName()));
        gui.messenger().answerConfirm(true);
        click(gui.shell(), "primary"); // RETURN
        settle();
        assertEquals("Book returned successfully", gui.messenger().lastInfo());

        open("returned");
        selectRow(gui.shell(), "returned.table", title);
        assertTrue(detailLines(gui).contains("Returned on"));
        open("books");
        selectRow(gui.shell(), "books.table", title);
        assertTrue(detailLines(gui).contains("14 days - Available"), "back on the shelf");
    }

    @Test
    void borrowingNeedsARegisteredMember() {
        PrintedBook book = TestData.book();
        gui.services().catalog().add(book);
        open("books");
        selectRow(gui.shell(), "books.table", book.getTitle());
        click(gui.shell(), "primary");
        settle();
        type(gui.shell(), "studentNumber", TestData.studentNumber());
        click(gui.shell(), "primary");
        settle();
        assertEquals("Student number is not a member.", gui.messenger().lastWarning());
        type(gui.shell(), "studentNumber", "12");
        click(gui.shell(), "primary");
        assertTrue(gui.messenger().lastWarning().contains("exactly 10 digits"), gui.messenger().lastWarning());
    }

    @Test
    void bookWizardRefusesADuplicateIsbnAndBadNumbers() {
        LibraryItem existing = gui.services().catalog().items(ItemType.BOOK).get(0);
        open("books");
        click(gui.shell(), "add");
        type(gui.shell(), "isbn", existing.getCode());
        type(gui.shell(), "title", "Whatever");
        type(gui.shell(), "author", "Someone");
        next();
        assertTrue(gui.messenger().lastWarning().contains("already been registered"), gui.messenger().lastWarning());

        type(gui.shell(), "isbn", TestData.itemCode());
        next();
        type(gui.shell(), "year", "20x1");
        type(gui.shell(), "publisher", "P");
        next();
        assertTrue(gui.messenger().lastWarning().contains("Publication year must be a whole number"), gui.messenger().lastWarning());
        type(gui.shell(), "year", "1500000");
        next();
        assertTrue(gui.messenger().lastWarning().contains("Publication year must be between"), gui.messenger().lastWarning());
    }

    // ---------- digital books and other materials ----------

    @Test
    @DisplayName("digital books: add, and many members can borrow the same one")
    void digitalBookLifecycle() {
        String epub = TestData.itemCode();
        String title = "Flow EBook " + epub.substring(8);
        Student first = TestData.student();
        Student second = TestData.student();
        gui.services().members().register(first);
        gui.services().members().register(second);

        open("ebooks");
        click(gui.shell(), "add");
        type(gui.shell(), "epubId", epub);
        type(gui.shell(), "title", title);
        type(gui.shell(), "author", "Digital Author");
        next();
        type(gui.shell(), "year", "2022");
        type(gui.shell(), "pages", "99");
        choose(gui.shell(), "genre", "Science Fiction");
        next();
        type(gui.shell(), "days", "7");
        type(gui.shell(), "url", "not a url");
        next();
        assertTrue(gui.messenger().lastWarning().contains("Digital book URL"), gui.messenger().lastWarning());
        type(gui.shell(), "url", "https://library.example.edu/ebooks/" + epub + ".epub");
        next();
        click(gui.shell(), "primary");
        settle();
        assertEquals("Digital book added successfully", gui.messenger().lastInfo());

        for (Student borrower : List.of(first, second)) {
            selectRow(gui.shell(), "ebooks.table", title);
            click(gui.shell(), "primary");
            settle();
            assertEquals(epub, text(gui.shell(), "code"));
            type(gui.shell(), "studentNumber", borrower.getStudentNumber());
            click(gui.shell(), "primary");
            settle();
            assertEquals("Digital book issued successfully", gui.messenger().lastInfo());
        }
        DigitalBook stored = (DigitalBook) gui.services().catalog().find(epub).orElseThrow();
        assertTrue(stored.isAvailable(), "an e-book never runs out");
    }

    @Test
    @DisplayName("other details are reference only: no BORROW button, just DONE")
    void otherDetailsCannotBeBorrowed() {
        String code = TestData.itemCode();
        String title = "Flow Thesis " + code.substring(8);
        open("others");
        click(gui.shell(), "add");
        type(gui.shell(), "barcode", code);
        type(gui.shell(), "title", title);
        next();
        type(gui.shell(), "year", "2020");
        type(gui.shell(), "pages", "150");
        choose(gui.shell(), "type", "Thesis");
        next();
        assertTrue(reviewLines(gui).contains("Thesis") && reviewLines(gui).contains("150 pages"), reviewLines(gui));
        click(gui.shell(), "primary");
        settle();
        assertEquals("Other detail added successfully", gui.messenger().lastInfo());

        selectRow(gui.shell(), "others.table", title);
        JButton primary = find(gui.shell(), "primary", JButton.class);
        assertEquals("DONE", primary.getText());
        assertFalse(exists(gui.shell(), "secondary"), "there is no BACK button when nothing can be borrowed");
        assertTrue(detailLines(gui).contains("Reference only"));
        click(gui.shell(), "primary");
        assertTrue(exists(gui.shell(), "add"), "DONE returns to the idle state");
    }

    @Test
    void theDetailCardAdaptsToWhatWasClicked() {
        open("books");
        selectRow(gui.shell(), "books.table", 0);
        assertEquals("BORROW", find(gui.shell(), "primary", JButton.class).getText());
        open("others");
        selectRow(gui.shell(), "others.table", 0);
        assertEquals("DONE", find(gui.shell(), "primary", JButton.class).getText());
        open("ebooks");
        selectRow(gui.shell(), "ebooks.table", 0);
        assertEquals("BORROW", find(gui.shell(), "primary", JButton.class).getText());
    }

    // ---------- missing (overdue) ----------

    @Test
    @DisplayName("missing: a loan past its due date appears there, and returning it clears it")
    void overdueLoanShowsUpInMissing() {
        Services setup = Services.create(db, Clock.systemDefaultZone(), new PasswordHasher(2_000));
        PrintedBook book = TestData.book();
        Student member = TestData.student();
        setup.catalog().add(book);
        setup.members().register(member);
        var librarian = setup.staff().librarians().get(0);
        Loan loan = setup.circulation().issue(book.getCode(), member.getStudentNumber(), librarian);

        TestClock inFuture = new TestClock();
        inFuture.advance(Duration.ofDays(40)); // the book is 30 days: now it is 10 days late
        GuiHarness later = new GuiHarness(db, inFuture);
        later.signIn("librarian", "Library@2026");

        click(later.shell(), "nav.missing");
        settle();
        selectRow(later.shell(), "missing.table", book.getTitle());
        assertTrue(String.join("\n", find(later.shell(), "detail", DetailCard.class).lineTexts()).contains(member.getStudentNumber()));
        later.messenger().answerConfirm(true);
        click(later.shell(), "primary"); // RETURN
        settle();
        assertEquals("Book returned successfully", later.messenger().lastInfo());
        assertTrue(later.services().circulation().loans(LoanView.MISSING).stream().noneMatch(l -> l.id() == loan.id()));
    }
}
