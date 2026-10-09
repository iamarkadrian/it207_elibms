package edu.bulsu.lms.ui.kit;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;

/** Factory for the three button styles the design uses. */
public final class Buttons {

    private Buttons() {
        // static helpers only
    }

    /** Solid blue call-to-action, e.g. "NEXT". */
    public static JButton primary(String text) {
        JButton button = new JButton(text);
        button.setFont(Theme.font(Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(Theme.BLUE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("FlatLaf.style", "hoverBackground: " + Theme.hex(Theme.BLUE_HOVER)
                + "; pressedBackground: " + Theme.hex(Theme.BLUE_PRESSED) + "; focusedBackground: " + Theme.hex(Theme.BLUE));
        return button;
    }

    /** Quiet text-only button, e.g. "CANCEL". */
    public static JButton text(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(Theme.font(Font.BOLD, 14));
        button.setForeground(color);
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /** Square icon button for toolbars. */
    public static JButton icon(Icon icon, String tooltip, Color background) {
        JButton button = new JButton(icon);
        button.setToolTipText(tooltip);
        button.getAccessibleContext().setAccessibleName(tooltip);
        button.setBackground(background);
        button.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("FlatLaf.style", "hoverBackground: " + Theme.hex(background.brighter()));
        return button;
    }
}
