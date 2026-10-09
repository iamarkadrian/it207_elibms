package edu.bulsu.lms.ui.kit;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** The idle state of a side panel: a gentle instruction, and optionally the round "+" button. */
public final class HintCard extends JPanel {

    /** @param palette colors for the card; @param addTooltip null for read-only screens (no "+" button) */
    public HintCard(Palette palette, String text, String addTooltip, Runnable onAdd) {
        super(new BorderLayout());
        setBackground(palette.card());
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel message = new JLabel("<html><div style='width: 200px; text-align: center'>" + text + "</div></html>",
                SwingConstants.CENTER);
        message.setFont(Theme.font(Font.BOLD, 14));
        message.setForeground(palette.dark() ? palette.text() : palette.hint());
        message.setName("hint");
        add(message, BorderLayout.CENTER);

        if (onAdd != null) {
            JButton add = new JButton(Theme.icon("btn_plus.png"));
            add.setName("add");
            add.setToolTipText(addTooltip);
            add.getAccessibleContext().setAccessibleName(addTooltip);
            add.setContentAreaFilled(false);
            add.setBorderPainted(false);
            add.setFocusPainted(true);
            add.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            add.addActionListener(e -> onAdd.run());
            JPanel corner = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            corner.setOpaque(false);
            corner.add(add);
            add(corner, BorderLayout.SOUTH);
        }
    }
}
