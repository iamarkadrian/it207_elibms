package edu.bulsu.lms.ui.kit;

import edu.bulsu.lms.domain.UserFacingException;
import edu.bulsu.lms.service.Services;
import java.awt.Component;

/**
 * What every screen needs: the services (business logic) and the messenger (dialogs). Passed down
 * through constructors, so no screen reaches for global state.
 */
public record UiContext(Services services, Messenger messenger) {

    private static final System.Logger LOG = System.getLogger(UiContext.class.getName());

    /**
     * Shows a failure the right way: rule violations and bad input are explained plainly; anything
     * else is logged in full and shown as a generic apology, so SQL or stack traces never reach the screen.
     */
    public void report(Component parent, Throwable failure) {
        if (failure instanceof UserFacingException expected) {
            messenger.warn(parent, expected.getMessage());
            return;
        }
        LOG.log(System.Logger.Level.ERROR, "Unexpected failure", failure);
        messenger.error(parent, "The action could not be completed. Please try again, and tell the "
                + "administrator if it keeps happening.\n(The technical details were written to the log.)");
    }
}
