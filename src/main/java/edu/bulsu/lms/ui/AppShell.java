package edu.bulsu.lms.ui;

import edu.bulsu.lms.domain.Staff;
import edu.bulsu.lms.ui.admin.AdminView;
import edu.bulsu.lms.ui.kit.UiContext;
import edu.bulsu.lms.ui.librarian.LibrarianView;
import java.awt.CardLayout;
import java.util.Optional;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * The whole application as one panel: the sign-in screen, and after a successful sign-in the
 * dashboard that matches the account's role. There is a single login for everyone.
 */
public final class AppShell extends JPanel {

    private static final String LOGIN = "login";
    private static final String DASHBOARD = "dashboard";

    private final UiContext ctx;
    private final CardLayout cards = new CardLayout();
    private final LoginPanel login;
    private JComponent dashboard;

    public AppShell(UiContext ctx) {
        this.ctx = ctx;
        setLayout(cards);
        setName("appShell");
        login = new LoginPanel(ctx, this::open);
        add(login, LOGIN);
        cards.show(this, LOGIN);
    }

    /** The role decides the dashboard; nothing else about sign-in differs. */
    private void open(Staff staff) {
        discardDashboard();
        dashboard = switch (staff.getRole()) {
            case ADMIN -> new AdminView(ctx, staff, this::signOut);
            case LIBRARIAN -> new LibrarianView(ctx, staff, this::signOut);
        };
        add(dashboard, DASHBOARD);
        cards.show(this, DASHBOARD);
    }

    private void signOut() {
        cards.show(this, LOGIN);
        discardDashboard();
        login.reset();
    }

    private void discardDashboard() {
        if (dashboard != null) {
            remove(dashboard);
            dashboard = null;
        }
    }

    public LoginPanel login() {
        return login;
    }

    /** The dashboard currently shown, if anybody is signed in. */
    public Optional<JComponent> dashboard() {
        return Optional.ofNullable(dashboard);
    }
}
