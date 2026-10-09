package edu.bulsu.lms.ui.kit;

import edu.bulsu.lms.domain.ValidationException;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import java.util.function.Function;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

/**
 * One labelled input of a form. A field knows how to turn what the user typed into a validated
 * value ({@link #value()}), usually by delegating to the same static validators the domain classes
 * use, so a rule is written once.
 *
 * @param <T> the type of the validated value
 */
public abstract class Field<T> {

    private static final int HEIGHT = 48;

    private final String id;
    private final String label;
    private String hint = "";

    protected Field(String id, String label) {
        this.id = id;
        this.label = label;
    }

    /** A text box whose content is parsed (and validated) by {@code parser}. */
    public static <T> Text<T> text(String id, String label, Function<String, T> parser) {
        return new Text<>(id, label, parser);
    }

    /** A masked password box. */
    public static Secret secret(String id, String label, boolean required) {
        return new Secret(id, label, required);
    }

    /** A drop-down list. */
    public static <T> Choice<T> choice(String id, String label) {
        return new Choice<>(id, label);
    }

    /** A box the user cannot edit; the program fills it in. */
    public static Fixed fixed(String id, String label) {
        return new Fixed(id, label);
    }

    public final String id() {
        return id;
    }

    public final String label() {
        return label;
    }

    /** Small gray text shown to the right of the label, e.g. "4-20 characters". */
    public final String hint() {
        return hint;
    }

    public Field<T> hint(String hint) {
        this.hint = hint;
        return this;
    }

    public abstract JComponent component();

    /** @throws edu.bulsu.lms.domain.UserFacingException if the input is not acceptable */
    public abstract T value();

    /** True when the user has not typed or chosen anything (used to decide whether CANCEL needs a confirmation). */
    public abstract boolean isEmpty();

    public abstract void reset();

    abstract void style(Palette palette);

    abstract void onEnter(Runnable action);

    // ------------------------------------------------------------------------------------------

    static void styleText(JTextComponent input, Palette palette) {
        String background = Theme.hex(palette.field());
        input.setFont(Theme.font(Font.PLAIN, 16));
        input.setForeground(palette.text());
        input.setBackground(palette.field());
        input.setCaretColor(palette.text());
        input.setPreferredSize(new Dimension(10, HEIGHT));
        input.putClientProperty("FlatLaf.style", "arc: 0; borderWidth: 1; borderColor: " + background
                + "; focusedBorderColor: " + Theme.hex(Theme.BLUE) + "; focusWidth: 0; innerFocusWidth: 0; margin: 0,16,0,16"
                + "; inactiveBackground: " + background + "; disabledBackground: " + background);
    }

    // ------------------------------------------------------------------------------------------

    public static final class Text<T> extends Field<T> {
        private final JTextField input = new JTextField();
        private final Function<String, T> parser;

        private Text(String id, String label, Function<String, T> parser) {
            super(id, label);
            this.parser = parser;
            input.setName(id);
        }

        @Override
        public JComponent component() {
            return input;
        }

        @Override
        public T value() {
            return parser.apply(input.getText());
        }

        @Override
        public boolean isEmpty() {
            return input.getText().isBlank();
        }

        @Override
        public void reset() {
            input.setText("");
        }

        /** Pre-fills the box (used when editing). */
        public Text<T> fill(String text) {
            input.setText(text);
            return this;
        }

        @Override
        public Text<T> hint(String hint) {
            super.hint(hint);
            return this;
        }

        @Override
        void style(Palette palette) {
            styleText(input, palette);
        }

        @Override
        void onEnter(Runnable action) {
            input.addActionListener(event -> action.run());
        }
    }

    public static final class Secret extends Field<char[]> {
        private final JPasswordField input = new JPasswordField();
        private final boolean required;

        private Secret(String id, String label, boolean required) {
            super(id, label);
            this.required = required;
            input.setName(id);
        }

        @Override
        public JComponent component() {
            return input;
        }

        @Override
        public Secret hint(String hint) {
            super.hint(hint);
            return this;
        }

        @Override
        public char[] value() {
            char[] password = input.getPassword();
            if (required && password.length == 0) {
                throw new ValidationException(label() + " cannot be blank.");
            }
            return password;
        }

        @Override
        public boolean isEmpty() {
            return input.getPassword().length == 0;
        }

        @Override
        public void reset() {
            input.setText("");
        }

        @Override
        void style(Palette palette) {
            styleText(input, palette);
            input.setEchoChar('•');
            input.putClientProperty("JPasswordField.showRevealButton", Boolean.TRUE);
        }

        @Override
        void onEnter(Runnable action) {
            input.addActionListener(event -> action.run());
        }
    }

    public static final class Choice<T> extends Field<T> {
        private final JComboBox<T> input = new JComboBox<>();
        private final List<Runnable> changeListeners = new java.util.ArrayList<>();

        private Choice(String id, String label) {
            super(id, label);
            input.setName(id);
            input.addActionListener(event -> changeListeners.forEach(Runnable::run));
        }

        /**
         * Replaces the list of options; the first one becomes selected. Swapping the model does not
         * make Swing announce a selection, so the change listeners are told explicitly: a dependent
         * drop-down (courses of the chosen college) must fill itself even if the user never touches this one.
         */
        public Choice<T> items(List<? extends T> options) {
            DefaultComboBoxModel<T> model = new DefaultComboBoxModel<>();
            options.forEach(model::addElement);
            input.setModel(model);
            changeListeners.forEach(Runnable::run);
            return this;
        }

        public Choice<T> select(T option) {
            input.setSelectedItem(option);
            return this;
        }

        /** Runs {@code listener} whenever the selection or the list of options changes. */
        public Choice<T> onChange(Runnable listener) {
            changeListeners.add(listener);
            return this;
        }

        @Override
        public JComponent component() {
            return input;
        }

        @Override
        public T value() {
            int index = input.getSelectedIndex();
            if (index < 0) {
                throw new ValidationException(label() + " must be selected.");
            }
            return input.getItemAt(index);
        }

        @Override
        public boolean isEmpty() {
            return true; // a drop-down always shows something; only typed text counts as "entered information"
        }

        @Override
        public void reset() {
            if (input.getItemCount() > 0) {
                input.setSelectedIndex(0);
            }
        }

        @Override
        void style(Palette palette) {
            String background = Theme.hex(palette.field());
            input.setFont(Theme.font(Font.PLAIN, 16));
            input.setForeground(palette.text());
            input.setBackground(palette.field());
            input.setPreferredSize(new Dimension(10, HEIGHT));
            input.putClientProperty("FlatLaf.style", "arc: 0; borderWidth: 0; padding: 0,16,0,0; buttonBackground: " + background
                    + "; buttonEditableBackground: " + background + "; buttonArrowColor: " + Theme.hex(palette.text())
                    + "; popupBackground: " + background);
        }

        @Override
        void onEnter(Runnable action) {
            // Enter on a drop-down opens its list; nothing to submit.
        }
    }

    public static final class Fixed extends Field<String> {
        private final JTextField input = new JTextField();

        private Fixed(String id, String label) {
            super(id, label);
            input.setName(id);
            input.setEditable(false);
            input.setFocusable(false);
        }

        public Fixed show(String text) {
            input.setText(text);
            input.setCaretPosition(0); // show the start of a long title, not its end
            return this;
        }

        @Override
        public JComponent component() {
            return input;
        }

        @Override
        public String value() {
            return input.getText();
        }

        @Override
        public boolean isEmpty() {
            return true; // filled by the program, not typed by the user
        }

        @Override
        public void reset() {
            input.setText("");
        }

        @Override
        void style(Palette palette) {
            styleText(input, palette);
        }

        @Override
        void onEnter(Runnable action) {
            // read-only
        }
    }
}
