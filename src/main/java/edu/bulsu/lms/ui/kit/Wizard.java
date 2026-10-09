package edu.bulsu.lms.ui.kit;

import edu.bulsu.lms.domain.Displayable;
import edu.bulsu.lms.domain.UserFacingException;
import java.awt.CardLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * A multi-step "add something" flow: several {@link FormPanel} steps, then a review card.
 *
 * <ul>
 *   <li>Each step validates its own fields; a wrong value stops you on that step with a dialog.</li>
 *   <li>A step may also run a {@link Check} against the database (for example "is this ISBN taken?").</li>
 *   <li>The review card shows the finished domain object; CONFIRM saves it, EDIT goes back to step one.</li>
 *   <li>CANCEL asks "are you sure?" only if something was typed.</li>
 * </ul>
 * One class drives the member, book, e-book and other-material wizards; they differ only in the
 * steps and the two lambdas they pass in.
 */
public final class Wizard extends JPanel {

    /** Extra validation that needs the database. Throw a {@link UserFacingException} to stop the user. */
    @FunctionalInterface
    public interface Check {
        void run(Values values) throws Exception;
    }

    /** Saves the finished record and returns the success message to show. */
    @FunctionalInterface
    public interface Confirm {
        String run(Values values) throws Exception;
    }

    private record Step(FormPanel form, JButton primary, Check check) {
    }

    private static final String REVIEW = "review";

    private final UiContext ctx;
    private final Palette palette;
    private final Runnable onExit;
    private final CardLayout cards = new CardLayout();
    private final List<Step> steps = new ArrayList<>();
    private final Values values = new Values();
    /** Bumped by every restart, so the answer to a slow check started before CANCEL is ignored. */
    private int generation;
    private DetailCard review;
    private Function<Values, Displayable> preview;
    private Confirm confirm;

    /** @param onExit called after a finished or cancelled wizard, so the screen can return to its idle state */
    public Wizard(UiContext ctx, Palette palette, Runnable onExit) {
        super();
        this.ctx = ctx;
        this.palette = palette;
        this.onExit = onExit;
        setLayout(cards);
        setOpaque(false);
    }

    /** Adds a step. {@code check} may be {@code null}. */
    public Wizard step(FormPanel form, String primaryText, Check check) {
        int index = steps.size();
        JButton primary = Buttons.primary(primaryText);
        primary.setName("primary");
        JButton cancel = Buttons.text("CANCEL", palette.text());
        cancel.setName("secondary");
        primary.addActionListener(event -> next(index));
        cancel.addActionListener(event -> cancel());
        form.buttons(primary, cancel, false);
        steps.add(new Step(form, primary, check));
        add(form, "step" + index);
        return this;
    }

    /**
     * Adds the final review card.
     *
     * @param preview builds the record from the collected values (this runs the domain validation)
     * @param confirm saves it
     */
    public Wizard review(Function<Values, Displayable> preview, Confirm confirm) {
        this.preview = preview;
        this.confirm = confirm;
        review = new DetailCard(palette, "CONFIRM", "EDIT");
        review.setName("review");
        review.primary().addActionListener(event -> save());
        review.secondary().addActionListener(event -> goTo(0));
        add(review, REVIEW);
        return this;
    }

    /** Clears every answer and shows the first step. */
    public void restart() {
        generation++;
        steps.forEach(step -> step.form().reset());
        values.clear();
        goTo(0);
    }

    /** True once the user has typed anything on any step. */
    public boolean isDirty() {
        return steps.stream().anyMatch(step -> !step.form().isEmpty());
    }

    private void goTo(int index) {
        cards.show(this, "step" + index);
        steps.get(index).form().focusFirst();
    }

    private void next(int index) {
        Step step = steps.get(index);
        Values entered;
        try {
            entered = step.form().read();
        } catch (UserFacingException problem) {
            ctx.messenger().warn(this, problem.getMessage());
            return;
        }
        Values merged = values.copy();
        merged.putAll(entered);

        if (step.check() == null) {
            accept(index, merged);
            return;
        }
        step.primary().setEnabled(false);
        int startedIn = generation;
        Tasks.run(ctx, this, () -> {
            step.check().run(merged);
            return merged;
        }, ok -> {
            if (startedIn == generation) { // ignore the answer if the user cancelled while we waited
                accept(index, ok);
            }
        }, () -> step.primary().setEnabled(true));
    }

    private void accept(int index, Values merged) {
        values.putAll(merged);
        if (index + 1 < steps.size()) {
            goTo(index + 1);
            return;
        }
        try {
            review.show(preview.apply(values));
        } catch (UserFacingException problem) {
            ctx.messenger().warn(this, problem.getMessage());
            return;
        }
        cards.show(this, REVIEW);
    }

    private void save() {
        review.primary().setEnabled(false);
        Values snapshot = values.copy();
        Tasks.run(ctx, this, () -> confirm.run(snapshot), message -> {
            ctx.messenger().info(this, message);
            restart();
            onExit.run();
        }, () -> review.primary().setEnabled(true));
    }

    private void cancel() {
        if (isDirty() && !ctx.messenger().confirm(this,
                "You have entered some information, do you really want to cancel?", "Question")) {
            return;
        }
        restart();
        onExit.run();
    }
}
