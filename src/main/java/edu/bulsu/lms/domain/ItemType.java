package edu.bulsu.lms.domain;

import java.util.List;

/**
 * The kinds of catalog entries. The enum name is stored in the {@code item.item_type} column, so it
 * doubles as the discriminator when rows are turned back into {@link LibraryItem} subclasses.
 */
public enum ItemType {
    BOOK("Books", List.of("ISBN", "Title", "Author", "Published", "Genre", "LC Class", "Pages", "Borrow")),
    DIGITAL("Digital Books", List.of("EPUB ID", "Title", "Author", "Published", "Genre", "Pages", "Borrow", "URL")),
    OTHER("Other Details", List.of("Barcode", "Title", "Author", "Published", "Pages", "Type"));

    private final String label;
    private final List<String> columns;

    ItemType(String label, List<String> columns) {
        this.label = label;
        this.columns = columns;
    }

    /** Plural name used in navigation and statistics, e.g. "Digital Books". */
    public String label() {
        return label;
    }

    /** Table headers; each {@link LibraryItem#tableRow()} lines up with these. */
    public List<String> columns() {
        return columns;
    }
}
