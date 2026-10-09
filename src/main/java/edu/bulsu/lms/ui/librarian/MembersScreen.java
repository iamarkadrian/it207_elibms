package edu.bulsu.lms.ui.librarian;

import edu.bulsu.lms.domain.Address;
import edu.bulsu.lms.domain.Course;
import edu.bulsu.lms.domain.Department;
import edu.bulsu.lms.domain.Gender;
import edu.bulsu.lms.domain.Person;
import edu.bulsu.lms.domain.Student;
import edu.bulsu.lms.domain.ValidationException;
import edu.bulsu.lms.service.ReferenceService;
import edu.bulsu.lms.ui.kit.Field;
import edu.bulsu.lms.ui.kit.FormPanel;
import edu.bulsu.lms.ui.kit.ObjectTable;
import edu.bulsu.lms.ui.kit.Palette;
import edu.bulsu.lms.ui.kit.StatCard;
import edu.bulsu.lms.ui.kit.Tasks;
import edu.bulsu.lms.ui.kit.UiContext;
import edu.bulsu.lms.ui.kit.Values;
import edu.bulsu.lms.ui.kit.Wizard;
import java.util.List;

/** Library members: browse, add through a five-step wizard, and remove. */
final class MembersScreen extends MasterDetailScreen<Student> {

    MembersScreen(UiContext ctx) {
        super(ctx, "members", new StatCard("Total\nMembers", StatCard.Tone.NEUTRAL),
                new ObjectTable<Student>("members.table", List.of("Student Number", "Name", "Course", "Section"),
                        student -> List.of(student.getStudentNumber(), student.fullName(), student.getCourseCode(), student.getSection()),
                        new int[]{120, 220, 100, 80}, Palette.LIGHT),
                "Click the \"+\" button to add a new member or click a student record on the left table.",
                "Add member", "REMOVE", "DONE");
        addCard(WIZARD, wizard());
        detail().primary().addActionListener(event -> remove());
    }

    @Override
    protected List<Student> load() {
        return ctx.services().members().members();
    }

    private void remove() {
        table().selected().ifPresent(student -> {
            if (!ctx.messenger().confirm(this, "Remove " + student.fullName() + " from the library members?", "Question")) {
                return;
            }
            Tasks.run(ctx, this, () -> {
                ctx.services().members().remove(student.getStudentNumber());
                return null;
            }, done -> {
                ctx.messenger().info(this, "Member removed successfully");
                onShow();
            }, this::reload);
        });
    }

    // ---- the "Add Member" wizard ----

    private Wizard wizard() {
        ReferenceService reference = ctx.services().reference();

        Field.Choice<Department> college = Field.<Department>choice("college", "College");
        Field.Choice<Course> course = Field.<Course>choice("course", "Course");
        // Choosing a college narrows the course list to that college's programs.
        college.onChange(() -> {
            try {
                course.items(reference.coursesOf(college.value().code()));
            } catch (ValidationException noCollegeYet) {
                course.items(List.of());
            }
        });
        Tasks.run(ctx, this, () -> reference.departments().stream()
                        .filter(department -> !reference.coursesOf(department.code()).isEmpty())
                        .toList(),
                college::items);

        return new Wizard(ctx, Palette.LIGHT, this::finish)
                .step(new FormPanel(Palette.LIGHT, "Add Member")
                                .row(Field.text("studentNumber", "Student Number", Student::requireStudentNumber))
                                .row(new double[]{4, 1},
                                        Field.text("firstName", "First Name", text -> Person.requireName(text, "First name")),
                                        Field.text("middleInitial", "MI", Person::optionalInitial))
                                .row(Field.text("lastName", "Last Name", text -> Person.requireName(text, "Last name"))),
                        "CHECK", values -> ctx.services().members().ensureNumberIsFree(values.text("studentNumber")))
                .step(new FormPanel(Palette.LIGHT, "Add Member")
                        .row(college)
                        .row(course)
                        .row(Field.text("section", "Section", Student::requireSection)), "NEXT", null)
                .step(new FormPanel(Palette.LIGHT, "Add Member")
                        .row(Field.<Gender>choice("gender", "Gender").items(List.of(Gender.values())))
                        .row(Field.text("contact", "Contact Number", Student::requireContactNumber)), "NEXT", null)
                .step(new FormPanel(Palette.LIGHT, "Add Member")
                        .row(new double[]{1, 2},
                                Field.text("houseNo", "House Number", Address::requireHouseNo),
                                Field.text("street", "Street", Address::requireStreet))
                        .row(Field.text("barangay", "Barangay", Address::requireBarangay))
                        .row(Field.text("municipality", "City or Town", Address::requireMunicipality))
                        .row(Field.text("province", "Province", Address::requireProvince)), "NEXT", null)
                .review(MembersScreen::student, values -> {
                    ctx.services().members().register(student(values));
                    return "Member added successfully";
                });
    }

    /** Builds the finished member from the wizard answers; the domain constructor validates the whole record. */
    private static Student student(Values v) {
        return new Student(v.text("studentNumber"), v.text("firstName"), v.text("middleInitial"), v.text("lastName"),
                v.get("college", Department.class).code(), v.get("course", Course.class).code(), v.text("section"),
                v.get("gender", Gender.class), v.text("contact"),
                new Address(v.text("houseNo"), v.text("street"), v.text("barangay"), v.text("municipality"), v.text("province")));
    }
}
