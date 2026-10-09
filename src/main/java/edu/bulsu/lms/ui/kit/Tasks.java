package edu.bulsu.lms.ui.kit;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Runs database work off the event-dispatch thread so the window never freezes, then hands the
 * result back on the EDT. Failures are reported through {@link UiContext#report}.
 */
public final class Tasks {

    private static final AtomicInteger PENDING = new AtomicInteger();

    private Tasks() {
        // static helpers only
    }

    public static <T> void run(UiContext ctx, Component parent, Callable<T> work, Consumer<T> onSuccess) {
        run(ctx, parent, work, onSuccess, null);
    }

    /**
     * @param work      runs on a background thread
     * @param onSuccess runs on the EDT with the result
     * @param onDone    runs on the EDT afterwards, success or failure (re-enable a button, hide a spinner)
     */
    public static <T> void run(UiContext ctx, Component parent, Callable<T> work, Consumer<T> onSuccess, Runnable onDone) {
        PENDING.incrementAndGet();
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                try {
                    try {
                        onSuccess.accept(get());
                    } catch (ExecutionException e) {
                        ctx.report(parent, e.getCause());
                    } catch (RuntimeException e) {
                        ctx.report(parent, e); // a bug in the success handler must not vanish silently
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        if (onDone != null) {
                            onDone.run();
                        }
                    }
                } finally {
                    PENDING.decrementAndGet();
                }
            }
        }.execute();
    }

    /** For tests: blocks until every started task, and everything it triggered, has finished. */
    public static void awaitIdle() throws InterruptedException, InvocationTargetException {
        long deadline = System.nanoTime() + 60_000_000_000L;
        do {
            while (PENDING.get() > 0) {
                if (System.nanoTime() > deadline) {
                    throw new IllegalStateException("Background tasks did not finish within 60 seconds");
                }
                Thread.sleep(5);
            }
            SwingUtilities.invokeAndWait(() -> { }); // let queued done() callbacks run
        } while (PENDING.get() > 0);
    }
}
