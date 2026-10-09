package edu.bulsu.lms.docs;

import edu.bulsu.lms.domain.Address;
import edu.bulsu.lms.domain.Course;
import edu.bulsu.lms.domain.Department;
import edu.bulsu.lms.domain.DigitalBook;
import edu.bulsu.lms.domain.Displayable;
import edu.bulsu.lms.domain.Gender;
import edu.bulsu.lms.domain.ItemType;
import edu.bulsu.lms.domain.LibraryItem;
import edu.bulsu.lms.domain.Loan;
import edu.bulsu.lms.domain.MaterialType;
import edu.bulsu.lms.domain.OtherMaterial;
import edu.bulsu.lms.domain.Person;
import edu.bulsu.lms.domain.PrintedBook;
import edu.bulsu.lms.domain.Role;
import edu.bulsu.lms.domain.Staff;
import edu.bulsu.lms.domain.Student;
import edu.bulsu.lms.persistence.Database;
import edu.bulsu.lms.persistence.ItemRepository;
import edu.bulsu.lms.persistence.LoanRepository;
import edu.bulsu.lms.persistence.StaffRepository;
import edu.bulsu.lms.persistence.StudentRepository;
import edu.bulsu.lms.security.PasswordHasher;
import edu.bulsu.lms.service.AuthService;
import edu.bulsu.lms.service.CatalogService;
import edu.bulsu.lms.service.CirculationService;
import edu.bulsu.lms.service.DashboardService;
import edu.bulsu.lms.service.MemberService;
import edu.bulsu.lms.service.ReferenceService;
import edu.bulsu.lms.service.StaffService;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Draws the UML class diagrams from the compiled classes themselves, so the diagram cannot disagree
 * with the code. {@code UmlDiagramTest} fails when the committed file is stale.
 *
 * <p>To regenerate after changing a class:
 * <pre>
 *   ./mvnw -q test-compile
 *   java -cp "target/classes:target/test-classes" edu.bulsu.lms.docs.UmlDiagramGenerator
 * </pre>
 * (use {@code ;} instead of {@code :} on Windows). Add a class to the lists below to include it.
 */
public final class UmlDiagramGenerator {

    static final Path OUTPUT = Path.of("docs", "uml", "class-diagram.md");

    private static final List<Class<?>> DOMAIN = List.of(
            Displayable.class, Person.class, Student.class, Staff.class, Role.class, Gender.class, Address.class,
            LibraryItem.class, PrintedBook.class, DigitalBook.class, OtherMaterial.class, ItemType.class,
            MaterialType.class, Loan.class, Department.class, Course.class);

    private static final List<Class<?>> APPLICATION = List.of(
            AuthService.class, StaffService.class, MemberService.class, CatalogService.class,
            CirculationService.class, DashboardService.class, ReferenceService.class,
            StaffRepository.class, StudentRepository.class, ItemRepository.class, LoanRepository.class,
            Database.class, PasswordHasher.class);

    private UmlDiagramGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Files.createDirectories(OUTPUT.getParent());
        Files.writeString(OUTPUT, generate(), StandardCharsets.UTF_8);
        System.out.println("Wrote " + OUTPUT.toAbsolutePath());
    }

    /** The complete Markdown document, with a trailing newline and Unix line endings. */
    public static String generate() {
        return """
                # UML class diagrams

                > **Generated from the compiled classes** by `UmlDiagramGenerator` (test sources). Do not edit by hand:
                > `UmlDiagramTest` fails when this file no longer matches the code. See the header of the generator for the
                > regeneration command.

                ## Domain model

                Inheritance (`Person` and `LibraryItem` are abstract and sealed), the `Displayable` interface shared by
                unrelated classes, and the value types they use.

                ```mermaid
                %s```

                ## Services, repositories and infrastructure

                ```mermaid
                %s```
                """.formatted(diagram(DOMAIN), diagram(APPLICATION));
    }

    private static String diagram(List<Class<?>> classes) {
        StringBuilder out = new StringBuilder("classDiagram\n    direction TB\n");
        for (Class<?> type : classes) {
            declare(out, type);
        }
        Set<String> relations = new LinkedHashSet<>();
        for (Class<?> type : classes) {
            if (type.getSuperclass() != null && classes.contains(type.getSuperclass())) {
                relations.add(type.getSuperclass().getSimpleName() + " <|-- " + type.getSimpleName());
            }
            for (Class<?> parent : type.getInterfaces()) {
                if (classes.contains(parent)) {
                    relations.add(parent.getSimpleName() + " <|.. " + type.getSimpleName());
                }
            }
            for (Field field : sortedFields(type)) {
                for (Class<?> target : classes) {
                    if (target != type && mentions(field.getGenericType().getTypeName(), target)
                            && !inherits(type, target)) {
                        relations.add(type.getSimpleName() + " --> " + target.getSimpleName() + " : " + field.getName());
                    }
                }
            }
        }
        relations.forEach(line -> out.append("    ").append(line).append('\n'));
        return out.toString();
    }

    private static void declare(StringBuilder out, Class<?> type) {
        out.append("    class ").append(type.getSimpleName()).append(" {\n");
        String stereotype = stereotype(type);
        if (stereotype != null) {
            out.append("        <<").append(stereotype).append(">>\n");
        }
        if (type.isEnum()) {
            for (Object constant : type.getEnumConstants()) {
                out.append("        ").append(constant).append('\n');
            }
            // an enum's constants say it all; show its methods only
        }
        for (Field field : sortedFields(type)) {
            out.append("        ").append(visibility(field.getModifiers())).append(simple(field.getGenericType().getTypeName()))
                    .append(' ').append(field.getName()).append(Modifier.isStatic(field.getModifiers()) ? "$" : "").append('\n');
        }
        for (Constructor<?> constructor : sortedConstructors(type)) {
            out.append("        ").append(visibility(constructor.getModifiers())).append(type.getSimpleName())
                    .append('(').append(parameters(constructor.getParameters())).append(")\n");
        }
        for (Method method : sortedMethods(type)) {
            out.append("        ").append(visibility(method.getModifiers())).append(method.getName())
                    .append('(').append(parameters(method.getParameters())).append(')')
                    .append(Modifier.isStatic(method.getModifiers()) ? "$" : "")
                    .append(Modifier.isAbstract(method.getModifiers()) && !type.isInterface() ? "*" : "")
                    .append(' ').append(simple(method.getGenericReturnType().getTypeName())).append('\n');
        }
        out.append("    }\n");
    }

    private static String stereotype(Class<?> type) {
        if (type.isInterface()) {
            return "interface";
        }
        if (type.isEnum()) {
            return "enumeration";
        }
        if (type.isRecord()) {
            return "record";
        }
        return Modifier.isAbstract(type.getModifiers()) ? "abstract" : null;
    }

    private static List<Field> sortedFields(Class<?> type) {
        if (type.isEnum()) {
            return List.of();
        }
        return Arrays.stream(type.getDeclaredFields())
                .filter(f -> !f.isSynthetic())
                .filter(f -> !Modifier.isStatic(f.getModifiers()) || Modifier.isPublic(f.getModifiers()))
                .sorted(Comparator.comparing(Field::getName))
                .toList();
    }

    private static List<Constructor<?>> sortedConstructors(Class<?> type) {
        if (type.isEnum() || type.isInterface()) {
            return List.of();
        }
        return Arrays.stream(type.getDeclaredConstructors())
                .filter(c -> !c.isSynthetic() && !Modifier.isPrivate(c.getModifiers()))
                .filter(c -> !type.isRecord())
                .sorted(Comparator.comparingInt((Constructor<?> c) -> c.getParameterCount()).reversed())
                .collect(Collectors.toList());
    }

    private static List<Method> sortedMethods(Class<?> type) {
        List<String> noise = type.isRecord() ? List.of("equals", "hashCode", "toString") : List.of();
        return Arrays.stream(type.getDeclaredMethods())
                .filter(m -> !m.isSynthetic() && !m.isBridge())
                .filter(m -> !Modifier.isPrivate(m.getModifiers()))
                .filter(m -> !type.isEnum() || !(m.getName().equals("values") || m.getName().equals("valueOf")))
                .filter(m -> !noise.contains(m.getName()))
                .sorted(Comparator.comparing(Method::getName).thenComparingInt(Method::getParameterCount))
                .toList();
    }

    private static String parameters(Parameter[] parameters) {
        List<String> parts = new ArrayList<>();
        for (Parameter parameter : parameters) {
            parts.add(simple(parameter.getParameterizedType().getTypeName()) + " " + parameter.getName());
        }
        return String.join(", ", parts);
    }

    private static String visibility(int modifiers) {
        if (Modifier.isPublic(modifiers)) {
            return "+";
        }
        if (Modifier.isProtected(modifiers)) {
            return "#";
        }
        return Modifier.isPrivate(modifiers) ? "-" : "~";
    }

    /** "java.util.List&lt;edu.bulsu.Foo&gt;" becomes Mermaid's "List~Foo~". */
    private static String simple(String typeName) {
        return typeName.replaceAll("\\b[a-z][a-z0-9_]*\\.", "")
                .replaceAll("\\b[A-Za-z0-9_]+\\$", "")
                .replace("? extends ", "")
                .replace('<', '~')
                .replace('>', '~');
    }

    private static boolean mentions(String typeName, Class<?> target) {
        return typeName.matches(".*\\b" + target.getName().replace(".", "\\.").replace("$", "\\$") + "\\b.*");
    }

    private static boolean inherits(Class<?> type, Class<?> target) {
        return target.isAssignableFrom(type);
    }
}
