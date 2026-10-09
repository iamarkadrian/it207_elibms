package edu.bulsu.lms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.bulsu.lms.domain.BusinessRuleException;
import edu.bulsu.lms.domain.Staff;
import edu.bulsu.lms.domain.ValidationException;
import edu.bulsu.lms.persistence.Database;
import edu.bulsu.lms.security.PasswordHasher;
import edu.bulsu.lms.testsupport.TestClock;
import edu.bulsu.lms.testsupport.TestData;
import edu.bulsu.lms.testsupport.TestDatabase;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The administrator's librarian management: create, read, update, delete, unlock. */
class StaffServiceIT {

    private static Services services;

    @BeforeAll
    static void connect() {
        Database db = TestDatabase.require();
        services = Services.create(db, new TestClock(), new PasswordHasher(2_000));
    }

    private static Staff find(int id) {
        return services.staff().librarians().stream().filter(s -> s.getId() == id).findFirst().orElseThrow();
    }

    @Test
    void createdLibrarianIsListedAndCanSignIn() {
        String username = TestData.username();
        int id = services.staff().addLibrarian(TestData.admin(), "Mia", "R.", "Tester", "Reference Librarian", "CICT", username,
                "Passw0rd-ok".toCharArray());

        Staff saved = find(id);
        assertEquals(username, saved.getUsername());
        assertEquals("Reference Librarian", saved.getPosition());
        assertFalse(saved.isLocked());
        assertFalse(saved.getPasswordHash().contains("Passw0rd"), "only a hash is stored, never the password");
        assertTrue(id >= 2100101, "ids continue from the configured starting point");
        assertInstanceOf(LoginResult.Success.class, services.auth().login(username, "Passw0rd-ok".toCharArray()));
    }

    @Test
    @DisplayName("only an administrator may manage accounts, enforced in the service, not just hidden in the UI")
    void aLibrarianCannotManageAccounts() {
        Staff librarian = services.staff().librarians().get(0);
        int target = services.staff().addLibrarian(TestData.admin(), "Target", null, "Account", "Librarian", "CICT",
                TestData.username(), "Passw0rd-ok".toCharArray());

        assertThrows(BusinessRuleException.class, () -> services.staff().addLibrarian(librarian, "Evil", null, "Twin",
                "Librarian", "CICT", TestData.username(), "Passw0rd-ok".toCharArray()));
        assertThrows(BusinessRuleException.class, () -> services.staff().updateLibrarian(librarian, target,
                TestData.username(), new char[0], "CICT", "Librarian"));
        assertThrows(BusinessRuleException.class, () -> services.staff().unlock(librarian, target));
        assertThrows(BusinessRuleException.class, () -> services.staff().remove(librarian, target));
        assertThrows(BusinessRuleException.class, () -> services.staff().remove(null, target));
        assertTrue(services.staff().librarians().stream().anyMatch(s -> s.getId() == target), "the account is still there");
    }

    @Test
    void theAdministratorIsNotInTheLibrarianList() {
        assertTrue(services.staff().librarians().stream().noneMatch(s -> s.getUsername().equals("admin")));
    }

    @Test
    void duplicateUsernameIsRefusedEvenWithDifferentCase() {
        String username = TestData.username();
        services.staff().addLibrarian(TestData.admin(), "One", null, "Tester", "Librarian", "CICT", username, "Passw0rd-ok".toCharArray());
        assertThrows(BusinessRuleException.class, () -> services.staff().addLibrarian(TestData.admin(), 
                "Two", null, "Tester", "Librarian", "CICT", username.toUpperCase(), "Passw0rd-ok".toCharArray()));
    }

    @Test
    void invalidInputIsRefusedBeforeTouchingTheDatabase() {
        assertThrows(ValidationException.class, () -> services.staff().addLibrarian(TestData.admin(), 
                "", null, "Tester", "Librarian", "CICT", TestData.username(), "Passw0rd-ok".toCharArray()));
        assertThrows(ValidationException.class, () -> services.staff().addLibrarian(TestData.admin(), 
                "Mia", null, "Tester", "Librarian", "CICT", "ab", "Passw0rd-ok".toCharArray()));
        assertThrows(ValidationException.class, () -> services.staff().addLibrarian(TestData.admin(), 
                "Mia", null, "Tester", "Librarian", "CICT", TestData.username(), "weak".toCharArray()));
    }

    @Test
    void updateChangesFieldsAndKeepsThePasswordWhenBlank() {
        String username = TestData.username();
        int id = services.staff().addLibrarian(TestData.admin(), "Edit", null, "Tester", "Librarian", "CICT", username, "Passw0rd-ok".toCharArray());

        String renamed = TestData.username();
        services.staff().updateLibrarian(TestData.admin(), id, renamed, new char[0], "COE", "Unit Head Librarian");
        Staff after = find(id);
        assertEquals(renamed, after.getUsername());
        assertEquals("COE", after.getDepartmentCode());
        assertEquals("Unit Head Librarian", after.getPosition());
        assertInstanceOf(LoginResult.Success.class, services.auth().login(renamed, "Passw0rd-ok".toCharArray()),
                "a blank new password keeps the old one");

        services.staff().updateLibrarian(TestData.admin(), id, renamed, "Brand-New-9".toCharArray(), "COE", "Unit Head Librarian");
        assertInstanceOf(LoginResult.InvalidCredentials.class, services.auth().login(renamed, "Passw0rd-ok".toCharArray()));
        assertInstanceOf(LoginResult.Success.class, services.auth().login(renamed, "Brand-New-9".toCharArray()));
    }

    @Test
    void updateCannotStealAnotherUsername() {
        String first = TestData.username();
        String second = TestData.username();
        services.staff().addLibrarian(TestData.admin(), "A", null, "Tester", "Librarian", "CICT", first, "Passw0rd-ok".toCharArray());
        int id = services.staff().addLibrarian(TestData.admin(), "B", null, "Tester", "Librarian", "CICT", second, "Passw0rd-ok".toCharArray());
        assertThrows(BusinessRuleException.class,
                () -> services.staff().updateLibrarian(TestData.admin(), id, first, new char[0], "CICT", "Librarian"));
    }

    @Test
    void unlockOnlyWorksOnLockedAccounts() {
        int id = services.staff().addLibrarian(TestData.admin(), "Un", null, "Locked", "Librarian", "CICT", TestData.username(), "Passw0rd-ok".toCharArray());
        BusinessRuleException refused = assertThrows(BusinessRuleException.class, () -> services.staff().unlock(TestData.admin(), id));
        assertTrue(refused.getMessage().contains("not currently locked"));

        List<Staff> locked = services.staff().librarians().stream().filter(Staff::isLocked).toList();
        assertFalse(locked.isEmpty(), "the demo data has locked librarians");
        int lockedId = locked.get(0).getId();
        services.staff().unlock(TestData.admin(), lockedId);
        assertFalse(find(lockedId).isLocked());
    }

    @Test
    void removeDeletesALibrarianWithoutHistory() {
        int id = services.staff().addLibrarian(TestData.admin(), "Gone", null, "Tester", "Librarian", "CICT", TestData.username(), "Passw0rd-ok".toCharArray());
        services.staff().remove(TestData.admin(), id);
        assertTrue(services.staff().librarians().stream().noneMatch(s -> s.getId() == id));
        assertThrows(BusinessRuleException.class, () -> services.staff().remove(TestData.admin(), id), "removing twice is refused politely");
    }

    @Test
    void removeRefusesALibrarianWhoHasIssuedLoans() {
        Staff librarian = services.staff().librarians().stream()
                .filter(s -> services.circulation().loans(LoanView.RETURNED).stream().anyMatch(l -> l.issuedById() == s.getId()))
                .findFirst().orElseThrow();
        BusinessRuleException refused = assertThrows(BusinessRuleException.class, () -> services.staff().remove(TestData.admin(), librarian.getId()));
        assertTrue(refused.getMessage().contains("cannot be removed"));
        assertEquals(librarian.getId(), find(librarian.getId()).getId(), "the account is still there");
    }

    @Test
    void administratorAccountCannotBeManagedThroughTheLibrarianScreens() {
        int adminId = db().query(c -> new edu.bulsu.lms.persistence.StaffRepository().findByUsername(c, "admin")).orElseThrow().getId();
        assertThrows(BusinessRuleException.class, () -> services.staff().remove(TestData.admin(), adminId));
        assertThrows(BusinessRuleException.class, () -> services.staff().unlock(TestData.admin(), adminId));
    }

    private static Database db() {
        return TestDatabase.require();
    }
}
