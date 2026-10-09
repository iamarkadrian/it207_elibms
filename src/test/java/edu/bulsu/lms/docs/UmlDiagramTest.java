package edu.bulsu.lms.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Keeps the published UML honest: it must be exactly what the compiled classes say. */
class UmlDiagramTest {

    @Test
    @DisplayName("docs/uml/class-diagram.md matches the delivered code exactly")
    void committedDiagramMatchesTheCode() throws IOException {
        assertTrue(Files.exists(UmlDiagramGenerator.OUTPUT), "Missing " + UmlDiagramGenerator.OUTPUT
                + ". Generate it with UmlDiagramGenerator (see its Javadoc).");
        String committed = Files.readString(UmlDiagramGenerator.OUTPUT, StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertEquals(UmlDiagramGenerator.generate(), committed,
                "The UML is out of date. Regenerate it: run UmlDiagramGenerator.main (see its Javadoc).");
    }

    @Test
    void theDiagramShowsTheHierarchiesTheCourseAsksFor() {
        String uml = UmlDiagramGenerator.generate();
        assertTrue(uml.contains("LibraryItem <|-- PrintedBook"));
        assertTrue(uml.contains("LibraryItem <|-- DigitalBook"));
        assertTrue(uml.contains("LibraryItem <|-- OtherMaterial"));
        assertTrue(uml.contains("Person <|-- Student"));
        assertTrue(uml.contains("Person <|-- Staff"));
        assertTrue(uml.contains("Displayable <|.. LibraryItem"));
        assertTrue(uml.contains("Displayable <|.. Loan"));
    }
}
