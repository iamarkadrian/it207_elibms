package edu.bulsu.lms.service;

import edu.bulsu.lms.domain.BusinessRuleException;
import edu.bulsu.lms.domain.Role;
import edu.bulsu.lms.domain.Staff;
import edu.bulsu.lms.persistence.DataAccessException;
import edu.bulsu.lms.persistence.Database;
import edu.bulsu.lms.persistence.LoanRepository;
import edu.bulsu.lms.persistence.StaffRepository;
import edu.bulsu.lms.security.PasswordHasher;
import edu.bulsu.lms.security.PasswordPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * What the administrator does with librarian accounts: list, add, edit, unlock, remove.
 *
 * <p>Every change takes the {@code actor} who asks for it and checks <em>in the service</em>, against
 * the database, that the actor is an unlocked administrator. The UI only shows the admin screens to
 * administrators, but a rule that exists only in the UI disappears the day someone calls the service
 * from somewhere else.
 */
public final class StaffService {

    private static final String USERNAME_TAKEN = "That username is already taken. Please choose another one.";

    private final Database database;
    private final StaffRepository staffRepository;
    private final LoanRepository loanRepository;
    private final PasswordHasher hasher;

    public StaffService(Database database, StaffRepository staffRepository, LoanRepository loanRepository,
                        PasswordHasher hasher) {
        this.database = database;
        this.staffRepository = staffRepository;
        this.loanRepository = loanRepository;
        this.hasher = hasher;
    }

    public List<Staff> librarians() {
        return database.query(connection -> staffRepository.findByRole(connection, Role.LIBRARIAN));
    }

    /** @return the id assigned to the new librarian */
    public int addLibrarian(Staff actor, String firstName, String middleInitial, String lastName, String position,
                            String departmentCode, String username, char[] password) {
        PasswordPolicy.require(password);
        Staff candidate = new Staff(firstName, middleInitial, lastName, position, departmentCode,
                username, hasher.hash(password), Role.LIBRARIAN);
        try {
            return database.transaction(connection -> {
                requireAdministrator(connection, actor);
                if (staffRepository.usernameTaken(connection, candidate.getUsername(), 0)) {
                    throw new BusinessRuleException(USERNAME_TAKEN);
                }
                return staffRepository.insert(connection, candidate);
            });
        } catch (DataAccessException e) {
            throw e.isDuplicateKey() ? new BusinessRuleException(USERNAME_TAKEN) : e;
        }
    }

    /**
     * Changes username, designation and position. A blank {@code newPassword} keeps the current one.
     */
    public void updateLibrarian(Staff actor, int id, String username, char[] newPassword, String departmentCode,
                                String position) {
        boolean changePassword = newPassword != null && newPassword.length > 0;
        String newHash = null;
        if (changePassword) {
            PasswordPolicy.require(newPassword);
            newHash = hasher.hash(newPassword);
        }
        String passwordHash = newHash;
        try {
            database.transaction(connection -> {
                requireAdministrator(connection, actor);
                Staff staff = requireLibrarian(connection, id);
                staff.setUsername(username);
                staff.setDepartmentCode(departmentCode);
                staff.setPosition(position);
                if (passwordHash != null) {
                    staff.setPasswordHash(passwordHash);
                }
                if (staffRepository.usernameTaken(connection, staff.getUsername(), id)) {
                    throw new BusinessRuleException(USERNAME_TAKEN);
                }
                staffRepository.updateAccount(connection, staff);
                return null;
            });
        } catch (DataAccessException e) {
            throw e.isDuplicateKey() ? new BusinessRuleException(USERNAME_TAKEN) : e;
        }
    }

    public void unlock(Staff actor, int id) {
        database.transaction(connection -> {
            requireAdministrator(connection, actor);
            Staff staff = requireLibrarian(connection, id);
            if (!staff.isLocked()) {
                throw new BusinessRuleException("The librarian account matched with your entered ID is not currently locked.");
            }
            staff.unlock();
            staffRepository.updateLoginState(connection, staff);
            return null;
        });
    }

    public void remove(Staff actor, int id) {
        database.transaction(connection -> {
            requireAdministrator(connection, actor);
            Staff staff = requireLibrarian(connection, id);
            int issued = loanRepository.countIssuedBy(connection, id);
            if (issued > 0) {
                throw new BusinessRuleException(staff.fullName() + " has issued " + issued
                        + " loan(s), so the account cannot be removed. Their history must stay intact.");
            }
            staffRepository.delete(connection, id);
            return null;
        });
    }

    /** Re-reads the actor from the database: a demoted, locked or deleted administrator must not keep working. */
    private void requireAdministrator(Connection connection, Staff actor) throws SQLException {
        boolean allowed = actor != null && staffRepository.findById(connection, actor.getId())
                .filter(current -> current.getRole() == Role.ADMIN && !current.isLocked())
                .isPresent();
        if (!allowed) {
            throw new BusinessRuleException("Only an administrator can manage librarian accounts.");
        }
    }

    private Staff requireLibrarian(Connection connection, int id) throws SQLException {
        return staffRepository.findById(connection, id)
                .filter(staff -> staff.getRole() == Role.LIBRARIAN)
                .orElseThrow(() -> new BusinessRuleException("The Librarian ID you entered does not match any librarian in the system."));
    }
}
