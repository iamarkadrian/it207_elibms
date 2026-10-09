package edu.bulsu.lms.domain;

import java.util.List;

/** A physical book. Lending removes the only copy from the shelf until it is returned. */
public final class PrintedBook extends LibraryItem {

    private String publisher;
    private String genre;
    private String lcClass;

    /** Full constructor, used when loading a book from the database. */
    public PrintedBook(String isbn, String title, String author, String publisher, int publicationYear,
                       String genre, String lcClass, int pages, int borrowDays, boolean available) {
        super("ISBN", isbn, title, author, true, publicationYear, pages, 1, borrowDays, available);
        setPublisher(publisher);
        setGenre(genre);
        setLcClass(lcClass);
    }

    /** Overloaded constructor for a newly catalogued book: it starts on the shelf. */
    public PrintedBook(String isbn, String title, String author, String publisher, int publicationYear,
                       String genre, String lcClass, int pages, int borrowDays) {
        this(isbn, title, author, publisher, publicationYear, genre, lcClass, pages, borrowDays, true);
    }

    public static String requirePublisher(String raw) {
        return Check.text(raw, "Publisher", 120);
    }

    public static String requireGenre(String raw) {
        return Check.text(raw, "Genre", 60);
    }

    public static String requireLcClass(String raw) {
        return Check.text(raw, "LC class", 120);
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = requirePublisher(publisher);
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = requireGenre(genre);
    }

    public String getLcClass() {
        return lcClass;
    }

    public void setLcClass(String lcClass) {
        this.lcClass = requireLcClass(lcClass);
    }

    @Override
    public ItemType type() {
        return ItemType.BOOK;
    }

    @Override
    public boolean isBorrowable() {
        return true;
    }

    @Override
    public boolean consumesCopyOnLoan() {
        return true;
    }

    @Override
    protected String borrowText() {
        return getBorrowDays() + " days - " + (isAvailable() ? "Available" : "Unavailable");
    }

    @Override
    public List<String> tableRow() {
        return List.of(getCode(), getTitle(), authorOrNotAvailable(), publishedText(), genre, lcClass,
                getPages() + " pages", borrowText());
    }

    @Override
    public List<String> lines() {
        return List.of(getCode(), genre, lcClass, publishedText(), getPages() + " pages", borrowText());
    }

    private String publishedText() {
        return publisher + " [" + getPublicationYear() + "]";
    }
}
