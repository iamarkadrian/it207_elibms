package edu.bulsu.lms.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

/** A library member. Every field is private and every setter validates before it stores. */
public final class Student extends Person {

    private static final Pattern NUMBER = Pattern.compile("\\d{10}");
    private static final Pattern CONTACT = Pattern.compile("09\\d{9}");
    private static final Pattern SECTION = Pattern.compile("[A-Za-z0-9]{1,5}");

    private String studentNumber;
    private String departmentCode;
    private String courseCode;
    private String section;
    private Gender gender;
    private String contactNumber;
    private Address address;
    private LocalDate membershipDate;

    /** Full constructor, used when loading an existing member from the database. */
    public Student(String studentNumber, String firstName, String middleInitial, String lastName,
                   String departmentCode, String courseCode, String section, Gender gender,
                   String contactNumber, Address address, LocalDate membershipDate) {
        super(firstName, middleInitial, lastName);
        setStudentNumber(studentNumber);
        setDepartmentCode(departmentCode);
        setCourseCode(courseCode);
        setSection(section);
        setGender(gender);
        setContactNumber(contactNumber);
        setAddress(address);
        setMembershipDate(membershipDate);
    }

    /** Overloaded constructor for a brand-new member: the membership starts today. */
    public Student(String studentNumber, String firstName, String middleInitial, String lastName,
                   String departmentCode, String courseCode, String section, Gender gender,
                   String contactNumber, Address address) {
        this(studentNumber, firstName, middleInitial, lastName, departmentCode, courseCode, section,
                gender, contactNumber, address, LocalDate.now());
    }

    /** Exactly ten digits, e.g. {@code 2019125410}. */
    public static String requireStudentNumber(String raw) {
        return Check.matching(raw, "Student number", 10, NUMBER, "must be exactly 10 digits.");
    }

    /** Required contact number: 11 digits starting with 09. */
    public static String requireContactNumber(String raw) {
        return Check.matching(raw, "Contact number", 11, CONTACT, "must be 11 digits and start with 09.");
    }

    public static String requireSection(String raw) {
        return Check.matching(raw, "Section", 5, SECTION, "may only contain letters and digits (max 5).");
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = requireStudentNumber(studentNumber);
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = Check.text(departmentCode, "College", 8);
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = Check.text(courseCode, "Course", 16);
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = requireSection(section);
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        if (gender == null) {
            throw new ValidationException("Gender must be selected.");
        }
        this.gender = gender;
    }

    /** The contact number, or {@code null} when none was recorded. */
    public String getContactNumber() {
        return contactNumber;
    }

    /** A blank value clears the contact number; otherwise it must be a valid mobile number. */
    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber == null || contactNumber.isBlank() ? null : requireContactNumber(contactNumber);
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        if (address == null) {
            throw new ValidationException("Address is required.");
        }
        this.address = address;
    }

    public LocalDate getMembershipDate() {
        return membershipDate;
    }

    public void setMembershipDate(LocalDate membershipDate) {
        if (membershipDate == null || membershipDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Membership date cannot be empty or in the future.");
        }
        this.membershipDate = membershipDate;
    }

    @Override
    public String roleLabel() {
        return "Student";
    }

    @Override
    public String identifier() {
        return studentNumber;
    }

    /** "E. Santos - Male": middle initial (if any), last name and gender. */
    @Override
    public String subtitle() {
        String initial = getMiddleInitial() == null ? "" : getMiddleInitial() + " ";
        return initial + getLastName() + " - " + gender.label();
    }

    @Override
    public List<String> lines() {
        return List.of(
                studentNumber,
                courseCode + " " + section,
                departmentCode,
                "",
                "Contact Details:",
                contactNumber == null ? "N/A" : contactNumber,
                address.streetLine(),
                address.areaLine());
    }
}
