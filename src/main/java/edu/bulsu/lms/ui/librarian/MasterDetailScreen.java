package edu.bulsu.lms.ui.librarian;

import edu.bulsu.lms.domain.Displayable;
import edu.bulsu.lms.ui.kit.DetailCard;
import edu.bulsu.lms.ui.kit.HintCard;
import edu.bulsu.lms.ui.kit.ObjectTable;
import edu.bulsu.lms.ui.kit.Palette;
import edu.bulsu.lms.ui.kit.StatCard;
import edu.bulsu.lms.ui.kit.Tasks;
import edu.bulsu.lms.ui.kit.UiContext;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * The shape shared by Members, Books, Digital Books, Other Details, Issued, Returned and Missing:
 * a table on the left, and on the right a statistic and a card that is either an idle hint, a
 * detail view of the clicked row, or something the subclass adds (a wizard, a form).
 *
 * <p>Subclasses say how to load the rows and what the buttons do; everything else lives here once.
 *
 * @param <T> the kind of record in the table
 */
abstract class MasterDetailScreen<T extends Displayable> extends JPanel implements Screen {

    static final String HINT = "hint";
    static final String DETAIL = "detail";
    static final String WIZARD = "wizard";

    protected final UiContext ctx;

    private final ObjectTable<T> table;
    private final StatCard stat;
    private final CardLayout cards = new CardLayout();
    private final JPanel side = new JPanel(cards);
    private final DetailCard detail;

    /**
     * @param addTooltip text for the round "+" button, or {@code null} for screens that cannot add records
     */
    protected MasterDetailScreen(UiContext ctx, String name, StatCard stat, ObjectTable<T> table, String hintText,
                                 String addTooltip, String primaryText, String secondaryText) {
        super(new GridBagLayout());
        this.ctx = ctx;
        this.table = table;
        this.stat = stat;
        setName("screen." + name);
        setOpaque(false);

        detail = new DetailCard(Palette.LIGHT, primaryText, secondaryText);
        detail.setName("detail");
        detail.secondary().addActionListener(event -> showHint());

        side.setOpaque(false);
        side.add(new HintCard(Palette.LIGHT, hintText, addTooltip, addTooltip == null ? null : this::addRequested), HINT);
        side.add(detail, DETAIL);
        table.onSelect(this::showDetail);

        JPanel right = new JPanel(new BorderLayout(0, 24));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(332, 10));
        right.add(stat, BorderLayout.NORTH);
        right.add(side, BorderLayout.CENTER);

        GridBagConstraints left = new GridBagConstraints();
        left.weightx = 1;
        left.weighty = 1;
        left.fill = GridBagConstraints.BOTH;
        left.insets = new Insets(48, 48, 48, 0);
        add(table, left);

        GridBagConstraints rightSide = new GridBagConstraints();
        rightSide.gridx = 1;
        rightSide.weighty = 1;
        rightSide.fill = GridBagConstraints.VERTICAL;
        rightSide.insets = new Insets(48, 48, 48, 48);
        add(right, rightSide);
    }

    /** Runs on a background thread: fetch the rows. */
    protected abstract List<T> load() throws Exception;

    /** Adjust the detail card for {@code item}, e.g. hide a button that does not apply. */
    protected void customizeDetail(T item) {
    }

    /** The "+" button was pressed. The default opens the wizard card. */
    protected void addRequested() {
        showCard(WIZARD);
    }

    @Override
    public final void onShow() {
        showHint();
        reload();
    }

    protected final void reload() {
        Tasks.run(ctx, this, this::load, items -> {
            table.setItems(items);
            stat.setCount(items.size());
        });
    }

    protected final void showHint() {
        table.clearSelection();
        cards.show(side, HINT);
    }

    /** Back to the idle card and reload the table and the statistic: use after anything that changed data. */
    protected final void finish() {
        showHint();
        reload();
    }

    protected final void showCard(String name) {
        cards.show(side, name);
    }

    protected final void addCard(String name, JComponent card) {
        side.add(card, name);
    }

    protected final ObjectTable<T> table() {
        return table;
    }

    protected final DetailCard detail() {
        return detail;
    }

    private void showDetail(T item) {
        detail.show(item);
        customizeDetail(item);
        cards.show(side, DETAIL);
    }
}
