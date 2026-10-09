package edu.bulsu.lms.ui.librarian;

import edu.bulsu.lms.domain.Staff;
import edu.bulsu.lms.service.LibrarySummary;
import edu.bulsu.lms.ui.kit.ImagePanel;
import edu.bulsu.lms.ui.kit.Tasks;
import edu.bulsu.lms.ui.kit.Theme;
import edu.bulsu.lms.ui.kit.UiContext;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * Home screen: a time-of-day greeting, a live clock, and today's numbers.
 *
 * <p>Layout: {@code BorderLayout} for the page, {@code GridBagLayout} for the banner and clock,
 * {@code GridLayout} for the tiles.
 */
final class DashboardScreen extends JPanel implements Screen {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMMM dd yyyy");
    private static final Color TILE = new Color(250, 250, 250);

    /** What the banner says and shows at a given hour. */
    private record Mood(String greeting, String sky, Color text) {

        static Mood at(int hour) {
            Color dark = new Color(31, 31, 31);
            if (hour < 6 || hour >= 21) {
                return new Mood("Good Night", "bg_night.png", Color.WHITE);
            }
            if (hour < 12) {
                return new Mood("Good Morning", "bg_morning.png", dark);
            }
            if (hour < 18) {
                return new Mood("Good Afternoon", "bg_afternoon.png", dark);
            }
            return new Mood("Good Evening", "bg_evening.png", dark);
        }
    }

    private final UiContext ctx;
    private final JLabel time = new JLabel("", SwingConstants.CENTER);
    private final JLabel date = new JLabel("", SwingConstants.CENTER);
    private final Map<String, JLabel> numbers = new LinkedHashMap<>();
    private final Timer clock = new Timer(1000, event -> tick());

    DashboardScreen(UiContext ctx, Staff staff) {
        super(new BorderLayout(0, 48));
        this.ctx = ctx;
        setName("screen.dashboard");
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(48, 48, 48, 48));
        add(topRow(staff), BorderLayout.NORTH);
        add(summaryCard(), BorderLayout.CENTER);
        tick();
    }

    @Override
    public void onShow() {
        Tasks.run(ctx, this, () -> ctx.services().dashboard().summary(), this::show);
    }

    /** The clock only needs to run while the window exists. */
    @Override
    public void addNotify() {
        super.addNotify();
        clock.start();
    }

    @Override
    public void removeNotify() {
        clock.stop();
        super.removeNotify();
    }

    private void tick() {
        LocalDateTime now = LocalDateTime.now();
        time.setText(TIME.format(now));
        date.setText(DATE.format(now));
    }

    private void show(LibrarySummary summary) {
        numbers.get("books").setText(format(summary.books()));
        numbers.get("digital").setText(format(summary.digitalBooks()));
        numbers.get("others").setText(format(summary.otherDetails()));
        numbers.get("members").setText(format(summary.members()));
        numbers.get("issued").setText(format(summary.issued()));
        numbers.get("returned").setText(format(summary.returned()));
        numbers.get("missing").setText(format(summary.missing()));
    }

    private static String format(int value) {
        return String.format("%,d", value);
    }

    // ---- top row: greeting banner and clock ----

    private JPanel topRow(Staff staff) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);

        Mood mood = Mood.at(LocalDateTime.now().getHour());
        JPanel banner = new ImagePanel(mood.sky(), new BorderLayout());
        banner.setPreferredSize(new Dimension(627, 128));
        JLabel greeting = new JLabel("<html>" + mood.greeting() + ",<br>" + Theme.escapeHtml(staff.getFirstName()) + "!</html>");
        greeting.setName("greeting");
        greeting.setFont(Theme.font(Font.PLAIN, 34));
        greeting.setForeground(mood.text());
        greeting.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 0));
        banner.add(greeting, BorderLayout.WEST);

        JPanel face = new JPanel();
        face.setLayout(new BoxLayout(face, BoxLayout.Y_AXIS));
        face.setBackground(Theme.CLOCK_FACE);
        face.setPreferredSize(new Dimension(253, 128));
        time.setName("clock.time");
        time.setFont(Theme.font(Font.PLAIN, 48));
        time.setForeground(Theme.CLOCK_TIME);
        date.setName("clock.date");
        date.setFont(Theme.font(Font.BOLD, 20));
        date.setForeground(Color.WHITE);
        time.setAlignmentX(CENTER_ALIGNMENT);
        date.setAlignmentX(CENTER_ALIGNMENT);
        face.add(Box.createVerticalGlue());
        face.add(time);
        face.add(date);
        face.add(Box.createVerticalGlue());

        GridBagConstraints left = new GridBagConstraints();
        left.weightx = 627;
        left.fill = GridBagConstraints.BOTH;
        row.add(banner, left);
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.weightx = 253;
        right.fill = GridBagConstraints.BOTH;
        right.insets = new Insets(0, 48, 0, 0);
        row.add(face, right);
        return row;
    }

    // ---- summary card ----

    private JPanel summaryCard() {
        JPanel card = new JPanel(new BorderLayout(0, 24));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("Today's Summary");
        title.setFont(Theme.font(Font.PLAIN, 34));
        title.setForeground(Theme.INK);
        card.add(title, BorderLayout.NORTH);

        JPanel big = new JPanel(new GridLayout(2, 2, 24, 24));
        big.setOpaque(false);
        big.add(bigTile("Books", "books"));
        big.add(bigTile("Digital Books", "digital"));
        big.add(bigTile("Other Details", "others"));
        big.add(bigTile("Members", "members"));
        card.add(big, BorderLayout.CENTER);

        JPanel loans = new JPanel(new GridLayout(1, 3, 24, 0));
        loans.setOpaque(false);
        loans.setPreferredSize(new Dimension(10, 96));
        loans.add(loanTile("Issued Books", "issued", Theme.ORANGE, new Color(255, 247, 235), new Color(255, 240, 217)));
        loans.add(loanTile("Returned Books", "returned", Theme.GREEN, new Color(238, 251, 240), new Color(222, 247, 226)));
        loans.add(loanTile("Missing Books", "missing", Theme.RED, new Color(255, 248, 248), new Color(255, 233, 232)));
        card.add(loans, BorderLayout.SOUTH);
        return card;
    }

    private JPanel bigTile(String label, String key) {
        JPanel tile = new JPanel(new BorderLayout());
        tile.setBackground(TILE);
        tile.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 24));
        JLabel caption = new JLabel(label);
        caption.setFont(Theme.font(Font.PLAIN, 24));
        caption.setForeground(Theme.INK);
        caption.setVerticalAlignment(SwingConstants.TOP);
        tile.add(caption, BorderLayout.WEST);
        tile.add(number(key, 48, Theme.INK), BorderLayout.EAST);
        return tile;
    }

    private JPanel loanTile(String label, String key, Color color, Color background, Color square) {
        JPanel tile = new JPanel(new BorderLayout(12, 0));
        tile.setBackground(background);
        tile.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(square);
        box.setPreferredSize(new Dimension(72, 72));
        box.add(number(key, 34, color), BorderLayout.CENTER);
        tile.add(box, BorderLayout.WEST);
        JLabel caption = new JLabel(label);
        caption.setFont(Theme.font(Font.BOLD, 20));
        caption.setForeground(color);
        tile.add(caption, BorderLayout.CENTER);
        return tile;
    }

    private JLabel number(String key, int size, Color color) {
        JLabel label = new JLabel("0", SwingConstants.CENTER);
        label.setName("count." + key);
        label.setFont(Theme.font(Font.BOLD, size));
        label.setForeground(color);
        numbers.put(key, label);
        return label;
    }
}
