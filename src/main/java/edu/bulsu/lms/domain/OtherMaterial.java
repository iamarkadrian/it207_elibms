package edu.bulsu.lms.domain;

import java.util.List;

/** A journal, magazine or thesis. It is kept for reference and is never lent out. */
public final class OtherMaterial extends LibraryItem {

    private MaterialType materialType;
    private String publisher;

    public OtherMaterial(String barcode, String title, String author, String publisher,
                         int publicationYear, int pages, MaterialType materialType) {
        super("Barcode", barcode, title, author, false, publicationYear, pages, 0, 0, false);
        setMaterialType(materialType);
        setPublisher(publisher);
    }

    public static String optionalPublisher(String raw) {
        return Check.optional(raw, "Publisher", 120);
    }

    public MaterialType getMaterialType() {
        return materialType;
    }

    public void setMaterialType(MaterialType materialType) {
        if (materialType == null) {
            throw new ValidationException("Type must be selected.");
        }
        this.materialType = materialType;
    }

    /** The publisher, or {@code null} when unknown (common for theses). */
    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = optionalPublisher(publisher);
    }

    @Override
    public ItemType type() {
        return ItemType.OTHER;
    }

    @Override
    public boolean isBorrowable() {
        return false;
    }

    @Override
    public boolean consumesCopyOnLoan() {
        return false;
    }

    @Override
    protected String borrowText() {
        return "Reference only";
    }

    @Override
    public List<String> tableRow() {
        return List.of(getCode(), getTitle(), authorOrNotAvailable(), publishedText(), getPages() + " pages",
                materialType.label());
    }

    @Override
    public List<String> lines() {
        return List.of(getCode(), materialType.label(), publishedText(), getPages() + " pages", borrowText());
    }

    private String publishedText() {
        return publisher == null ? String.valueOf(getPublicationYear()) : publisher + " [" + getPublicationYear() + "]";
    }
}
