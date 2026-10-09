package edu.bulsu.lms.ui;

import edu.bulsu.lms.ui.kit.Theme;
import edu.bulsu.lms.ui.kit.UiContext;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;

/** The one and only top-level window. It just hosts the {@link AppShell}. */
public final class MainWindow extends JFrame {

    public MainWindow(UiContext ctx) {
        super("BulSU Library Management System");
        setIconImage(Theme.icon("bulsuelibrary.png").getImage());

        AppShell shell = new AppShell(ctx);
        shell.setPreferredSize(new Dimension(1280, 720));
        setContentPane(shell);
        pack();
        setMinimumSize(new Dimension(1100, 680));
        setLocationRelativeTo(null);

        // Closing the window asks first: an accidental click should not throw away a half-finished form.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                if (ctx.messenger().confirm(MainWindow.this, "Are you sure you want to exit the program?", "Question")) {
                    dispose();
                    System.exit(0);
                }
            }
        });
    }
}
