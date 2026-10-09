package edu.bulsu.lms.ui.admin;

import edu.bulsu.lms.domain.Check;
import edu.bulsu.lms.domain.Department;
import edu.bulsu.lms.domain.Person;
import edu.bulsu.lms.domain.Staff;
import edu.bulsu.lms.domain.UserFacingException;
import edu.bulsu.lms.ui.kit.Buttons;
import edu.bulsu.lms.ui.kit.Field;
import edu.bulsu.lms.ui.kit.FormPanel;
import edu.bulsu.lms.ui.kit.HintCard;
import edu.bulsu.lms.ui.kit.ImagePanel;
import edu.bulsu.lms.ui.kit.ObjectTable;
import edu.bulsu.lms.ui.kit.Palette;
import edu.bulsu.lms.ui.kit.Tasks;
import edu.bulsu.lms.ui.kit.Theme;
import edu.bulsu.lms.ui.kit.UiContext;
import edu.bulsu.lms.ui.kit.Values;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The administrator's dashboard (dark theme): a table of librarian accounts and a form on the
 * right for adding, editing, unlocking and removing them.
 *
 * <p>Layout: {@code BorderLayout} for the page, {@code GridBagLayout} for table + card,
 * {@code CardLayout} for the four forms, {@code FlowLayout} for the toolbar.
 */
public final class AdminView extends JPanel {

    /** Listed first in the Designation drop-down. */
    private static final String UNIVERSITY_WIDE = "UWIDE";

    private static final String HINT = "hint";
    private static final String ADD = "add";
    private static final String EDIT = "edit";
    private static final String UNLOCK = "unlock";
    private static final String REMOVE = "remove";

    private final UiContext ctx;
    private final Staff actor;
    private final ObjectTable<Staff> table;
    private final CardLayout cards = new CardLayout();
    private final JPanel side = new JPanel(cards);
    private String active = HINT;
    private List<Department> departments = List.of();

    // add form
    private final Field.Text<String> addFirstName = Field.text("firstName", "First Name", text -> Person.requireName(text, "First name"));
    private final Field.Text<String> addInitial = Field.text("middleInitial", "Middle Initial", Person::optionalInitial);
    private final Field.Text<String> addLastName = Field.text("lastName", "Last Name", text -> Person.requireName(text, "Last name"));
    private final Field.Text<String> addUsername = Field.text("username", "Username", Staff::requireUsername).hint("4-20 characters");
    private final Field.Secret addPassword = Field.secret("password", "Password", true).hint("Min. 8, letter + digit");
    private final Field.Choice<Department> addDepartment = Field.<Department>choice("designation", "Designation");
    private final Field.Choice<String> addPosition = Field.<String>choice("position", "Position").items(Staff.POSITIONS);

    // edit form
    private final Field.Text<Integer> editId = Field.text("librarianId", "Librarian ID", text -> Check.integer(text, "Librarian ID"));
    private final Field.Text<String> editUsername = Field.text("username", "New Username", Staff::requireUsername).hint("4-20 characters");
    private final Field.Secret editPassword = Field.secret("password", "New Password", false).hint("Blank keeps current");
    private final Field.Choice<Department> editDepartment = Field.<Department>choice("designation", "Designation");
    private final Field.Choice<String> editPosition = Field.<String>choice("position", "Position").items(Staff.POSITIONS);

    // unlock / remove forms
    private final Field.Text<Integer> unlockId = Field.text("librarianId", "Librarian ID", text -> Check.integer(text, "Librarian ID"));
    private final Field.Text<Integer> removeId = Field.text("librarianId", "Librarian ID", text -> Check.integer(text, "Librarian ID"));

    private final FormPanel addForm;
    private final FormPanel editForm;
    private final FormPanel unlockForm;
    private final FormPanel removeForm;

    public AdminView(UiContext ctx, Staff admin, Runnable onSignOut) {
        super(new BorderLayout());
        this.ctx = ctx;
        this.actor = admin;
        setName("adminView");

        table = new ObjectTable<>("staff.table", List.of("Librarian ID", "Name", "Position", "Designation", "Account Status"),
                staff -> List.of(String.valueOf(staff.getId()), staff.listName(), staff.getPosition(),
                        staff.getDepartmentCode(), staff.isLocked() ? "Locked" : "Unlocked"),
                new int[]{90, 170, 150, 90, 100}, Palette.DARK);
        table.onSelect(this::fillFromSelection);

        addForm = form("Add Librarian", "ADD", this::add, ADD)
                .row(new double[]{1, 1, 1}, addFirstName, addInitial, addLastName)
                .row(addUsername, addPassword)
                .row(addDepartment, addPosition);
        editForm = form("Edit Librarian Information", "CHANGE", this::change, EDIT)
                .row(editId)
                .row(editUsername, editPassword)
                .row(editDepartment, editPosition);
        unlockForm = form("Unlock Librarian", "UNLOCK", this::unlock, UNLOCK).row(unlockId);
        removeForm = form("Remove Librarian", "REMOVE", this::remove, REMOVE).row(removeId);

        side.setOpaque(false);
        side.add(new HintCard(Palette.DARK, "No action selected", null, null), HINT);
        side.add(addForm, ADD);
        side.add(editForm, EDIT);
        side.add(unlockForm, UNLOCK);
        side.add(removeForm, REMOVE);

        JPanel page = new ImagePanel("bg_dark.png", new BorderLayout(0, 48));
        page.setBorder(BorderFactory.createEmptyBorder(48, 48, 48, 48));
        page.add(header(onSignOut), BorderLayout.NORTH);
        page.add(body(), BorderLayout.CENTER);
        add(page, BorderLayout.CENTER);

        loadDepartments();
        reload();
    }

    // ---- layout ----

    private JPanel header(Runnable onSignOut) {
        JLabel caption = new JLabel("BULACAN STATE UNIVERSITY - LIBRARY MANAGEMENT SYSTEM");
        caption.setFont(Theme.font(Font.BOLD, 20));
        caption.setForeground(Theme.MUTED);
        JLabel title = new JLabel("Administrator Dashboard");
        title.setName("title");
        title.setFont(Theme.font(Font.PLAIN, 34));
        title.setForeground(Color.WHITE);

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        caption.setAlignmentX(LEFT_ALIGNMENT);
        title.setAlignmentX(LEFT_ALIGNMENT);
        titles.add(caption);
        titles.add(title);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.add(tool("toolbar.add", "add.png", "Add a librarian", ADD));
        toolbar.add(tool("toolbar.edit", "edit.png", "Edit a librarian", EDIT));
        toolbar.add(tool("toolbar.unlock", "unlock.png", "Unlock a librarian", UNLOCK));
        toolbar.add(tool("toolbar.remove", "delete.png", "Remove a librarian", REMOVE));
        JButton signOut = Buttons.primary("SIGN OUT");
        signOut.setName("toolbar.signOut");
        signOut.setIcon(Theme.icon("logout_white.png"));
        signOut.setIconTextGap(8);
        signOut.addActionListener(event -> onSignOut.run());
        toolbar.add(signOut);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titles, BorderLayout.WEST);
        header.add(toolbar, BorderLayout.EAST);
        return header;
    }

    private JPanel body() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);

        GridBagConstraints left = new GridBagConstraints();
        left.weightx = 1;
        left.weighty = 1;
        left.fill = GridBagConstraints.BOTH;
        body.add(table, left);

        side.setPreferredSize(new Dimension(526, 10));
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.weighty = 1;
        right.fill = GridBagConstraints.VERTICAL;
        right.insets = new Insets(0, 48, 0, 0);
        body.add(side, right);
        return body;
    }

    private JButton tool(String name, String icon, String tooltip, String card) {
        JButton button = Buttons.icon(Theme.icon(icon), tooltip, Theme.TOOLBAR_BUTTON);
        button.setName(name);
        button.addActionListener(event -> open(card));
        return button;
    }

    /** A dark card form with its main button and a CANCEL that asks first if anything was typed. */
    private FormPanel form(String heading, String primaryText, Runnable onPrimary, String card) {
        FormPanel form = new FormPanel(Palette.DARK, heading);
        JButton primary = Buttons.primary(primaryText);
        primary.setName("primary");
        primary.addActionListener(event -> onPrimary.run());
        JButton cancel = Buttons.text("CANCEL", Color.WHITE);
        cancel.setName("secondary");
        cancel.addActionListener(event -> cancel(card));
        return form.buttons(primary, cancel, true);
    }

    private FormPanel formOf(String card) {
        return switch (card) {
            case ADD -> addForm;
            case EDIT -> editForm;
            case UNLOCK -> unlockForm;
            default -> removeForm;
        };
    }

    // ---- navigation between the four forms ----

    private void open(String card) {
        active = card;
        cards.show(side, card);
        formOf(card).focusFirst();
        reload(); // another administrator may have changed accounts since the table was last loaded
    }

    private void cancel(String card) {
        FormPanel form = formOf(card);
        if (!form.isEmpty() && !ctx.messenger().confirm(this,
                "You have entered some information, do you really want to cancel?", "Question")) {
            return;
        }
        form.reset();
        active = HINT;
        table.clearSelection();
        cards.show(side, HINT);
    }

    /** Clicking a row fills in the Librarian ID of whichever form is open, so nobody has to type it. */
    private void fillFromSelection(Staff staff) {
        String id = String.valueOf(staff.getId());
        switch (active) {
            case EDIT -> {
                editId.fill(id);
                editUsername.fill(staff.getUsername());
                departments.stream().filter(d -> d.code().equals(staff.getDepartmentCode())).findFirst().ifPresent(editDepartment::select);
                editPosition.select(staff.getPosition());
            }
            case UNLOCK -> unlockId.fill(id);
            case REMOVE -> removeId.fill(id);
            default -> { }
        }
    }

    // ---- data ----

    private void reload() {
        Tasks.run(ctx, this, () -> ctx.services().staff().librarians(), table::setItems);
    }

    private void loadDepartments() {
        Tasks.run(ctx, this, () -> ctx.services().reference().departments().stream()
                        .sorted(Comparator.comparing((Department d) -> !d.code().equals(UNIVERSITY_WIDE)).thenComparing(Department::name))
                        .toList(),
                list -> {
                    departments = list;
                    addDepartment.items(list);
                    editDepartment.items(list);
                });
    }

    // ---- actions ----

    private void add() {
        Values v = read(addForm);
        if (v == null) {
            return;
        }
        char[] password = v.get("password", char[].class);
        run(() -> ctx.services().staff().addLibrarian(actor, v.text("firstName"), v.text("middleInitial"), v.text("lastName"),
                        v.text("position"), v.get("designation", Department.class).code(), v.text("username"), password),
                "Librarian added successfully", addForm, ADD, password);
    }

    private void change() {
        Values v = read(editForm);
        if (v == null) {
            return;
        }
        char[] password = v.get("password", char[].class);
        run(() -> {
            ctx.services().staff().updateLibrarian(actor, v.get("librarianId", Integer.class), v.text("username"), password,
                    v.get("designation", Department.class).code(), v.text("position"));
            return null;
        }, "Librarian updated successfully", editForm, HINT, password);
    }

    private void unlock() {
        Values v = read(unlockForm);
        if (v == null) {
            return;
        }
        run(() -> {
            ctx.services().staff().unlock(actor, v.get("librarianId", Integer.class));
            return null;
        }, "Librarian unlocked successfully", unlockForm, HINT, null);
    }

    private void remove() {
        Values v = read(removeForm);
        if (v == null) {
            return;
        }
        int id = v.get("librarianId", Integer.class);
        Staff target = table.items().stream().filter(s -> s.getId() == id).findFirst().orElse(null);
        if (target == null) {
            ctx.messenger().warn(this, "The Librarian ID you entered does not match any librarian in the system.");
            return;
        }
        // Removing is permanent, so ask the administrator to type the username to prove it is deliberate.
        String typed = ctx.messenger().prompt(this, "Removing " + target.fullName() + " cannot be undone.\n"
                + "Type the username \"" + target.getUsername() + "\" to confirm.", "Remove librarian");
        if (typed == null || !typed.strip().equalsIgnoreCase(target.getUsername())) {
            ctx.messenger().info(this, "Nothing was removed.");
            return;
        }
        run(() -> {
            ctx.services().staff().remove(actor, id);
            return null;
        }, "Librarian removed successfully.", removeForm, HINT, null);
    }

    /** Validates a form; shows the problem and returns {@code null} if something is wrong. */
    private Values read(FormPanel form) {
        try {
            return form.read();
        } catch (UserFacingException problem) {
            ctx.messenger().warn(this, problem.getMessage());
            return null;
        }
    }

    /** Runs {@code work} in the background; on success tells the user, tidies the form, and refreshes the table. */
    private <T> void run(java.util.concurrent.Callable<T> work, String success, FormPanel form, String next, char[] password) {
        JButton button = form.primary();
        button.setEnabled(false); // hashing takes a moment; a second click must not submit the form again
        Tasks.run(ctx, this, work, result -> {
            ctx.messenger().info(this, success);
            form.reset();
            active = next;
            cards.show(side, next);
            reload();
        }, () -> {
            button.setEnabled(true);
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        });
    }
}
