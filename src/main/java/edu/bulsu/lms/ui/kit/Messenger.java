package edu.bulsu.lms.ui.kit;

import java.awt.Component;

/**
 * Everything the app says to the user in a dialog. The UI talks to this interface, not to
 * {@code JOptionPane}, so tests can supply a recording stand-in that never blocks.
 */
public interface Messenger {

    /** Good news: "Member added successfully". */
    void info(Component parent, String message);

    /** The user can fix this: invalid input, a rule that forbids the action. */
    void warn(Component parent, String message);

    /** Something broke that the user cannot fix. */
    void error(Component parent, String message);

    /** A yes/no question. @return true for yes */
    boolean confirm(Component parent, String message, String title);

    /** Asks for one line of text. @return the text, or {@code null} if the user cancelled */
    String prompt(Component parent, String message, String title);
}
