package edu.bulsu.lms.ui.librarian;

import edu.bulsu.lms.domain.ItemType;
import edu.bulsu.lms.domain.LibraryItem;
import edu.bulsu.lms.domain.Staff;
import edu.bulsu.lms.domain.Student;
import edu.bulsu.lms.domain.UserFacingException;
import edu.bulsu.lms.ui.kit.Buttons;
import edu.bulsu.lms.ui.kit.Field;
import edu.bulsu.lms.ui.kit.FormPanel;
import edu.bulsu.lms.ui.kit.ObjectTable;
import edu.bulsu.lms.ui.kit.Palette;
import edu.bulsu.lms.ui.kit.StatCard;
import edu.bulsu.lms.ui.kit.Tasks;
import edu.bulsu.lms.ui.kit.UiContext;
import edu.bulsu.lms.ui.kit.Values;
import java.util.List;
import javax.swing.JButton;

/**
 * Books, Digital Books and Other Details share this one screen. The table, the detail card and the
 * BORROW button all ask the {@link LibraryItem} what to do, so there is no {@code if (book) ...
 * else if (ebook)} here: the polymorphism lives in the domain classes.
 */
final class CatalogScreen extends MasterDetailScreen<LibraryItem> {

    private static final String BORROW = "borrow";

    private final ItemType type;
    private final Staff staff;
    private final FormPanel borrowForm;
    private final Field.Fixed borrowCode;
    private final Field.Fixed borrowTitle;
    private final JButton issue;

    CatalogScreen(UiContext ctx, Staff staff, ItemType type) {
        super(ctx, key(type), new StatCard("Total\n" + type.label(), StatCard.Tone.NEUTRAL),
                new ObjectTable<LibraryItem>(key(type) + ".table", type.columns(), LibraryItem::tableRow, widths(type), Palette.LIGHT),
                "Click the \"+\" button to add a new " + noun(type) + " or click a " + noun(type) + " on the left table.",
                "Add " + noun(type), "BORROW", "BACK");
        this.type = type;
        this.staff = staff;
        addCard(WIZARD, ItemWizards.create(ctx, type, this::finish));

        borrowCode = Field.fixed("code", type == ItemType.DIGITAL ? "EPUB ID" : "ISBN");
        borrowTitle = Field.fixed("title", "Title");
        issue = Buttons.primary("ISSUE");
        issue.setName("primary");
        JButton cancel = Buttons.text("CANCEL", Palette.LIGHT.text());
        cancel.setName("secondary");
        borrowForm = new FormPanel(Palette.LIGHT, type == ItemType.DIGITAL ? "Borrow D-Book" : "Borrow Book")
                .row(borrowCode)
                .row(borrowTitle)
                .row(Field.text("studentNumber", "Student Number", Student::requireStudentNumber))
                .buttons(issue, cancel, false);
        addCard(BORROW, borrowForm);
        issue.addActionListener(event -> issue());
        cancel.addActionListener(event -> showHint());

        detail().primary().addActionListener(event -> primaryClicked());
    }

    @Override
    protected List<LibraryItem> load() {
        return ctx.services().catalog().items(type);
    }

    /** Only kinds that can be lent offer BORROW; reference material just offers DONE. */
    @Override
    protected void customizeDetail(LibraryItem item) {
        boolean lendable = item.isBorrowable();
        detail().primary().setText(lendable ? "BORROW" : "DONE");
        detail().secondary().setVisible(lendable);
    }

    private void primaryClicked() {
        LibraryItem selected = table().selected().orElse(null);
        if (selected == null) {
            return;
        }
        if (!selected.isBorrowable()) {
            showHint();
            return;
        }
        // The table may be stale (another librarian could have lent it); ask the database now.
        Tasks.run(ctx, this, () -> ctx.services().catalog().find(selected.getCode()), found -> {
            LibraryItem current = found.orElse(null);
            if (current == null || !current.canBeBorrowedNow()) {
                ctx.messenger().warn(this, current == null
                        ? "This entry no longer exists."
                        : "\"" + current.getTitle() + "\" is currently borrowed.");
                onShow();
                return;
            }
            borrowCode.show(current.getCode());
            borrowTitle.show(current.getTitle());
            borrowForm.fields().stream().filter(f -> f.id().equals("studentNumber")).forEach(Field::reset);
            showCard(BORROW);
            borrowForm.focusFirst();
        });
    }

    private void issue() {
        Values answers;
        try {
            answers = borrowForm.read();
        } catch (UserFacingException problem) {
            ctx.messenger().warn(this, problem.getMessage());
            return;
        }
        issue.setEnabled(false);
        Tasks.run(ctx, this,
                () -> ctx.services().circulation().issue(answers.text("code"), answers.text("studentNumber"), staff),
                loan -> {
                    ctx.messenger().info(this, (type == ItemType.DIGITAL ? "Digital book" : "Book") + " issued successfully");
                    borrowForm.reset();
                    onShow();
                },
                () -> issue.setEnabled(true));
    }

    // ---- per-kind wording and column widths ----

    private static String key(ItemType type) {
        return switch (type) {
            case BOOK -> "books";
            case DIGITAL -> "ebooks";
            case OTHER -> "others";
        };
    }

    private static String noun(ItemType type) {
        return switch (type) {
            case BOOK -> "book";
            case DIGITAL -> "digital book";
            case OTHER -> "other detail";
        };
    }

    private static int[] widths(ItemType type) {
        return switch (type) {
            case BOOK -> new int[]{140, 230, 150, 200, 130, 190, 90, 150};
            case DIGITAL -> new int[]{140, 230, 150, 90, 130, 90, 150, 240};
            case OTHER -> new int[]{150, 260, 150, 210, 90, 100};
        };
    }
}
