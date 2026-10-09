package edu.bulsu.lms.service;

import edu.bulsu.lms.domain.BusinessRuleException;
import edu.bulsu.lms.domain.LibraryItem;
import edu.bulsu.lms.domain.Loan;
import edu.bulsu.lms.domain.Staff;
import edu.bulsu.lms.domain.Student;
import edu.bulsu.lms.persistence.Database;
import edu.bulsu.lms.persistence.ItemRepository;
import edu.bulsu.lms.persistence.LoanRepository;
import edu.bulsu.lms.persistence.StaffRepository;
import edu.bulsu.lms.persistence.StudentRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Lending and returning. Each operation is one transaction, so a loan row and the item's
 * availability can never disagree, even if the application crashes half-way.
 */
public final class CirculationService {

    private final Database database;
    private final ItemRepository itemRepository;
    private final StudentRepository studentRepository;
    private final LoanRepository loanRepository;
    private final StaffRepository staffRepository;
    private final Clock clock;

    public CirculationService(Database database, ItemRepository itemRepository, StudentRepository studentRepository,
                              LoanRepository loanRepository, StaffRepository staffRepository, Clock clock) {
        this.database = database;
        this.itemRepository = itemRepository;
        this.studentRepository = studentRepository;
        this.loanRepository = loanRepository;
        this.staffRepository = staffRepository;
        this.clock = clock;
    }

    public List<Loan> loans(LoanView view) {
        LocalDateTime now = now();
        return database.query(connection -> switch (view) {
            case ISSUED -> loanRepository.findOpen(connection);
            case RETURNED -> loanRepository.findReturned(connection);
            case MISSING -> loanRepository.findOverdue(connection, now);
        });
    }

    /** Lends {@code itemCode} to a member. @throws BusinessRuleException if it cannot be lent right now */
    public Loan issue(String itemCode, String studentNumber, Staff issuedBy) {
        String code = LibraryItem.requireCode(itemCode, "Item code");
        String number = Student.requireStudentNumber(studentNumber);
        LocalDateTime issuedAt = now();

        int loanId = database.transaction(connection -> {
            // Do not trust the Staff object the screen holds: the account may have been locked or removed since sign-in.
            Staff issuer = staffRepository.findById(connection, issuedBy.getId())
                    .filter(current -> !current.isLocked())
                    .orElseThrow(() -> new BusinessRuleException("This account can no longer lend books. Please sign in again."));
            LibraryItem item = itemRepository.find(connection, code)
                    .orElseThrow(() -> new BusinessRuleException("No catalog entry matches " + code + "."));
            if (!studentRepository.exists(connection, number)) {
                throw new BusinessRuleException("Student number is not a member.");
            }
            item.markLent(); // the domain decides: reference-only items and lent-out books are refused
            if (item.consumesCopyOnLoan() && itemRepository.tryClaim(connection, code) == 0) {
                // Someone else borrowed the last copy between our read and our update.
                throw new BusinessRuleException("\"" + item.getTitle() + "\" is currently borrowed.");
            }
            return loanRepository.insert(connection, code, number, issuer.getId(), issuedAt, item.dueDateFrom(issuedAt));
        });
        return reload(loanId);
    }

    /** Records the return of a loan and puts the copy back on the shelf. */
    public Loan giveBack(int loanId) {
        LocalDateTime returnedAt = now();
        database.transaction(connection -> {
            Loan loan = loanRepository.findById(connection, loanId)
                    .orElseThrow(() -> new BusinessRuleException("No loan has issue number " + loanId + "."));
            if (loanRepository.markReturned(connection, loanId, returnedAt) == 0) {
                throw new BusinessRuleException("Book has already returned.");
            }
            LibraryItem item = itemRepository.find(connection, loan.itemCode())
                    .orElseThrow(() -> new BusinessRuleException("The catalog entry for this loan no longer exists."));
            if (item.consumesCopyOnLoan()) {
                item.markReturned();
                itemRepository.setAvailable(connection, item.getCode(), item.isAvailable());
            }
            return null;
        });
        return reload(loanId);
    }

    private Loan reload(int loanId) {
        return database.query(connection -> loanRepository.findById(connection, loanId)).orElseThrow();
    }

    private LocalDateTime now() {
        // Databases keep whole seconds; truncating here keeps what we hold equal to what we stored.
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
