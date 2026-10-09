package edu.bulsu.lms.ui;

import static edu.bulsu.lms.testsupport.SwingTestKit.choose;
import static edu.bulsu.lms.testsupport.SwingTestKit.click;
import static edu.bulsu.lms.testsupport.SwingTestKit.exists;
import static edu.bulsu.lms.testsupport.SwingTestKit.rowCount;
import static edu.bulsu.lms.testsupport.SwingTestKit.selectRow;
import static edu.bulsu.lms.testsupport.SwingTestKit.settle;
import static edu.bulsu.lms.testsupport.SwingTestKit.text;
import static edu.bulsu.lms.testsupport.SwingTestKit.type;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.bulsu.lms.domain.Staff;
import edu.bulsu.lms.persistence.Database;
import edu.bulsu.lms.service.LoginResult;
import edu.bulsu.lms.testsupport.GuiHarness;
import edu.bulsu.lms.testsupport.TestData;
import edu.bulsu.lms.testsupport.TestDatabase;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The administrator dashboard: add, edit, unlock and remove librarians through the real forms. */
class AdminFlowIT {

    private static Database db;
    private GuiHarness gui;

    @BeforeAll
    static void connect() {
        db = TestDatabase.require();
    }

    @BeforeEach
    void signInAsAdministrator() {
        gui = new GuiHarness(db);
        gui.signIn("admin", "Admin@2026");
    }

    private Staff librarian(String username) {
        return gui.services().staff().librarians().stream()
                .filter(s -> s.getUsername().equals(username)).findFirst().orElseThrow();
    }

    private Optional<Staff> maybeLibrarian(String username) {
        return gui.services().staff().librarians().stream().filter(s -> s.getUsername().equals(username)).findFirst();
    }

    @Test
    void theTableListsEveryLibrarianButNotTheAdministrator() {
        settle();
        assertEquals(gui.services().staff().librarians().size(), rowCount(gui.shell(), "staff.table"));
        assertTrue(exists(gui.shell(), "hint"), "no form is open until a toolbar button is pressed");
    }

    @Test
    @DisplayName("add: the new librarian appears in the table and can sign in")
    void addLibrarian() {
        String username = TestData.username();
        int before = rowCount(gui.shell(), "staff.table");

        click(gui.shell(), "toolbar.add");
        type(gui.shell(), "firstName", "Gina");
        type(gui.shell(), "middleInitial", "R.");
        type(gui.shell(), "lastName", "Addtest");
        type(gui.shell(), "username", username);
        type(gui.shell(), "password", "Passw0rd-ok");
        choose(gui.shell(), "designation", "College of Engineering");
        choose(gui.shell(), "position", "Technical Librarian");
        click(gui.shell(), "primary");
        settle();

        assertEquals("Librarian added successfully", gui.messenger().lastInfo());
        assertEquals(before + 1, rowCount(gui.shell(), "staff.table"));
        Staff saved = librarian(username);
        assertEquals("COE", saved.getDepartmentCode());
        assertEquals("Technical Librarian", saved.getPosition());
        assertEquals("Gina R. Addtest", saved.fullName());
        assertTrue(gui.services().auth().login(username, "Passw0rd-ok".toCharArray()) instanceof LoginResult.Success);
    }

    @Test
    void addRefusesWeakPasswordsAndTakenUsernames() {
        click(gui.shell(), "toolbar.add");
        type(gui.shell(), "firstName", "Weak");
        type(gui.shell(), "lastName", "Password");
        type(gui.shell(), "username", TestData.username());
        type(gui.shell(), "password", "short1");
        click(gui.shell(), "primary");
        settle();
        assertTrue(gui.messenger().lastWarning().contains("at least 8 characters"), gui.messenger().lastWarning());

        type(gui.shell(), "username", "librarian"); // already exists
        type(gui.shell(), "password", "Passw0rd-ok");
        click(gui.shell(), "primary");
        settle();
        assertTrue(gui.messenger().lastWarning().contains("already taken"), gui.messenger().lastWarning());

        type(gui.shell(), "firstName", "R2D2");
        click(gui.shell(), "primary");
        assertTrue(gui.messenger().lastWarning().contains("First name may only contain letters"), gui.messenger().lastWarning());
    }

    @Test
    void cancelAsksBeforeThrowingAwayWhatWasTyped() {
        click(gui.shell(), "toolbar.add");
        type(gui.shell(), "firstName", "Half");

        gui.messenger().answerConfirm(false);
        click(gui.shell(), "secondary");
        assertEquals("Half", text(gui.shell(), "firstName"), "answering No keeps the form");

        gui.messenger().answerConfirm(true);
        click(gui.shell(), "secondary");
        assertTrue(exists(gui.shell(), "hint"), "answering Yes closes it");
    }

    @Test
    @DisplayName("edit: designation, position and password change; the old password stops working")
    void editLibrarian() {
        String username = TestData.username();
        int id = gui.services().staff().addLibrarian(TestData.admin(), "Edit", null, "Flowtest", "Librarian", "CICT", username,
                "Passw0rd-ok".toCharArray());
        click(gui.shell(), "toolbar.edit");
        settle();
        selectRow(gui.shell(), "staff.table", String.valueOf(id));
        assertEquals(String.valueOf(id), text(gui.shell(), "librarianId"), "clicking a row fills in the id");
        assertEquals(username, text(gui.shell(), "username"));

        String renamed = TestData.username();
        type(gui.shell(), "username", renamed);
        type(gui.shell(), "password", "Brand-New-9");
        choose(gui.shell(), "designation", "College of Law");
        choose(gui.shell(), "position", "Unit Head Librarian");
        click(gui.shell(), "primary");
        settle();

        assertEquals("Librarian updated successfully", gui.messenger().lastInfo());
        Staff after = librarian(renamed);
        assertEquals("COL", after.getDepartmentCode());
        assertEquals("Unit Head Librarian", after.getPosition());
        assertTrue(gui.services().auth().login(renamed, "Passw0rd-ok".toCharArray()) instanceof LoginResult.InvalidCredentials);
        assertTrue(gui.services().auth().login(renamed, "Brand-New-9".toCharArray()) instanceof LoginResult.Success);
    }

    @Test
    void editWithAnUnknownIdIsExplained() {
        click(gui.shell(), "toolbar.edit");
        type(gui.shell(), "librarianId", "1");
        type(gui.shell(), "username", TestData.username());
        click(gui.shell(), "primary");
        settle();
        assertTrue(gui.messenger().lastWarning().contains("does not match any librarian"), gui.messenger().lastWarning());

        type(gui.shell(), "librarianId", "abc");
        click(gui.shell(), "primary");
        assertTrue(gui.messenger().lastWarning().contains("must be a whole number"), gui.messenger().lastWarning());
    }

    @Test
    void unlockOpensALockedAccount() {
        Staff locked = gui.services().staff().librarians().stream().filter(Staff::isLocked).findFirst().orElseThrow();
        click(gui.shell(), "toolbar.unlock");
        selectRow(gui.shell(), "staff.table", String.valueOf(locked.getId()));
        click(gui.shell(), "primary");
        settle();

        assertEquals("Librarian unlocked successfully", gui.messenger().lastInfo());
        assertFalse(gui.services().staff().librarians().stream().filter(s -> s.getId() == locked.getId()).findFirst().orElseThrow().isLocked());
    }

    @Test
    void unlockingAnOpenAccountIsExplained() {
        Staff open = gui.services().staff().librarians().stream().filter(s -> !s.isLocked()).findFirst().orElseThrow();
        click(gui.shell(), "toolbar.unlock");
        type(gui.shell(), "librarianId", String.valueOf(open.getId()));
        click(gui.shell(), "primary");
        settle();
        assertTrue(gui.messenger().lastWarning().contains("not currently locked"), gui.messenger().lastWarning());
    }

    @Test
    @DisplayName("remove: needs the librarian's username as confirmation, then deletes")
    void removeLibrarian() {
        String username = TestData.username();
        int id = gui.services().staff().addLibrarian(TestData.admin(), "Gone", null, "Soon", "Librarian", "CICT", username, "Passw0rd-ok".toCharArray());
        click(gui.shell(), "toolbar.remove");
        settle();
        type(gui.shell(), "librarianId", String.valueOf(id));

        gui.messenger().answerPrompt("not-the-username");
        click(gui.shell(), "primary");
        settle();
        assertEquals("Nothing was removed.", gui.messenger().lastInfo());
        assertTrue(maybeLibrarian(username).isPresent());

        gui.messenger().answerPrompt(username);
        click(gui.shell(), "primary");
        settle();
        assertEquals("Librarian removed successfully.", gui.messenger().lastInfo());
        assertTrue(maybeLibrarian(username).isEmpty());
    }

    @Test
    void aLibrarianWithLoanHistoryCannotBeRemoved() {
        Staff withHistory = gui.services().staff().librarians().stream()
                .filter(s -> gui.services().circulation().loans(edu.bulsu.lms.service.LoanView.RETURNED).stream()
                        .anyMatch(l -> l.issuedById() == s.getId()))
                .findFirst().orElseThrow();
        click(gui.shell(), "toolbar.remove");
        settle();
        type(gui.shell(), "librarianId", String.valueOf(withHistory.getId()));
        gui.messenger().answerPrompt(withHistory.getUsername());
        click(gui.shell(), "primary");
        settle();
        assertTrue(gui.messenger().lastWarning().contains("cannot be removed"), gui.messenger().lastWarning());
        assertTrue(maybeLibrarian(withHistory.getUsername()).isPresent());
    }
}
