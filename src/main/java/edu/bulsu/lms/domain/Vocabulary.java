package edu.bulsu.lms.domain;

import java.util.List;

/** The fixed choices offered when cataloguing: genres and Library of Congress classes. */
public final class Vocabulary {

    public static final List<String> GENRES = List.of(
            "Adventure", "African-American Studies", "Anthropology", "Art & Photography", "Biography",
            "Biography and History Books", "Business", "Computer Science", "Crime", "Drama", "Fantasy", "Fiction",
            "Fiction and Literature", "Geography", "Guide", "Harvard Classics", "Health", "History",
            "Industrial Engineering", "Instructional", "Law", "Literature", "Mystery/Detective", "Nature",
            "Philosophy", "Pirate Tales", "Poetry", "Politics", "Psychology", "Religion", "Romance", "Science",
            "Science Fiction", "Short Story Collection", "Textbook");

    public static final List<String> LC_CLASSES = List.of(
            "Class A – General Works",
            "Class B – Philosophy. Psychology. Religion",
            "Class C – Auxiliary Sciences of History",
            "Class D – World History and History of Europe, Asia, Africa, Australia, New Zealand, etc.",
            "Class E – History of America",
            "Class F – Local History of the Americas",
            "Class G – Geography, Anthropology, Recreation",
            "Class H – Social Sciences",
            "Class J – Political Science",
            "Class K – Law",
            "Class L – Education",
            "Class M – Music",
            "Class N – Fine Arts",
            "Class P – Language and Literature",
            "Class Q – Science",
            "Class R – Medicine",
            "Class S – Agriculture",
            "Class T – Technology",
            "Class U – Military Science",
            "Class V – Naval Science",
            "Class Z – Bibliography, Library Science");

    private Vocabulary() {
        // constants only
    }
}
