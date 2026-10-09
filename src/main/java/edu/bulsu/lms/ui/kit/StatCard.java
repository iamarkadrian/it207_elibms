package edu.bulsu.lms.ui.kit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** A label with a big number, e.g. "Total Books  1,240". Colored tones are used for loan statistics. */
public final class StatCard extends JPanel {

    /** background, tile behind the number, text color */
    public enum Tone {
        NEUTRAL(Theme.INK, new Color(250, 250, 250), new Color(250, 250, 250)),
        ORANGE(Theme.ORANGE, new Color(255, 247, 235), new Color(255, 240, 217)),
        GREEN(Theme.GREEN, new Color(238, 251, 240), new Color(222, 247, 226)),
        RED(Theme.RED, new Color(255, 248, 248), new Color(255, 233, 232));

        private final Color text;
        private final Color background;
        private final Color tile;

        Tone(Color text, Color background, Color tile) {
            this.text = text;
            this.background = background;
            this.tile = tile;
        }
    }

    private final JLabel count = new JLabel("0", SwingConstants.CENTER);

    /** @param label may contain a line break, e.g. "Total\nBooks" */
    public StatCard(String label, Tone tone) {
        super(new BorderLayout(12, 0));
        setBackground(tone.background);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setPreferredSize(new Dimension(332, 96));

        JLabel caption = new JLabel("<html>" + label.replace("\n", "<br>") + "</html>");
        caption.setFont(Theme.font(Font.PLAIN, 24));
        caption.setForeground(tone.text);
        add(caption, BorderLayout.CENTER);

        count.setFont(Theme.font(Font.BOLD, 34));
        count.setForeground(tone.text);
        count.setName("count");
        JPanel tile = new JPanel(new BorderLayout());
        tile.setBackground(tone.tile);
        tile.setPreferredSize(new Dimension(144, 72));
        tile.add(count, BorderLayout.CENTER);
        add(tile, BorderLayout.EAST);
    }

    public void setCount(int value) {
        count.setText(String.format("%,d", value));
    }

    public String countText() {
        return count.getText();
    }
}
