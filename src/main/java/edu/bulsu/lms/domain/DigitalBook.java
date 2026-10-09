package edu.bulsu.lms.domain;

import java.net.URI;
import java.util.List;

/** An e-book. Lending only records the loan: any number of members can read the same copy. */
public final class DigitalBook extends LibraryItem {

    private String genre;
    private String url;

    /** Digital books are always available, so unlike {@link PrintedBook} there is no availability argument. */
    public DigitalBook(String epubId, String title, String author, int publicationYear, String genre,
                       int pages, int borrowDays, String url) {
        super("EPUB ID", epubId, title, author, true, publicationYear, pages, 1, borrowDays, true);
        setGenre(genre);
        setUrl(url);
    }

    public static String requireUrl(String raw) {
        String value = Check.text(raw, "Digital book URL", 500);
        try {
            URI uri = URI.create(value);
            boolean web = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
            if (!web || uri.getHost() == null) {
                throw new ValidationException("Digital book URL must start with http:// or https://.");
            }
            if (uri.getUserInfo() != null) {
                throw new ValidationException("Digital book URL must not contain a user name or password.");
            }
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Digital book URL is not a valid web address.");
        }
        return value;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = PrintedBook.requireGenre(genre);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = requireUrl(url);
    }

    @Override
    public ItemType type() {
        return ItemType.DIGITAL;
    }

    @Override
    public boolean isBorrowable() {
        return true;
    }

    @Override
    public boolean consumesCopyOnLoan() {
        return false;
    }

    @Override
    protected String borrowText() {
        return getBorrowDays() + " days - Available";
    }

    @Override
    public List<String> tableRow() {
        return List.of(getCode(), getTitle(), authorOrNotAvailable(), String.valueOf(getPublicationYear()), genre,
                getPages() + " pages", borrowText(), url);
    }

    @Override
    public List<String> lines() {
        return List.of(getCode(), String.valueOf(getPublicationYear()), genre, getPages() + " pages",
                getBorrowDays() + " days", url);
    }
}
