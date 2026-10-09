package edu.bulsu.lms.ui.kit;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Font;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

/**
 * The look of the application: colors, fonts and images in one place, so a change of brand means
 * editing one file. The palette follows iOS system colors, as the original design did.
 */
public final class Theme {

    /** Falls back to the JVM's default sans-serif when this font is not installed. */
    public static final String FONT_FAMILY = "SF Pro Display";

    public static final Color BLUE = new Color(0, 122, 255);
    public static final Color BLUE_HOVER = new Color(46, 143, 255);
    public static final Color BLUE_PRESSED = new Color(0, 98, 204);
    public static final Color INK = new Color(33, 33, 33);
    public static final Color MUTED = new Color(142, 142, 147);
    public static final Color NAV_TEXT = new Color(97, 97, 97);
    public static final Color NAV_ACTIVE_BACKGROUND = new Color(235, 245, 255);
    public static final Color RED = new Color(255, 59, 48);
    public static final Color ORANGE = new Color(255, 149, 0);
    public static final Color GREEN = new Color(40, 205, 65);
    public static final Color CLOCK_FACE = new Color(33, 33, 33);
    public static final Color CLOCK_TIME = new Color(255, 235, 59);
    public static final Color LOGIN_FIELD = new Color(238, 238, 238);
    public static final Color TOOLBAR_BUTTON = new Color(56, 56, 56);

    private static final Map<String, ImageIcon> ICONS = new ConcurrentHashMap<>();

    private Theme() {
        // static helpers only
    }

    /** Installs the FlatLaf look and feel. Call once, before any component is created. */
    public static void install() {
        FlatLightLaf.setup();
        UIManager.put("Button.arc", 0); // square corners, as in the original design
    }

    public static Font font(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    /** Loads {@code /images/<file>} from the classpath (cached). */
    public static ImageIcon icon(String file) {
        return ICONS.computeIfAbsent(file, name -> {
            URL url = Theme.class.getResource("/images/" + name);
            if (url == null) {
                throw new IllegalStateException("Missing image: /images/" + name);
            }
            return new ImageIcon(url);
        });
    }

    /**
     * Makes text safe to put inside an HTML label or tooltip. Swing renders any label that starts with
     * {@code <html>} as a web page (including remote images), so data typed by users must be escaped.
     */
    public static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** "#RRGGBB" for FlatLaf style strings. */
    public static String hex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
