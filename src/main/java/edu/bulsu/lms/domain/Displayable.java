package edu.bulsu.lms.domain;

import java.util.List;

/**
 * Anything the detail card can show: a heading, a gray sub-heading and a list of text lines.
 *
 * <p>Implemented by unrelated classes ({@link LibraryItem}, {@link Person}, {@link Loan}), so one
 * UI component can render every kind of record without knowing its concrete type.
 */
public interface Displayable {

    /** Large heading of the card. */
    String title();

    /** Gray line below the heading. */
    String subtitle();

    /** Body lines in display order. A blank string renders as a spacer. */
    List<String> lines();
}
