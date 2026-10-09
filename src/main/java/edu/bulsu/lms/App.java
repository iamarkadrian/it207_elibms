package edu.bulsu.lms;

import edu.bulsu.lms.config.AppConfig;
import edu.bulsu.lms.persistence.DataAccessException;
import edu.bulsu.lms.persistence.Database;
import edu.bulsu.lms.service.Services;
import edu.bulsu.lms.ui.MainWindow;
import edu.bulsu.lms.ui.kit.Messenger;
import edu.bulsu.lms.ui.kit.SwingMessenger;
import edu.bulsu.lms.ui.kit.Theme;
import edu.bulsu.lms.ui.kit.UiContext;
import java.time.Clock;
import javax.swing.SwingUtilities;

/** Application entry point: read the settings, check the database, open the window. */
public final class App {

    private static final System.Logger LOG = System.getLogger(App.class.getName());

    private App() {
        // not instantiable
    }

    public static void main(String[] args) {
        // Everything that touches Swing, including start-up error dialogs, belongs on the event thread.
        SwingUtilities.invokeLater(App::start);
    }

    private static void start() {
        Theme.install();
        Messenger messenger = new SwingMessenger();

        AppConfig config;
        try {
            config = AppConfig.load();
        } catch (IllegalArgumentException | IllegalStateException e) {
            messenger.error(null, "The database settings are not usable.\n\n" + e.getMessage());
            System.exit(1);
            return;
        }
        LOG.log(System.Logger.Level.INFO, "Starting with a {0} database", config.vendor());

        Database database = new Database(config);
        try {
            database.verifyConnection();
        } catch (DataAccessException e) {
            messenger.error(null, "Cannot connect to the " + config.vendor() + " database.\n\n"
                    + "Check that the database server is running and that the settings in lms.properties "
                    + "(or the LMS_DB_URL, LMS_DB_USER and LMS_DB_PASSWORD environment variables) are correct. "
                    + "See the README, section \"Configure the connection\".");
            System.exit(1);
            return;
        }

        UiContext context = new UiContext(Services.create(database, Clock.systemDefaultZone()), messenger);
        new MainWindow(context).setVisible(true);
    }
}
