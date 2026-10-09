package edu.bulsu.lms.ui.librarian;

import edu.bulsu.lms.domain.Loan;
import edu.bulsu.lms.service.LoanView;
import edu.bulsu.lms.ui.kit.ObjectTable;
import edu.bulsu.lms.ui.kit.Palette;
import edu.bulsu.lms.ui.kit.StatCard;
import edu.bulsu.lms.ui.kit.Tasks;
import edu.bulsu.lms.ui.kit.UiContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Issued, Returned and Missing share this read-mostly screen; the only action is returning a book.
 * "Missing" means not returned and past the due date.
 */
final class LoansScreen extends MasterDetailScreen<Loan> {

    private final LoanView view;

    LoansScreen(UiContext ctx, LoanView view) {
        super(ctx, key(view), new StatCard(switch (view) {
                    case ISSUED -> "Issued\nBooks";
                    case RETURNED -> "Returned\nBooks";
                    case MISSING -> "Missing\nBooks";
                }, switch (view) {
                    case ISSUED -> StatCard.Tone.ORANGE;
                    case RETURNED -> StatCard.Tone.GREEN;
                    case MISSING -> StatCard.Tone.RED;
                }),
                new ObjectTable<Loan>(key(view) + ".table", columns(view), loan -> cells(view, loan), widths(view), Palette.LIGHT),
                hint(view), null, view == LoanView.RETURNED ? "DONE" : "RETURN", view == LoanView.RETURNED ? "BACK" : "CANCEL");
        this.view = view;
        detail().primary().addActionListener(event -> primaryClicked());
    }

    @Override
    protected List<Loan> load() {
        return ctx.services().circulation().loans(view);
    }

    /** A returned loan has nothing left to do, so it offers a single DONE button. */
    @Override
    protected void customizeDetail(Loan loan) {
        detail().secondary().setVisible(view != LoanView.RETURNED);
    }

    private void primaryClicked() {
        if (view == LoanView.RETURNED) {
            showHint();
            return;
        }
        table().selected().ifPresent(loan -> {
            if (!ctx.messenger().confirm(this, "Mark \"" + loan.itemTitle() + "\" as returned?", "Question")) {
                return;
            }
            Tasks.run(ctx, this, () -> ctx.services().circulation().giveBack(loan.id()), returned -> {
                ctx.messenger().info(this, "Book returned successfully");
                onShow();
            }, this::reload); // also after a failure: someone else may have just returned it, so show the truth
        });
    }

    private static String key(LoanView view) {
        return switch (view) {
            case ISSUED -> "issued";
            case RETURNED -> "returned";
            case MISSING -> "missing";
        };
    }

    private static String hint(LoanView view) {
        return switch (view) {
            case ISSUED -> "Click an issue record on the left table to see more information and to mark an issue as returned.";
            case RETURNED -> "Click a record on the left table to see the details of a returned book.";
            case MISSING -> "Click a missing book on the left table to see who borrowed it and to mark it as returned.";
        };
    }

    private static List<String> columns(LoanView view) {
        List<String> columns = new ArrayList<>(List.of("Issue No", "Item Code", "Title", "Student Name", "Course", "Issued", "Due"));
        if (view == LoanView.RETURNED) {
            columns.add("Returned");
        }
        return columns;
    }

    private static int[] widths(LoanView view) {
        return view == LoanView.RETURNED
                ? new int[]{70, 150, 230, 170, 80, 100, 100, 100}
                : new int[]{70, 150, 230, 170, 80, 100, 100};
    }

    private static List<String> cells(LoanView view, Loan loan) {
        List<String> cells = new ArrayList<>(List.of(String.valueOf(loan.id()), loan.itemCode(), loan.itemTitle(),
                loan.studentName(), loan.courseCode(), loan.issuedText(), loan.dueText()));
        if (view == LoanView.RETURNED) {
            cells.add(loan.returnedText());
        }
        return cells;
    }
}
