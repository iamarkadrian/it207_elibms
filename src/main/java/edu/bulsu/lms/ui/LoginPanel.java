package edu.bulsu.lms.ui;

import edu.bulsu.lms.domain.Staff;
import edu.bulsu.lms.service.LoginResult;
import edu.bulsu.lms.ui.kit.Buttons;
import edu.bulsu.lms.ui.kit.Field;
import edu.bulsu.lms.ui.kit.FormPanel;
import edu.bulsu.lms.ui.kit.Palette;
import edu.bulsu.lms.ui.kit.Tasks;
import edu.bulsu.lms.ui.kit.Theme;
import edu.bulsu.lms.ui.kit.UiContext;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * The single sign-in screen for librarians and the administrator.
 *
 * <p>Layout: {@code BorderLayout} - the form on the left, the library photo on the right.
 */
public final class LoginPanel extends JPanel {

    static final String NOT_MATCHED = "Your username and password do not match, or the account is restricted. "
            + "If you think it was locked after too many attempts, ask your library administrator to unlock it.";

    private final UiContext ctx;
    private final Consumer<Staff> onSignedIn;
    private final Field.Text<String> username = Field.text("username", "Username", text -> text);
    private final Field.Secret password = Field.secret("password", "Password", false);
    private final JButton signIn = Buttons.primary("SIGN IN");

    public LoginPanel(UiContext ctx, Consumer<Staff> onSignedIn) {
        super(new BorderLayout());
        this.ctx = ctx;
        this.onSignedIn = onSignedIn;
        setName("login");
        setBackground(Color.WHITE);

        signIn.setName("signIn");
        signIn.addActionListener(event -> signIn());

        FormPanel form = new FormPanel(Palette.LOGIN, "")
                .row(username)
                .row(password)
                .buttonsInline(signIn);

        JLabel university = new JLabel("BULACAN STATE UNIVERSITY");
        university.setFont(Theme.font(Font.BOLD, 20));
        university.setForeground(Theme.MUTED);

        // The heading sits on a FlowLayout row so it keeps its natural width: at other display scales the
        // text can be wider than the 400px form column, and it must not be cut short with an ellipsis.
        JLabel heading = new JLabel("Library Management System");
        heading.setFont(Theme.font(Font.PLAIN, 34));
        heading.setForeground(Theme.INK);
        heading.setName("heading");
        JPanel headingRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headingRow.setOpaque(false);
        headingRow.add(heading);

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        titles.setBorder(BorderFactory.createEmptyBorder(0, 24, 100, 0));
        university.setAlignmentX(LEFT_ALIGNMENT);
        headingRow.setAlignmentX(LEFT_ALIGNMENT);
        titles.add(university);
        titles.add(Box.createVerticalStrut(4));
        titles.add(headingRow);

        JLabel audience = new JLabel("Administrators and librarians sign in here.");
        audience.setFont(Theme.font(Font.PLAIN, 14));
        audience.setForeground(Theme.MUTED);
        audience.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 0));

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.setBorder(BorderFactory.createEmptyBorder(48, 24, 48, 0));
        left.setPreferredSize(new Dimension(510, 10));
        left.add(titles, BorderLayout.NORTH);
        left.add(form, BorderLayout.CENTER);
        left.add(audience, BorderLayout.SOUTH);
        add(left, BorderLayout.WEST);

        JLabel picture = new JLabel(Theme.icon("bulsuelibrary.png"));
        picture.setVerticalAlignment(SwingConstants.TOP);
        picture.setHorizontalAlignment(SwingConstants.RIGHT);
        picture.setBorder(BorderFactory.createEmptyBorder(47, 0, 0, 48));
        picture.getAccessibleContext().setAccessibleName("Bulacan State University library building");
        add(picture, BorderLayout.CENTER);
    }

    /** Clears the boxes, e.g. after signing out. */
    public void reset() {
        username.reset();
        password.reset();
        username.component().requestFocusInWindow();
    }

    private void signIn() {
        String user = username.value();
        char[] secret = password.value();
        signIn.setEnabled(false);
        Tasks.run(ctx, this, () -> ctx.services().auth().login(user, secret), this::handle, () -> {
            signIn.setEnabled(true);
            Arrays.fill(secret, '\0'); // do not keep the password in memory longer than needed
        });
    }

    private void handle(LoginResult result) {
        switch (result) {
            case LoginResult.Success(Staff staff) -> {
                reset();
                onSignedIn.accept(staff);
            }
            case LoginResult.InvalidCredentials() -> {
                password.reset();
                ctx.messenger().warn(this, NOT_MATCHED);
            }
            // A locked account gets exactly the message of a wrong password, so this screen cannot be used to
            // find out which usernames exist or which accounts are locked. The hint tells the real owner what to do.
            case LoginResult.Locked locked -> {
                password.reset();
                ctx.messenger().warn(this, NOT_MATCHED);
            }
            case LoginResult.Throttled(int secondsLeft) -> ctx.messenger().error(this,
                    "You entered three incorrect usernames or passwords in a row. Please wait "
                            + secondsLeft + " seconds and try again.");
        }
    }
}
