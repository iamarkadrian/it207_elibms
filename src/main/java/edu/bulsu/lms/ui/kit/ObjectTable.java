package edu.bulsu.lms.ui.kit;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * A read-only table of domain objects. The caller says how an object becomes a row of text; the
 * table keeps the objects, so a click hands back the real object, not a copy of its cells.
 */
public final class ObjectTable<T> extends JScrollPane {

    private final List<String> columns;
    private final Function<T, List<String>> rowOf;
    private final List<T> items = new ArrayList<>();
    private final JTable table;
    private Consumer<T> selectionListener = item -> { };

    /**
     * @param widths preferred column widths in pixels; the table scrolls sideways instead of squeezing
     *               columns until their headings are unreadable
     */
    public ObjectTable(String name, List<String> columns, Function<T, List<String>> rowOf, int[] widths, Palette palette) {
        this.columns = columns;
        this.rowOf = rowOf;

        AbstractTableModel model = new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return items.size();
            }

            @Override
            public int getColumnCount() {
                return ObjectTable.this.columns.size();
            }

            @Override
            public String getColumnName(int column) {
                return ObjectTable.this.columns.get(column);
            }

            @Override
            public Object getValueAt(int row, int column) {
                List<String> cells = ObjectTable.this.rowOf.apply(items.get(row));
                return column < cells.size() ? cells.get(column) : "";
            }
        };

        table = new JTable(model) {
            /** Stretch to fill the viewport when there is room; scroll sideways when there is not. */
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getPreferredSize().width < getParent().getWidth();
            }
        };
        table.setName(name);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(32);
        table.setShowVerticalLines(false);
        table.setFont(Theme.font(java.awt.Font.PLAIN, 14));
        table.setBackground(palette.dark() ? palette.card() : Color.WHITE);
        table.setForeground(palette.dark() ? Color.WHITE : Theme.INK);
        table.setGridColor(palette.dark() ? new Color(55, 55, 55) : new Color(232, 232, 232));
        table.setSelectionBackground(Theme.BLUE);
        table.setSelectionForeground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(10, 32));
        table.getTableHeader().setFont(Theme.font(java.awt.Font.PLAIN, 13));

        centerHeader();
        table.setDefaultRenderer(Object.class, new TooltipRenderer());
        for (int i = 0; i < widths.length && i < columns.size(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        table.getSelectionModel().addListSelectionListener(event -> {
            int viewRow = table.getSelectedRow();
            if (!event.getValueIsAdjusting() && viewRow >= 0) {
                selectionListener.accept(items.get(table.convertRowIndexToModel(viewRow)));
            }
        });

        setViewportView(table);
        // A JTable only gets its header row automatically once it is on screen; install it explicitly so the
        // headings are always there (and so tests and screenshots, which have no window, see them too).
        setColumnHeaderView(table.getTableHeader());
        setBorder(BorderFactory.createLineBorder(palette.dark() ? palette.card() : new Color(200, 200, 200)));
        getViewport().setBackground(table.getBackground());
    }

    public void setItems(List<? extends T> newItems) {
        items.clear();
        items.addAll(newItems);
        ((AbstractTableModel) table.getModel()).fireTableDataChanged();
    }

    public List<T> items() {
        return List.copyOf(items);
    }

    /** Called with the clicked (or keyboard-selected) object. */
    public void onSelect(Consumer<T> listener) {
        this.selectionListener = listener;
    }

    public Optional<T> selected() {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? Optional.empty() : Optional.of(items.get(table.convertRowIndexToModel(viewRow)));
    }

    /** Forget the selection so that clicking the same row again opens it again. */
    public void clearSelection() {
        table.clearSelection();
    }

    public JTable table() {
        return table;
    }

    private void centerHeader() {
        TableCellRenderer base = table.getTableHeader().getDefaultRenderer();
        table.getTableHeader().setDefaultRenderer((tbl, value, selected, focus, row, column) -> {
            Component component = base.getTableCellRendererComponent(tbl, value, selected, focus, row, column);
            if (component instanceof JLabel label) {
                label.setHorizontalAlignment(SwingConstants.CENTER);
            }
            return component;
        });
    }

    /** Pads cells and shows the full text as a tooltip when a column is too narrow for it. */
    private static final class TooltipRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            // Titles and authors are typed by people: show them as text, never as HTML.
            putClientProperty("html.disable", Boolean.TRUE);
            setToolTipText(value == null ? null : "<html>" + Theme.escapeHtml(value.toString()) + "</html>");
            return this;
        }
    }
}
