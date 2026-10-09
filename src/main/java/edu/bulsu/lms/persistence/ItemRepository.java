package edu.bulsu.lms.persistence;

import edu.bulsu.lms.domain.DigitalBook;
import edu.bulsu.lms.domain.ItemType;
import edu.bulsu.lms.domain.LibraryItem;
import edu.bulsu.lms.domain.MaterialType;
import edu.bulsu.lms.domain.OtherMaterial;
import edu.bulsu.lms.domain.PrintedBook;
import edu.bulsu.lms.domain.UserFacingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD for the {@code item} table.
 *
 * <p>All catalog kinds share one table (single-table inheritance). The {@code item_type} column
 * says which {@link LibraryItem} subclass a row becomes.
 */
public final class ItemRepository {

    private static final String COLUMNS = "code, item_type, title, author, publisher, publication_year, genre, "
            + "lc_class, pages, borrow_days, url, material_type, available";

    /** The columns that only some kinds use. */
    private record Extras(String publisher, String genre, String lcClass, String url, String materialType) {
    }

    public List<LibraryItem> findAll(Connection connection, ItemType type) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM item WHERE item_type = ? ORDER BY title, code";
        List<LibraryItem> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, type.name());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    public Optional<LibraryItem> find(Connection connection, String code) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM item WHERE code = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public boolean exists(Connection connection, String code) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM item WHERE code = ?")) {
            statement.setString(1, code);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insert(Connection connection, LibraryItem item) throws SQLException {
        // The switch is exhaustive because LibraryItem is sealed: adding a subclass breaks the build here.
        Extras extras = switch (item) {
            case PrintedBook b -> new Extras(b.getPublisher(), b.getGenre(), b.getLcClass(), null, null);
            case DigitalBook d -> new Extras(null, d.getGenre(), null, d.getUrl(), null);
            case OtherMaterial o -> new Extras(o.getPublisher(), null, null, null, o.getMaterialType().name());
        };
        String sql = "INSERT INTO item (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int i = 1;
            statement.setString(i++, item.getCode());
            statement.setString(i++, item.type().name());
            statement.setString(i++, item.getTitle());
            statement.setString(i++, item.getAuthor());
            statement.setString(i++, extras.publisher());
            statement.setInt(i++, item.getPublicationYear());
            statement.setString(i++, extras.genre());
            statement.setString(i++, extras.lcClass());
            statement.setInt(i++, item.getPages());
            statement.setInt(i++, item.getBorrowDays());
            statement.setString(i++, extras.url());
            statement.setString(i++, extras.materialType());
            statement.setBoolean(i, item.isAvailable());
            statement.executeUpdate();
        }
    }

    /**
     * Takes the only copy off the shelf, but only if it is still there. The condition in the
     * {@code WHERE} clause makes two simultaneous borrowers safe: exactly one update succeeds.
     *
     * @return 1 if this caller got the copy, 0 if someone else already has it
     */
    public int tryClaim(Connection connection, String code) throws SQLException {
        String sql = "UPDATE item SET available = ? WHERE code = ? AND available = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, false);
            statement.setString(2, code);
            statement.setBoolean(3, true);
            return statement.executeUpdate();
        }
    }

    public int setAvailable(Connection connection, String code, boolean available) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE item SET available = ? WHERE code = ?")) {
            statement.setBoolean(1, available);
            statement.setString(2, code);
            return statement.executeUpdate();
        }
    }

    public int count(Connection connection, ItemType type) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM item WHERE item_type = ?")) {
            statement.setString(1, type.name());
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static LibraryItem map(ResultSet rs) throws SQLException {
        String code = rs.getString("code");
        try {
            String title = rs.getString("title");
            String author = rs.getString("author");
            int year = rs.getInt("publication_year");
            int pages = rs.getInt("pages");
            int borrowDays = rs.getInt("borrow_days");
            return switch (ItemType.valueOf(rs.getString("item_type"))) {
                case BOOK -> new PrintedBook(code, title, author, rs.getString("publisher"), year,
                        rs.getString("genre"), rs.getString("lc_class"), pages, borrowDays, rs.getBoolean("available"));
                case DIGITAL -> new DigitalBook(code, title, author, year, rs.getString("genre"), pages, borrowDays,
                        rs.getString("url"));
                case OTHER -> new OtherMaterial(code, title, author, rs.getString("publisher"), year, pages,
                        MaterialType.valueOf(rs.getString("material_type")));
            };
        } catch (UserFacingException | IllegalArgumentException e) {
            throw DataAccessException.corruptRow("item", code, e);
        }
    }
}
