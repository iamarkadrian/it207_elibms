package edu.bulsu.lms.service;

import edu.bulsu.lms.domain.BusinessRuleException;
import edu.bulsu.lms.domain.Student;
import edu.bulsu.lms.persistence.DataAccessException;
import edu.bulsu.lms.persistence.Database;
import edu.bulsu.lms.persistence.LoanRepository;
import edu.bulsu.lms.persistence.StudentRepository;
import java.util.List;

/** Library members (students): list, register and remove. */
public final class MemberService {

    private static final String ALREADY_REGISTERED = "The Student ID has already registered.";

    private final Database database;
    private final StudentRepository studentRepository;
    private final LoanRepository loanRepository;

    public MemberService(Database database, StudentRepository studentRepository, LoanRepository loanRepository) {
        this.database = database;
        this.studentRepository = studentRepository;
        this.loanRepository = loanRepository;
    }

    public List<Student> members() {
        return database.query(studentRepository::findAll);
    }

    public int count() {
        return database.query(studentRepository::count);
    }

    /** First wizard step: fail early if the student number is already taken. */
    public void ensureNumberIsFree(String studentNumber) {
        String number = Student.requireStudentNumber(studentNumber);
        if (database.query(connection -> studentRepository.exists(connection, number))) {
            throw new BusinessRuleException(ALREADY_REGISTERED);
        }
    }

    public void register(Student student) {
        try {
            database.transaction(connection -> {
                if (studentRepository.exists(connection, student.getStudentNumber())) {
                    throw new BusinessRuleException(ALREADY_REGISTERED);
                }
                studentRepository.insert(connection, student);
                return null;
            });
        } catch (DataAccessException e) {
            // Two librarians registering the same number at once: the unique key decides.
            throw e.isDuplicateKey() ? new BusinessRuleException(ALREADY_REGISTERED) : e;
        }
    }

    /** Removes a member who has never borrowed anything. Members with history are kept for the records. */
    public void remove(String studentNumber) {
        String number = Student.requireStudentNumber(studentNumber);
        database.transaction(connection -> {
            if (!studentRepository.exists(connection, number)) {
                throw new BusinessRuleException("That member no longer exists.");
            }
            if (loanRepository.countForStudent(connection, number) > 0) {
                throw new BusinessRuleException("This member has borrowing records, so they cannot be removed.");
            }
            studentRepository.delete(connection, number);
            return null;
        });
    }
}
