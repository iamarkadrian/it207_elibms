package edu.bulsu.lms.ui.kit;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * A card with a heading, rows of labelled {@link Field}s and a button bar.
 *
 * <p>Layout: {@code BorderLayout} for heading / body / buttons, {@code GridBagLayout} for the rows
 * (so two fields can share a row with different widths), {@code FlowLayout} for the buttons.
 */
public final class FormPanel extends JPanel {

    private final Palette palette;
    private final JLabel title;
    private final JPanel body = new JPanel(new GridBagLayout());
    private final JPanel footer = new JPanel();
    private final List<Field<?>> fields = new ArrayList<>();
    private JButton primary;
    private int row;

    public FormPanel(Palette palette, String heading) {
        super(new BorderLayout());
        this.palette = palette;
        setBackground(palette.card());
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        title = new JLabel(heading);
        title.setFont(Theme.font(Font.PLAIN, 34));
        title.setForeground(palette.text());
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        title.setName("heading");
        title.setVisible(!heading.isBlank()); // a form may supply its own heading elsewhere
        add(title, BorderLayout.NORTH);

        body.setOpaque(false);
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = 1000;
        filler.weighty = 1;
        body.add(new JPanel(), filler);
        ((JPanel) body.getComponent(0)).setOpaque(false);
        add(body, BorderLayout.CENTER);

        footer.setOpaque(false);
        add(footer, BorderLayout.SOUTH);
    }

    /** A row whose fields share the width equally. */
    public FormPanel row(Field<?>... rowFields) {
        double[] equal = new double[rowFields.length];
        Arrays.fill(equal, 1.0);
        return row(equal, rowFields);
    }

    /** A row whose fields share the width in proportion to {@code weights}. */
    public FormPanel row(double[] weights, Field<?>... rowFields) {
        // Every row gets its own grid, so a two-field row is not forced into the columns of a three-field row.
        JPanel line = new JPanel(new GridBagLayout());
        line.setOpaque(false);
        for (int i = 0; i < rowFields.length; i++) {
            Field<?> field = rowFields[i];
            field.style(palette);
            field.onEnter(this::submit);
            fields.add(field);

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = i;
            c.weightx = weights[i];
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTH;
            c.insets = new Insets(0, i == 0 ? 0 : 24, 0, 0);
            line.add(cell(field), c);
        }
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets(0, 0, 20, 0);
        body.add(line, c);
        row++;
        return this;
    }

    /** Puts the main and the secondary button in the button bar; Enter in any text box clicks {@code primary}. */
    public FormPanel buttons(JButton primaryButton, JButton secondaryButton, boolean alignRight) {
        this.primary = primaryButton;
        footer.setLayout(new FlowLayout(alignRight ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        footer.add(primaryButton);
        if (secondaryButton != null) {
            footer.add(secondaryButton);
        }
        return this;
    }

    /** Like {@link #buttons} but the button follows the last row instead of sticking to the bottom of the card. */
    public FormPanel buttonsInline(JButton primaryButton) {
        buttons(primaryButton, null, false);
        remove(footer);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(8, 0, 0, 0);
        body.add(footer, c);
        return this;
    }

    /** The main button of this form, so a caller can disable it while a task runs. */
    public JButton primary() {
        return primary;
    }

    /** Validates every field and returns the answers by field id. @throws edu.bulsu.lms.domain.UserFacingException */
    public Values read() {
        Values values = new Values();
        for (Field<?> field : fields) {
            values.put(field.id(), field.value());
        }
        return values;
    }

    /** True while the user has typed nothing in any text box. */
    public boolean isEmpty() {
        return fields.stream().allMatch(Field::isEmpty);
    }

    public void reset() {
        fields.forEach(Field::reset);
    }

    public List<Field<?>> fields() {
        return List.copyOf(fields);
    }

    /** Moves the cursor into the first box that can be typed in. */
    public void focusFirst() {
        SwingUtilities.invokeLater(() -> fields.stream()
                .map(Field::component)
                .filter(c -> c.isFocusable() && c.isEnabled())
                .findFirst()
                .ifPresent(java.awt.Component::requestFocusInWindow));
    }

    private void submit() {
        if (primary != null && primary.isEnabled()) {
            primary.doClick();
        }
    }

    private JPanel cell(Field<?> field) {
        JLabel label = new JLabel(field.label());
        label.setFont(Theme.font(Font.PLAIN, 14));
        label.setForeground(palette.text());
        label.setLabelFor(field.component());

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(label, BorderLayout.WEST);
        if (!field.hint().isEmpty()) {
            JLabel hint = new JLabel(field.hint());
            hint.setFont(Theme.font(Font.PLAIN, 12));
            hint.setForeground(palette.hint());
            header.add(hint, BorderLayout.EAST);
        }

        // Report a tiny preferred width so GridBagLayout splits the row purely by the weights,
        // not by how long each label happens to be.
        JPanel cell = new JPanel(new BorderLayout(0, 8)) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(1, super.getPreferredSize().height);
            }

            @Override
            public Dimension getMinimumSize() {
                return new Dimension(1, super.getMinimumSize().height);
            }
        };
        cell.setOpaque(false);
        cell.add(header, BorderLayout.NORTH);
        cell.add(field.component(), BorderLayout.CENTER);
        return cell;
    }
}
