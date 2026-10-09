package edu.bulsu.lms.testsupport;

import edu.bulsu.lms.ui.kit.Messenger;
import java.awt.Component;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** A {@link Messenger} that remembers what was said and answers questions as scripted, so no dialog ever blocks a test. */
public final class RecordingMessenger implements Messenger {

    private final List<String> infos = new CopyOnWriteArrayList<>();
    private final List<String> warnings = new CopyOnWriteArrayList<>();
    private final List<String> errors = new CopyOnWriteArrayList<>();
    private final List<String> questions = new CopyOnWriteArrayList<>();
    private volatile boolean confirmAnswer = true;
    private volatile String promptAnswer;

    /** What {@link #confirm} will answer from now on. */
    public RecordingMessenger answerConfirm(boolean yes) {
        this.confirmAnswer = yes;
        return this;
    }

    /** What {@link #prompt} will answer from now on ({@code null} = the user cancelled). */
    public RecordingMessenger answerPrompt(String text) {
        this.promptAnswer = text;
        return this;
    }

    public List<String> infos() {
        return infos;
    }

    public List<String> warnings() {
        return warnings;
    }

    public List<String> errors() {
        return errors;
    }

    public List<String> questions() {
        return questions;
    }

    public String lastInfo() {
        return last(infos, "info");
    }

    public String lastWarning() {
        return last(warnings, "warning");
    }

    public String lastError() {
        return last(errors, "error");
    }

    public void clear() {
        infos.clear();
        warnings.clear();
        errors.clear();
        questions.clear();
    }

    private static String last(List<String> messages, String kind) {
        if (messages.isEmpty()) {
            throw new AssertionError("No " + kind + " message was shown");
        }
        return messages.get(messages.size() - 1);
    }

    @Override
    public void info(Component parent, String message) {
        infos.add(message);
    }

    @Override
    public void warn(Component parent, String message) {
        warnings.add(message);
    }

    @Override
    public void error(Component parent, String message) {
        errors.add(message);
    }

    @Override
    public boolean confirm(Component parent, String message, String title) {
        questions.add(message);
        return confirmAnswer;
    }

    @Override
    public String prompt(Component parent, String message, String title) {
        questions.add(message);
        return promptAnswer;
    }
}
