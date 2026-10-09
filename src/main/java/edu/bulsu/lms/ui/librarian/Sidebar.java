package edu.bulsu.lms.ui.librarian;

import edu.bulsu.lms.domain.Staff;
import edu.bulsu.lms.ui.kit.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** The white column on the left: who is signed in, the navigation, and the sign-out button. */
final class Sidebar extends JPanel {

    private static final int WIDTH = 256;
    private static final int ITEM_WIDTH = 208;

    private final JPanel nav = new JPanel();
    private final Map<String, NavItem> items = new LinkedHashMap<>();

    Sidebar(Staff staff, Runnable onSignOut) {
        super(new BorderLayout());
        setName("sidebar");
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(WIDTH, 10));

        add(header(staff), BorderLayout.NORTH);

        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setOpaque(false);
        nav.setBorder(BorderFactory.createEmptyBorder(36, 24, 0, 24));
        add(nav, BorderLayout.CENTER);

        add(signOut(onSignOut), BorderLayout.SOUTH);
    }

    /** Appends a navigation entry. {@code icon} is the file prefix, e.g. "members" for members_active.png. */
    void addItem(String key, String label, String icon, Runnable onChosen) {
        NavItem item = new NavItem(key, label, icon);
        item.addActionListener(event -> onChosen.run());
        items.put(key, item);
        if (nav.getComponentCount() > 0) {
            nav.add(Box.createVerticalStrut(8));
        }
        nav.add(item);
    }

    /** Highlights the entry registered under {@code key}. */
    void select(String key) {
        items.forEach((name, item) -> item.setActive(name.equals(key)));
    }

    private static JPanel header(Staff staff) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(48, 24, 0, 24));
        header.add(clipped(staff.getDepartmentCode(), Font.BOLD, 20, Theme.MUTED, "user.department"));
        header.add(clipped(staff.getFirstName(), Font.PLAIN, 34, Theme.INK, "user.firstName"));
        header.add(clipped(staff.getLastName(), Font.PLAIN, 34, Theme.INK, "user.lastName"));
        return header;
    }

    /** A label that ends in an ellipsis instead of spilling out of the sidebar; the tooltip shows the full text. */
    private static JLabel clipped(String text, int style, int size, Color color, String name) {
        JLabel label = new JLabel(text);
        label.setName(name);
        label.setFont(Theme.font(style, size));
        label.setForeground(color);
        label.setToolTipText(text);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(ITEM_WIDTH, label.getPreferredSize().height));
        return label;
    }

    private static JPanel signOut(Runnable onSignOut) {
        JButton button = new JButton("Sign out", Theme.icon("logout_red.png"));
        button.setName("nav.signOut");
        button.setFont(Theme.font(Font.BOLD, 14));
        button.setForeground(Theme.RED);
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        button.setIconTextGap(12);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(event -> onSignOut.run());

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 12, 40, 24));
        panel.add(button, BorderLayout.WEST);
        return panel;
    }

    /** One row of the navigation: icon and label, blue on a pale background when active. */
    private static final class NavItem extends JButton {

        private final String icon;

        NavItem(String key, String label, String icon) {
            super(label);
            this.icon = icon;
            setName("nav." + key);
            setFont(Theme.font(Font.BOLD, 14));
            setHorizontalAlignment(SwingConstants.LEFT);
            setIconTextGap(12);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(LEFT_ALIGNMENT);
            Dimension size = new Dimension(ITEM_WIDTH, 36);
            setPreferredSize(size);
            setMaximumSize(size);
            setActive(false);
        }

        void setActive(boolean active) {
            setForeground(active ? Theme.BLUE : Theme.NAV_TEXT);
            setBackground(active ? Theme.NAV_ACTIVE_BACKGROUND : Color.WHITE);
            setIcon(Theme.icon(icon + (active ? "_active.png" : "_inactive.png")));
            getAccessibleContext().setAccessibleDescription(active ? "current page" : "");
        }
    }
}
