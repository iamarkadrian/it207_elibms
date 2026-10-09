package edu.bulsu.lms.service;

import edu.bulsu.lms.domain.ItemType;
import edu.bulsu.lms.persistence.Database;
import edu.bulsu.lms.persistence.ItemRepository;
import edu.bulsu.lms.persistence.LoanRepository;
import edu.bulsu.lms.persistence.StudentRepository;
import java.time.Clock;
import java.time.LocalDateTime;

/** Collects the summary numbers on one connection. */
public final class DashboardService {

    private final Database database;
    private final ItemRepository itemRepository;
    private final StudentRepository studentRepository;
    private final LoanRepository loanRepository;
    private final Clock clock;

    public DashboardService(Database database, ItemRepository itemRepository, StudentRepository studentRepository,
                            LoanRepository loanRepository, Clock clock) {
        this.database = database;
        this.itemRepository = itemRepository;
        this.studentRepository = studentRepository;
        this.loanRepository = loanRepository;
        this.clock = clock;
    }

    public LibrarySummary summary() {
        LocalDateTime now = LocalDateTime.now(clock);
        return database.query(connection -> new LibrarySummary(
                itemRepository.count(connection, ItemType.BOOK),
                itemRepository.count(connection, ItemType.DIGITAL),
                itemRepository.count(connection, ItemType.OTHER),
                studentRepository.count(connection),
                loanRepository.countOpen(connection),
                loanRepository.countReturned(connection),
                loanRepository.countOverdue(connection, now)));
    }
}
