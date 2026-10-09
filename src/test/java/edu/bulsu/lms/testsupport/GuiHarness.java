package edu.bulsu.lms.testsupport;

import static edu.bulsu.lms.testsupport.SwingTestKit.click;
import static edu.bulsu.lms.testsupport.SwingTestKit.onEdtResult;
import static edu.bulsu.lms.testsupport.SwingTestKit.settle;
import static edu.bulsu.lms.testsupport.SwingTestKit.type;

import edu.bulsu.lms.persistence.Database;
import edu.bulsu.lms.security.PasswordHasher;
import edu.bulsu.lms.service.Services;
import edu.bulsu.lms.ui.AppShell;
import edu.bulsu.lms.ui.kit.UiContext;
import edu.bulsu.lms.ui.librarian.LibrarianView;
import java.time.Clock;

/**
 * The whole application (sign-in, dashboards, services) over a real database, with dialogs
 * replaced by a {@link RecordingMessenger}. Each GUI test builds a fresh one.
 */
public final class GuiHarness {

    private final RecordingMessenger messenger = new RecordingMessenger();
    private final Services services;
    private final AppShell shell;

    public GuiHarness(Database database) {
        this(database, Clock.systemDefaultZone());
    }

    /** @param clock what the application believes "now" is (move it forward to make loans overdue) */
    public GuiHarness(Database database, Clock clock) {
        services = Services.create(database, clock, new PasswordHasher(2_000));
        UiContext context = new UiContext(services, messenger);
        shell = onEdtResult(() -> new AppShell(context));
    }

    public AppShell shell() {
        return shell;
    }

    public Services services() {
        return services;
    }

    public RecordingMessenger messenger() {
        return messenger;
    }

    /** Types the credentials into the sign-in screen and presses SIGN IN, like a user would. */
    public void signIn(String username, String password) {
        type(shell, "username", username);
        type(shell, "password", password);
        click(shell, "signIn");
        settle();
    }

    /** The librarian workspace; fails the test if somebody else (or nobody) is signed in. */
    public LibrarianView librarian() {
        return (LibrarianView) shell.dashboard()
                .filter(LibrarianView.class::isInstance)
                .orElseThrow(() -> new AssertionError("The librarian dashboard is not open"));
    }
}
