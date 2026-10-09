package edu.bulsu.lms.ui.librarian;

import edu.bulsu.lms.domain.Check;
import edu.bulsu.lms.domain.DigitalBook;
import edu.bulsu.lms.domain.ItemType;
import edu.bulsu.lms.domain.LibraryItem;
import edu.bulsu.lms.domain.MaterialType;
import edu.bulsu.lms.domain.OtherMaterial;
import edu.bulsu.lms.domain.PrintedBook;
import edu.bulsu.lms.domain.Vocabulary;
import edu.bulsu.lms.ui.kit.Field;
import edu.bulsu.lms.ui.kit.FormPanel;
import edu.bulsu.lms.ui.kit.Palette;
import edu.bulsu.lms.ui.kit.UiContext;
import edu.bulsu.lms.ui.kit.Values;
import edu.bulsu.lms.ui.kit.Wizard;
import java.util.function.Function;

/**
 * The "add" wizards for the three catalog kinds. Each one declares its steps and how the collected
 * answers become a domain object; the {@link Wizard} does the rest. Field rules come straight from
 * the domain classes' static validators.
 */
final class ItemWizards {

    private ItemWizards() {
        // factory only
    }

    static Wizard create(UiContext ctx, ItemType type, Runnable onExit) {
        return switch (type) {
            case BOOK -> book(ctx, onExit);
            case DIGITAL -> digital(ctx, onExit);
            case OTHER -> other(ctx, onExit);
        };
    }

    private static FormPanel form(String heading) {
        return new FormPanel(Palette.LIGHT, heading);
    }

    private static Wizard book(UiContext ctx, Runnable onExit) {
        Function<Values, LibraryItem> build = v -> new PrintedBook(v.text("isbn"), v.text("title"), v.text("author"),
                v.text("publisher"), v.get("year", Integer.class), v.text("genre"), v.text("lcClass"),
                v.get("pages", Integer.class), v.get("days", Integer.class));

        return new Wizard(ctx, Palette.LIGHT, onExit)
                .step(form("Add Book")
                                .row(Field.text("isbn", "ISBN", text -> LibraryItem.requireCode(text, "ISBN")))
                                .row(Field.text("title", "Title", LibraryItem::requireTitle))
                                .row(Field.text("author", "Author", text -> LibraryItem.requireAuthor(text, true))),
                        "CHECK", v -> ctx.services().catalog().ensureCodeIsFree(v.text("isbn"), "ISBN"))
                .step(form("Add Book")
                        .row(Field.text("year", "Publication Year", ItemWizards::year))
                        .row(Field.text("publisher", "Publisher", PrintedBook::requirePublisher)), "NEXT", null)
                .step(form("Add Book")
                        .row(Field.<String>choice("genre", "Genre").items(Vocabulary.GENRES))
                        .row(Field.<String>choice("lcClass", "LC Class").items(Vocabulary.LC_CLASSES)), "NEXT", null)
                .step(form("Add Book")
                        .row(Field.text("pages", "Pages", ItemWizards::pages))
                        .row(Field.text("days", "Borrow Duration (days)", text -> LibraryItem.requireBorrowDays(Check.integer(text, "Borrow duration"), 1))),
                        "NEXT", null)
                .review(build::apply, v -> {
                    ctx.services().catalog().add(build.apply(v));
                    return "Book added successfully";
                });
    }

    private static Wizard digital(UiContext ctx, Runnable onExit) {
        Function<Values, LibraryItem> build = v -> new DigitalBook(v.text("epubId"), v.text("title"), v.text("author"),
                v.get("year", Integer.class), v.text("genre"), v.get("pages", Integer.class), v.get("days", Integer.class),
                v.text("url"));

        return new Wizard(ctx, Palette.LIGHT, onExit)
                .step(form("Add D-Book")
                                .row(Field.text("epubId", "EPUB ID", text -> LibraryItem.requireCode(text, "EPUB ID")))
                                .row(Field.text("title", "Title", LibraryItem::requireTitle))
                                .row(Field.text("author", "Author", text -> LibraryItem.requireAuthor(text, true))),
                        "CHECK", v -> ctx.services().catalog().ensureCodeIsFree(v.text("epubId"), "EPUB ID"))
                .step(form("Add D-Book")
                        .row(Field.text("year", "Publication Year", ItemWizards::year),
                                Field.text("pages", "Pages", ItemWizards::pages))
                        .row(Field.<String>choice("genre", "Genre").items(Vocabulary.GENRES)), "NEXT", null)
                .step(form("Add D-Book")
                        .row(Field.text("days", "Borrow Duration (days)", text -> LibraryItem.requireBorrowDays(Check.integer(text, "Borrow duration"), 1)))
                        .row(Field.text("url", "Digital Book URL", DigitalBook::requireUrl)), "NEXT", null)
                .review(build::apply, v -> {
                    ctx.services().catalog().add(build.apply(v));
                    return "Digital book added successfully";
                });
    }

    private static Wizard other(UiContext ctx, Runnable onExit) {
        Function<Values, LibraryItem> build = v -> new OtherMaterial(v.text("barcode"), v.text("title"), v.text("author"),
                v.text("publisher"), v.get("year", Integer.class), v.get("pages", Integer.class),
                v.get("type", MaterialType.class));

        return new Wizard(ctx, Palette.LIGHT, onExit)
                .step(form("Add Other Detail")
                                .row(Field.text("barcode", "Barcode", text -> LibraryItem.requireCode(text, "Barcode")))
                                .row(Field.text("title", "Title", LibraryItem::requireTitle))
                                .row(Field.text("author", "Author", text -> LibraryItem.requireAuthor(text, false))),
                        "CHECK", v -> ctx.services().catalog().ensureCodeIsFree(v.text("barcode"), "Barcode"))
                .step(form("Add Other Detail")
                        .row(Field.text("year", "Publication Year", ItemWizards::year),
                                Field.text("pages", "Pages", ItemWizards::pages))
                        .row(Field.text("publisher", "Publisher", OtherMaterial::optionalPublisher))
                        .row(Field.<MaterialType>choice("type", "Type").items(java.util.List.of(MaterialType.values()))),
                        "NEXT", null)
                .review(build::apply, v -> {
                    ctx.services().catalog().add(build.apply(v));
                    return "Other detail added successfully";
                });
    }

    private static int year(String text) {
        return Check.publicationYear(Check.integer(text, "Publication year"));
    }

    private static int pages(String text) {
        return LibraryItem.requirePages(Check.integer(text, "Pages"));
    }
}
