package edu.bulsu.lms.ui.librarian;

import edu.bulsu.lms.domain.ItemType;
import edu.bulsu.lms.domain.Staff;
import edu.bulsu.lms.service.LoanView;
import edu.bulsu.lms.ui.kit.ImagePanel;
import edu.bulsu.lms.ui.kit.UiContext;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * The librarian's workspace: a sidebar on the left and one screen at a time on the right.
 *
 * <p>Layout: {@code BorderLayout} (sidebar west, content center) and {@code CardLayout} for the
 * screens. Each nav item shows its screen and asks it to refresh.
 */
public final class LibrarianView extends JPanel {

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new ImagePanel("bg_light.png", cards);
    private final Map<String, Screen> screens = new LinkedHashMap<>();
    private final Sidebar sidebar;

    public LibrarianView(UiContext ctx, Staff staff, Runnable onSignOut) {
        super(new BorderLayout());
        setName("librarianView");
        sidebar = new Sidebar(staff, onSignOut);

        register("dashboard", "Dashboard", "dashboard", new DashboardScreen(ctx, staff));
        register("members", "Members", "members", new MembersScreen(ctx));
        register("books", "Books", "books", new CatalogScreen(ctx, staff, ItemType.BOOK));
        register("ebooks", "Digital Books", "ebooks", new CatalogScreen(ctx, staff, ItemType.DIGITAL));
        register("others", "Other Details", "otherdetails", new CatalogScreen(ctx, staff, ItemType.OTHER));
        register("issued", "Issued", "issued", new LoansScreen(ctx, LoanView.ISSUED));
        register("returned", "Returned", "returned", new LoansScreen(ctx, LoanView.RETURNED));
        register("missing", "Missing", "missing", new LoansScreen(ctx, LoanView.MISSING));

        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        show("dashboard");
    }

    /** Opens the screen registered under {@code key} ("members", "books", ...). */
    public void show(String key) {
        cards.show(content, key);
        sidebar.select(key);
        screens.get(key).onShow();
    }

    private <S extends JComponent & Screen> void register(String key, String label, String icon, S screen) {
        screens.put(key, screen);
        content.add(screen, key);
        sidebar.addItem(key, label, icon, () -> show(key));
    }
}

/** Something that fills the content area and refreshes itself whenever it is shown. */
interface Screen {

    void onShow();
}
