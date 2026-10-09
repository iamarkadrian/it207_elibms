package edu.bulsu.lms.ui;

import static edu.bulsu.lms.testsupport.SwingTestKit.click;
import static edu.bulsu.lms.testsupport.SwingTestKit.exists;
import static edu.bulsu.lms.testsupport.SwingTestKit.find;
import static edu.bulsu.lms.testsupport.SwingTestKit.settle;
import static edu.bulsu.lms.testsupport.SwingTestKit.text;
import static edu.bulsu.lms.testsupport.SwingTestKit.type;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.bulsu.lms.persistence.Database;
import edu.bulsu.lms.testsupport.GuiHarness;
import edu.bulsu.lms.testsupport.TestData;
import edu.bulsu.lms.testsupport.TestDatabase;
import edu.bulsu.lms.ui.admin.AdminView;
import edu.bulsu.lms.ui.librarian.LibrarianView;
import javax.swing.JPasswordField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The single sign-in screen, driven through the real components. */
class LoginFlowIT {

    private static Database db;
    private GuiHarness gui;

    @BeforeAll
    static void connect() {
        db = TestDatabase.require();
    }

    @BeforeEach
    void open() {
        gui = new GuiHarness(db);
    }

    @Test
    void theSignInScreenIsShownFirst() {
        assertTrue(exists(gui.shell(), "signIn"));
        assertTrue(gui.shell().dashboard().isEmpty());
    }

    @Test
    void wrongPasswordExplainsAndStaysOnTheLoginScreen() {
        gui.signIn("librarian", "not-the-Password1");

        assertEquals(LoginPanel.NOT_MATCHED, gui.messenger().lastWarning());
        assertTrue(gui.shell().dashboard().isEmpty());
        assertEquals(0, ((JPasswordField) find(gui.shell(), "password", JPasswordField.class)).getPassword().length,
                "the password box is cleared after a failed attempt");
    }

    @Test
    void blankFieldsAreExplained() {
        gui.signIn("", "");
        assertTrue(gui.messenger().lastWarning().startsWith("The username and password field cannot be blank."));
        gui.signIn("librarian", "");
        assertTrue(gui.messenger().lastWarning().startsWith("The password field cannot be blank."));
    }

    @Test
    @DisplayName("a librarian lands on the librarian dashboard and can sign out again")
    void librarianSignsInAndOut() {
        gui.signIn("librarian", "Library@2026");

        LibrarianView view = assertInstanceOf(LibrarianView.class, gui.shell().dashboard().orElseThrow());
        assertEquals("Juan", text(view, "user.firstName"));
        assertEquals("Dela Cruz", text(view, "user.lastName"));
        assertEquals("UWIDE", text(view, "user.department"));
        assertFalse(exists(gui.shell(), "signIn"), "the login screen is hidden once signed in");

        click(gui.shell(), "nav.signOut");
        assertTrue(gui.shell().dashboard().isEmpty());
        assertTrue(exists(gui.shell(), "signIn"));
        assertEquals("", text(gui.shell(), "username"), "the boxes are empty again for the next person");
    }

    @Test
    @DisplayName("the administrator uses the same screen and lands on the administrator dashboard")
    void administratorSignsInThroughTheSameScreen() {
        gui.signIn("admin", "Admin@2026");

        assertInstanceOf(AdminView.class, gui.shell().dashboard().orElseThrow());
        assertEquals("Administrator Dashboard", text(gui.shell(), "title"));

        click(gui.shell(), "toolbar.signOut");
        assertTrue(gui.shell().dashboard().isEmpty());
    }

    @Test
    void usernameIsNotCaseSensitive() {
        gui.signIn("  Librarian ", "Library@2026");
        assertInstanceOf(LibrarianView.class, gui.shell().dashboard().orElseThrow());
    }

    @Test
    void threeFailuresInARowPauseSignIn() {
        gui.signIn("ghost-user", "wrong-one-1");
        gui.signIn("ghost-user", "wrong-two-2");
        gui.signIn("ghost-user", "wrong-three-3");
        assertTrue(gui.messenger().lastError().contains("wait 15 seconds"), gui.messenger().lastError());

        gui.signIn("librarian", "Library@2026"); // still inside the pause
        assertTrue(gui.messenger().lastError().contains("Please wait"));
        assertTrue(gui.shell().dashboard().isEmpty());
    }

    @Test
    @DisplayName("end to end: a librarian is locked out, the administrator unlocks them, they sign in again")
    void lockedLibrarianIsUnlockedByTheAdministrator() {
        String username = TestData.username();
        int id = gui.services().staff().addLibrarian(TestData.admin(), "Lock", null, "Flow", "Librarian", "CICT", username,
                "Passw0rd-ok".toCharArray());

        gui.signIn(username, "wrong-one-1");
        gui.signIn(username, "wrong-two-2");
        gui.signIn(username, "wrong-three-3");
        String afterLocking = gui.messenger().lastWarning();

        gui.signIn(username, "Passw0rd-ok"); // the right password no longer helps
        assertTrue(gui.shell().dashboard().isEmpty());
        // A locked account looks exactly like a wrong password, so the screen cannot be used to find locked users.
        assertEquals(LoginPanel.NOT_MATCHED, afterLocking);
        assertEquals(LoginPanel.NOT_MATCHED, gui.messenger().lastWarning());

        // The administrator signs in on the very same screen and unlocks the account.
        gui.signIn("admin", "Admin@2026");
        assertInstanceOf(AdminView.class, gui.shell().dashboard().orElseThrow());
        click(gui.shell(), "toolbar.unlock");
        type(gui.shell(), "librarianId", String.valueOf(id));
        click(gui.shell(), "primary");
        settle();
        assertEquals("Librarian unlocked successfully", gui.messenger().lastInfo());
        click(gui.shell(), "toolbar.signOut");

        gui.signIn(username, "Passw0rd-ok");
        assertInstanceOf(LibrarianView.class, gui.shell().dashboard().orElseThrow());
    }
}
