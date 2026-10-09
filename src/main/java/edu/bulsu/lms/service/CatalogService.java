package edu.bulsu.lms.service;

import edu.bulsu.lms.domain.BusinessRuleException;
import edu.bulsu.lms.domain.ItemType;
import edu.bulsu.lms.domain.LibraryItem;
import edu.bulsu.lms.persistence.DataAccessException;
import edu.bulsu.lms.persistence.Database;
import edu.bulsu.lms.persistence.ItemRepository;
import java.util.List;
import java.util.Optional;

/** The catalog: books, digital books and other materials. */
public final class CatalogService {

    private final Database database;
    private final ItemRepository itemRepository;

    public CatalogService(Database database, ItemRepository itemRepository) {
        this.database = database;
        this.itemRepository = itemRepository;
    }

    public List<LibraryItem> items(ItemType type) {
        return database.query(connection -> itemRepository.findAll(connection, type));
    }

    /** The current state of one entry, straight from the database. */
    public Optional<LibraryItem> find(String code) {
        return database.query(connection -> itemRepository.find(connection, code));
    }

    public int count(ItemType type) {
        return database.query(connection -> itemRepository.count(connection, type));
    }

    /** First wizard step: fail early if the ISBN / EPUB ID / barcode is already catalogued. */
    public void ensureCodeIsFree(String code, String label) {
        String validated = LibraryItem.requireCode(code, label);
        if (database.query(connection -> itemRepository.exists(connection, validated))) {
            throw new BusinessRuleException("The entry with this " + label + " has already been registered.");
        }
    }

    public void add(LibraryItem item) {
        String message = "The entry with this " + item.getCodeLabel() + " has already been registered.";
        try {
            database.transaction(connection -> {
                if (itemRepository.exists(connection, item.getCode())) {
                    throw new BusinessRuleException(message);
                }
                itemRepository.insert(connection, item);
                return null;
            });
        } catch (DataAccessException e) {
            throw e.isDuplicateKey() ? new BusinessRuleException(message) : e;
        }
    }
}
