package edu.bulsu.lms.service;

import edu.bulsu.lms.persistence.Database;
import edu.bulsu.lms.persistence.ItemRepository;
import edu.bulsu.lms.persistence.LoanRepository;
import edu.bulsu.lms.persistence.ReferenceRepository;
import edu.bulsu.lms.persistence.StaffRepository;
import edu.bulsu.lms.persistence.StudentRepository;
import edu.bulsu.lms.security.PasswordHasher;
import java.time.Clock;

/**
 * Every service the UI needs, wired together once at start-up (manual dependency injection:
 * plain constructors, no framework).
 */
public record Services(AuthService auth, StaffService staff, MemberService members, CatalogService catalog,
                       CirculationService circulation, DashboardService dashboard, ReferenceService reference) {

    public static Services create(Database database, Clock clock) {
        return create(database, clock, new PasswordHasher());
    }

    /** Tests pass a cheaper hasher so they run fast. */
    public static Services create(Database database, Clock clock, PasswordHasher hasher) {
        StaffRepository staffRepository = new StaffRepository();
        StudentRepository studentRepository = new StudentRepository();
        ItemRepository itemRepository = new ItemRepository();
        LoanRepository loanRepository = new LoanRepository();
        ReferenceRepository referenceRepository = new ReferenceRepository();

        return new Services(
                new AuthService(database, staffRepository, hasher, clock),
                new StaffService(database, staffRepository, loanRepository, hasher),
                new MemberService(database, studentRepository, loanRepository),
                new CatalogService(database, itemRepository),
                new CirculationService(database, itemRepository, studentRepository, loanRepository, staffRepository, clock),
                new DashboardService(database, itemRepository, studentRepository, loanRepository, clock),
                new ReferenceService(database, referenceRepository));
    }
}
