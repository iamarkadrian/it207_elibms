package edu.bulsu.lms.ui;

import static edu.bulsu.lms.testsupport.SwingTestKit.choose;
import static edu.bulsu.lms.testsupport.SwingTestKit.click;
import static edu.bulsu.lms.testsupport.SwingTestKit.onEdtResult;
import static edu.bulsu.lms.testsupport.SwingTestKit.render;
import static edu.bulsu.lms.testsupport.SwingTestKit.save;
import static edu.bulsu.lms.testsupport.SwingTestKit.selectRow;
import static edu.bulsu.lms.testsupport.SwingTestKit.settle;
import static edu.bulsu.lms.testsupport.SwingTestKit.type;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import edu.bulsu.lms.persistence.Database;
import edu.bulsu.lms.security.PasswordHasher;
import edu.bulsu.lms.service.Services;
import edu.bulsu.lms.testsupport.RecordingMessenger;
import edu.bulsu.lms.testsupport.TestDatabase;
import edu.bulsu.lms.ui.kit.Theme;
import edu.bulsu.lms.ui.kit.UiContext;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.Clock;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import org.junit.jupiter.api.Test;

/**
 * Generates the README pictures by driving the real application against the seeded database.
 * Skipped unless a target folder is given:
 * <pre>
 *   ./mvnw verify -Dit.test=ScreenshotsIT -Dlms.screenshots=docs/screenshots
 * </pre>
 */
class ScreenshotsIT {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    private AppShell shell;
    private Path folder;

    @Test
    void captureEveryScreen() throws Exception {
        String target = System.getProperty("lms.screenshots");
        assumeTrue(target != null && !target.isBlank(), "Set -Dlms.screenshots=<folder> to regenerate the screenshots");
        folder = Path.of(target);

        Database db = TestDatabase.require();
        Theme.install();
        UiContext ctx = new UiContext(Services.create(db, Clock.systemDefaultZone(), new PasswordHasher(2_000)), new RecordingMessenger());
        shell = new AppShell(ctx);

        shot("01-login");
        signIn("librarian", "Library@2026");
        settle();
        shot("02-librarian-dashboard");

        members();
        books();
        digitalBooks();
        otherDetails();
        loans();

        click(shell, "nav.signOut");
        signIn("admin", "Admin@2026");
        settle();
        shot("23-admin-dashboard");
        for (String action : new String[]{"add", "edit", "unlock", "remove"}) {
            click(shell, "toolbar." + action);
            shot("24-admin-" + action);
        }
        dialogs();
    }

    /** The dialogs are real {@link JOptionPane}s, painted on their own because a modal dialog would block the test. */
    private void dialogs() throws Exception {
        dialog("30-dialog-validation", new JOptionPane(label("Student number must be exactly 10 digits."),
                JOptionPane.WARNING_MESSAGE));
        dialog("31-dialog-confirm-cancel", new JOptionPane(label("You have entered some information, do you really want to cancel?"),
                JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION));
        dialog("32-dialog-success", new JOptionPane(label("Member added successfully"), JOptionPane.INFORMATION_MESSAGE));
        JOptionPane prompt = new JOptionPane(label("Removing Lara R. Tolentino cannot be undone.\nType the username \"ltolentino\" to confirm."),
                JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        prompt.setWantsInput(true);
        dialog("33-dialog-type-to-confirm", prompt);
    }

    private static JLabel label(String message) {
        return new JLabel("<html><div style='width: 340px'>" + message.replace("\n", "<br>") + "</div></html>");
    }

    private void dialog(String name, JOptionPane pane) throws Exception {
        Dimension size = onEdtResult(() -> {
            Dimension preferred = pane.getPreferredSize();
            return new Dimension(preferred.width + 2, preferred.height + 2);
        });
        BufferedImage image = render(pane, size.width, size.height);
        save(image, folder.resolve(name + ".png"));
    }

    private void members() throws Exception {
        click(shell, "nav.members");
        settle();
        shot("03-members");
        selectRow(shell, "members.table", 0);
        shot("04-member-detail");
        click(shell, "secondary");

        click(shell, "add");
        shot("05-add-member");
        type(shell, "studentNumber", "2026100001");
        type(shell, "firstName", "Mara");
        type(shell, "middleInitial", "L.");
        type(shell, "lastName", "Reyes");
        click(shell, "primary");
        settle();
        choose(shell, "college", "Information and Communications");
        shot("06-add-member-course");
        type(shell, "section", "3B");
        click(shell, "primary");
        settle();
        choose(shell, "gender", "Female");
        type(shell, "contact", "09171234567");
        click(shell, "primary");
        settle();
        type(shell, "houseNo", "12");
        type(shell, "street", "Rizal St.");
        type(shell, "barangay", "Poblacion");
        type(shell, "municipality", "Malolos");
        type(shell, "province", "Bulacan");
        click(shell, "primary");
        settle();
        shot("07-add-member-review");
        click(shell, "secondary");
        settle();
    }

    private void books() throws Exception {
        click(shell, "nav.books");
        settle();
        shot("08-books");
        selectRow(shell, "books.table", 0);
        shot("09-book-detail");
        click(shell, "primary");
        settle();
        shot("10-borrow-book");
        click(shell, "secondary");
        click(shell, "add");
        type(shell, "isbn", "9781492078005");
        type(shell, "title", "Learning Java");
        type(shell, "author", "Ada Tester");
        shot("11-add-book");
        click(shell, "secondary");
    }

    private void digitalBooks() throws Exception {
        click(shell, "nav.ebooks");
        settle();
        shot("12-digital-books");
        selectRow(shell, "ebooks.table", 0);
        shot("13-digital-book-detail");
    }

    private void otherDetails() throws Exception {
        click(shell, "nav.others");
        settle();
        shot("14-other-details");
        selectRow(shell, "others.table", 0);
        shot("15-other-detail");
    }

    private void loans() throws Exception {
        click(shell, "nav.issued");
        settle();
        shot("16-issued");
        selectRow(shell, "issued.table", 0);
        shot("17-issued-detail");
        click(shell, "nav.returned");
        settle();
        shot("18-returned");
        selectRow(shell, "returned.table", 0);
        shot("19-returned-detail");
        click(shell, "nav.missing");
        settle();
        shot("20-missing");
        selectRow(shell, "missing.table", 0);
        shot("21-missing-detail");
        click(shell, "nav.dashboard");
        settle();
        shot("22-dashboard-after");
    }

    private void signIn(String user, String password) {
        type(shell, "username", user);
        type(shell, "password", password);
        click(shell, "signIn");
    }

    private void shot(String name) throws Exception {
        settle();
        save(render(shell, WIDTH, HEIGHT), folder.resolve(name + ".png"));
    }
}
