package edu.bulsu.lms.testsupport;

import edu.bulsu.lms.ui.kit.Tasks;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

/**
 * Drives real Swing components the way a user would, but without opening a window, so the GUI
 * tests also run on a build server with no display.
 *
 * <p>Components are found by the {@code name} the application gives them. Only components that are
 * currently visible count: a card hidden by a {@code CardLayout} is not found, just as it would not
 * be clickable on screen.
 */
public final class SwingTestKit {

    private SwingTestKit() {
    }

    // ---------- finding ----------

    public static <T extends Component> List<T> findAll(Container root, String name, Class<T> type) {
        return onEdtResult(() -> {
            List<T> found = new ArrayList<>();
            collect(root, name, type, found);
            return found;
        });
    }

    public static <T extends Component> T find(Container root, String name, Class<T> type) {
        List<T> found = findAll(root, name, type);
        if (found.isEmpty()) {
            throw new AssertionError("No visible " + type.getSimpleName() + " named '" + name + "' under " + root.getName());
        }
        return found.get(0);
    }

    public static boolean exists(Container root, String name) {
        return !findAll(root, name, Component.class).isEmpty();
    }

    private static <T extends Component> void collect(Component node, String name, Class<T> type, List<T> found) {
        if (!node.isVisible()) {
            return;
        }
        if (name.equals(node.getName()) && type.isInstance(node)) {
            found.add(type.cast(node));
        }
        if (node instanceof Container container) {
            for (Component child : container.getComponents()) {
                collect(child, name, type, found);
            }
        }
    }

    // ---------- acting ----------

    public static void click(Container root, String name) {
        AbstractButton button = find(root, name, AbstractButton.class);
        if (!button.isEnabled()) {
            throw new AssertionError("Button '" + name + "' is disabled");
        }
        onEdt(() -> button.doClick(0));
    }

    public static void type(Container root, String name, String text) {
        JTextComponent field = find(root, name, JTextComponent.class);
        onEdt(() -> field.setText(text));
    }

    /** Picks the drop-down entry whose text contains {@code text}. */
    public static void choose(Container root, String name, String text) {
        JComboBox<?> combo = find(root, name, JComboBox.class);
        onEdt(() -> {
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (String.valueOf(combo.getItemAt(i)).contains(text)) {
                    combo.setSelectedIndex(i);
                    return;
                }
            }
            throw new AssertionError("'" + text + "' is not an option of " + name);
        });
    }

    /** Selects the first row of a table (in the order shown) that has a cell containing {@code text}. */
    public static void selectRow(Container root, String tableName, String text) {
        JTable table = find(root, tableName, JTable.class);
        onEdt(() -> {
            for (int row = 0; row < table.getRowCount(); row++) {
                for (int column = 0; column < table.getColumnCount(); column++) {
                    if (String.valueOf(table.getValueAt(row, column)).contains(text)) {
                        table.setRowSelectionInterval(row, row);
                        return;
                    }
                }
            }
            throw new AssertionError("No row containing '" + text + "' in " + tableName);
        });
    }

    public static void selectRow(Container root, String tableName, int row) {
        JTable table = find(root, tableName, JTable.class);
        onEdt(() -> table.setRowSelectionInterval(row, row));
    }

    /** Waits for every background task started by the UI (and anything those started). */
    public static void settle() {
        try {
            Tasks.awaitIdle();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for the UI", e);
        } catch (InvocationTargetException e) {
            throw new AssertionError(e.getCause());
        }
    }

    // ---------- reading ----------

    public static String text(Container root, String name) {
        Component component = find(root, name, Component.class);
        return onEdtResult(() -> {
            if (component instanceof JLabel label) {
                return label.getText();
            }
            if (component instanceof JTextComponent field) {
                return field.getText();
            }
            if (component instanceof AbstractButton button) {
                return button.getText();
            }
            throw new AssertionError(name + " has no text");
        });
    }

    public static int rowCount(Container root, String tableName) {
        JTable table = find(root, tableName, JTable.class);
        return onEdtResult(table::getRowCount);
    }

    public static String cell(Container root, String tableName, int row, int column) {
        JTable table = find(root, tableName, JTable.class);
        return onEdtResult(() -> String.valueOf(table.getValueAt(row, column)));
    }

    // ---------- pictures ----------

    /** Lays the component out at the given size and paints it, with no window involved. */
    public static BufferedImage render(Component root, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        onEdt(() -> {
            root.setSize(width, height);
            // Without a window nothing is "valid", so Swing's usual upward invalidation stops at once and
            // layout managers such as BoxLayout keep stale cached sizes. Invalidate everything explicitly.
            invalidateTree(root);
            layoutTree(root);
            layoutTree(root); // a second pass settles components whose size depends on their parent's
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            root.paint(g);
            g.dispose();
        });
        return image;
    }

    public static void save(BufferedImage image, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "png", file.toFile());
    }

    private static void invalidateTree(Component component) {
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                invalidateTree(child);
            }
        }
        component.invalidate();
    }

    private static void layoutTree(Component component) {
        if (component instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                layoutTree(child);
            }
        }
    }

    // ---------- the event-dispatch thread ----------

    public static void onEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw new AssertionError(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted", e);
        }
    }

    public static <T> T onEdtResult(java.util.function.Supplier<T> supplier) {
        List<T> holder = new ArrayList<>(1);
        onEdt(() -> holder.add(supplier.get()));
        return holder.get(0);
    }
}
