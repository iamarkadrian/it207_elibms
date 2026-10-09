package edu.bulsu.lms.service;

/** The numbers on the librarian dashboard's "Today's Summary" card. */
public record LibrarySummary(int books, int digitalBooks, int otherDetails, int members,
                             int issued, int returned, int missing) {
}
