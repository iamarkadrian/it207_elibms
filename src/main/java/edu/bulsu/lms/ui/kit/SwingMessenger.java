package edu.bulsu.lms.ui.kit;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/** The real thing: shows modal {@link JOptionPane} dialogs. */
public final class SwingMessenger implements Messenger {

    private static final String TITLE = "BulSU Library";

    @Override
    public void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, wrap(message), TITLE, JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, wrap(message), "Please check", JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, wrap(message), "Something went wrong", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public boolean confirm(Component parent, String message, String title) {
        int answer = JOptionPane.showConfirmDialog(parent, wrap(message), title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return answer == JOptionPane.YES_OPTION;
    }

    @Override
    public String prompt(Component parent, String message, String title) {
        return JOptionPane.showInputDialog(parent, wrap(message), title, JOptionPane.QUESTION_MESSAGE);
    }

    /** Long sentences would otherwise stretch the dialog across the whole screen. */
    private static JLabel wrap(String message) {
        String escaped = Theme.escapeHtml(message).replace("\n", "<br>");
        return new JLabel("<html><div style='width: 340px'>" + escaped + "</div></html>");
    }
}
