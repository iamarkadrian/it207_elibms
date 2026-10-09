package edu.bulsu.lms.ui.kit;

import edu.bulsu.lms.domain.Displayable;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Shows any {@link Displayable} - a member, a book, a loan - as a heading, a gray sub-heading and
 * lines of text, with two buttons underneath. One class renders every kind of record.
 */
public final class DetailCard extends JPanel {

    /** Swing's HTML renderer scales CSS pixels by about 1.33, so 190 "px" fills the card's 284px text area. */
    private static final int TEXT_WIDTH_PX = 190;

    private final Palette palette;
    private final JLabel title = new JLabel();
    private final JLabel subtitle = new JLabel();
    private final JPanel lines = new JPanel();
    private final JButton primary;
    private final JButton secondary;

    public DetailCard(Palette palette, String primaryText, String secondaryText) {
        super(new BorderLayout());
        this.palette = palette;
        setBackground(palette.card());
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        subtitle.setFont(Theme.font(Font.PLAIN, 20));
        subtitle.setForeground(palette.muted());
        title.setForeground(palette.text());
        title.setName("title");
        subtitle.setName("subtitle");
        align(title);
        align(subtitle);
        header.add(title);
        header.add(subtitle);
        add(header, BorderLayout.NORTH);

        lines.setLayout(new BoxLayout(lines, BoxLayout.Y_AXIS));
        lines.setOpaque(false);
        lines.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));
        lines.setName("lines");
        add(lines, BorderLayout.CENTER);

        primary = Buttons.primary(primaryText);
        primary.setName("primary");
        secondary = Buttons.text(secondaryText, palette.text());
        secondary.setName("secondary");
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        footer.setOpaque(false);
        footer.add(primary);
        footer.add(secondary);
        add(footer, BorderLayout.SOUTH);
    }

    public JButton primary() {
        return primary;
    }

    public JButton secondary() {
        return secondary;
    }

    /** Renders {@code subject}. Long titles wrap and shrink; long lines are cut with a tooltip. */
    public void show(Displayable subject) {
        String heading = subject.title();
        int size = heading.length() <= 12 ? 34 : heading.length() <= 32 ? 26 : 20;
        title.setFont(Theme.font(Font.PLAIN, size));
        title.setText(wrapped(heading));
        subtitle.putClientProperty("html.disable", Boolean.TRUE); // user text is shown as text, never as HTML
        subtitle.setText(subject.subtitle());

        lines.removeAll();
        for (String text : subject.lines()) {
            if (text.isBlank()) {
                lines.add(Box.createVerticalStrut(14));
                continue;
            }
            JLabel line = new JLabel(text);
            line.setFont(Theme.font(Font.PLAIN, 16));
            line.setForeground(palette.text());
            line.putClientProperty("html.disable", Boolean.TRUE);
            line.setToolTipText("<html>" + Theme.escapeHtml(text) + "</html>");
            align(line);
            lines.add(line);
            lines.add(Box.createVerticalStrut(2));
        }
        revalidate();
        repaint();
    }

    /** The visible text of the lines, for tests. */
    public java.util.List<String> lineTexts() {
        return java.util.Arrays.stream(lines.getComponents())
                .filter(c -> c instanceof JLabel)
                .map(c -> ((JLabel) c).getText())
                .toList();
    }

    private static String wrapped(String text) {
        return "<html><div style='width: " + TEXT_WIDTH_PX + "px'>" + Theme.escapeHtml(text) + "</div></html>";
    }

    private static void align(JComponent component) {
        component.setAlignmentX(LEFT_ALIGNMENT);
    }
}
