package edu.bulsu.lms.service;

import edu.bulsu.lms.domain.Course;
import edu.bulsu.lms.domain.Department;
import edu.bulsu.lms.persistence.Database;
import edu.bulsu.lms.persistence.ReferenceRepository;
import java.util.List;

/**
 * Colleges and courses for the combo boxes. They change roughly once a decade, so they are
 * loaded once and cached for the life of the application.
 */
public final class ReferenceService {

    private final Database database;
    private final ReferenceRepository referenceRepository;

    private volatile List<Department> departments;
    private volatile List<Course> courses;

    public ReferenceService(Database database, ReferenceRepository referenceRepository) {
        this.database = database;
        this.referenceRepository = referenceRepository;
    }

    public List<Department> departments() {
        List<Department> cached = departments;
        if (cached == null) {
            cached = List.copyOf(database.query(referenceRepository::departments));
            departments = cached;
        }
        return cached;
    }

    /** The courses a college offers; empty if it offers none. */
    public List<Course> coursesOf(String departmentCode) {
        List<Course> cached = courses;
        if (cached == null) {
            cached = List.copyOf(database.query(referenceRepository::courses));
            courses = cached;
        }
        return cached.stream().filter(course -> course.departmentCode().equals(departmentCode)).toList();
    }
}
